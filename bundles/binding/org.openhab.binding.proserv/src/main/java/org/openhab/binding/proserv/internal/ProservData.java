package org.openhab.binding.proserv.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteFloat;
import tuwien.auto.calimero.exception.KNXFormatException;

import org.openhab.binding.proserv.internal.ProservCronJobs.CronJob;
import org.openhab.config.core.ConfigDispatcher;

public class ProservData {
	private static final Logger logger = LoggerFactory.getLogger(ProservData.class);
	private static String chartItemRefreshHour = null;
	private static String chartItemRefreshDay = null;
	private static String chartItemRefreshWeek = null;
	private static String chartItemRefreshMonth = null;
	private static String chartItemRefreshYear = null;
	private static String language = null;
	private String[] zoneNames = new String[18];
	private byte[][] functionCodes = new byte[18][16];
	private String[][] functionDescriptions = new String[18][16];
	private String[][] functionUnits = new String[18][16];
	private byte[][] functionProfiles = new byte[18][16];
	private byte[][] functionDefs = new byte[18][16];
	private boolean[][][] functionLogThis = new boolean[18][16][2];
	private int[][][] functionMapId = new int[18][16][2];
	private int[][][] functionDataPoint = new int[18][16][2];
	private boolean[][] functionStateIsInverted = new boolean[18][16];
	private boolean[][] functionIsEmailTrigger = new boolean[18][16];
	private boolean[][] functionIsTimer = new boolean[18][16];

	private byte[] heatingCodes = new byte[18];
	private String[] heatingDescriptions = new String[18];
	private byte[] heatingProfiles = new byte[18];
	private byte[] heatingDefs = new byte[18];
	private boolean[] heatingLogThis = new boolean[18];
	private int[][] heatingMapId = new int[18][2];
	private int[][] heatingDataPoint = new int[18][2];
	private boolean[] heatingIsTimer = new boolean[18];

	private byte weatherStationCode = 0x71;
	byte weatherStationDefs = 0;
	private int[] weatherStationMapId = new int[6];
	@SuppressWarnings("unused")
	private int weatherStationDataPoint;
	private int nofUsedMapId = 0;

	private String[] urlSchemes = new String[20];
	
	private static Map<String, String> mapProservLang = new HashMap<String, String>();
	private String allItemNames;
	public boolean refresh = false;

	public ProservData(String chartItemRefreshHour, String chartItemRefreshDay, String chartItemRefreshWeek, String chartItemRefreshMonth,
			String chartItemRefreshYear, String language) {
		ProservData.chartItemRefreshHour = chartItemRefreshHour;
		ProservData.chartItemRefreshDay = chartItemRefreshDay;
		ProservData.chartItemRefreshWeek = chartItemRefreshWeek;
		ProservData.chartItemRefreshMonth = chartItemRefreshMonth;
		ProservData.chartItemRefreshYear = chartItemRefreshYear;
		ProservData.language = language;
	}

	public String getAllItemNames() {
		return allItemNames;
	}

	public String getmapProservLang(String x) {
		return mapProservLang.get(x);
	}

	public String getZoneName(int x) {
		return zoneNames[x];
	}

	public byte getFunctionCodes(int x, int y) {
		return functionCodes[x][y];
	}

	public byte getFunctionDefs(int x, int y) {
		return functionDefs[x][y];
	}
	
	public boolean getFunctionLogThis(int x, int y, int i) {
		return functionLogThis[x][y][i];
	}

	public int getFunctionMapId(int x, int y, int i) {
		return functionMapId[x][y][i];
	}

	public String getFunctionDescription(int x, int y) {
		return functionDescriptions[x][y];
	}

	public String getFunctionUnits(int x, int y) {
		return functionUnits[x][y];
	}

	public int getFunctionDataPoint(int x, int y, int i) {
		return functionDataPoint[x][y][i];
	}

	public boolean getFunctionStateIsInverted(int x, int y) {
		return functionStateIsInverted[x][y];
	}

	public boolean getFunctionIsEmailTrigger(int x, int y) {
		return functionIsEmailTrigger[x][y];
	}

	public boolean getFunctionIsTimer(int x, int y) {
		return functionIsTimer[x][y];
	}

	public void setFunctionDataPoint(int dataPoint, int x, int y, int i) {
		functionDataPoint[x][y][i] = dataPoint;
	}

	public byte getHeatingCodes(int x) {
		return heatingCodes[x];
	}

	public boolean getHeatingLogThis(int x) {
		return heatingLogThis[x];
	}

	public int getHeatingMapId(int x, int i) {
		return heatingMapId[x][i];
	}

	public String getHeatingDescription(int x) {
		return heatingDescriptions[x];
	}

	public boolean getHeatingIsTimer(int x) {
		return heatingIsTimer[x];
	}

	public int getHeatingDataPoint(int x, int i) {
		return heatingDataPoint[x][i];
	}

	public void setHeatingDataPoint(int dataPoint, int x, int i) {
		heatingDataPoint[x][i] = dataPoint;
	}

	public boolean getWeatherStationLogThis() {
		if(getWeatherStationBrigtnessEastIsEnabled() || getWeatherStationBrigtnessSouthIsEnabled() || 
			getWeatherStationBrigtnessWestIsEnabled() || getWeatherStationOutdoorTempIsEnabled() || 
			getWeatherStationRainIsEnabled() || getWeatherStationWindSpeedIsEnabled() )
			return true;
		return false;
	}
	
	public boolean getWeatherStationBrigtnessEastIsEnabled() {
		return (weatherStationDefs & 0x80) == 0x80;
	}
	
	public boolean getWeatherStationBrigtnessSouthIsEnabled() {
		return (weatherStationDefs & 0x40) == 0x40;
	}
	
	public boolean getWeatherStationBrigtnessWestIsEnabled() {
		return (weatherStationDefs & 0x20) == 0x20;
	}
	
	public boolean getWeatherStationWindSpeedIsEnabled() {
		return (weatherStationDefs & 0x10) == 0x10;
	}

	public boolean getWeatherStationOutdoorTempIsEnabled() {
		return (weatherStationDefs & 0x8) == 0x8;
	}

	public boolean getWeatherStationRainIsEnabled() {
		return (weatherStationDefs & 0x4) == 0x4;
	}

	public byte getWeatherStationCode() {
		return weatherStationCode;
	}

	public int getWeatherStationMapId(int index) {
		return weatherStationMapId[index];
	}

	public int getNofUsedMapId() {
		return nofUsedMapId;
	}
	
