package cn.com.analysys.agentimpoter.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolUtil {
	private static ThreadPoolExecutor threadPoolExecutor;
	private static AtomicLong counter = new AtomicLong();
	private static final AtomicBoolean poolStop = new AtomicBoolean(false);
	
	static {
		ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(24);
		threadPoolExecutor = new ThreadPoolExecutor(3, 5, 120L, TimeUnit.SECONDS, workQueue);
	}
	
	public static ThreadPoolExecutor getThreadPool() {
		return threadPoolExecutor;
	}
	
	public static void executeTask(Runnable task) {
		getThreadPool().execute(task);
	}
	
	public static void counterAdd() {
		counter.addAndGet(1);
	}
	
	public static long getCounter() {
		return counter.get();
	}

	public static String monitorThreadPool() {
		StringBuffer bufer = new StringBuffer();
		int queueSize = threadPoolExecutor.getQueue().size();
		bufer.append(String.format("Threads Status: Queued:[%s]", queueSize));

        int activeCount = threadPoolExecutor.getActiveCount();
        bufer.append(String.format("  Active:[%s]", activeCount));

        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        bufer.append(String.format("  Completion:[%s]", completedTaskCount));

        long taskCount = threadPoolExecutor.getTaskCount();
        bufer.append(String.format("  Total:[%s]", taskCount));
        
        long count = getCounter();
        bufer.append(String.format("      Lines Counter: Total:[%s]", count));
        return bufer.toString();
	}
	
	public static boolean isStop() {
		return poolStop.get();
	}
	
	public static void closeThreadPool() {
		threadPoolExecutor.shutdown();
		poolStop.set(true);
		boolean open = true;
		do{
			try {
				open = !threadPoolExecutor.awaitTermination(300, TimeUnit.SECONDS);
			} catch (Exception e) {
				LoggerUtil.error(e.getMessage(), e);
			}
		} while(open);
	}
}
