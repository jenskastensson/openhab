/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv.internal;

//import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.openhab.config.core.ConfigDispatcher;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import org.apache.commons.lang.StringUtils;
import org.openhab.binding.proserv.ProservBindingProvider;
import org.openhab.binding.proserv.ProservBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.action.mail.internal.Mail;
//import java.io.PrintWriter;
//import org.rrd4j.core.RrdDb;
//import org.rrd4j.graph.RrdGraph;
//import org.rrd4j.graph.RrdGraphDef;



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
	
	/** Default refresh interval (currently 1 minute) */
	private long refreshInterval = 60000L;

	/* The IP address to connect to */
	private static String ip;
	private static int port = 80;
	private static String mailTo = "";
	private static String mailSubject = "";
	private static String mailContent = "";
	private static Boolean previousEmailTrigger = null;
	private static String chartItemRefreshHour = null;
	private static String chartItemRefreshDay = null;
	private static String chartItemRefreshWeek = null;
	private static String chartItemRefreshMonth = null;
	private static String chartItemRefreshYear = null;

	
	private ProservData proservData = null;

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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public void updated(Dictionary config) throws ConfigurationException {
		if (config != null) {
			String ip = (String) config.get("ip");
			String portString = (String) config.get("port");
			ProservBinding.mailTo = (String) config.get("mailto");
			ProservBinding.mailSubject = (String) config.get("mailsubject");
			ProservBinding.mailContent = (String) config.get("mailcontent");
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
		}
	}

	@Override
	public void internalReceiveCommand(String itemName, Command command) {
		logger.debug("proServ received commaBnd for itemName:{}, command:{}", itemName, command.toString());
		
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
			if(f.exists() && !f.isDirectory()){
				if(Mail.sendMail(mailTo, mailSubject, mailContent, pathZippedCsvFiles.toUri().toString())){
					eventPublisher.postUpdate(itemName, new StringType("SUCCESS"));
				} else {
					eventPublisher.postUpdate(itemName, new StringType("FAILED:" + Mail.getLastError()));
				}
			} else {
				eventPublisher.postUpdate(itemName, new StringType("FAILED:The CSV data file is missing, please export csv data file!"));
			}
		}
	}
	
	private static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}
	
	private void sendAlertMail() {
		if(!mailTo.isEmpty())
			Mail.sendMail(mailTo, "Alert", "This is an alert mail triggered by proserv #m");
		else
			logger.warn("proserv:mailto adress is not configured");
	}
	
	public void updateSendEmail(int x, int y, byte[] dataValue) {
		int startDatapoint = (48*x) + (y*3) + 1;
		proservData.setFunctionDataPoint(startDatapoint, x, y, 0);
		switch ((int)proservData.getFunctionCodes(x, y) & 0xFF) {
		case 0x31:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			if(proservData.getFunctionStateIsInverted(x,y))
				b = !b;
			if(previousEmailTrigger!=null && previousEmailTrigger==false && b==true && proservData.getFunctionIsEmailTrigger(x, y))
			{
				previousEmailTrigger = b;
				sendAlertMail();
		    }
			previousEmailTrigger = b;
		} break;
		default:
			logger.debug("proServ binding, unhandled functioncode 0x{}", 
					Integer.toHexString(((int)proservData.getFunctionCodes(x, y) & 0xFF)));
		}		
	}
	
	// The postUpdateSingleValueFunction function takes one function value, identified by x, y & z 
	// (z signifies actual or setpoint) and a single value as input and post update on the event bus.
	// It is called from the the Monitor thread when the proServ notifies for a value change. 
	public void postUpdateSingleValueFunction(int x, int y, int z, byte[] dataValue) {
		int startDatapoint = (48*x) + (y*3) + 1;
		int Id = proservData.getFunctionMapId(x,y,z);
		
		switch ((int)proservData.getFunctionCodes(x, y) & 0xFF) {
		case 0x01:{
			boolean b = proservData.parse1ByteBooleanValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x02:{
			int i = proservData.parse1BytePercentValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
		case 0x12:{
			int i = proservData.parse1BytePercentValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
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
		case 0x91:{
			int i = proservData.parse1BytePercentValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
		case 0x92:{
			int i = proservData.parse1ByteUnsignedValue(dataValue[0]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
		case 0x94:{
			float f = proservData.parse2ByteFloatValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), 
					new DecimalType(new BigDecimal(f).setScale(2, RoundingMode.HALF_EVEN)));
		} break;
		case 0x95:{
			long uint32 = proservData.parse4ByteUnsignedValue(dataValue,0);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(uint32));
		} break;
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
		case 0x01:{
			proservData.setFunctionDataPoint(startDatapoint+1, x, y, 0);
			boolean b = proservData.parse1ByteBooleanValue(dataValue[1]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), b ? OnOffType.ON : OnOffType.OFF);
		} break;
		case 0x02:{
			proservData.setFunctionDataPoint(startDatapoint+1, x, y, 0);
			int i = proservData.parse1BytePercentValue(dataValue[1]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
		case 0x12:{
			proservData.setFunctionDataPoint(startDatapoint+2, x, y, 0);
			int i = proservData.parse1BytePercentValue(dataValue[2]);
			eventPublisher.postUpdate("itemProServLog" + Integer.toString(Id), new DecimalType(i));
		} break;
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
	

	@Override
	public void execute() {

		logger.debug("proServ binding refresh cycle starts!");

		if(connector==null)
		{
			connector = new ProservConnector(ip, port);	
		}
		
		try {
			connector.connect();
			if (proservData == null) {
				proservData = new ProservData(chartItemRefreshHour,chartItemRefreshDay, chartItemRefreshWeek, chartItemRefreshMonth, chartItemRefreshYear );
				byte[] proservAllConfigValues = getConfigValues();
				if (proservAllConfigValues == null) {
					logger.debug("proServ getConfigValues failed try again");
					proservAllConfigValues = getConfigValues(); // try again..
				}
				if (proservAllConfigValues != null) {
					proservData.parseRawConfigData(proservAllConfigValues);
					proservData.updateProservMapFile();
					proservData.updateProservItemFile();
					proservData.updateProservSitemapFile();
					proservData.updateRrd4jPersistFile();
					proservData.updateDb4oPersistFile();
					connector.startMonitor(this.eventPublisher, this.proservData, this);
				} else {
					logger.debug("proServ getConfigValues failed twice in a row, try next refresh cycle!");
					proservData = null; // force a reload of configdata
				}
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
			}
			logger.debug("proServ binding refresh cycle completed");				
		} catch (NullPointerException e) {
			logger.warn("proServ NullPointerException");
		} catch (UnsupportedEncodingException e) {
			logger.warn("proServ UnsupportedEncodingException");
		} catch (UnknownHostException e) {
			logger.warn("proServ the given hostname '{}' : port'{}' of the proServ is unknown", ip, port);
		} catch (IOException e) {
			logger.warn("proServ couldn't establish network connection [host '{}' : port'{}'] error:'{}'", ip, port, e);
		} catch (Exception e) {
			logger.warn("proServ Exception in execute error:{}", e);
		} finally {
			logger.debug("proServ binding refresh cycle reached finally");			
			if (connector != null) {
				connector.disconnect();
			}
		}
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

			short PROSERV_MEMORY_LENGTH = 19500;
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
				if (offset > 10300)
					break; // quick fix for now
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