	public void parseRawConfigData(byte[] proServAllValues) throws UnsupportedEncodingException {
		int lengthDescription = 24;
		int lengthUnit = 5;

		// zone 1-18
		int startFunctionOffset = 0;
		for (int x = 0; x < 18; x++) {
			try {
				int offset = startFunctionOffset + 25 * x;
				int actualLength = 0;
				for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset + 1 + actualLength] != 0; actualLength++) {
				}
				if (actualLength > 0)
					zoneNames[x] = (new String(proServAllValues, offset + 1, actualLength, "ISO-8859-1"));
				else
					zoneNames[x] = new String();
			} catch (NullPointerException e) {
				logger.warn("proserv NullPointerException zonenames");
			} finally {
			}
		}

		// function 1-1 .. function 18-16
		startFunctionOffset = 512;
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 16; y++) {
				try {
					int offset = startFunctionOffset + 512 * x + 32 * y;
					functionCodes[x][y] = proServAllValues[offset];
					int actualLength = 0;
					for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset + 1 + actualLength] != 0; actualLength++) {
					}
					if (actualLength > 0)
						functionDescriptions[x][y] = (new String(proServAllValues, offset + 1, actualLength, "ISO-8859-1"));
					else
						functionDescriptions[x][y] = new String();

					for (actualLength = 0; actualLength < lengthUnit && proServAllValues[offset + 25 + actualLength] != 0; actualLength++) {
					}
					if (actualLength > 0) {
						functionUnits[x][y] = (new String(proServAllValues, offset + 25, actualLength, "ISO-8859-1"));
					} else {
						if ((functionDefs[x][y] & 0xFF) == 0x32 || (functionDefs[x][y] & 0xFF) == 0x91)
							functionUnits[x][y] = "%";
						else
							functionUnits[x][y] = new String();
					}

					functionProfiles[x][y] = proServAllValues[offset + 30];
					functionDefs[x][y] = proServAllValues[offset + 31];
					
					// #m only with 0x31 
					if (functionDescriptions[x][y].toLowerCase().contains("#m")) {
						if (((int) functionCodes[x][y] & 0xFF) == 0x31) {
							functionIsEmailTrigger[x][y] = true;
						}
					}

					// #t with 0x01, 0x02, 0x04, 0x05, 0x11, 0x12, 0x13, 0x21, 0x23, 0x31 
					if (functionDescriptions[x][y].toLowerCase().contains("#t")) {
						switch ((int) functionCodes[x][y] & 0xFF) {
						case 0x01:
						case 0x02:
						case 0x04:
						case 0x05:
						case 0x11:
						case 0x12:
						case 0x13:
						case 0x21:
						case 0x23:
						case 0x31: {
							functionIsTimer[x][y] = true;
/// TEST DEBUG:
//functionLogThis[x][y][0] = true;							
							} break;
						default:
							logger.error("functionDescriptions[{}][{}]={} has #t but is not supported datatype ({})", x, y, 
									functionDescriptions[x][y], Integer.toHexString((int) functionCodes[x][y] & 0xFF));
						}
					}

					//#l no restrictions
		            if(functionDescriptions[x][y].toLowerCase().contains("#l"))
		            {
		            	if( ((int)functionCodes[x][y] & 0xFF)>=0x91 && ((int)functionCodes[x][y] & 0xFF)<=0x97 )
		            	{		            	
							if( (functionDefs[x][y] & 0x30) >= 0x10) // 'y' is 01, 10, 11 : show actual value
								functionLogThis[x][y][0] = true;
							if( (functionDefs[x][y] & 0x1) == 1) // 'x' is 01, 11 : show preset value
								functionLogThis[x][y][1] = true;
						} else {
							functionLogThis[x][y][0] = true;
						}
					}
		            
	            	if( ((int)functionCodes[x][y] & 0xFF)==0x31 ) 
	            	{	//State - 1 bit value	            	
						if( (functionDefs[x][y] & 0x1) == 0) // x) 1=high active; 0=low active
							functionStateIsInverted[x][y] = true;
					}
	            	
	            	if( ((int)functionCodes[x][y] & 0xFF)>=0x11 && ((int)functionCodes[x][y] & 0xFF)<=0x13 )  
	            	{	//State - 1 bit value	            	
						if( (functionDefs[x][y] & 0x1) == 1) // Invert direction:
															 // 1 : yes
															 // 0 : no (default)
							functionStateIsInverted[x][y] = true;
					}		            	
		            
					if (functionDescriptions[x][y].toLowerCase().contains("#l") || functionDescriptions[x][y].toLowerCase().contains("#m")
							|| functionDescriptions[x][y].toLowerCase().contains("#t")) {
						functionDescriptions[x][y] = functionDescriptions[x][y].substring(0, functionDescriptions[x][y].indexOf("#"));
						logger.debug("-----x{}y{}  offset:{}  {}  code:0x{}  log actual:{}, log preset:{}  StateIsInverted:{}", x, y, offset,
								functionDescriptions[x][y], Integer.toHexString((int) functionCodes[x][y] & 0xFF), functionLogThis[x][y][0],
								functionLogThis[x][y][1], functionStateIsInverted[x][y]);
					}
				} catch (NullPointerException e) {
					logger.warn("proserv NullPointerException functions");
				} finally {

				}

			}
		}

		// heating 1-18
		int startHeatingOffset = 9728;
		for (int x = 0; x < 18; x++) {
			try {
				int offset = startHeatingOffset + 32 * x;
				heatingCodes[x] = proServAllValues[offset];

				int actualLength = 0;
				for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset + 1 + actualLength] != 0; actualLength++) {
				}
				if (actualLength > 0)
					heatingDescriptions[x] = (new String(proServAllValues, offset + 1, actualLength, "ISO-8859-1"));
				else
					heatingDescriptions[x] = new String();

				heatingProfiles[x] = proServAllValues[offset + 30];
				heatingDefs[x] = proServAllValues[offset + 31];

				if (heatingDescriptions[x].toLowerCase().contains("#t")) {
					heatingIsTimer[x] = true;
				}

				if (heatingDescriptions[x].toLowerCase().contains("#l") || heatingDescriptions[x].toLowerCase().contains("#t")) {
					heatingLogThis[x] = true;
					heatingDescriptions[x] = heatingDescriptions[x].substring(0, heatingDescriptions[x].indexOf("#"));
					int startDatapoint = 865 + x * 5;
					logger.debug("-----x:{}  offset:{} {}   code:0x{}  log actual:{}  startDatapoint{}:", x, offset, heatingDescriptions[x],
							Integer.toHexString((int) heatingCodes[x] & 0xFF), heatingLogThis[x], startDatapoint);
				}
			} catch (NullPointerException e) {
				logger.warn("proserv NullPointerException zonenames");
			} finally {
			}
		}

		// weatherStation
		int weatherStationOffset = 16065;
		weatherStationDefs = proServAllValues[weatherStationOffset];

		// URLSchemes
		int urlSchemesOffset = 16194;
		int lengthSchemes = 150;
		for(int i=0;i<20;i++){
			int offset = urlSchemesOffset + lengthSchemes * i;
			int actualLength = 0;
			for (actualLength = 0; actualLength < lengthSchemes && proServAllValues[offset + actualLength] != 0; actualLength++) {
			}
			if (actualLength > 0)
				urlSchemes[i] = (new String(proServAllValues, offset, actualLength, "ISO-8859-1"));
			else
				urlSchemes[i] = new String();
		}
	}

	public float parse2ByteFloatValue(byte[] dataValue, int index) {
		float dataFloat = 0;
		try {
			DPTXlator2ByteFloat dPTXlator2ByteFloat = new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_HUMIDITY);
			dPTXlator2ByteFloat.setData(dataValue, index);
			dataFloat = dPTXlator2ByteFloat.getValueFloat();

		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		} finally {
		}
		return dataFloat;
	}

	public float parse4ByteFloatValue(byte[] dataValue, int index) {
		float dataFloat = 0;
		try {
			DPTXlator4ByteFloat dPTXlator4ByteFloat = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_ELECTRIC_CHARGE);
			dPTXlator4ByteFloat.setData(dataValue, index);
			dataFloat = dPTXlator4ByteFloat.getValueFloat();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		} finally {
		}
		return dataFloat;
	}

	public boolean parse1ByteBooleanValue(byte dataValue) {
		return (dataValue != 0);
	}

	public int parse1BytePercentValue(byte dataValue) {
		return ((((int) dataValue & 0xFF) + 1) * 100 / 255);
	}

	public int parse1ByteUnsignedValue(byte dataValue) {
		return (int) dataValue & 0xFF;
	}

	public long parse4ByteUnsignedValue(byte[] dataValue, int index) {
		long dataUnsigned = 0;
		try {
			DPTXlator4ByteUnsigned dPTXlator4ByteUnsigned = new DPTXlator4ByteUnsigned(DPTXlator4ByteUnsigned.DPT_VALUE_4_UCOUNT);
			dPTXlator4ByteUnsigned.setData(dataValue, index);
			dataUnsigned = dPTXlator4ByteUnsigned.getValueUnsigned();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		} finally {
		}
		return dataUnsigned;
	}

	public long parse4ByteSignedValue(byte[] dataValue, int index) {
		long dataSigned = 0;
		try {
			DPTXlator4ByteSigned dPTXlator4ByteSigned = new DPTXlator4ByteSigned(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY);
			dPTXlator4ByteSigned.setData(dataValue, index);
			dataSigned = dPTXlator4ByteSigned.getValueSigned();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		} finally {
		}
		return dataSigned;
	}

	private void updateMapIds() {
		int mapId = 0;
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 16; y++) {
				if (functionLogThis[x][y][0]) {
					functionMapId[x][y][0] = mapId++;
				}
				if (functionLogThis[x][y][1]) {
					functionMapId[x][y][1] = mapId++;
				}
			}
		}
		for (int x = 0; x < 18; x++) {
			if (heatingLogThis[x]) {
				heatingMapId[x][0] = mapId++;
				heatingMapId[x][1] = mapId++;
			}
		}

		nofUsedMapId = mapId;
		if (getWeatherStationLogThis()) {
			for (int x = 0; x < 6; x++) {
				weatherStationMapId[x] = 0;
			}
			mapId++;
			if (getWeatherStationBrigtnessEastIsEnabled()) {
				weatherStationMapId[0] = mapId;
				nofUsedMapId++;
			}
			mapId++;
			if (getWeatherStationBrigtnessSouthIsEnabled()) {
				weatherStationMapId[1] = mapId;
				nofUsedMapId++;
			}
			mapId++;
			if (getWeatherStationBrigtnessWestIsEnabled()) {
				weatherStationMapId[2] = mapId;
				nofUsedMapId++;
			}
			mapId++;
			if (getWeatherStationWindSpeedIsEnabled()) {
				weatherStationMapId[3] = mapId;
				nofUsedMapId++;
			}
			mapId++;
			if (getWeatherStationOutdoorTempIsEnabled()){
				weatherStationMapId[4] = mapId;
				nofUsedMapId++;
			}
			mapId++;
			if (getWeatherStationRainIsEnabled())  {
				weatherStationMapId[5] = mapId;
				nofUsedMapId++;
			}	

		}
	}

	private static synchronized void updateLine(String fileName, String toUpdate, String updated) throws IOException {
		logger.debug("START updateLine filename:{},  toUpdate:{},  updated:{}", fileName, toUpdate, updated);
	    BufferedReader file = new BufferedReader(new FileReader(fileName));
	    String line;
	    String input = "";
	    while ((line = file.readLine()) != null)
	        input += line + System.lineSeparator();
	    input = input.replace(toUpdate, updated);
	    FileOutputStream os = new FileOutputStream(fileName);
	    os.write(input.getBytes());
	    file.close();
	    os.close();
	}	
	
	private static synchronized void changeProperty(String filename, String key, String value) throws IOException {
		final File tmpFile = new File(filename + ".tmp");
		Files.deleteIfExists(tmpFile.toPath());
		final File file = new File(filename);
		PrintWriter pw = new PrintWriter(tmpFile);
		BufferedReader br = new BufferedReader(new FileReader(file));
		boolean found = false;
		final String toAdd = key + '=' + value;
		for (String line; (line = br.readLine()) != null;) {
			if( line.startsWith(key + '=') || line.startsWith('#' + key + '=') ) {
				line = toAdd;
				found = true;
			}
			pw.println(line);
		}
		if (!found)
			pw.println(toAdd);
		br.close();
		pw.close();
		Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public static synchronized boolean writeConfigData(String key, String value) {
		logger.info("START writeConfigData key:{},  value:{}", key, value);
		String filename = "openhab.cfg";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + filename;
			changeProperty(path, key, value);
			logger.info("END writeConfigData");
			return true;
		} catch (Throwable e) {
			String message = "writeConfigData value: " + value + " key: " + key + " file: " + filename + " throws exception" + e.toString();
			logger.error(message, e);
		}
		logger.info("END writeConfigData FAILED");
		return false;
	}

	public static void loadProservLang() {
		Reader reader = null;
		String filename = ProservData.language + ".map";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "transform" + File.separator + filename;
			Properties properties = new Properties();
			reader = new FileReader(path);
			properties.load(reader);

			mapProservLang.clear();
			mapProservLang.put("ACTIVATE", properties.getProperty("ACTIVATE"));
			mapProservLang.put("ACTUAL", properties.getProperty("ACTUAL"));
			mapProservLang.put("PRESET", properties.getProperty("PRESET"));
			mapProservLang.put("SCROLL-FOR-MORE-CHARTS", properties.getProperty("SCROLL-FOR-MORE-CHARTS"));
			mapProservLang.put("PROSERV-CHARTS", properties.getProperty("PROSERV-CHARTS"));
			mapProservLang.put("ALL-VALUES", properties.getProperty("ALL-VALUES"));
			mapProservLang.put("PROSERV-CHARTS", properties.getProperty("PROSERV-CHARTS"));
			mapProservLang.put("BRIGHTNESS-EAST", properties.getProperty("BRIGHTNESS-EAST"));
			mapProservLang.put("BRIGHTNESS-SOUTH", properties.getProperty("BRIGHTNESS-SOUTH"));
			mapProservLang.put("BRIGHTNESS-NORTH", properties.getProperty("BRIGHTNESS-NORTH"));
			mapProservLang.put("BRIGHTNESS-WEST", properties.getProperty("BRIGHTNESS-WEST"));
			mapProservLang.put("WINDSPEED", properties.getProperty("WINDSPEED"));
			mapProservLang.put("TEMPERATURE", properties.getProperty("TEMPERATURE"));
			mapProservLang.put("RAIN", properties.getProperty("RAIN"));
			mapProservLang.put("OUTSIDE", properties.getProperty("OUTSIDE"));
			mapProservLang.put("SAVEALLCHANGES", properties.getProperty("SAVEALLCHANGES"));
			mapProservLang.put("PROSERVSCHEDULER", properties.getProperty("PROSERVSCHEDULER"));
			mapProservLang.put("ON", properties.getProperty("ON"));
			mapProservLang.put("OFF", properties.getProperty("OFF"));
			mapProservLang.put("COMFORT", properties.getProperty("COMFORT"));
			mapProservLang.put("NIGHT", properties.getProperty("NIGHT"));
			mapProservLang.put("SAVECONFRIM", properties.getProperty("SAVECONFRIM"));
			mapProservLang.put("SAVEFAILED", properties.getProperty("SAVEFAILED"));
			mapProservLang.put("OPEN", properties.getProperty("OPEN"));
			mapProservLang.put("CLOSE", properties.getProperty("CLOSE"));
			mapProservLang.put("UP", properties.getProperty("UP"));
			mapProservLang.put("DOWN", properties.getProperty("DOWN"));
			mapProservLang.put("IN", properties.getProperty("IN"));
			mapProservLang.put("OUT", properties.getProperty("OUT"));
			mapProservLang.put("PLEASE-CONFIGURE-IP", properties.getProperty("PLEASE-CONFIGURE-IP"));
			mapProservLang.put("NO-CONNECTION-PROSERV", properties.getProperty("NO-CONNECTION-PROSERV"));
			mapProservLang.put("PLEASE-CONFIGURE-CLASSIC", properties.getProperty("PLEASE-CONFIGURE-CLASSIC"));
			mapProservLang.put("NO-CONNECTION-PROSERV-CLASSIC", properties.getProperty("NO-CONNECTION-PROSERV-CLASSIC"));
			
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public void updateProservMapFile() {
		loadProservLang();
		updateMapIds();
		Writer writer = null;
		String filename = "proserv.map";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "transform" + File.separator + filename;
			Properties properties = new Properties();
			writer = new FileWriter(path);
			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z <= 1; z++) {
						if (functionLogThis[x][y][z]) {
							String key = "STRING-" + Integer.toString(functionMapId[x][y][z]);
							if (functionDescriptions[x][y] != null && !zoneNames[x].isEmpty() && !functionDescriptions[x][y].isEmpty()) {
								properties.setProperty(key, zoneNames[x] + " /// " + functionDescriptions[x][y]);
							}
						}
					}
				}
			}
			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					String key0 = "STRING-" + Integer.toString(heatingMapId[x][0]);
					String key1 = "STRING-" + Integer.toString(heatingMapId[x][1]);
					if (heatingDescriptions[x] != null && !zoneNames[x].isEmpty()  && !heatingDescriptions[x].isEmpty()) {
						properties.setProperty(key0, zoneNames[x] + " /// " + heatingDescriptions[x]);
						properties.setProperty(key1, zoneNames[x] + " /// " + heatingDescriptions[x]);
					}
				}
			}
			{
										
				String key;
				if (getWeatherStationBrigtnessEastIsEnabled()) {
					key = "STRING-" + Integer.toString(weatherStationMapId[0]);
					properties.setProperty(key, mapProservLang.get("BRIGHTNESS-EAST"));
				}
				if (getWeatherStationBrigtnessSouthIsEnabled()) {
					key = "STRING-" + Integer.toString(weatherStationMapId[1]);
					properties.setProperty(key, mapProservLang.get("BRIGHTNESS-SOUTH"));
				}
				if (getWeatherStationBrigtnessWestIsEnabled()) {
					key = "STRING-" + Integer.toString(weatherStationMapId[2]);
					properties.setProperty(key, mapProservLang.get("BRIGHTNESS-WEST"));
				}
				if (getWeatherStationWindSpeedIsEnabled()) {
					key = "STRING-" + Integer.toString(weatherStationMapId[3]);
					properties.setProperty(key, mapProservLang.get("WINDSPEED"));
				}
				if (getWeatherStationOutdoorTempIsEnabled()){
					key = "STRING-" + Integer.toString(weatherStationMapId[4]);
					properties.setProperty(key, mapProservLang.get("TEMPERATURE"));
				}
				if (getWeatherStationRainIsEnabled())  {
					key = "STRING-" + Integer.toString(weatherStationMapId[5]);
					properties.setProperty(key, mapProservLang.get("RAIN"));
				}
			}
			properties.store(writer, "");

		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public void updateProservItemFile(ProservCronJobs proservCronJobs) {

		String filename = "proserv.items";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "items" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "UTF-8");
			writer.println("Group gProserv");
			
			int nofMapIdGroupsOfTen = getNofUsedMapId() / 10;
			for (int n = 0; n <= nofMapIdGroupsOfTen; n++) {
				writer.println("Group gProserv"+ n);
			}
			writer.println("");

			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
						if (functionLogThis[x][y][0] && functionLogThis[x][y][1]) {
							String indexActual = Integer.toString(functionMapId[x][y][0]);
							String indexPreset = Integer.toString(functionMapId[x][y][1]);
							String indexGroupActual = Integer.toString(functionMapId[x][y][0] / 10);
							String indexGroupPreset = Integer.toString(functionMapId[x][y][0] / 10);
							writer.println("Group gitemProServLog" + indexActual + indexPreset);
							String item0 = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + indexActual
									+ "   \"{MAP(proserv.map):STRING-" + indexActual + "} " + mapProservLang.get("ACTUAL") + " "
									+ getFormatString(functionCodes[x][y], functionUnits[x][y]) + "\" <none> (gProserv, gProserv" 
									+ indexGroupActual + ", gitemProServLog" + indexActual + indexPreset + ")";
							writer.println(item0);
							String item1 = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + indexPreset
									+ "   \"{MAP(proserv.map):STRING-" + indexPreset + "} " + mapProservLang.get("PRESET") + " "
									+ getFormatString(functionCodes[x][y], functionUnits[x][y]) + "\" <none> (gProserv, gProserv" 
									+ indexGroupPreset + ", gitemProServLog" + indexActual + indexPreset + ")";
							writer.println(item1);
						} else {
							for (int z = 0; z <= 1; z++) {
								if (functionLogThis[x][y][z]) {
									String index = Integer.toString(functionMapId[x][y][z]);
									String indexGroup = Integer.toString(functionMapId[x][y][0] / 10);
									String actualOrPreset = (z == 0 ? mapProservLang.get("ACTUAL") : mapProservLang.get("ACTUAL"));
									String item = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + index
											+ "   \"{MAP(proserv.map):STRING-" + index + "} " + actualOrPreset + " "
											+ getFormatString(functionCodes[x][y], functionUnits[x][y]) + "\" <none> (gProserv, gProserv" + indexGroup + ")";
									writer.println(item);
								}
							}
						}
					}
				}
			}
			String lastIndexGroup = "";
			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						String indexActual = Integer.toString(heatingMapId[x][0]);
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						String indexGroupActual = Integer.toString(heatingMapId[x][0] / 10);
						String indexGroupPreset = Integer.toString(heatingMapId[x][1] / 10);
						lastIndexGroup = indexGroupPreset;
						writer.println("Group gitemProServLog" + indexActual + indexPreset);
						String item0 = "Number itemProServLog" + indexActual + "   \"{MAP(proserv.map):STRING-" + indexActual + "} "
								+ mapProservLang.get("ACTUAL") + " " + getFormatString(heatingCodes[x], "°C")
								+ "\" <none> (gProserv, gProserv" + indexGroupActual + ", gitemProServLog" + indexActual + indexPreset + ")";
						writer.println(item0);
						String item1 = "Number itemProServLog" + indexPreset + "   \"{MAP(proserv.map):STRING-" + indexPreset + "} "
								+ mapProservLang.get("PRESET") + " " + getFormatString(heatingCodes[x], "°C")
								+ "\" <none> (gProserv, gProserv" + indexGroupPreset + ", gitemProServLog" + indexActual + indexPreset + ")";
						writer.println(item1);
						if (getWeatherStationOutdoorTempIsEnabled()) {
							String indexOutdoorTemp = Integer.toString(weatherStationMapId[4]);
							String item2 = "Number itemProServLog" + indexOutdoorTemp + "   \"{MAP(proserv.map):STRING-" + indexOutdoorTemp + "} "
									+ mapProservLang.get("OUTSIDE") + " " + getFormatString(weatherStationCode, "°C")
									+ "\" <none> (gitemProServLog" + indexActual + indexPreset + ")";
							writer.println(item2);
						}
					}
				}
			}
			
			// weatherStation
			if (getWeatherStationBrigtnessEastIsEnabled()) {
				String index = Integer.toString(getWeatherStationMapId(0));						
				String item = "Number itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + "[%.0f Lux]" + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";	
				writer.println(item);
			}
			if (getWeatherStationBrigtnessSouthIsEnabled()) {
				String index = Integer.toString(getWeatherStationMapId(1));						
				String item = "Number itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + "[%.0f Lux]" + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";	
				writer.println(item);			
			}
			if (getWeatherStationBrigtnessWestIsEnabled()) {
				String index = Integer.toString(getWeatherStationMapId(2));						
				String item = "Number itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + "[%.0f Lux]" + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";	
				writer.println(item);
			}
			if (getWeatherStationWindSpeedIsEnabled()) {
				String index = Integer.toString(getWeatherStationMapId(3));						
				String item = "Number itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + getFormatString(weatherStationCode, "m/s") + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";				
				writer.println(item);
			}
			if (getWeatherStationOutdoorTempIsEnabled()){
				String index = Integer.toString(getWeatherStationMapId(4));	
				String item = "Number itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + getFormatString(weatherStationCode, "°C") + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";				
				writer.println(item);
			}
			if (getWeatherStationRainIsEnabled())  {
				String index = Integer.toString(getWeatherStationMapId(5));						
				String item = "Switch itemProServLog" + index + "   \"{MAP(proserv.map):STRING-" + index + "} "
						+ mapProservLang.get("ACTUAL") + " " + getFormatString(0x31, "") + "\" <none> (gProserv, gProserv" + lastIndexGroup + ")";	
				writer.println(item);
			}
				
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				//Switch proServTimer0  "proServTimer0" { proserv="proserv_timer" }
				String s = "Switch " + entry.getValue().dataPointID + " \"" + entry.getValue().dataPointID + "\" { proserv=\"proserv_timer\" }";
				writer.println(s);
			}

			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {

		}
	}

	private String getDataTypeString(int functionCode) {
		String dataTypeString = new String();
		dataTypeString = "Number";
		switch (functionCode & 0xFF) {
		case 0x01:
		case 0x02:
		case 0x04:
		case 0x05:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x21:
		case 0x31: {
			dataTypeString = "Switch";
		}
			break;
		}
		return dataTypeString;
	}

	private String getFormatString(int functionCode, String unit) {
		unit = unit.replace("%", "%%");
		if (!unit.isEmpty())
			unit = " " + unit;
		String formatString = new String();
		formatString = "[%d" + unit + "]";
		switch (functionCode & 0xFF) {
		case 0x11: 
		case 0x12: 
		case 0x13: {
			String mapFileName = ProservData.language + ".map";
			if(unit.trim().equals("0"))				
				formatString = "[MAP("+mapFileName+"):UPDOWN%d]";
			else if(unit.trim().equals("1"))				
				formatString = "[MAP("+mapFileName+"):OPENCLOSE%d]";
			if(unit.trim().equals("2"))				
				formatString = "[MAP("+mapFileName+"):INOUT%d]";	
		}
			break;
		case 0x32:
		case 0x91: {
			formatString = "[%.1f" + unit + "]";
		}
			break;
		case 0x34:
		case 0x36:
		case 0x94:
		case 0x97:
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
		case 0x71: {
			formatString = "[%.2f" + unit + "]";
		}
			break;
		}
		return formatString;
	}

	public static void updateProservDefaultSitemapFiles(String language) {
		String filenameStandard = "proserv.sitemap";
		String filenameClassic = "proserv-classic.sitemap";
		ProservData.language = language;
		try {
			loadProservLang();
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "sitemaps" + File.separator + filenameStandard;
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			String labelPleaseConfigure = mapProservLang.get("PLEASE-CONFIGURE-IP");
			String labelNoConnectionProserv = mapProservLang.get("NO-CONNECTION-PROSERV");
			writer.println("sitemap proserv label=\"" + labelPleaseConfigure + "\"\n{\n   Frame {\n		Group icon=\"pie\" label=\"" + labelNoConnectionProserv + "\"\n   }\n");			
			writer.println("}");
			writer.close();
			path = ConfigDispatcher.getConfigFolder() + File.separator + "sitemaps" + File.separator + filenameClassic;
			writer = new PrintWriter(path, "UTF-8");
			labelPleaseConfigure = mapProservLang.get("PLEASE-CONFIGURE-CLASSIC");
			labelNoConnectionProserv = mapProservLang.get("NO-CONNECTION-PROSERV-CLASSIC");
			writer.println("sitemap proserv label=\"" + labelPleaseConfigure + "\"\n{\n   Frame {\n		Group icon=\"pie\" label=\"" + labelNoConnectionProserv + "\"\n   }\n");			
			writer.println("}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filenameStandard + "' throws exception";
			logger.error(message, e);
		} finally {

		}
		
	}
	
	private void sitemapFileHelper(PrintWriter writer, String index) {
		writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + index + "}\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + index);
		if (!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()) {
			writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
		} else { writer.println("         Frame {"); 					}
		if (chartItemRefreshHour != null)
			writer.println("			Chart item=itemProServLog" + index + " period=h refresh=" + chartItemRefreshHour);
		if (chartItemRefreshDay != null)
			writer.println("			Chart item=itemProServLog" + index + " period=D refresh=" + chartItemRefreshDay);
		if (chartItemRefreshWeek != null)
			writer.println("			Chart item=itemProServLog" + index + " period=W refresh=" + chartItemRefreshWeek);
		if (chartItemRefreshMonth != null)
			writer.println("			Chart item=itemProServLog" + index + " period=M refresh=" + chartItemRefreshMonth);
		if (chartItemRefreshYear != null)
			writer.println("			Chart item=itemProServLog" + index + " period=Y refresh=" + chartItemRefreshYear);
		writer.println("        }\n      }\n   }");
	}

	public void updateProservSitemapFile() {

		String filename = "proserv.sitemap";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "sitemaps" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "UTF-8");
			String labelAllValues = mapProservLang.get("ALL-VALUES");
			String labelProservCharts = mapProservLang.get("PROSERV-CHARTS");
			writer.println("sitemap proserv label=\"" + labelProservCharts + "\"\n{\n   Frame {\n		Group item=gProserv icon=\"pie\" label=\"" + labelAllValues + "\"\n   }\n");			

			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
						if (functionLogThis[x][y][0] && functionLogThis[x][y][1]) {
							String indexActual = Integer.toString(functionMapId[x][y][0]);
							String indexPreset = Integer.toString(functionMapId[x][y][1]);
							writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + indexActual + "}\" icon=\"chart\" {");
							writer.println("         Text item=itemProServLog" + indexActual);
							writer.println("         Text item=itemProServLog" + indexPreset);
							if (!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()) {
								writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
							} else { writer.println("         Frame {"); }
							if (chartItemRefreshHour != null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh="
										+ chartItemRefreshHour);
							if (chartItemRefreshDay != null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh="
										+ chartItemRefreshDay);
							if (chartItemRefreshWeek != null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh="
										+ chartItemRefreshWeek);
							if (chartItemRefreshMonth != null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh="
										+ chartItemRefreshMonth);
							if (chartItemRefreshYear != null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh="
										+ chartItemRefreshYear);
							writer.println("        }\n      }\n   }");
						} else {
							for (int z = 0; z <= 1; z++) {
								if (functionLogThis[x][y][z]) {
									String index = Integer.toString(functionMapId[x][y][z]);
									writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + index + "}\" icon=\"chart\" {");
									writer.println("         Text item=itemProServLog" + index);
									if (!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()) {
										writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
									} else { writer.println("         Frame {"); }
									if (chartItemRefreshHour != null)
										writer.println("			Chart item=itemProServLog" + index + " period=h refresh=" + chartItemRefreshHour);
									if (chartItemRefreshDay != null)
										writer.println("			Chart item=itemProServLog" + index + " period=D refresh=" + chartItemRefreshDay);
									if (chartItemRefreshWeek != null)
										writer.println("			Chart item=itemProServLog" + index + " period=W refresh=" + chartItemRefreshWeek);
									if (chartItemRefreshMonth != null)
										writer.println("			Chart item=itemProServLog" + index + " period=M refresh=" + chartItemRefreshMonth);
									if (chartItemRefreshYear != null)
										writer.println("			Chart item=itemProServLog" + index + " period=Y refresh=" + chartItemRefreshYear);
									writer.println("        }\n      }\n   }");
								}
							}
						}
					}
				}
			}

			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						String indexActual = Integer.toString(heatingMapId[x][0]);
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + indexActual + "}\" icon=\"chart\" {");
						writer.println("         Text item=itemProServLog" + indexActual);
						writer.println("         Text item=itemProServLog" + indexPreset);
						if (getWeatherStationOutdoorTempIsEnabled()) {
							String indexOutdoorTemp = Integer.toString(weatherStationMapId[4]);
							writer.println("         Text item=itemProServLog" + indexOutdoorTemp);
						}
						if (!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()) {
							writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
						} else { writer.println("         Frame {"); }
						if (chartItemRefreshHour != null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh=" + chartItemRefreshHour);
						if (chartItemRefreshDay != null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh=" + chartItemRefreshDay);
						if (chartItemRefreshWeek != null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh=" + chartItemRefreshWeek);
						if (chartItemRefreshMonth != null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh=" + chartItemRefreshMonth);
						if (chartItemRefreshYear != null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh=" + chartItemRefreshYear);
						writer.println("        }\n      }\n   }");
					}
				}
			}
			
			if (getWeatherStationBrigtnessEastIsEnabled()) {
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(0)));
			}
			if (getWeatherStationBrigtnessSouthIsEnabled()) {
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(1)));
			}
			if (getWeatherStationBrigtnessWestIsEnabled()) {
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(2)));
			}
			if (getWeatherStationWindSpeedIsEnabled()) {
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(3)));
			}
			if (getWeatherStationOutdoorTempIsEnabled()){
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(4)));
			}
			if (getWeatherStationRainIsEnabled())  {
				sitemapFileHelper(writer, Integer.toString(getWeatherStationMapId(5)));
			}	
			
			writer.println("}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {

		}

	}

	private void sitemapFileHelperClassic(PrintWriter writer, String index) {
		writer.println("		Group label=\"{MAP(proserv.map):STRING-" + index + "}\" icon=\"chart\" {");
		writer.println("		Text item=itemProServLog" + index);
		
		// All charts
		writer.println("			Group label=\"All charts\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()) {
			writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
		} else { writer.println("         Frame {"); 					}
		if (chartItemRefreshHour != null)
			writer.println("				Chart item=itemProServLog" + index+ " period=h refresh=" + chartItemRefreshHour);
		if (chartItemRefreshDay != null)
			writer.println("				Chart item=itemProServLog" + index + " period=D refresh=" + chartItemRefreshDay);
		if (chartItemRefreshWeek != null)
			writer.println("				Chart item=itemProServLog" + index + " period=W refresh=" + chartItemRefreshWeek);
		if (chartItemRefreshMonth != null)
			writer.println("				Chart item=itemProServLog" + index + " period=M refresh=" + chartItemRefreshMonth);
		if (chartItemRefreshYear != null)
			writer.println("				Chart item=itemProServLog" + index + " period=Y refresh=" + chartItemRefreshYear);
		writer.println("			}}");
		
		// Hour chart
		writer.println("			Group label=\"Hour chart\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (chartItemRefreshHour != null)
			writer.println("				Chart item=itemProServLog" + index + " period=h refresh=" + chartItemRefreshHour);
		writer.println("         }");

		// Day chart
		writer.println("			Group label=\"Day chart\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (chartItemRefreshDay != null)
			writer.println("				Chart item=itemProServLog" + index + " period=D refresh=" + chartItemRefreshDay);
		writer.println("         }");

		// Week chart
		writer.println("			Group label=\"Week chart\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (chartItemRefreshWeek != null)
			writer.println("				Chart item=itemProServLog" + index + " period=W refresh=" + chartItemRefreshWeek);
		writer.println("         }");

		// Month chart
		writer.println("			Group label=\"Month chart\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (chartItemRefreshMonth != null)
			writer.println("				Chart item=itemProServLog" + index + " period=M refresh=" + chartItemRefreshMonth);
		writer.println("         }");

		// Year chart
		writer.println("			Group label=\"Year chart\" icon=\"chart\" {");
		writer.println("				Text item=itemProServLog" + index);
		if (chartItemRefreshYear != null)
			writer.println("				Chart item=itemProServLog" + index + " period=Y refresh=" + chartItemRefreshYear);
		writer.println("         }");
		writer.println("		}\n");
	}

	private void sitemapFileHelperClassic(PrintWriter writer, String indexActual, String indexPreset) {		
		
		String outdoorTempString = "";
		if (getWeatherStationOutdoorTempIsEnabled()) {
			String indexOutdoorTemp = Integer.toString(weatherStationMapId[4]);
			outdoorTempString = "         Text item=itemProServLog" + indexOutdoorTemp;
		}
		
		writer.println("      Group label=\"{MAP(proserv.map):STRING-" + indexActual + "}\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		
		// All charts
		writer.println("         Group label=\"All charts\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshHour != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh=" + chartItemRefreshHour);
		if (chartItemRefreshDay != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh=" + chartItemRefreshDay);
		if (chartItemRefreshWeek != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh=" + chartItemRefreshWeek);
		if (chartItemRefreshMonth != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh=" + chartItemRefreshMonth);
		if (chartItemRefreshYear != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh=" + chartItemRefreshYear);
		writer.println("         }");
				
		// Hour chart
		writer.println("         Group label=\"Hour chart\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshHour != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh=" + chartItemRefreshHour);
		writer.println("         }");

		// Day chart
		writer.println("         Group label=\"Day chart\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshDay != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh=" + chartItemRefreshDay);
		writer.println("         }");

		// Week chart
		writer.println("         Group label=\"Week chart\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshWeek != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh=" + chartItemRefreshWeek);
		writer.println("         }");

		// Month chart
		writer.println("         Group label=\"Month chart\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshMonth != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh=" + chartItemRefreshMonth);
		writer.println("         }");

		// Year chart
		writer.println("         Group label=\"Year chart\" icon=\"chart\" {");
		writer.println("         Text item=itemProServLog" + indexActual);
		writer.println("         Text item=itemProServLog" + indexPreset);
		if (getWeatherStationOutdoorTempIsEnabled())
			writer.println(outdoorTempString);
		if (chartItemRefreshYear != null)
			writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh=" + chartItemRefreshYear);
		writer.println("         }");
		writer.println("		}\n");	
	}
	
	public void updateProservSitemapClassicFile() {

		String filename = "proserv-classic.sitemap";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "sitemaps" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "UTF-8");
			String labelAllValues = mapProservLang.get("ALL-VALUES");
			String labelProservCharts = mapProservLang.get("PROSERV-CHARTS");
			writer.println("sitemap proserv label=\"" + labelProservCharts + "\"\n{\n   Frame {\n		Group icon=\"pie\" label=\"" + labelAllValues + "\" {");			
			writer.println("			Group item=gProserv icon=\"pie\" label=\"" + labelAllValues + "\"");
			int nofMapIdGroupsOfTen = (getNofUsedMapId() / 10);
			for (int n = 0; n <= nofMapIdGroupsOfTen; n++) {
				int m = n+1;
				writer.println("			Group item=gProserv"+ n +" icon=\"pie\" label=\"" + labelAllValues + " " + m + "\"");
			}
			writer.println("		}\n   }\n");
			
			writer.println("	Frame  icon=\"chart\" {");			
			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
						if (functionLogThis[x][y][0] && functionLogThis[x][y][1]) {
							sitemapFileHelperClassic(writer, Integer.toString(functionMapId[x][y][0]), Integer.toString(functionMapId[x][y][1]));
						} else {
							for (int z = 0; z <= 1; z++) {
								if (functionLogThis[x][y][z]) {
									sitemapFileHelperClassic(writer, Integer.toString(functionMapId[x][y][z]));
								}
							}
						}
					}
				}
			}

			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						sitemapFileHelperClassic(writer, Integer.toString(heatingMapId[x][0]), Integer.toString(heatingMapId[x][1]));
					}
				}
			}
			
			if (getWeatherStationBrigtnessEastIsEnabled()) {
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(0)));
			}
			if (getWeatherStationBrigtnessSouthIsEnabled()) {
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(1)));
			}
			if (getWeatherStationBrigtnessWestIsEnabled()) {
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(2)));
			}
			if (getWeatherStationWindSpeedIsEnabled()) {
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(3)));
			}
			if (getWeatherStationOutdoorTempIsEnabled()){
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(4)));
			}
			if (getWeatherStationRainIsEnabled())  {
				sitemapFileHelperClassic(writer, Integer.toString(getWeatherStationMapId(5)));
			}	
			writer.println("	}\n");
			
			writer.println("}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {

		}

	}
	
	
	public void updateRrd4jPersistFile() {

		String filename = "rrd4j.persist";
		allItemNames = "";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "persistence" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "ISO-8859-1");
			writer.println("Strategies {\n	everyMinute : \"0 * * * * ?\"\n}\nItems {");

			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
						for (int z = 0; z <= 1; z++) {
							if (functionLogThis[x][y][z]) {
								String index = Integer.toString(functionMapId[x][y][z]);
								String thisItem = "itemProServLog" + index + ",";
								writer.println("	" + thisItem);
								allItemNames += thisItem;
							}
						}
					}
				}
			}

			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						String indexActual = Integer.toString(heatingMapId[x][0]);
						String thisItem = "itemProServLog" + indexActual + ",";
						writer.println("	" + thisItem);
						allItemNames += thisItem;
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						thisItem = "itemProServLog" + indexPreset + ",";
						writer.println("	" + thisItem);
						allItemNames += thisItem;
					}
				}
			}

			// always create rrd files for all weather data
			for(int i=0; i<=5; i++){
				String index = Integer.toString(weatherStationMapId[i]);
				if(weatherStationMapId[i]!=0){
					String thisItem = "itemProServLog" + index + ",";
					writer.println("	" + thisItem);
					allItemNames += thisItem;
				}
			}

			String[] customItems = getCustomItems();
			for (String s: customItems)
		    {
				writer.println("	" + s + ",");
		    }			
									
			writer.println("	dummy: strategy = everyChange, everyMinute, restoreOnStartup\n}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {

		}

	}

	public void updateDb4oPersistFile() {

		String filename = "db4o.persist";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "persistence" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "ISO-8859-1");
			writer.println("Strategies {\n	everyHour 	: \"0 0 * * * ?\"\n	everyDay 	: \"0 0 0 * * ?\"\n}\nItems {");

			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
						for (int z = 0; z <= 1; z++) {
							if (functionLogThis[x][y][z]) {
								String index = Integer.toString(functionMapId[x][y][z]);
								writer.println("	itemProServLog" + index + ",");
							}
						}
					}
				}
			}

			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						String indexActual = Integer.toString(heatingMapId[x][0]);
						writer.println("	itemProServLog" + indexActual + ",");
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						writer.println("	itemProServLog" + indexPreset + ",");
					}
				}
			}

			for(int i=0; i<=5; i++){
				String index = Integer.toString(weatherStationMapId[i]);
				if(weatherStationMapId[i]!=0){
					String thisItem = "itemProServLog" + index + ",";
					writer.println("	" + thisItem);
				}
			}
						
			String[] customItems = getCustomItems();
			for (String s: customItems)
		    {
				writer.println("	" + s + ",");
		    }			
						
			writer.println("	dummy: strategy = everyDay\n}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
		}
	}
	
	public String[] getCustomItems() throws IOException{
		String fileName = ConfigDispatcher.getConfigFolder() + File.separator + "items" + File.separator + "proserv-custom.items";
		Path path = Paths.get(fileName);
	    Scanner scanner = new Scanner(path);
	    List<String> customItems = new ArrayList<String>();
	    while(scanner.hasNext()){
	        String[] tokens = scanner.nextLine().split("\\s+");
	        if(tokens.length>=2){
		        if(tokens[0].equalsIgnoreCase("Number") || tokens[0].equalsIgnoreCase("Switch")){
		        	if(tokens[1].length()>0){
		        		if(tokens[1].startsWith("itemProServLog")){
		        		    logger.error("Invalid Custom items found '{}' (itemProServLog is reserved) ", tokens[1]);	    		        			
		        		}
		        		else{
		        			customItems.add(tokens[1]);
		        		}
		        	}
		        }
	        }
	    }
	    scanner.close();
	    String[] s = new String[ customItems.size() ];
	    customItems.toArray( s );	    
	    logger.debug("Custom items found '{}'", s.toString());	    
	    return s;
	}

	public void updateSonosRulesFile(String proservIp) {

		String filename = "proserv-sonos-5z.rules";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "rules" + File.separator + filename;

			int sonosxDefinition = -1;
			for(int i=0; i<20; i++){
				if(urlSchemes[i].startsWith("#SONOSX#")){
					sonosxDefinition = i;
					break;
				}
			}
			int radioDefinition = -1;
			for(int i=0; i<20; i++){
				if(urlSchemes[i].startsWith("#RADIO#")){
					radioDefinition = i;				
					break;
				}
			}

			if(sonosxDefinition != -1 && radioDefinition != -1){
				String[] rinCons = urlSchemes[sonosxDefinition].replaceFirst("#SONOSX#", "").split("#");
				int count = 0;
				for (String s: rinCons)
			    {					
					if(s.startsWith("RINCON")){
						count++;
						String key = "var String[] RinconString" + count + " ";
						String newValue = " \"" + s + "\"";
						changeProperty(path, key, newValue );
						key = "sonos:zone_" + count + ".udn";
						writeConfigData(key, s);	
					}
			    }
				for(;count<5;){
					count++;
					String key = "var String[] RinconString" + count + " ";
					String newValue = " \"\"";
					changeProperty(path, key, newValue );										
					key = "sonos:zone_" + count + ".udn";
					writeConfigData(key, "");	
				}

				String[] radioStations = urlSchemes[radioDefinition].replaceFirst("#RADIO#", "").split("#");
				count = 0;
				for (String s: radioStations)
			    {					
					count++;
					String key = "var String[] RadioStation" + count + " ";
					String newValue = " \"" + s + "\"";
					changeProperty(path, key, newValue );						
			    }
				for(;count<5;){
					count++;
					String key = "var String[] RadioStation" + count + " ";
					String newValue = " \"\"";
					changeProperty(path, key, newValue );										
				}
				updateLine(path, "COMMENT-THIS-LINE-TO-ACTIVATE-THIS-SCRIPT", "");
				
				int altIpDefinition = -1;
				for(int i=0; i<20; i++){
					if(urlSchemes[i].startsWith("#ALTIP#")){
						altIpDefinition = i;
						break;
					}
				}
				if(altIpDefinition != -1){
					String altIp = urlSchemes[altIpDefinition].replaceFirst("#ALTIP#", "");
					writeConfigData("knx:ip", altIp);	
				}
				else{
					writeConfigData("knx:ip", proservIp);	
				}
				
			}
			
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
		}
		
	}
	
	public void updateProservRulesFile(ProservCronJobs proservCronJobs) {

		String filename = "proserv.rules";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "rules" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "US-ASCII");
			writer.println("import org.openhab.core.library.types.*\nimport org.openhab.core.persistence.*\nimport org.openhab.model.script.actions.*\n\n");
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				// rule "dpIDxx-ON" when Time cron "0 1 10 ? * 1-7" then postUpdate(dpIDxx, ON) end
				if (entry.getValue().isActive1) {
					String actionON = "ON"; // NOTE don't change this word, it is used for parsing in eventhandler
					String sON = "rule \"" + entry.getValue().dataPointID + "ON\" when Time cron \"" + entry.getValue().cron1 + "\" then postUpdate("
							+ entry.getValue().dataPointID + ", " + actionON + ") end";
					writer.println(sON);
				}
				if (entry.getValue().isActive2 && !entry.getValue().cron2.equals("")) {					
					String actionOFF = "OFF";
					String sOFF = "rule \"" + entry.getValue().dataPointID + "OFF\" when Time cron \"" + entry.getValue().cron2
							+ "\" then postUpdate(" + entry.getValue().dataPointID + ", " + actionOFF + ") end";
					writer.println(sOFF);
				}
			}
			writer.close();

		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
		}

	}

	public void updateSchedulerHtmlFile(ProservCronJobs proservCronJobs) {

		String start = "<!DOCTYPE HTML>"
				+ "\n<html>"
				+ "\n<head>"
				+ "\n<meta charset=\"ISO-8859-1\">"
				+ "\n<title>" + mapProservLang.get("PROSERVSCHEDULER") + "</title>"
				+ "\n<script type=\"text/javascript\" src=\"jquery-1.6.4.min.js\"></script>"
				+ "\n<link rel=\"stylesheet\" type=\"text/css\" href=\"jqCron.css\" />"
				+ "\n<script type=\"text/javascript\" src=\"jqCron.js\"></script>"
				+ "\n<script type=\"text/javascript\" src=\"jquery.blockUI.js\"></script>"
				+ "\n<style>"
				+ "\n* { margin: 0; padding: 0; }"
				+ "\nbody { font-family: Helvetica,Arial,sans-serif; color: #222; background-color: #ddd;line-height: 24px; }"
				+ "\nul { margin-left: 20px; }"
				+ "\nol { margin: 15px 0px 0px 20px; }"
				+ "\nol li { margin-top: 10px; }"
				+ "\nh1 { margin: 30px; font-size: 2.5em; font-weight: bold; color: #000; text-align: center; }"
				+ "\nh2 { margin: 30px 0 10px 10px; font-size: 1.3em; color: #555; }"
				+ "\nh3 { border-left: 20px solid #999; padding-left: 10px; line-height: 1.2; font-size: 1.1em; color: #333; margin: 30px 0 10px 0; }"
				+ "\np { line-height: 1.3;  margin-top: 20px; }"
				+ "\npre { line-height: 1.3; background-color: #369; color: #fff; padding: 10px; margin-top: 20px;}"
				+ "\na { color: #369; font-weight: bold; text-decoration: none; }"
				+ "\n.schedule { margin: 5px; border: 1px dashed #ccc; padding: 5px;}"
				+ "\n.schedule-inner { margin: 3px; padding: 3px;}"
				+ "\n.schedule-text { visibility:hidden; font-family: monospace; }"
				+ "\n.button-text { font-size: 1.3em; margin: 10px 10px 10px 10px; padding: 6px; font-weight: bold; text-decoration: none; }"
				+ "\n#content { margin: 0 auto;  padding: 20px 20px 40px 20px; width: 760px; background-color: #fff; border: 1px solid #777; border-width: 0 1px 0px 1px; }"
				+ "\n#footer { margin: 0 auto; padding: 20px; padding-top: 2px; width: 760px; font-size: 0.8em; text-align: center; color: #888; }"
				+ "\n</style> <script type=\"text/javascript\"> $(document).ready(function() {";
		String filename = "scheduler.html";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "webapps" + File.separator + "proserv"
					+ File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "ISO-8859-1");
			writer.println(start);
			for (Map.Entry<String, CronJob> entry : proservCronJobs.getSorted().entrySet()) {
				String dp = entry.getValue().dataPointID;
				String s = "\n$(\"#schedule" + dp
						+ "ON-frame\").block({ message: null, overlayCSS: {backgroundColor: '#000', opacity: 0.25, cursor: null},});";
				s += "\n$(\"#schedule" + dp
						+ "OFF-frame\").block({ message: null, overlayCSS: {backgroundColor: '#000', opacity: 0.25, cursor: null},});";
				writer.println(s);
				String cron = entry.getValue().cron1;
				s = "\n$('#schedule"
						+ dp
						+ "a').jqCron({enabled_minute: true,\nmultiple_dom: true,\nmultiple_month: true,\nmultiple_mins: true,\nmultiple_dow: true,\nmultiple_time_hours: "
						+ "true,\nmultiple_time_minutes: true,\nnumeric_zero_pad: true,\ndefault_period:'week',\ndefault_value: '" + cron
						+ "'.substr(1).replace(/\\?/g,\"*\"),no_reset_button: true,\nlang: '" + ProservData.language + "',\nbind_to: $('#schedule"
						+ dp + "a-val'),bind_method: { set: function($element, value) { $element.html(value); }}});";
				writer.println(s);
				cron = entry.getValue().cron2;
				s = "\n$('#schedule"
						+ dp
						+ "b').jqCron({enabled_minute: true,\nmultiple_dom: true,\nmultiple_month: true,\nmultiple_mins: true,\nmultiple_dow: true,\nmultiple_time_hours: "
						+ "true,\nmultiple_time_minutes: true,\nnumeric_zero_pad: true,\ndefault_period:'week',\ndefault_value: '" + cron
						+ "'.substr(1).replace(/\\?/g,\"*\"),no_reset_button: true,\nlang: '" + ProservData.language + "',bind_to: $('#schedule" + dp
						+ "b-val'),bind_method: { set: function($element, value) { $element.html(value); }}});";
				writer.println(s);
				s = "\n$(\"#" + dp + "ON-enabled\").change(function() {\nif($(this).is(\":checked\")) {$(\"#schedule" + dp
						+ "ON-frame\").unblock(); }\nelse {$(\"#schedule" + dp
						+ "ON-frame\").block({ message: null,\n overlayCSS: {backgroundColor: '#000',\n opacity: 0.25,\n cursor: null},\n}); }});";
				s += "\n$(\"#" + dp + "OFF-enabled\").change(function() {\nif($(this).is(\":checked\")) {$(\"#schedule" + dp
						+ "OFF-frame\").unblock(); }\nelse {$(\"#schedule" + dp
						+ "OFF-frame\").block({ message: null,\n overlayCSS: {backgroundColor: '#000',\n opacity: 0.25,\n cursor: null},\n}); }});";
				writer.println(s);
				if (entry.getValue().isActive1) {
					writer.println("$(\"#" + dp + "ON-enabled\").click();");
				}
				if (entry.getValue().isActive2) {
					writer.println("$(\"#" + dp + "OFF-enabled\").click();");
				}
			}
			{
				String s = "$('#save-all').click(function(){var current_value=";
				for (Map.Entry<String, CronJob> entry : proservCronJobs.getSorted().entrySet()) {
					// dpIDxx:true:0 0 8 ? * 2-6:true:0 0 21 ? * 1,7;dpIDyy:true:0 0 8 ? * 2-6:false:0 0 21 ? * 1,7;
					String dp = entry.getValue().dataPointID;
					String dpType = Integer.toString(entry.getValue().scheduleType);
					String active1 = "\"+($('#" + dp + "ON-enabled').is(':checked')?'true':'false')+\"";
					String active2 = "\"+($('#" + dp + "OFF-enabled').is(':checked')?'true':'false')+\"";
					s += "\"" + dp + ":" + dpType + ":" + active1 + ":\"" + "+$('#schedule" + dp + "a-val').text()+\":" + active2 + ":\" +  $('#schedule" + dp + "b-val').text() + " + "\n" + " \";\"+";
				}
				s += "\"\";";
				writer.println(s);
				s = "/*alert(current_value);*/$.ajax({type:\"GET\",url:\"/CMD?ProservCronJobs=\"+current_value,success:function(){alert(\""
						+ mapProservLang.get("SAVECONFRIM") + "\");},error:function(){alert(\"" + mapProservLang.get("SAVEFAILED") + "\");}});});";
//				s = "alert(current_value);Ajax.request({timeout : 5000,async:   false, type:\"GET\",url:\"/CMD?ProservCronJobs=\"+current_value,success:function(response){\nalert(response.toLowerCase());\n alert($('#theDiv').html(response));\nif (response.toLowerCase() == 'success'){alert(\""
//						+ mapProservLang.get("SAVECONFRIM") + "\");}else {alert(\"" + mapProservLang.get("SAVEFAILED") + "\");}},error:function(){alert(\"" + mapProservLang.get("SAVEFAILED") + "\");}});});";
				writer.println(s);
			}
			writer.println("});");
			writer.println("</script></head><body><div id=\"content\"><h1>" + mapProservLang.get("PROSERVSCHEDULER") + "</h1>");
			writer.println("<button class='cron-period button-text' id='save-all'>" + mapProservLang.get("SAVEALLCHANGES") + "</button>");
			String hiddenCronJobStrings = new String();
			for (Map.Entry<String, CronJob> entry : proservCronJobs.getSorted().entrySet()) {
				String dp = entry.getValue().dataPointID;
				String zone = entry.getValue().zoneName;
				String name = entry.getValue().dataPointName;
				String sActionOn;
				String sActionOff = "";
				if (entry.getValue().scheduleType == 1) {
					sActionOn = mapProservLang.get("COMFORT");
					sActionOff = mapProservLang.get("NIGHT");
				} else if (entry.getValue().scheduleType == 2) {
					sActionOn = mapProservLang.get("COMFORT");
					sActionOff = mapProservLang.get("NIGHT");
				} else if (entry.getValue().scheduleType == 3) {
					sActionOn = mapProservLang.get("ACTIVATE");
				} else if (entry.getValue().scheduleType == 4) {
					sActionOn = mapProservLang.get("DOWN");
					sActionOff = mapProservLang.get("UP");
				} else if (entry.getValue().scheduleType == 5) {
					sActionOn = mapProservLang.get("CLOSE");
					sActionOff = mapProservLang.get("OPEN");
				} else if (entry.getValue().scheduleType == 6) {
					sActionOn = mapProservLang.get("OUT");
					sActionOff = mapProservLang.get("IN");
				} else {// if(entry.getValue().scheduleType == 0){
					sActionOn = mapProservLang.get("ON");
					sActionOff = mapProservLang.get("OFF");
				}
				String s1, s2 = "", s3; 
				s1 = "<h2 id='intro'>" + zone + " /// " + name + "</h2>\n" + "<div class='schedule'>"
						+ "\n<table style='width: 100%'><tr><td style='width: 18%'><a><div ><input style='width: 20px' type='checkbox' name=\"" + dp
						+ "ON-enabled\" id=\"" + dp + "ON-enabled\" />" + sActionOn + " :</div></a></td><td><div class='schedule-inner' id='schedule" + dp
						+ "ON-frame'><div id='schedule" + dp
						+ "a'></div></td></tr></table>";
				if(!entry.getValue().cron2.equals("")){
					s2 = "\n<table style='width: 100%'><tr><td style='width: 18%'><a><div ><input style='width: 20px' type='checkbox' name=\"" + dp
							+ "OFF-enabled\" id=\"" + dp + "OFF-enabled\" />" + sActionOff + " :</div></a></td><td><div class='schedule-inner' id='schedule" + dp
							+ "OFF-frame'><div id='schedule" + dp
							+ "b'></div></td></tr></table>";
				}
				s3 = "</div>";
				writer.println(s1 + s2 + s3);
				hiddenCronJobStrings += "<p class='schedule-text'>Generated cron entries: <span class='schedule-text' id='schedule" + dp
						+ "a-val'>  </span><span class='schedule-text' id='schedule" + dp + "b-val'></span></p>\n";
			}

			writer.println("</div>\n" + hiddenCronJobStrings + "</body></html>");
			writer.close();

		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
		}

	}
	

	public static void updateLangDirJsFile(String language) {
		String filename = "langdir.js";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "webapps" + File.separator + "proserv"
					+ File.separator + filename;
	
			PrintWriter writer = new PrintWriter(path, "US-ASCII");
			writer.println("var langCode='" + language + "';");
			writer.println("var langcodes=['en','fr','de'];var lang=langCode.toLowerCase();lang=lang.substr(0,2);var dest=window.location.origin+'/proserv/index.html';for(i=langcodes.length-1;i>=0;i--){if(lang==langcodes[i]){dest=dest.substr(0,dest.lastIndexOf('.'))+'-'+lang.substr(0,2)+dest.substr(dest.lastIndexOf('.'));window.location.replace?window.location.replace(dest):window.location=dest;}}");
			writer.close();

		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {
		}
	
	}
	
}
