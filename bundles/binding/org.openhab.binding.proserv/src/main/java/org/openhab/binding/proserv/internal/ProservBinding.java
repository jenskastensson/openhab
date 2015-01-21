/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import org.openhab.config.core.ConfigDispatcher;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openhab.binding.proserv.ProservBindingProvider;
import org.openhab.binding.proserv.ProservBindingProvider;
import org.openhab.binding.proserv.internal.ProservCronJobs;
import org.openhab.binding.proserv.internal.ProservCronJobs.CronJob;
import org.openhab.core.binding.AbstractActiveBinding;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.action.mail.internal.Mail;




/**
 * The proServ binding connects to a proServ device with the
 * {@link ProservConnector} and read the internal state array every minute. With
 * the state array each binding will be updated.
 * 
 * @author JEKA
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class ProservBinding extends AbstractActiveBinding<ProservBindingProvider> implements ManagedService {

	
	private static final Logger logger = LoggerFactory.getLogger(ProservBinding.class);

	private static ProservConnector connector = null;
	ProservXConnect proservXConnect = new ProservXConnect();
	
	/** Default refresh interval (currently 2 minute) */
	private long refreshInterval = 125000L;

	/* The IP address to connect to */
	private static String ip;
	private static int port = 80;
	private static String mailTo = "";
	private static String mailSubject = "";
	private static String mailContent = "";
	private static Boolean previousEmailTrigger[][] = new Boolean[18][16];
	private static String language = null;
	private static String chartItemRefreshHour = null;
	private static String chartItemRefreshDay = null;
	private static String chartItemRefreshWeek = null;
	private static String chartItemRefreshMonth = null;
	private static String chartItemRefreshYear = null;

	
	private static ProservData proservData = null;
	private static ProservCronJobs proservCronJobs = new ProservCronJobs();

	public void deactivate() {
		connector.stopMonitor();
		if (connector != null) {
			connector.disconnect();
		}
		connector = null;
	}

	public void activate() {
		super.activate();
		setProperlyConfigured(true);
	}

	private boolean isSupportedLanguage(String language) {
		if (language.equals("en") || language.equals("de") || language.equals("fr")){
			return true;
		}
		if (!language.equals("xx")){
			logger.error("Unsupported language: {}", language);
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public void updated(Dictionary config) throws ConfigurationException {
		if (config != null) {
			String ip = (String) config.get("ip");
			String portString = (String) config.get("port");
			ProservBinding.mailTo = (String) config.get("mailto");
			
			ProservBinding.language = (String) config.get("language");
			if(ProservBinding.language != null)
			{
				if(!isSupportedLanguage(ProservBinding.language))
					ProservBinding.language = "en";
			}
			else
			{
				logger.error("Mising config proserv:language");
				ProservBinding.language = "en";
			}
			
			ProservBinding.chartItemRefreshHour = (String) config.get("chartItemRefreshHour");
			ProservBinding.chartItemRefreshDay = (String) config.get("chartItemRefreshDay");
			ProservBinding.chartItemRefreshWeek = (String) config.get("chartItemRefreshWeek");
			ProservBinding.chartItemRefreshMonth = (String) config.get("chartItemRefreshMonth");
			ProservBinding.chartItemRefreshYear = (String) config.get("chartItemRefreshYear");
			if(ProservBinding.chartItemRefreshHour != null)
				if(Integer.parseInt(ProservBinding.chartItemRefreshHour)<5000)
					ProservBinding.chartItemRefreshHour = "5000";
			
			if(ProservBinding.chartItemRefreshDay != null)
				if(Integer.parseInt(ProservBinding.chartItemRefreshDay)<5000)
					ProservBinding.chartItemRefreshDay = "5000";
			
			if(ProservBinding.chartItemRefreshWeek != null)
				if(Integer.parseInt(ProservBinding.chartItemRefreshWeek)<5000)
					ProservBinding.chartItemRefreshWeek = "5000";
			
			if(ProservBinding.chartItemRefreshMonth != null)
				if(Integer.parseInt(ProservBinding.chartItemRefreshMonth)<5000)
					ProservBinding.chartItemRefreshMonth = "5000";
			
			if(ProservBinding.chartItemRefreshYear != null)
				if(Integer.parseInt(ProservBinding.chartItemRefreshYear)<5000)
					ProservBinding.chartItemRefreshYear = "5000";
					
			int portTmp = 80;
			if (StringUtils.isNotBlank(portString)) {
				portTmp = (int) Long.parseLong(portString);
			}

			if ((StringUtils.isNotBlank(ip) && !ip.equals(ProservBinding.ip)) || portTmp != ProservBinding.port) {
				// only do something if the ip or port has changed
				ProservBinding.ip = ip;
				ProservBinding.port = portTmp;

				String refreshIntervalString = (String) config.get("refresh");
				if (StringUtils.isNotBlank(refreshIntervalString)) {
					refreshInterval = Long.parseLong(refreshIntervalString);
				}

				setProperlyConfigured(true);
			}
			
			// Load language strings
			Reader reader = null;
			String filename = ProservBinding.language + ".map";
			try {
				String path = ConfigDispatcher.getConfigFolder() + File.separator + "transform" + File.separator + filename ;
				Properties properties = new Properties();
				reader = new FileReader(path);
				properties.load(reader);
							
				ProservBinding.mailSubject = properties.getProperty("MAIL-SUBJECT");
				ProservBinding.mailContent = properties.getProperty("MAIL-CONTENT");

			} catch (Throwable e) {
				String message = "opening file '" + filename + "' throws exception";
				logger.error(message, e);
			} finally {
				IOUtils.closeQuietly(reader);
			}		
			
			if(proservData != null){
				logger.debug("proServ force a reload of configdata!");
				proservData.refresh = true; // force a reload of configdata
				//execute();
			}
		}
	}
	
	private boolean isInverted(int dataPointID){
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 2; z++) {
					if (proservData.getFunctionDataPoint(x, y, z) == dataPointID) {
						if(proservData.getFunctionStateIsInverted(x, y)) {
							logger.debug("isInverted: x:{}, y:{}, z:{}, dataPointID={}", x, y, x, dataPointID);						
							return true;
						}				
						return false;
					}																														
				}
			}
		}
		return false;
	}
	
	private byte getDef(int dataPointID){
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 2; z++) {
					if (proservData.getFunctionDataPoint(x, y, z) == dataPointID) {
						return proservData.getFunctionDefs(x, y);
					}																														
				}
			}
		}
		return 0;
	}
	
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		logger.debug("proServ received UPDATE for itemName:{}, newState:{}", itemName, newState.toString());

		// updated values from rules
		if (itemName.contains("dpID")) {
			String dpID = itemName.substring(itemName.indexOf("dpID") + "dpID".length());
			short dataPoint = Short.parseShort(dpID);
			short offset = 0;
			byte state = 0;
			int scheduleType = 0;
			if(proservCronJobs.cronJobs.containsKey(itemName)==true){
				scheduleType = proservCronJobs.cronJobs.get(itemName).scheduleType;
				if( scheduleType == 0 || scheduleType == 4 || scheduleType == 5 || scheduleType == 6 ){
					state = newState.toString().equals("ON") ? (byte) 1 : (byte) 0;
					logger.debug("internalReceiveUpdate state={}",state);						
					if(isInverted(dataPoint)){
						state = newState.toString().equals("ON") ? (byte) 0 : (byte) 1;					
						logger.debug("internalReceiveUpdate isInverted state={}",state);
					}
				}
				else if( scheduleType == 1){
					state = newState.toString().equals("ON") ? (byte) 1 : (byte) 0;
					offset = 4;
				}
				else if( scheduleType == 2 ){
					state = newState.toString().equals("ON") ? (byte) 1 : (byte) 3; //0x44 ON=COMFORT=1, OFF=NIGHT=3
					offset = 3;
				}				
				else if( scheduleType == 3){
					state = getDef(dataPoint);
				}
				else {
					logger.error("ERROR: internalReceiveUpdate unsupported scheduleType!!!!!");
					return;
				}
			}
			else {
				logger.error("ERROR: internalReceiveUpdate could not find datapoint !!!!!");
				return;
			}
			ProservConnector con = new ProservConnector(ip, port);
			try {
				con.connect();
				boolean skipSetDataPoint = false;
				if( scheduleType == 2){ // check if dp+3 is 2 (absent) or 4 (freeze protection), if so do not send
					byte[] dataValue = con.getDataPointValue((short)(dataPoint+offset), (short) 1);
					if (dataValue != null) {
						if(dataValue[0]==2 || dataValue[0]==4)
							skipSetDataPoint = true;
					}
				}
				logger.debug("internalReceiveUpdate state={}, dataPoint={}, offset={}, skipSetDataPoint={}, scheduleType={}",state, dataPoint, offset, skipSetDataPoint, scheduleType);					
				if(!skipSetDataPoint){
					if(false == con.setDataPointValue((short)(dataPoint+offset), state)){
						logger.error("internalReceiveUpdate con.setDataPointValue attempt 1 failed, re-try again immediately");
						if(false == con.setDataPointValue((short)(dataPoint+offset), state)){
							logger.error("internalReceiveUpdate con.setDataPointValue attempt 2 failed, sleep 100 ms and re-try again");
							try {
								Thread.sleep(100);
							} catch (InterruptedException ie) {
							}							
							if(false == con.setDataPointValue((short)(dataPoint+offset), state)){
								logger.error("++++++++++++++++++++++++++++++++internalReceiveUpdate con.setDataPointValue attempt 3 failed");
							}
						}
					}
				}
			} catch (NullPointerException e) {
				logger.error("internalReceiveUpdate NullPointerException");
			} catch (UnsupportedEncodingException e) {
				logger.error("internalReceiveUpdate UnsupportedEncodingException");
			} catch (UnknownHostException e) {
				logger.error("internalReceiveUpdate the given hostname '{}' : port'{}' of the proServ is unknown", ip, port);
				con = null;
			} catch (IOException e) {
				logger.warn("internalReceiveUpdate couldn't establish network connection [host '{}' : port'{}'] error:'{}'", ip, port, e);
				con = null;
			} catch (Exception e) {
				logger.warn("internalReceiveUpdate Exception error:{}", e);
			} finally {
				logger.debug("internalReceiveUpdate reached finally");
				if (con != null) {
					con.disconnect();
				}
			}
		}
	}
	
	@Override
	public synchronized void internalReceiveCommand(String itemName, Command command) {
		logger.debug("proServ received command for itemName:{}, command:{}", itemName, command.toString());
		
		String pathLogsDir = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "logs";
	    Path pathBackupRrd = FileSystems.getDefault().getPath(pathLogsDir + File.separator + "BackupRrd.zip").toAbsolutePath();
	    Path pathZippedCsvFiles = FileSystems.getDefault().getPath(pathLogsDir + File.separator + "ZippedCsvFiles.zip").toAbsolutePath();
		
		if (itemName.equals("ProservTest") && command.toString().equals("START")) {
			eventPublisher.postUpdate(itemName, new StringType("PROCESSING"));
			try {Thread.sleep(3000);} catch (InterruptedException ie) {}
			//eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
			eventPublisher.postUpdate(itemName, new StringType("FAILED:Please see the logfile for details. This is a very long message which includes a log gfile path : C:\\Users\\jeka\\Documents\\OpenHAB\\source\\openhab-fork\\distribution\\openhabhome\\logs"));
		}
		
		//http://localhost:8080/CMD?ProservBackupResetRrd=START		
		//http://localhost:8080/CMD?ProservBackupRrd=START
		if (itemName.equals("ProservBackupResetRrd") && command.toString().equals("START") || 
				itemName.equals("ProservBackupRrd") && command.toString().equals("START")) {
			try {
				// save a copy of all rrd files in BackupRrd.zip
				Date now = new Date();
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String zipFolderName = simpleDateFormat.format(now);
				ProservLogfileProvider proservLogfileProvider = new ProservLogfileProvider();
				File directory = new File(ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "etc" + File.separator
						+ "rrd4j");
				for (File f : directory.listFiles()) {
					if (f.getName().startsWith("itemProServLog")) {
						proservLogfileProvider.createZip(pathBackupRrd, f.toPath(), zipFolderName + "/" + f.getName());
					}
				}
				
				// clean up garbage from java.nio
				File directoryLogs = new File(ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "logs");
				for(File f: directoryLogs.listFiles())
				    if(f.getName().startsWith("zipfstmp"))
				        f.delete();
				
				// delete all rrd files
				if (itemName.equals("ProservBackupResetRrd")) {
					int failedToDeleteFiles = 0;
					for (File f : directory.listFiles()) {
						if (f.getName().startsWith("itemProServLog")) {
							int count = 0;
							while (!f.delete()) {
								System.gc();
								Thread.sleep(300);
								if (++count > 10) {
									failedToDeleteFiles++;
									break;
								}
							}
						}
					}
					if (failedToDeleteFiles > 0) {
						eventPublisher.postUpdate(itemName, new StringType("FAILED:Failed to delete all history data files, but the backup is done. Number of files that couldn't be deleted: "
								+ new Integer(failedToDeleteFiles).toString()));
					} else {
						eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
					}
				} else {
					eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
				}
				
			} catch (Throwable e) {
				logger.error("ProservBackupResetRrd exception: {}", e.toString());
				eventPublisher.postUpdate(itemName, new StringType("FAILED:" + e.toString()));
			}
			
		}
		//http://localhost:8080/CMD?ProservSendRrdBackup=START
		else if(itemName.equals("ProservSendRrdBackup") && command.toString().equals("START")){
			File f = new File(pathBackupRrd.toString());
			if(f.exists() && !f.isDirectory()){
				if(Mail.sendMail(mailTo, mailSubject, mailContent, pathBackupRrd.toUri().toString())){
					eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
				} else {
					eventPublisher.postUpdate(itemName, new StringType("FAILED:" + Mail.getLastError()));
				}
			} else {
				eventPublisher.postUpdate(itemName, new StringType("FAILED:The history data file is missing, please save history data!"));
			}
		}
		//http://localhost:8080/CMD?ProservExportCsvFiles=START
		else if(itemName.equals("ProservExportCsvFiles") && command.toString().equals("START")){
			if(proservData==null ){
				eventPublisher.postUpdate(itemName, new StringType("FAILED: unable to communicate with proserv, please check the communcation link (e.g the IP address)!"));
				return;
			}			
			ProservLogfileProvider proservLogfileProvider = new ProservLogfileProvider();
			try {
				proservLogfileProvider.doSnapshot(proservData.getAllItemNames());
				eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
			} catch (Throwable e) {
				eventPublisher.postUpdate(itemName, new StringType("FAILED:" + e.toString()));
			}
		}
		//http://localhost:8080/CMD?ProservSendCsvFiles=START
		else if(itemName.equals("ProservSendCsvFiles") && command.toString().equals("START")){
			File f = new File(pathZippedCsvFiles.toString());
			if(mailTo.isEmpty()){
				eventPublisher.postUpdate(itemName, new StringType("FAILED:The email address is missing, please configure the email address!"));
			}
			else if(f.exists() && !f.isDirectory()){
				if(Mail.sendMail(mailTo, mailSubject, mailContent, pathZippedCsvFiles.toUri().toString())){
					eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
				} else {
					eventPublisher.postUpdate(itemName, new StringType("FAILED:" + Mail.getLastError()));
				}
			} else {
				eventPublisher.postUpdate(itemName, new StringType("FAILED:The CSV data file is missing, please export csv data file!"));
			}
		}
		//http://localhost:8080/CMD?ProservLanguage=de
		else if(itemName.equals("ProservLanguage") ){
			boolean retVal = true;
			if(isSupportedLanguage(command.toString())){
				ProservBinding.language = command.toString();
				retVal = ProservData.writeConfigData("proserv:language", ProservBinding.language);	
			}
			if(retVal==false)
				eventPublisher.postUpdate(itemName, new StringType("FAILED:Failed to save the language setting. Please try later!"));
			else
			eventPublisher.postUpdate(itemName, new StringType(ProservBinding.language));
		}
		//http://localhost:8080/CMD?ProservEmail=aa@bb.cc
		else if(itemName.equals("ProservEmail") ){			
			if(command.toString().equals("?")){
				eventPublisher.postUpdate(itemName, new StringType(ProservBinding.mailTo));				
			} else {
				ProservBinding.mailTo = command.toString();
				if(ProservData.writeConfigData("proserv:mailto", ProservBinding.mailTo)){
					eventPublisher.postUpdate(itemName, new StringType(ProservBinding.mailTo));
				} else {
					eventPublisher.postUpdate(itemName, new StringType("FAILED:Failed to save the new setting, please try later!"));
				}
			}
		}
		//http://localhost:8080/CMD?ProservIP=192.168.2.1
		else if(itemName.equals("ProservIP") ){
			if(command.toString().equals("?")){
				eventPublisher.postUpdate(itemName, new StringType(ProservBinding.ip));				
			} else {
				ProservBinding.ip = command.toString();
				if(ProservData.writeConfigData("proserv:ip", ProservBinding.ip)){
					eventPublisher.postUpdate(itemName, new StringType(ProservBinding.ip));
				} else {
					eventPublisher.postUpdate(itemName, new StringType("FAILED:Failed to save the new setting, please try later!"));
				}
			}
		}
		//http://localhost:8080/CMD?ProservCronJobs=DPxx:true:0:0 0 8 ? * 2-6:0 0 21 ? * 1,7;DPyy:true:1:0 0 8 ? * 2-6:0 0 21 ? * 1,7;
		else if(itemName.equals("ProservCronJobs") ){
			if(proservData==null ){
				eventPublisher.postUpdate(itemName, new StringType("FAILED: unable to communicate with proserv, please check the communcation link (e.g the IP address)!"));
				return;
			}			
			if(proservCronJobs.add(command.toString())){
				proservData.updateSchedulerHtmlFile(proservCronJobs);
				proservData.updateProservRulesFile(proservCronJobs);
				eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
			} else {
				eventPublisher.postUpdate(itemName, new StringType("FAILED:Failed to save the new setting, please try later!"));
			}
		}
	}
	
	private static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}
	
	private void sendAlertMail(String s) {
	
		if(!mailTo.isEmpty()){
			// Load language strings
			Reader reader = null;
			String mailSubject = "xAlert";
			String mailContent = "xThis is an alert mail triggered by :";			
			String filename = ProservBinding.language + ".map";
			try {
				String path = ConfigDispatcher.getConfigFolder() + File.separator + "transform" + File.separator + filename ;
				Properties properties = new Properties();
				reader = new FileReader(path);
				properties.load(reader);
							
				mailSubject = properties.getProperty("MAIL-SUBJECT-ALERT");
				mailContent = properties.getProperty("MAIL-CONTENT-ALERT") + " " + s;

			} catch (Throwable e) {
				String message = "opening file '" + filename + "' throws exception";
				logger.error(message, e);
			} finally {
				IOUtils.closeQuietly(reader);
			}		
			Mail.sendMail(mailTo, mailSubject, mailContent);
		}
		else {
			logger.warn("proserv:mailto adress is not configured");
		}
	}
	
	public synchronized void updateSendEmail(int x, int y, byte[] dataValue) {
		int startDatapoint = (48*x) + (y*3) + 1;
		proservData.setFunctionDataPoint(startDatapoint, x, y, 0);
		switch ((int)proservData.getFunctionCodes(x, y) & 0xFF) {
		case 0x31:{
			boolean bCurrent = proservData.parse1ByteBooleanValue(dataValue[0]);
			//logger.debug("updateSendEmail: dataValue[0]:{},  bCurrent:{}, x:{}, y:{}", dataValue[0], bCurrent, x, y);
			if(proservData.getFunctionStateIsInverted(x,y)){
				bCurrent = !bCurrent;
				//logger.debug("updateSendEmail: isInverted bCurrent:{}, x:{}, y:{}", bCurrent, x, y);
			}
			if(previousEmailTrigger[x][y]!=null && previousEmailTrigger[x][y]==false && bCurrent==true && proservData.getFunctionIsEmailTrigger(x, y))
			{
				logger.debug("updateSendEmail: ---Sending!--- previousEmailTrigger[{}][{}]:{},  bCurrent:{}", x, y, previousEmailTrigger[x][y], bCurrent);
				sendAlertMail(proservData.getFunctionDescription(x, y));
		    }
			else{
				logger.debug("updateSendEmail: set previousEmailTrigger[{}][{}] = {} to new value:{}", x, y, previousEmailTrigger[x][y], bCurrent);
			}
			previousEmailTrigger[x][y] = bCurrent;
		} break;
		default:
			logger.debug("proServ binding, unhandled functioncode 0x{}", Integer.toHexString(((int)proservData.getFunctionCodes(x, y) & 0xFF)));
		}		
	}
	
	// The postUpdateSingleValueFunction function takes one function value, identified by x, y & z 
	// (z signifies actual or setpoint) and a single value as input and post update on the event bus.
	// It is called from the the Monitor thread when the proServ notifies for a value change. 
	public void postUpdateSingleValueFunction(int x, int y, int z, byte[] dataValue) {
		int startDatapoint = (48*x) + (y*3) + 1;
		int Id = proservData.getFunctionMapId(x,y,z);
		
		switch ((int)proservData.getFunctionCodes(x, y) & 0xFF) {
		case 0x01:
		case 0x02:
		case 0x04:
		case 0x05:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x11:
		case 0x12:
		case 0x13:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			if(proservData.getFunctionStateIsInverted(x,y))
				b = !b;
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x21:
		case 0x31:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			if(proservData.getFunctionStateIsInverted(x,y))
				b = !b;
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x26:
		case 0x34:{
			float f = proservData.parse2ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x38:{
			float f = proservData.parse4ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x32:		
		case 0x91:{
			int i = proservData.parse1BytePercentValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;

		case 0x33:
		case 0x92:{
			int i = proservData.parse1ByteUnsignedValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
		case 0x94:{
			float f = proservData.parse2ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x35:
		case 0x95:{
			long uint32 = proservData.parse4ByteUnsignedValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(uint32));
		} break;
		case 0x36:
		case 0x96:{
			long int32 = proservData.parse4ByteSignedValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(int32));
		} break;
		case 0x97:{
			float f = proservData.parse4ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		default:
			logger.debug("proServ binding, unhandled functioncode 0x{}", 
					Integer.toHexString(((int)proservData.getFunctionCodes(x, y) & 0xFF)));
		}	
		shortDelayBetweenBusEvents();
	}

	// The postUpdateFunction function takes one function value, x & y 
	// and a buffer with 3 data values as input and post update on the event bus.
	// It is called from the the polling thread. 
	// The postUpdateFunction also fill the proservData functionDataPoint values which are later used 
	// for lookup in the monitor thread. That is the async value update will only work after one successful data poll.
	public void postUpdateFunction(int x, int y, byte[] dataValue) {
		int startDatapoint = (48*x) + (y*3) + 1;
		int Id = proservData.getFunctionMapId(x,y,0);
		int IdPreset = proservData.getFunctionMapId(x,y,1);	
		
		proservData.setFunctionDataPoint(startDatapoint, x, y, 0);
		switch ((int)proservData.getFunctionCodes(x, y) & 0xFF) {
		case 0x01:
		case 0x02:
		case 0x04:
		case 0x05:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x11:
		case 0x12:
		case 0x13:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			if(proservData.getFunctionStateIsInverted(x,y))
				b = !b;			
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x21:
		case 0x31:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			if(proservData.getFunctionStateIsInverted(x,y))
				b = !b;
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id),  b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x26:
		case 0x34:{
			float f = proservData.parse2ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x38:{
			float f = proservData.parse4ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x32:		
		case 0x91:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				int i = proservData.parse1BytePercentValue(dataValue[0]);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				int preset = proservData.parse1BytePercentValue(dataValue[2]);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), new DecimalType(preset));
			}
		} break;
		case 0x33:
		case 0x92:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				int i = proservData.parse1ByteUnsignedValue(dataValue[0]);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				int preset = proservData.parse1ByteUnsignedValue(dataValue[2]);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset),new DecimalType(preset));										
			}
		} break;
		case 0x94:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				float f = proservData.parse2ByteFloatValue(dataValue,0);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
						new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				float f = proservData.parse2ByteFloatValue(dataValue,4);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), 
						new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
			}
		} break;
		case 0x35:
		case 0x95:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				long uint32 = proservData.parse4ByteUnsignedValue(dataValue,0);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(uint32));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				long uint32Preset = proservData.parse4ByteUnsignedValue(dataValue,8);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), 
						new DecimalType(uint32Preset));
			}
		} break;
		case 0x36:
		case 0x96:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				long int32 = proservData.parse4ByteSignedValue(dataValue,0);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(int32));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				long int32Preset = proservData.parse4ByteSignedValue(dataValue,8);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), 
						new DecimalType(int32Preset));
			}
		} break;
		case 0x97:{
			if(proservData.getFunctionLogThis(x,y,0)) {
				float f = proservData.parse4ByteFloatValue(dataValue,0);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
						new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
			}
			if(proservData.getFunctionLogThis(x,y,1)) {
				shortDelayBetweenBusEvents();
				proservData.setFunctionDataPoint(startDatapoint+2, x, y, 1);
				float f = proservData.parse4ByteFloatValue(dataValue,8);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), 
						new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
			}
		} break;
		default:
			logger.debug("proServ binding, unhandled functioncode 0x{}", 
					Integer.toHexString(((int)proservData.getFunctionCodes(x, y) & 0xFF)));
		}	
		shortDelayBetweenBusEvents();
	}

	// The postUpdateSingleValueHeating function takes one heating value, x (z signifies actual or setpoint)
	// and a single value as input and post update on the event bus.
	// It is called from the the Monitor thread when the proServ notifies for a value change. 
	public void postUpdateSingleValueHeating(int x, int z, byte[] dataValue) {
		int Id = proservData.getHeatingMapId(x,z);						
		int startDatapoint = 865 + x * 5;
		switch ( (int)(proservData.getHeatingCodes(x) & 0xFF) ) {
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
			float f = proservData.parse2ByteFloatValue(dataValue, 0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id),
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
			break;
		default:
			logger.debug("proServ binding, unhandled heatingCode {}", Integer.toHexString(((int)proservData.getHeatingCodes(x) & 0xFF)));
		}		
	}

	// The postUpdateHeating function takes one heating value, x 
	// and a buffer with 3 data values as input and post update on the event bus.
	// It is called from the the polling thread. 
	// The postUpdateHeating also fill the proservData heatingDataPoint values which are later used 
	// for lookup in the monitor thread. That is the async value update will only work after one successful data poll.
	public void postUpdateHeating(int x, byte[] dataValue) {
		int IdActual = proservData.getHeatingMapId(x,0);						
		int IdPreset = proservData.getHeatingMapId(x,1);

		int startDatapoint = 865 + x * 5;

		switch ( (int)(proservData.getHeatingCodes(x) & 0xFF) ) {
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
			proservData.setHeatingDataPoint(startDatapoint, x, 0);
			float f0 = proservData.parse2ByteFloatValue(dataValue, 0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdActual),
					new DecimalType(new BigDecimal(f0).setScale(2, RoundingMode.HALF_EVEN)));
			proservData.setHeatingDataPoint(startDatapoint+2, x, 1);
			float f1 = proservData.parse2ByteFloatValue(dataValue, 4);
			shortDelayBetweenBusEvents();
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(IdPreset), 
					new DecimalType(new BigDecimal(f1).setScale(2, RoundingMode.HALF_EVEN)));
			/*
			logger.info("{}{}: {}{}: {}", padRight(proservData.getHeatingDescription(x), 20), 
					padRight(proservData.getStringProservLang(0), 5), padRight(Float.toString(f0), 10), 
					padRight(proservData.getStringProservLang(1), 5), padRight(Float.toString(f1), 10));
					*/
			break;
		default:
			logger.debug("proServ binding, unhandled heatingCode {}", Integer.toHexString(((int)proservData.getHeatingCodes(x) & 0xFF)));
		}		
	}
	

	// The postUpdateWeather function is called from the the polling and the Monitor thread. 
	public void postUpdateWeather(byte[] dataValue, int i) {
		int Id = proservData.getWeatherStationMapId(i);
		if( Id!=0 ){
			if( i>=0 && i<=4 ){
				float f0 = proservData.parse2ByteFloatValue(dataValue, 0);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id),
						new DecimalType(new BigDecimal(f0).setScale(2, RoundingMode.HALF_EVEN)));
			}
			else if( i==5 ){
				boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
				eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id),  b ? OnOffType.ON : OnOffType.OFF);			
			}
		}
	}
	

	@Override
	public synchronized void execute() {

		logger.debug("proServ binding refresh cycle starts!");
	
		proservXConnect.handleProservConnectServer();
		
		try {
			if(connector == null && !ip.isEmpty()) {
				connector = new ProservConnector(ip, port);					
			}			
			
			if(connector != null) {
				connector.connect();
				
				if (proservData == null || proservData.refresh == true) {
					proservData = null;
					proservData = new ProservData(chartItemRefreshHour,chartItemRefreshDay, 
							chartItemRefreshWeek, chartItemRefreshMonth, chartItemRefreshYear, language );
					byte[] proservAllConfigValues = getConfigValues();
					if (proservAllConfigValues == null) {
						logger.debug("proServ getConfigValues failed try again");
						proservAllConfigValues = getConfigValues(); // try again..
					}
					if (proservAllConfigValues != null) {
						proservData.parseRawConfigData(proservAllConfigValues);
						handleCronJobs();
						createFiles();
						connector.startMonitor(this.eventPublisher, ProservBinding.proservData, this);										
					} else {
						logger.debug("proServ getConfigValues failed twice in a row, try next refresh cycle!");
						proservData.refresh = true; // force a reload of configdata
					}
					
				}
			}
			else {
				logger.debug("proServ binding refresh skipped connector is NULL");				
			}

			if (proservData != null) {
				// function 1-1 .. function 18-16
				for (int x = 0; x < 18; x++) {
					for (int y = 0; y < 16; y++) {
						if(proservData.getFunctionLogThis(x,y,0) || proservData.getFunctionLogThis(x,y,1) || 
								proservData.getFunctionIsEmailTrigger(x,y)) {
							int startDatapoint = (48*x) + (y*3) + 1;
							int numberOfDatapoints = 3;
							byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) numberOfDatapoints);
							if (dataValue != null) {
								if(proservData.getFunctionLogThis(x,y,0) || proservData.getFunctionLogThis(x,y,1)) {								
									postUpdateFunction(x, y, dataValue);
								}
								if(proservData.getFunctionIsEmailTrigger(x,y)) {
									updateSendEmail(x, y, dataValue);
								}
							}
						}
						else if ( proservData.getFunctionIsTimer(x, y) ){
							proservData.setFunctionDataPoint((48*x) + (y*3) + 1, x, y, 0);
						}

					}
				}

				// heating 1-18
				for (int x = 0; x < 18; x++) {
					if (proservData.getHeatingLogThis(x)) {
						int startDatapoint = 865 + x * 5;
						int numberOfDatapoints = 5;					
						byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) numberOfDatapoints);
						if (dataValue != null) {
							postUpdateHeating(x, dataValue);
						}
					}
				}
				
				// weatherStation
				if (proservData.getWeatherStationBrigtnessEastIsEnabled()) {
					int startDatapoint = 991;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,0);
					}
				}
				if (proservData.getWeatherStationBrigtnessSouthIsEnabled()) {
					int startDatapoint = 992;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,1);
					}
				}
				if (proservData.getWeatherStationBrigtnessWestIsEnabled()) {
					int startDatapoint = 993;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,2);
					}
				}
				if (proservData.getWeatherStationWindSpeedIsEnabled()) {
					int startDatapoint = 994;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,3);
					}
				}
				if (proservData.getWeatherStationOutdoorTempIsEnabled()) {
					int startDatapoint = 995;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,4);
					}
				}
				if (proservData.getWeatherStationRainIsEnabled()) {
					int startDatapoint = 996;
					byte[] dataValue = connector.getDataPointValue((short) startDatapoint, (short) 1);
					if (dataValue != null) {
						postUpdateWeather(dataValue,5);
					}
				}
				logger.debug("proServ binding refresh cycle completed");								
			}
			else {
				logger.debug("proServ binding refresh skipped proservdata is NULL");				
			}
		} catch (NullPointerException e) {
			logger.warn("proServ NullPointerException");
		} catch (UnsupportedEncodingException e) {
			logger.warn("proServ UnsupportedEncodingException");
		} catch (UnknownHostException e) {
			logger.warn("proServ the given hostname '{}' : port'{}' of the proServ is unknown", ip, port);
			connector = null;
		} catch (IOException e) {
			logger.warn("proServ couldn't establish network connection [host '{}' : port'{}'] error:'{}'", ip, port, e);
			connector = null;
		} catch (Exception e) {
			logger.warn("proServ Exception in execute error:{}", e);
		} finally {
			logger.debug("proServ binding refresh cycle reached finally");			
			if (connector != null) {
				connector.disconnect();
			}
		}
	}
	
	private void createFiles() {
		proservData.updateProservMapFile();
		proservData.updateProservItemFile(proservCronJobs);
		proservData.updateProservSitemapFile();
		proservData.updateProservSitemapClassicFile();
		proservData.updateRrd4jPersistFile();
		proservData.updateDb4oPersistFile();		
		// generate html file and rules file based on the proservData and cronjobs
		proservData.updateSchedulerHtmlFile(proservCronJobs);
		proservData.updateProservRulesFile(proservCronJobs);		
	}

	private void handleCronJobs() {
		// find all #t-datapoints defined in proserv, then merge those with old existing (persisted) cronjob definitions 
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 16; y++) {
				if(proservData.getFunctionIsTimer(x,y)) {
					String zoneName = proservData.getZoneName(x); 
					String dataPointName = proservData.getFunctionDescription(x,y);
					int scheduleType = 0;
					String cron2 = null;
					if ( (int)(proservData.getFunctionCodes(x,y) & 0xFF) == 0x23) {	
						scheduleType = 3;
						cron2 = "";
					}
					else if ( ((int)(proservData.getFunctionCodes(x,y) & 0xFF) >= 0x11) &&	
					          ((int)(proservData.getFunctionCodes(x,y) & 0xFF) <= 0x13) ) {
						if(proservData.getFunctionUnits(x, y).equals("0"))
							scheduleType = 4;
						else if(proservData.getFunctionUnits(x, y).equals("1"))
							scheduleType = 5;
						else if(proservData.getFunctionUnits(x, y).equals("2"))
							scheduleType = 6;
					}					
					proservCronJobs.add(proservCronJobs.new CronJob("dpID"+Integer.toString((48*x)+(y*3)+1), scheduleType, 
							zoneName, dataPointName, false, null, false, cron2));
				}
			}
		}
		for (int x = 0; x < 18; x++) {
			if(proservData.getHeatingIsTimer(x)) {
				String zoneName = proservData.getZoneName(x); 
				String dataPointName = proservData.getHeatingDescription(x);
				int scheduleType = 0;
				switch ( (int)(proservData.getHeatingCodes(x) & 0xFF) ) {
				case 0x42: 
					scheduleType = 1;
					break;
				case 0x43:
				case 0x44:
					scheduleType = 2;
					break;
				}
				proservCronJobs.add(proservCronJobs.new CronJob("dpID"+Integer.toString(865+(5*x)), scheduleType,
						zoneName, dataPointName, false, null, false, null));
			}
		}
		proservCronJobs.mergeOldJobs();
		proservCronJobs.saveJobs();		
	}

	private void shortDelayBetweenBusEvents() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException ie) {
			// Handle the exception
		}
	}

	private byte[] getConfigValues() {
		byte[] proservAllConfigValues = null;
		try {

			short PROSERV_MEMORY_LENGTH = 16067;// stop reading after weather data
			short NUMBER_OF_BYTES_IN_CHUNK = 500;
			proservAllConfigValues = new byte[PROSERV_MEMORY_LENGTH];

			// read a chunk of values
			short startByte = 1;
			for (int chunkId = 0;; chunkId++) {
				short numberOfBytesToRead = NUMBER_OF_BYTES_IN_CHUNK;
				if (startByte + NUMBER_OF_BYTES_IN_CHUNK >= PROSERV_MEMORY_LENGTH)
					numberOfBytesToRead = (short) (PROSERV_MEMORY_LENGTH - startByte);

				byte[] proServValues = null;
				for(int attempt=0;attempt<4;attempt++){
					proServValues = connector.getParameterBytes(startByte, numberOfBytesToRead);
					if(proServValues!=null) 
						break;
				}
				if(proServValues==null)
				{
					logger.info("proServ getConfigValues failed proServValues==null");
					return null;
				}
				int offset = chunkId * NUMBER_OF_BYTES_IN_CHUNK;
				startByte += NUMBER_OF_BYTES_IN_CHUNK;
				for (int i = 0; i < numberOfBytesToRead; i++) {
					proservAllConfigValues[offset + i] = proServValues[i];
				}
				if (numberOfBytesToRead != NUMBER_OF_BYTES_IN_CHUNK)
					break;
			}
			logger.debug("proServ succesfully loaded all config values");
		}
		finally {
			
		}
		return proservAllConfigValues;
	}


	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	@Override
	protected String getName() {
		return "proServ Refresh Service";
	}
	@Override
	public void addBindingProvider(ProservBindingProvider provider) {
		super.addBindingProvider(provider);		
		setProperlyConfigured(true);
	}
}
