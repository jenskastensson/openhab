package org.openhab.binding.proserv.internal;

import java.io.File;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;


import org.openhab.config.core.ConfigDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProservCronJobs implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(ProservData.class);

	private static final long serialVersionUID = -2701461983518957371L;
	public void add(CronJob cronJob) {
		cronJobs.put(cronJob.dataPointID, cronJob);
	}

	private static String serializeFile;

	public ProservCronJobs() {
		serializeFile = ConfigDispatcher.getConfigFolder() + File.separator + "ProservCronJobs.ser";
	}

	public class CronJob implements Serializable {
		private static final long serialVersionUID = -4226132487848107182L;
		public String dataPointID;
		public boolean isActive;
		public int scheduleType;
		public String cron1;
		public String cron2;

		public CronJob(String dataPointID, boolean isActive, int scheduleType, String cron1, String cron2) {
			this.dataPointID = dataPointID;
			this.isActive = isActive;
			this.scheduleType = scheduleType;
			if(cron1!=null) this.cron1 = cron1; else this.cron1 = "0 0 8 ? * 2-6";
			if(cron2!=null) this.cron2 = cron2; else this.cron2 = "0 0 21 ? * 1,7";
		}
	}

	public Map<String, CronJob> cronJobs = new HashMap<String, CronJob>();


	public void saveJobs() {
		try {

			FileOutputStream fileOut = new FileOutputStream(serializeFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(cronJobs);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();// TODO
		}
	}

	public void mergeOldJobs() {
		try {
			FileInputStream fileIn = new FileInputStream(serializeFile);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Map<String, CronJob> oldCronJobs = new HashMap<String, CronJob>();
			oldCronJobs = (HashMap<String, CronJob>) in.readObject();
			in.close();
			fileIn.close();

			// loop thru old jobs, if an old job is defined and still exist in proServ, use the old
			for (Map.Entry<String, CronJob> entry : oldCronJobs.entrySet()) {
				String key = entry.getKey();
				if (cronJobs.containsKey(key)) {
					cronJobs.put(key, entry.getValue());
				}
			}

		} catch (IOException i) {
			i.printStackTrace(); // TODO
			return;
		} catch (ClassNotFoundException c) {
			System.out.println("cronJobs class not found");// TODO
			c.printStackTrace();// TODO
			return;
		}
	}

	public boolean add(String jobsOption) {
		// DPxx:true:0:0 0 8 ? * 2-6:0 0 21 ? * 1,7;DPyy:true:1:0 0 8 ? * 2-6:0 0 21 ? * 1,7;
		Map<String, CronJob> tmpJobs = new HashMap<String, CronJob>();
		try {
			String[] jobs = jobsOption.split(";");
			for (String s : jobs) {
				String[] j = s.split(":");
				boolean active = j[1].equals("true") ? true : false;
				CronJob cronJob = new CronJob(j[0], active, Integer.parseInt(j[2]), j[3], j[4]);
				tmpJobs.put(cronJob.dataPointID, cronJob);
			}
		} catch (Throwable e) {
			String message = "parse jobsOption: " + jobsOption + "   throws exception" + e.toString();
			logger.error(message, e);
			return false;
		}
		cronJobs.putAll(tmpJobs);
		saveJobs();
		return true;
	}
	
}
