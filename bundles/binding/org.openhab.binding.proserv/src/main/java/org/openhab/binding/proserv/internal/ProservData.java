package org.openhab.binding.proserv.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

	private byte weatherStationCode = 0x71;
	private boolean weatherStationLogThis = true;
	private int weatherStationMapId;	
	private int weatherStationDataPoint;	

	private Map<String, String> mapProservLang = new HashMap<String, String>();
	private String allItemNames;
	public boolean refresh = false;

	
	public ProservData(String chartItemRefreshHour, String chartItemRefreshDay,
			String chartItemRefreshWeek, String chartItemRefreshMonth,
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
	public String getZoneName(int x){
		return zoneNames[x];
	}
	public byte getFunctionCodes(int x, int y) {
		return functionCodes[x][y];
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
	public int getFunctionDataPoint( int x, int y, int i)
	{
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
	
	public void setFunctionDataPoint(int dataPoint, int x, int y, int i)
	{
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
	
	public int getHeatingDataPoint( int x, int i)
	{
		return heatingDataPoint[x][i];
	}	
	public void setHeatingDataPoint(int dataPoint, int x, int i)
	{
		heatingDataPoint[x][i] = dataPoint;
	}

	public boolean getWeatherStationLogThis()
	{
		return weatherStationLogThis;
	}	
	public byte getWeatherStationCode()
	{
		return weatherStationCode;
	}
	public int getWeatherStationMapId(){
		return weatherStationMapId;
	}
	public int getWeatherStationDataPoint()
	{
		return weatherStationDataPoint;
	}
	public void setWeatherStationDataPoint(int dataPoint)
	{
		weatherStationDataPoint = dataPoint;
	}
	
	public void parseRawConfigData(byte[] proServAllValues) throws UnsupportedEncodingException
	{
	    int lengthDescription = 24;
	    int lengthUnit = 5;

	    // zone 1-18
		int startFunctionOffset = 0;
		for(int x=0;x<18;x++)
	    {
        	try {
	        	int offset = startFunctionOffset + 25*x;
	            int actualLength = 0;
	            for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset+1+actualLength] != 0; actualLength++) { }
				if(actualLength>0)
					zoneNames[x] = (new String(proServAllValues, offset+1, actualLength, "ISO-8859-1"));
				else
					zoneNames[x] = new String();			
        	} catch (NullPointerException e) {
	 			logger.warn("proserv NullPointerException zonenames");
	 		} finally {
        	}
	    }
		
	    // function 1-1 .. function 18-16
		startFunctionOffset = 512;
		for(int x=0;x<18;x++)
	    {
	        for(int y=0;y<16;y++)
	        {
	        	try {
		        	int offset = startFunctionOffset + 512*x + 32*y;
		            functionCodes[x][y] = proServAllValues[offset];
		            int actualLength = 0;
		            for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset+1+actualLength] != 0; actualLength++) { }
					if(actualLength>0)
						functionDescriptions[x][y] = (new String(proServAllValues, offset+1, actualLength, "ISO-8859-1"));
					else
						functionDescriptions[x][y] = new String();

		            for (actualLength = 0; actualLength < lengthUnit && proServAllValues[offset+25+actualLength] != 0; actualLength++) { }
					if(actualLength>0){ 
						functionUnits[x][y] = (new String(proServAllValues, offset+25, actualLength, "ISO-8859-1"));						
					}
					else {
						if( (functionDefs[x][y] & 0xFF) == 0x31 || (functionDefs[x][y] & 0xFF) == 0x91 )
							functionUnits[x][y] = "%";
						else
							functionUnits[x][y] = new String();
					}
		            
		            functionProfiles[x][y] = proServAllValues[offset+30];
		            functionDefs[x][y] = proServAllValues[offset+31];
		            if(functionDescriptions[x][y].contains("#m"))
		            {
		            	if( ((int)functionCodes[x][y] & 0xFF)==0x31 )
		            	{
		            		functionIsEmailTrigger[x][y] = true;
		            	}
		            }		            
//if(functionDescriptions[x][y].contains("#l"))
//{
//functionIsTimer[x][y] = true;
//}		            
		            if(functionDescriptions[x][y].contains("#t"))
		            {
		            	if( ((int)functionCodes[x][y] & 0xFF)==0x31 )
		            	{
		            		functionIsTimer[x][y] = true;
		            	}
		            }		            
          
		            if(functionDescriptions[x][y].contains("#l"))
		            {
		            	if( ((int)functionCodes[x][y] & 0xFF)>=0x91 && ((int)functionCodes[x][y] & 0xFF)<=0x97 )
		            	{		            	
							if( (functionDefs[x][y] & 0x30) >= 0x10) // 'y' is 01, 10, 11 : show actual value
								functionLogThis[x][y][0] = true;
							if( (functionDefs[x][y] & 0x1) == 1) // 'x' is 01, 11 : show preset value
								functionLogThis[x][y][1] = true;
		            	}
		            	else
		            	{
		            		functionLogThis[x][y][0]=true;
		            	}

		            	if( ((int)functionCodes[x][y] & 0xFF)==0x31 )
		            	{	//State - 1 bit value	            	
							if( (functionDefs[x][y] & 0x1) == 0) // x) 1=high activ; 0=low activ
								functionStateIsInverted[x][y] = true;
		            	}
		            	
						functionDescriptions[x][y] = functionDescriptions[x][y].substring(0, functionDescriptions[x][y].indexOf("#"));	
						logger.debug("-----x{}y{}  offset:{}  {}  code:0x{}  log actual:{}, log preset:{}  StateIsInverted:{}",x, y, offset, 
								functionDescriptions[x][y], Integer.toHexString((int)functionCodes[x][y] & 0xFF), 
								functionLogThis[x][y][0], functionLogThis[x][y][1], functionStateIsInverted[x][y]); 
		            }     
	        	} catch (NullPointerException e) {
		 			logger.warn("proserv NullPointerException functions");
		 		} finally {

	        	}
	        	
	        }
	    }
	    
	    // heating 1-18
	    int startHeatingOffset = 9728;
	    for(int x=0;x<18;x++)
	    {
	    	try{
	        	int offset = startHeatingOffset+32*x;
	            heatingCodes[x] = proServAllValues[offset];
	            
	            int actualLength = 0;
	            for (actualLength = 0; actualLength < lengthDescription && proServAllValues[offset+1+actualLength] != 0; actualLength++) { }
				if(actualLength>0)
					heatingDescriptions[x] = (new String(proServAllValues, offset+1, actualLength, "ISO-8859-1"));
				else
					heatingDescriptions[x] = new String();
	            
	            heatingProfiles[x] = proServAllValues[offset+30];
	            heatingDefs[x] = proServAllValues[offset+31];
	            
	            if(heatingDescriptions[x].contains("#l"))
	            {
	            	heatingLogThis[x]=true;
	            	heatingDescriptions[x] = heatingDescriptions[x].substring(0, heatingDescriptions[x].indexOf("#l"));
	            	int startDatapoint = 865 + x * 5;
					logger.debug("-----x:{}  offset:{} {}   code:0x{}  log actual:{}  startDatapoint{}:",x, offset, 
							heatingDescriptions[x], Integer.toHexString((int)heatingCodes[x] & 0xFF), heatingLogThis[x], startDatapoint);
	            } 
        	} catch (NullPointerException e) {
	 			logger.warn("proserv NullPointerException zonenames");
	 		} finally {
        	}
	    }
	    
		// weatherStation
		// int weatherStationOffset = 16065;	
	    // for now use hardcoded values, see declaration. There's no description field for weather station....
	    
	}


	public float parse2ByteFloatValue(byte[] dataValue, int index) {
		float dataFloat = 0;
		try {
			DPTXlator2ByteFloat dPTXlator2ByteFloat = new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_HUMIDITY);
			dPTXlator2ByteFloat.setData(dataValue,index);
			dataFloat = dPTXlator2ByteFloat.getValueFloat();

		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		}
		finally {
		}
		return dataFloat;
	}
		
	public float parse4ByteFloatValue(byte[] dataValue, int index) {
		float dataFloat = 0;
		try {
			DPTXlator4ByteFloat dPTXlator4ByteFloat = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_ELECTRIC_CHARGE);
			dPTXlator4ByteFloat.setData(dataValue,index);
			dataFloat = dPTXlator4ByteFloat.getValueFloat();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		}
		finally {
		}
		return dataFloat;
	}
		
	public boolean parse1ByteBooleanValue(byte dataValue) {
		return (dataValue!=0);
	}

	public int parse1BytePercentValue( byte dataValue) {
		return ( (((int)dataValue & 0xFF)+1) * 100 / 255);
	}

	public int parse1ByteUnsignedValue(byte dataValue) {
		return (int)dataValue & 0xFF;
	}
	
	public long parse4ByteUnsignedValue(byte[] dataValue, int index) {
		long dataUnsigned= 0;
		try {
			DPTXlator4ByteUnsigned dPTXlator4ByteUnsigned = new DPTXlator4ByteUnsigned(DPTXlator4ByteUnsigned.DPT_VALUE_4_UCOUNT);
			dPTXlator4ByteUnsigned.setData(dataValue,index);
			dataUnsigned = dPTXlator4ByteUnsigned.getValueUnsigned();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		}
		finally {
		}
		return dataUnsigned;		
	}
	
	public long parse4ByteSignedValue(byte[] dataValue, int index) {
		long dataSigned= 0;
		try {
			DPTXlator4ByteSigned dPTXlator4ByteSigned = new DPTXlator4ByteSigned(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY);
			dPTXlator4ByteSigned.setData(dataValue,index);
			dataSigned = dPTXlator4ByteSigned.getValueSigned();
		} catch (KNXFormatException e) {
			logger.warn("KNXFormatException [dataValue'{}'] error:'{}'", dataValue, e);
		}
		finally {
		}
		return dataSigned;		
	}
	
	private void updateMapIds() {
		int mapId = 0;
		for(int x=0;x<18;x++)
	    {
	        for(int y=0;y<16;y++)
	        {
	            if(functionLogThis[x][y][0])
	            {
	            	functionMapId[x][y][0]=mapId++;
	            }
	            if(functionLogThis[x][y][1])
	            {
	            	functionMapId[x][y][1]=mapId++;
	            }
	    	}
	    }
	    for(int x=0;x<18;x++)
	    {
            if(heatingLogThis[x])
            {
            	heatingMapId[x][0]=mapId++;
            	heatingMapId[x][1]=mapId++;
            }
	    }
	    if(weatherStationLogThis){
	    	weatherStationMapId = mapId++;
	    }
	}

	private static void changeProperty(String filename, String key, String value) throws IOException {
	    final File tmpFile = new File(filename + ".tmp");
	    final File file = new File(filename);
	    PrintWriter pw = new PrintWriter(tmpFile);
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    boolean found = false;
	    final String toAdd = key + '=' + value;
	    for (String line; (line = br.readLine()) != null; ) {
	        if (line.startsWith(key + '=')) {
	            line = toAdd;
	            found = true;
	        }
	        pw.println(line);
	    }
	    if (!found)
	        pw.println(toAdd);
	    br.close();
	    pw.close();
	    Files.move(tmpFile.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING );
	}
	
	public static boolean writeConfigData(String key, String value){
		String filename = "openhab.cfg";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + filename ;
			changeProperty(path, key, value);
			return true;
		} catch (Throwable e) {
			String message = "writeConfigData value: " + value + " key: " + key + " file: " + filename + " throws exception" + e.toString();
			logger.error(message, e);
		} 
		return false;
	}
	
	public void loadProservLang() {
		Reader reader = null;
		String filename = ProservData.language + ".map";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "transform" + File.separator + filename ;
			Properties properties = new Properties();
			reader = new FileReader(path);
			properties.load(reader);
						
			mapProservLang.put("ACTUAL", properties.getProperty("ACTUAL"));
			mapProservLang.put("PRESET", properties.getProperty("PRESET"));
			mapProservLang.put("SCROLL-FOR-MORE-CHARTS", properties.getProperty("SCROLL-FOR-MORE-CHARTS"));
			mapProservLang.put("PROSERV-CHARTS", properties.getProperty("PROSERV-CHARTS"));
			mapProservLang.put("ALL-VALUES", properties.getProperty("ALL-VALUES"));
			mapProservLang.put("PROSERV-CHARTS", properties.getProperty("PROSERV-CHARTS"));
			mapProservLang.put("TEMPERATURE", properties.getProperty("TEMPERATURE"));
			mapProservLang.put("OUTSIDE", properties.getProperty("OUTSIDE"));
			mapProservLang.put("SAVEALLCHANGES", properties.getProperty("SAVEALLCHANGES"));
			mapProservLang.put("PROSERVSCHEDULER", properties.getProperty("PROSERVSCHEDULER"));
			mapProservLang.put("ON", properties.getProperty("ON"));
			mapProservLang.put("OFF", properties.getProperty("OFF"));
			mapProservLang.put("SAVECONFRIM", properties.getProperty("SAVECONFRIM"));
			mapProservLang.put("SAVEFAILED", properties.getProperty("SAVEFAILED"));


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
							if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()) {
								properties.setProperty(key, functionDescriptions[x][y]);
							}
						}
					}
				}
			}
			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					String key0 = "STRING-" + Integer.toString(heatingMapId[x][0]);
					String key1 = "STRING-" + Integer.toString(heatingMapId[x][1]);
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()) {
						properties.setProperty(key0, heatingDescriptions[x]);
						properties.setProperty(key1, heatingDescriptions[x]);
					}
				}
			}
			if (weatherStationLogThis) {
				String key0 = "STRING-" + Integer.toString(weatherStationMapId);
				properties.setProperty(key0, mapProservLang.get("TEMPERATURE"));
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

			PrintWriter writer = new PrintWriter(path, "ISO-8859-1");
			writer.println("Group gProserv");
			writer.println("");
			
			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()){
						if (functionLogThis[x][y][0] && functionLogThis[x][y][1]) {
							String indexActual = Integer.toString(functionMapId[x][y][0]);
							String indexPreset = Integer.toString(functionMapId[x][y][1]);
							writer.println("Group gitemProServLog" + indexActual + indexPreset);
							String item0 = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + indexActual + 
									"   \"{MAP(proserv.map):STRING-" + indexActual + 
									"} " + mapProservLang.get("ACTUAL") + " " + getFormatString(functionCodes[x][y], functionUnits[x][y]) + 
									"\" <none> (gProserv, gitemProServLog" + indexActual + indexPreset + ")";
							writer.println(item0);
							String item1 = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + indexPreset + 
									"   \"{MAP(proserv.map):STRING-" + indexPreset + 
									"} " + mapProservLang.get("PRESET") + " " + getFormatString(functionCodes[x][y], functionUnits[x][y]) + 
									"\" <none> (gProserv, gitemProServLog" + indexActual + indexPreset + ")";
							writer.println(item1);
						}
						else { 
							for (int z = 0; z <=1; z++) {
								if (functionLogThis[x][y][z]) {
									String index = Integer.toString(functionMapId[x][y][z]);
									String actualOrPreset = (z==0?mapProservLang.get("ACTUAL"):mapProservLang.get("ACTUAL"));
									String item = getDataTypeString(functionCodes[x][y]) + " itemProServLog" + index + 
											"   \"{MAP(proserv.map):STRING-" + index + "} " + actualOrPreset + " " + 
											getFormatString(functionCodes[x][y], functionUnits[x][y]) + "\" <none> (gProserv)";
									writer.println(item);
								}
							}
						}
					}
				}
			}
			for (int x = 0; x < 18; x++) {
				if (heatingLogThis[x]) {
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()){
						String indexActual = Integer.toString(heatingMapId[x][0]);
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						writer.println("Group gitemProServLog" + indexActual + indexPreset);
						String item0 = "Number itemProServLog" + indexActual + 
								"   \"{MAP(proserv.map):STRING-" + indexActual + 
								"} " + mapProservLang.get("ACTUAL") + " " + getFormatString(heatingCodes[x], "°C") + "\" <none> (gProserv, gitemProServLog" 
								+ indexActual + indexPreset + ")";
						writer.println(item0);
						String item1 = "Number itemProServLog" + indexPreset + 
								"   \"{MAP(proserv.map):STRING-" + indexPreset + 
								"} " + mapProservLang.get("PRESET") + " " + getFormatString(heatingCodes[x], "°C") + "\" <none> (gProserv, gitemProServLog" 
								+ indexActual + indexPreset +  ")";
						writer.println(item1);
						if (weatherStationLogThis) {
							String indexOutdoorTemp = Integer.toString(weatherStationMapId);
							String item2 = "Number itemProServLog" + indexOutdoorTemp + 
									"   \"{MAP(proserv.map):STRING-" + indexOutdoorTemp + 
									"} " + mapProservLang.get("OUTSIDE") + " " + getFormatString(weatherStationCode, "°C") + "\" <none> (gProserv, gitemProServLog" 
									+ indexActual + indexPreset +  ")";
							writer.println(item2);
						}
					}
				}
			}
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				//Switch proServTimer0  "proServTimer0" { proserv="proserv_timer" }
				//if(entry.getValue().isActive){
					String s = "Switch " + entry.getValue().dataPointID + " \"" + entry.getValue().dataPointID + "\" { proserv=\"proserv_timer\" }";
					writer.println(s);
				//}
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
		switch (functionCode & 0xFF){
		case 0x1:
		case 0x31:{
			dataTypeString = "Switch";
		} break;		
		}
		return dataTypeString;
	}	
	
	
	private String getFormatString(int functionCode, String unit) {
		unit = unit.replace("%", "%%");
		unit = unit.replace("°", "Â°"); // ugly fix or it won't show up as a °
		//unit = unit.replace("Unit", "");
		if(!unit.isEmpty())
			unit = " " + unit;
		String formatString = new String();
		formatString = "[%d" + unit +"]";
		switch (functionCode & 0xFF){
		case 0x32:
		case 0x91:{
			formatString = "[%.1f" + unit +"]";
		} break;		
		case 0x34:
		case 0x36:
		case 0x94:
		case 0x97:
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
		case 0x71:{
			formatString = "[%.2f" + unit +"]";
		} break;			
		}
		return formatString;
	}
	public void updateProservSitemapFile() {

		String filename = "proserv.sitemap";
		try {			
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "sitemaps" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "ISO-8859-1");
			String labelAllValues = 	mapProservLang.get("ALL-VALUES");
			String labelProservCharts = mapProservLang.get("PROSERV-CHARTS");
			writer.println("sitemap proserv label=\"" + labelProservCharts + "\"\n{\n   Frame {\n\n		Group item=gProserv icon=\"pie\" label=\"" + labelAllValues + "\"\n   }\n");			
		
			for (int x = 0; x < 18; x++) {
				for (int y = 0; y < 16; y++) {
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()){
						if (functionLogThis[x][y][0] && functionLogThis[x][y][1]) {
							String indexActual = Integer.toString(functionMapId[x][y][0]);
							String indexPreset = Integer.toString(functionMapId[x][y][1]);
							writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + indexActual + "}\" icon=\"chart\" {");
							writer.println("         Text item=itemProServLog" + indexActual);
							writer.println("         Text item=itemProServLog" + indexPreset);
							//writer.println("         Frame label=\"Scroll down for different periods (Hours,Day,Week,Month)\"{");
							if(!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()){
								writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
							} else {
								writer.println("         Frame {");
							}
							if(chartItemRefreshHour!=null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh=" + chartItemRefreshHour);
							if(chartItemRefreshDay!=null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh=" + chartItemRefreshDay);
							if(chartItemRefreshWeek!=null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh=" + chartItemRefreshWeek);
							if(chartItemRefreshMonth!=null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh=" + chartItemRefreshMonth);
							if(chartItemRefreshYear!=null)
								writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh=" + chartItemRefreshYear);
							writer.println("        }\n      }\n   }");
						}
						else {
							for (int z = 0; z <=1; z++) {
								if (functionLogThis[x][y][z]) {
									String index = Integer.toString(functionMapId[x][y][z]);
									writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + index + "}\" icon=\"chart\" {");
									writer.println("         Text item=itemProServLog" + index);
									//writer.println("         Frame label=\"Scroll down for different periods (Hours,Day,Week,Month)\"{");
									if(!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()){
										writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
									} else {
										writer.println("         Frame {");
									}
									if(chartItemRefreshHour!=null)
										writer.println("			Chart item=itemProServLog" + index + " period=h refresh=" + chartItemRefreshHour);
									if(chartItemRefreshDay!=null)
										writer.println("			Chart item=itemProServLog" + index + " period=D refresh=" + chartItemRefreshDay);
									if(chartItemRefreshWeek!=null)
										writer.println("			Chart item=itemProServLog" + index + " period=W refresh=" + chartItemRefreshWeek);
									if(chartItemRefreshMonth!=null)
										writer.println("			Chart item=itemProServLog" + index + " period=M refresh=" + chartItemRefreshMonth);
									if(chartItemRefreshYear!=null)
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
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()){
						String indexActual = Integer.toString(heatingMapId[x][0]);
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						writer.println("	Frame {\n      Text label=\"{MAP(proserv.map):STRING-" + indexActual + "}\" icon=\"chart\" {");
						writer.println("         Text item=itemProServLog" + indexActual);
						writer.println("         Text item=itemProServLog" + indexPreset);
						if (weatherStationLogThis) {
							String indexOutdoorTemp = Integer.toString(weatherStationMapId);
							writer.println("         Text item=itemProServLog" + indexOutdoorTemp);
						}						
						if(!mapProservLang.get("SCROLL-FOR-MORE-CHARTS").isEmpty()){
							writer.println("         Frame label=\"" + mapProservLang.get("SCROLL-FOR-MORE-CHARTS") + "\"{");
						} else {
							writer.println("         Frame {");
						}
						if(chartItemRefreshHour!=null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=h refresh=" + chartItemRefreshHour);
						if(chartItemRefreshDay!=null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=D refresh=" + chartItemRefreshDay);
						if(chartItemRefreshWeek!=null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=W refresh=" + chartItemRefreshWeek);
						if(chartItemRefreshMonth!=null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=M refresh=" + chartItemRefreshMonth);
						if(chartItemRefreshYear!=null)
							writer.println("			Chart item=gitemProServLog" + indexActual + indexPreset + " period=Y refresh=" + chartItemRefreshYear);
						writer.println("        }\n      }\n   }");
					}
				}
			}				
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
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()){
						for (int z = 0; z <=1; z++) {
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
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()){
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

			if (weatherStationLogThis) {
				String indexOutdoorTemp = Integer.toString(weatherStationMapId);
				String thisItem = "itemProServLog" + indexOutdoorTemp + ",";
				writer.println("	" + thisItem);
				allItemNames += thisItem;
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
					if (functionDescriptions[x][y] != null && !functionDescriptions[x][y].isEmpty()){
						for (int z = 0; z <=1; z++) {
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
					if (heatingDescriptions[x] != null && !heatingDescriptions[x].isEmpty()){
						String indexActual = Integer.toString(heatingMapId[x][0]);
						writer.println("	itemProServLog" + indexActual + ",");
						String indexPreset = Integer.toString(heatingMapId[x][1]);
						writer.println("	itemProServLog" + indexPreset + ",");
					}
				}
			}			
			
			if (weatherStationLogThis) {
				String indexOutdoorTemp = Integer.toString(weatherStationMapId);
				writer.println("	itemProServLog" + indexOutdoorTemp + ",");				
			}
			
			writer.println("	dummy: strategy = everyDay\n}");
			writer.close();
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {}
	}

	public void updateProservRulesFile(ProservCronJobs proservCronJobs) {

		String filename = "proserv.rules";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + "rules" + File.separator + filename;

			PrintWriter writer = new PrintWriter(path, "US-ASCII");
			writer.println("import org.openhab.core.library.types.*\nimport org.openhab.core.persistence.*\nimport org.openhab.model.script.actions.*\n\n");
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				//rule "DataPoint0-ON" when Time cron "0 1 10 ? * 1-7" then postUpdate(dataPointIDxx, ON) end
				if(entry.getValue().isActive){
					String actionON = "ON";
					String sON = "rule \"" + entry.getValue().dataPointID + "ON\" when Time cron \"" + 
							entry.getValue().cron1 + "\" then postUpdate(" + entry.getValue().dataPointID + ", " + actionON +") end";
					writer.println(sON);
					String actionOFF = "OFF";
					String sOFF = "rule \"" + entry.getValue().dataPointID + "OFF\" when Time cron \"" + 
							entry.getValue().cron2 + "\" then postUpdate(" + entry.getValue().dataPointID + ", " + actionOFF +") end";
					writer.println(sOFF);
				}
			}
			writer.close();
			
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {}	
		
	}
	
	public void updateSchedulerHtmlFile(ProservCronJobs proservCronJobs) {

		String start = "<!DOCTYPE HTML>"
				+ "\n<html>"
				+ "\n<head>"
				+ "\n<meta charset=\"utf-8\">"
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
				+ "\nh2 { /*border-bottom: 1px solid #999;*/ margin: 30px 0 10px 0; font-size: 1.3em; color: #555; }"
				+ "\nh3 { border-left: 20px solid #999; padding-left: 10px; line-height: 1.2; font-size: 1.1em; color: #333; margin: 30px 0 10px 0; }"
				+ "\np { line-height: 1.3;  margin-top: 20px; }"
				+ "\npre { line-height: 1.3; background-color: #369; color: #fff; padding: 10px; margin-top: 20px;}"
				+ "\na { color: #369; font-weight: bold; text-decoration: none; }"
				+ "\n.schedule { margin: 10px; border: 1px dashed #ccc; padding: 10px;}"
				+ "\n.schedule-text { visibility:hidden; font-family: monospace; }"
				+ "\n.button-text { font-size: 1.3em; margin: 10px 10px 10px 10px; padding: 6px; font-weight: bold; text-decoration: none; }"
				+ "\n#content { margin: 0 auto;  padding: 20px 20px 40px 20px; width: 760px; background-color: #fff; border: 1px solid #777; border-width: 0 1px 0px 1px; }"
				+ "\n#footer { margin: 0 auto; padding: 20px; padding-top: 2px; width: 760px; font-size: 0.8em; text-align: center; color: #888; }"
				+ "\n</style> <script type=\"text/javascript\"> $(document).ready(function() {";
		String filename = "scheduler.html";
		try {
			String path = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "webapps"  + File.separator + "proserv" + File.separator +  filename;

			PrintWriter writer = new PrintWriter(path, "US-ASCII");
			writer.println(start);
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				String dp = entry.getValue().dataPointID;
			    //$("#schedule1-frame").block({ message: null, overlayCSS: {backgroundColor: '#000', opacity: 0.25, cursor: null},}); 
				String s = "\n$(\"#schedule" + dp + "-frame\").block({ message: null, overlayCSS: {backgroundColor: '#000', opacity: 0.25, cursor: null},});";
				writer.println(s);
				String cron = entry.getValue().cron1;
				s = "\n$('#schedule" + dp + "a').jqCron({enabled_minute: true,\nmultiple_dom: true,\nmultiple_month: true,\nmultiple_mins: true,\nmultiple_dow: true,\nmultiple_time_hours: " + 
				"true,\nmultiple_time_minutes: true,\nnumeric_zero_pad: true,\ndefault_period:'week',\ndefault_value: '" + cron + "'.substr(1).replace(/\\?/g,\"*\"),no_reset_button: true,\nlang: '" 
				+ ProservData.language + "',\nbind_to: $('#schedule" + dp + "a-val'),bind_method: { set: function($element, value) { $element.html(value); }}});";
				writer.println(s);
				cron = entry.getValue().cron2;
				s = "\n$('#schedule" + dp + "b').jqCron({enabled_minute: true,\nmultiple_dom: true,\nmultiple_month: true,\nmultiple_mins: true,\nmultiple_dow: true,\nmultiple_time_hours: " + 
				"true,\nmultiple_time_minutes: true,\nnumeric_zero_pad: true,\ndefault_period:'week',\ndefault_value: '" + cron + "'.substr(1).replace(/\\?/g,\"*\"),no_reset_button: true,\nlang: '" 
				+ ProservData.language + "',bind_to: $('#schedule" + dp + "b-val'),bind_method: { set: function($element, value) { $element.html(value); }}});";
				writer.println(s);
				s = "\n$(\"#" + dp + "-enabled\").change(function() {\nif($(this).is(\":checked\")) {$(\"#schedule" + dp + "-frame\").unblock(); }\nelse {$(\"#schedule" + dp +
						"-frame\").block({ message: null,\n overlayCSS: {backgroundColor: '#000',\n opacity: 0.25,\n cursor: null},\n}); }});"; 
				writer.println(s);
				if(entry.getValue().isActive){
					writer.println("$(\"#" + dp + "-enabled\").click();");
				}
			}
			{
				String s = "$('#save-all').click(function(){var current_value=";
				for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
					//dataPointxx:true:0:0 0 8 ? * 2-6:0 0 21 ? * 1,7;DPyy:true:1:0 0 8 ? * 2-6:0 0 21 ? * 1,7;
					String dp = entry.getValue().dataPointID;
 					String active = "\"+($('#" + dp + "-enabled').is(':checked')?'true':'false')+\"";
					s += "\"" + dp + ":" + active + ":0:\"" + "+$('#schedule" + dp + "a-val').text() + \":\" +  $('#schedule" + dp + "b-val').text() + "+"\n"+" \";\"+";
				}
				s += "\"\";";
				writer.println(s);
				s = "/*alert(current_value);*/$.ajax({type:\"GET\",url:\"/CMD?ProservCronJobs=\"+current_value,success:function(){alert(\"" + mapProservLang.get("SAVECONFRIM") + "\");},error:function(){alert(\"" + mapProservLang.get("SAVEFAILED") + "\");}});});";
				writer.println(s);
			}
			writer.println("});");
			writer.println("</script></head><body><div id=\"content\"><h1>" + mapProservLang.get("PROSERVSCHEDULER") + "</h1>");
			writer.println("<button class='cron-period button-text' id='save-all'>" + mapProservLang.get("SAVEALLCHANGES") + "</button>");
			String hiddenCronJobStrings = new String(); 
			for (Map.Entry<String, CronJob> entry : proservCronJobs.cronJobs.entrySet()) {
				String dp = entry.getValue().dataPointID;
				String zone = entry.getValue().zoneName;
				String name = entry.getValue().dataPointName;
				String s = "<h2 id='intro'> <input type=\"checkbox\" name=\"" + dp +"-enabled\" id=\"" + dp +"-enabled\" />  " + zone +" /// " + name +"</h2>\n"
						+"<div class='schedule' id='schedule" + dp +"-frame'>"
						+"\n<table><tr><td><a><div style=\"width: 100px\" >" + mapProservLang.get("ON") + " :</div></a></td><td><div id='schedule" + dp +"a'></div></td></tr></table>"
						+"\n<table><tr><td><a><div style=\"width: 100px\" >" + mapProservLang.get("OFF") + " :</div></a></td><td><div id='schedule" + dp +"b'></div></td></tr></table>"
						+"</div>";
				writer.println(s);
				hiddenCronJobStrings += "<p class='schedule-text'>Generated cron entries: <span class='schedule-text' id='schedule" + dp +"a-val'>  </span><span class='schedule-text' id='schedule" + dp +"b-val'></span></p>\n";
			}
			
			writer.println("</div>\n" + hiddenCronJobStrings + "</body></html>");			
			writer.close();
			
		} catch (IOException e) {
			String message = "opening file '" + filename + "' throws exception";
			logger.error(message, e);
		} finally {}	
				
	}

}
