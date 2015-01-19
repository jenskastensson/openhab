package org.openhab.binding.proserv.internal;

import java.io.File;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


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
		public String zoneName;
		public String dataPointName;
		public boolean isActive1;
		public boolean isActive2;
		public int scheduleType;
		public String cron1;
		public String cron2;

		public CronJob(String dataPointID, int scheduleType, String zoneName, String dataPointName, boolean isActive1, String cron1, boolean isActive2, String cron2) {
			this.dataPointID = dataPointID;
			this.zoneName = zoneName;
			this.dataPointName = dataPointName;
			this.isActive1 = isActive1;
			this.isActive2 = isActive2;
			this.scheduleType = scheduleType;
			if(cron1!=null) this.cron1 = cron1; else this.cron1 = "0 0 8 ? * 2-6";
			if(cron2!=null) this.cron2 = cron2; else this.cron2 = "0 0 21 ? * 2-6";
		}
	}
	class JobsComparator implements Comparator<String> {

	    Map<String, CronJob> base;
	    public JobsComparator(Map<String, CronJob> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        int z = base.get(a).zoneName.compareTo(base.get(b).zoneName);
		    if (z == 0) {
		    	// same zone
		    	int dp = base.get(a).dataPointName.compareTo(base.get(b).dataPointName);
				if(dp < 0) 
					return -1;
		    	else if (dp > 0) 
		    		return 1;
		    	else 
		    		return 0;
	        } 
		    else if (z < 0) 
	        	return -1;
	        else //if (z > 0) 
	        	return 1;
	    }
	}
	
	public Map<String, CronJob> cronJobs = new HashMap<String, CronJob>();
	transient JobsComparator bvc =  new JobsComparator(cronJobs);
	transient TreeMap<String,CronJob> Sorted = new TreeMap<String,CronJob>(bvc);	
 	

	public TreeMap<String,CronJob> getSorted(){
		Sorted.putAll(cronJobs);
		return Sorted; 
	}

	public void saveJobs() {
		try {

			FileOutputStream fileOut = new FileOutputStream(serializeFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(cronJobs);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			String message = "saveJobs: throws exception" + e.toString();
			logger.error(message, e);
		}
	}

	@SuppressWarnings("unchecked")
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
					// use names & scheduletype from proserv if they were changed, other data from file
					CronJob j = entry.getValue();
					j.dataPointName = cronJobs.get(key).dataPointName;
					j.zoneName = cronJobs.get(key).zoneName;
					j.scheduleType = cronJobs.get(key).scheduleType;
					cronJobs.put(key, j);
				}
			}

		} catch (IOException e) {
			String message = "mergeOldJobs: throws IOException" + e.toString();
			logger.error(message, e);
			return;
		} catch (ClassNotFoundException e) {
			String message = "mergeOldJobs: throws ClassNotFoundException" + e.toString();
			logger.error(message, e);
			return;
		}
	}
	
	public boolean add(String jobsOption) {
		// dpIDxx:0:true:0 0 8 ? * 2-6:true:0 0 21 ? * 1,7;dpIDyy:true:1:0 0 8 ? * 2-6:false:0 0 21 ? * 1,7;
		Map<String, CronJob> tmpJobs = new HashMap<String, CronJob>();
		try {
			String[] jobs = jobsOption.split(";");
			for (String s : jobs) {
				String[] j = s.split(":");
				boolean active1 = j[2].equals("true") ? true : false;
				boolean active2 = j[4].equals("true") ? true : false;
				int scheduleType = Integer.parseInt(j[1]);
				j[3] = j[3].replace("0 * * * * *", "0 * * * * ?");
				String cron2 = "";
				if(scheduleType!=3){
					cron2 = j[5].replace("0 * * * * *", "0 * * * * ?");
				}
				if (cronJobs.containsKey(j[0])) {
					String dataPointName = cronJobs.get(j[0]).dataPointName;
					String zoneName = cronJobs.get(j[0]).zoneName;
					CronJob cronJob = new CronJob(j[0], scheduleType, zoneName, dataPointName, active1, j[3], active2, cron2);
					tmpJobs.put(cronJob.dataPointID, cronJob);
				}
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
