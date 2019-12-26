package cn.com.analysys.agentimpoter.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

import cn.com.analysys.agentimpoter.util.ConstantTool;
import cn.com.analysys.agentimpoter.util.LoggerUtil;
import cn.com.analysys.agentimpoter.util.ThreadPoolUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Tailer implements Runnable {
	private static final String RAF_MODE = "r";
	private static final int DEFAULT_BUFSIZE = 4096;
	private static final int DEFAULT_BYTEBUFFER_BUFSIZE = 1024;
	private static final long REOPEN_WAIT_TIME = 180000;
	private Logger positionLog;
	private final byte inbuf[];
	private final File file;
	private final String appid;
	private final long delayMillis;
	private final boolean end;
	private final TailerListener listener;
	private boolean reOpen;
	private boolean run = true;
	
	public Tailer(File file, TailerListener listener, long delayMillis, boolean end, String appid) {
		this(file, listener, delayMillis, end, DEFAULT_BUFSIZE, appid);
	}

	public Tailer(File file, TailerListener listener, long delayMillis, boolean end, int bufSize, String appid) {
		this(file, listener, delayMillis, end, false, bufSize, appid);
	}

	public Tailer(File file, TailerListener listener, long delayMillis, boolean end, boolean reOpen, int bufSize, String appid) {
		this.file = file;
		this.appid = appid;
		this.delayMillis = delayMillis;
		this.end = end;
		this.inbuf = new byte[bufSize];
		this.listener = listener;
		listener.init(this);
		this.reOpen = reOpen;
		positionLog = LoggerUtil.getPositionLog(this.file.getName(), this.appid);
	}

	public File getFile() {
		return file;
	}

	public long getDelay() {
		return delayMillis;
	}

	@Override
	public void run() {
		RandomAccessFile reader = null;
		try {
			long position = 0;
			while (isRun() && reader == null) {
				try {
					reader = new RandomAccessFile(file, RAF_MODE);
				} catch (FileNotFoundException e) {
					listener.fileNotFound(this.file.getAbsolutePath());
					next();
				}
				if (reader == null) {
					try {Thread.sleep(delayMillis*2);} catch (Exception e) {}
				} else {
					try {
						position = end ? LoggerUtil.getPosition(this.file.getName(), this.appid) : 0;
						if(position == ConstantTool.TAILER_FILE_INDEX_POSITION)
							position = reader.length();
						reader.seek(position);
					} catch (Exception e) {
						LoggerUtil.error(e.getMessage(), e);
					}
				}
			}

			LoggerUtil.info(String.format("===Begin monitor logfile: %s  position: %s", file.getAbsolutePath(), position));
			int index = 0;
			while (isRun()) {
				long length = file.length();
				if (length <= position) {
					next();
					LoggerUtil.info(String.format("%s: noroll, legth=%s, position=%s", file.getName(), length, position));
					try {Thread.sleep(delayMillis*2);} catch (Exception e) {}
				} else {
					try {
						position = readLines(reader);
					} catch (Exception e) {
						LoggerUtil.error(e.getMessage(), e);
					}
				}
				if(++index * delayMillis >= REOPEN_WAIT_TIME){
					reOpen = true;
					index = 0;
				}
				try {Thread.sleep(delayMillis);} catch (Exception e) {}
				if (isRun() && reOpen) {
					try {
						IOUtils.closeQuietly(reader);
						reOpen = false;
						reader = new RandomAccessFile(file, RAF_MODE);
						reader.seek(position);
						LoggerUtil.info(String.format("%s: reopen, legth=%s, position=%s", file.getName(), length, position));
					} catch (FileNotFoundException e) {
						listener.fileNotFound(this.file.getName());
					} catch (Exception e) {
						LoggerUtil.error(e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			LoggerUtil.error(e.getMessage(), e);
			listener.handle(e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	public void next() {
		String nextFilePath = LoggerUtil.getNextLogName(file.getName(), appid);
		if(nextFilePath != null && nextFilePath.trim().length() > 0){
			File nextFile = new File(nextFilePath);
			if(nextFile.exists()){ //new file
				Tailer tailer = new Tailer(nextFile, new EGTailerListener(), ConstantTool.TAILER_FILE_DELAY, false, this.appid);
				ThreadPoolUtil.executeTask(tailer);
				stop();
			}
		}
	}

	public void stop() {
		this.run = false;
		LoggerUtil.info(String.format("Delete index file %s", this.file.getName()));
		try {
			Enumeration<?> e = positionLog.getAllAppenders();
			while (e.hasMoreElements()){
				Appender app = (Appender)e.nextElement();
				if (app instanceof RollingFileAppender){
					app.close();
				}
			}
		} finally {
			positionLog.removeAllAppenders();
			positionLog = null;
		}
		LoggerUtil.delLogPositionIndexFile(this.file.getName(), this.appid);
	}
	
	private long readLines(RandomAccessFile reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		long pos = reader.getFilePointer();
		long rePos = pos;
		int num;
		boolean seenCR = false;
		boolean fileEnd = false;
		ByteBuf byteBuffer = Unpooled.buffer(DEFAULT_BYTEBUFFER_BUFSIZE);
		while (isRun() && !fileEnd && ((num = reader.read(inbuf)) != -1)) {
			if(reader.length() <= rePos){
				fileEnd = true;
				continue;
			}
			for (int i = 0; i < num; i++) {
				byte ch = inbuf[i];
				switch (ch) {
				case '\n':
					seenCR = false;
					sb.append(new String(byteBuffer.array()).trim());
					clearBuffer(byteBuffer);
					listener.handle(sb.toString());
					sb.setLength(0);
					rePos = pos + i + 1;
					positionLog.info(rePos);
					break;
				case '\r':
					if (seenCR) {
						sb.append('\r');
					}
					seenCR = true;
					break;
				default:
					if (seenCR) {
						seenCR = false;
						sb.append(new String(byteBuffer.array()).trim());
						clearBuffer(byteBuffer);
						listener.handle(sb.toString());
						sb.setLength(0);
						rePos = pos + i + 1;
						positionLog.info(rePos);
					}
					byteBuffer.writeByte(ch);
				}
			}
			pos = reader.getFilePointer();
		}
		reader.seek(rePos);
		clearBuffer(byteBuffer);
		byteBuffer = null;
		return rePos;
	}
	
	private boolean isRun(){
		return run && !ThreadPoolUtil.isStop();
	}
	
	private void clearBuffer(ByteBuf byteBuffer){
		byteBuffer.setZero(0, byteBuffer.capacity());
		byteBuffer.clear();
	}
}