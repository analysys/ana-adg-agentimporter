package cn.com.analysys.agentimpoter.sender;

import java.util.Random;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

/**
 * 自定义分区
 */
public class AnaKafkaPartation implements Partitioner{
	private Random random = new Random();

	public AnaKafkaPartation(VerifiableProperties props) { }

	@Override
	public int partition(Object key, int numPartitions) {
		int partation = 0;
		try {
			if(key == null){
				partation = random.nextInt(numPartitions);
			} else {
				byte[] keyByte = (byte[])key;
				String keyVal = new String(keyByte);
				if(keyVal != null && keyVal.trim().length() == 0){
					partation = random.nextInt(numPartitions);
				} else {
					int hash = keyVal.hashCode();
					partation = Math.abs(hash) % numPartitions;
				}
			}
		} catch (Exception e) {
			partation = random.nextInt(numPartitions);
		}
		return partation;
	}
}
