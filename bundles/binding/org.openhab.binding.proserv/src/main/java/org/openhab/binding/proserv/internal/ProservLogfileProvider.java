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
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openhab.ui.items.ItemUIRegistry;
import org.openhab.core.items.Item;
import org.openhab.config.core.ConfigDispatcher;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ProservLogfileProvider {

	private static final Logger logger = LoggerFactory.getLogger(ProservLogfileProvider.class);
	
	static long totalDelayInZipCreate = 0;
	static long delayInZipCreate = 1000;
	static long maxTotalDelayInZipCreate = 10000;
	
	static protected ItemUIRegistry itemUIRegistry;
	static protected Map<String, QueryablePersistenceService> persistenceServices = new HashMap<String, QueryablePersistenceService>();
	protected static final Map<String, Long> PERIODS = new HashMap<String, Long>();

	static {
//		PERIODS.put("12h", 43200000L);
		PERIODS.put("Day", 86400000L);
		PERIODS.put("Week", 604800000L);
//		PERIODS.put("Month", 2592000000L);
		PERIODS.put("Year", 31536000000L);
	}
	
	public void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
		ProservLogfileProvider.itemUIRegistry = itemUIRegistry;
	}

	public void unsetItemUIRegistry(ItemUIRegistry itemUIRegistry) {
		ProservLogfileProvider.itemUIRegistry = null;
	}
	
	public void addPersistenceService(PersistenceService service) {
		if (service instanceof QueryablePersistenceService)
			persistenceServices.put(service.getName(), (QueryablePersistenceService) service);
	}

	public void removePersistenceService(PersistenceService service) {
		persistenceServices.remove(service.getName());
	}

	static public Map<String, QueryablePersistenceService> getPersistenceServices() {
		return persistenceServices;
	}
	
	protected void activate() {
	}

	protected void deactivate() {
	}

	public synchronized void doSnapshot(String itemNames) throws Throwable  {
		logger.debug("doSnapshot start");
		totalDelayInZipCreate = 0;
		Date now = new Date();
	    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    String zipFolderName = simpleDateFormat.format(now);		
	    
		for (Map.Entry<String, Long> entry : PERIODS.entrySet()) {
		    Long period = entry.getValue();		
			Date timeBegin = null;
			Date timeEnd = null;			
			timeEnd = new Date();
			timeBegin = new Date(timeEnd.getTime() - period);	
			createLogfile(timeBegin, timeEnd, entry.getKey(), zipFolderName, itemNames);		
		}
		// clean up garbage from java.nio
		File directory = new File(ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "logs");
		for(File f: directory.listFiles())
		    if(f.getName().startsWith("zipfstmp"))
		        f.delete();
	}
	
	public void createLogfile(Date startTime, Date endTime, String periodName, String zipFolderName, String items) throws Throwable{

		QueryablePersistenceService persistenceService;

		int seriesCounter = 0;

		// If a persistence service is specified, find the provider
		persistenceService = null;		
		
		// Otherwise, just get the first service
		Set<Entry<String, QueryablePersistenceService>> serviceEntry = getPersistenceServices().entrySet();
		if(serviceEntry != null && serviceEntry.size() != 0)
			persistenceService = serviceEntry.iterator().next().getValue();

		// Did we find a service?
		if (persistenceService == null) {
			logger.error("Persistence service not found ");
			return;
		}

		// Loop through all the items
		if (items != null) {
			String[] itemNames = items.split(",");
			for (String itemName : itemNames) {
				try {
					Item item = itemUIRegistry.getItem(itemName);
					if(addItem(persistenceService, startTime, endTime, item, periodName, zipFolderName, seriesCounter))
						seriesCounter++;
				} catch (ItemNotFoundException e) {
					e.printStackTrace();
					throw e;
				}
			}
		}
	}

	private boolean addItem( QueryablePersistenceService service, Date timeBegin, Date timeEnd, Item item, 
			String periodName, String zipFolderName, int seriesCounter) throws Throwable {

		// Get the item label
		String label = null;
		if (itemUIRegistry != null) {
			// Get the item label
			label = itemUIRegistry.transformLabel(itemUIRegistry.getLabel(item.getName()));
			if (label != null && label.contains("[") && label.contains("]")) {
				label = label.substring(0, label.indexOf('['));
			}
			if(label!=null && label.contains("{") && label.contains("}")) {
				label = label.substring(label.indexOf('}')+1, label.length());
			}			
			label = label.trim();
			label = label.replace(' ',  '-');			
		}
		if (label == null) {
			label = item.getName();
		}

		Iterable<HistoricItem> result;
		FilterCriteria filter;

		// Generate data collections
		Collection<Date> xData = new ArrayList<Date>();
		Collection<Number> yData = new ArrayList<Number>();
		
		// Declare state here so it will hold the last value at the end of the process
		org.openhab.core.types.State state = null;

		// First, get the value at the start time.
		// This is necessary for values that don't change often otherwise data will start
		// after the start of the graph (or not at all if there's no change during the graph period)
		filter = new FilterCriteria();
		filter.setEndDate(timeBegin);
		filter.setItemName(item.getName());
		filter.setPageSize(1);
		filter.setOrdering(Ordering.DESCENDING);
		result = service.query(filter);
		if(result.iterator().hasNext()) {
			HistoricItem historicItem = result.iterator().next();

			state = historicItem.getState();
			if (state instanceof DecimalType) {
				xData.add(timeBegin);
				yData.add((DecimalType) state);
			}
		}

		// Now, get all the data between the start and end time
		filter.setBeginDate(timeBegin);
		filter.setEndDate(timeEnd);
		filter.setPageSize(Integer.MAX_VALUE);
		filter.setOrdering(Ordering.ASCENDING);
		
		// Get the data from the persistence store
		result = service.query(filter);
		Iterator<HistoricItem> it = result.iterator();

		// Iterate through the data
		while (it.hasNext()) {
			HistoricItem historicItem = it.next();
			state = historicItem.getState();
			if (state instanceof DecimalType) {
				xData.add(historicItem.getTimestamp());
				yData.add((DecimalType) state);
			}
		}

		// Lastly, add the final state at the endtime
		if (state != null && state instanceof DecimalType) {
			xData.add(timeEnd);
			yData.add((DecimalType) state);
		}

		// Add the new series to the chart - only if there's data elements to display
		// The chart engine will throw an exception if there's no data
		if(xData.size() == 0) {
			return false;
		}

		// If there's only 1 data point, plot it again!
		if(xData.size() == 1) {

			xData.add(xData.iterator().next());
			yData.add(yData.iterator().next());
		}
		

		String csvFileContent = new String("Time;Value\r\n");
		int count = 0;
		for(@SuppressWarnings("unused") Object x : xData) {
			  Date time = ((ArrayList<Date>) xData).get(count);
		      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		      csvFileContent += simpleDateFormat.format(time);		  
			  csvFileContent += ";";
		      NumberFormat format = DecimalFormat.getInstance();
		      format.setRoundingMode(RoundingMode.FLOOR);
		      format.setMinimumFractionDigits(0);
		      format.setMaximumFractionDigits(2);
		      csvFileContent += format.format(((ArrayList<Number>) yData).get(count));
			  csvFileContent += "\r\n";
			  count++;
		}
		updateLogfile(zipFolderName, label, periodName, csvFileContent );
		
		return true;
	}


	private void updateLogfile(String zipFolderName, String label, String periodName, String data) throws Throwable {
		try {
			label = label.replaceAll("[^a-zA-Z0-9.-]", "_");			
			String filename = label + "-" + periodName + ".csv";
			filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
			String csvFilePath = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "logs" + File.separator + filename;
			PrintWriter writer = new PrintWriter(csvFilePath, "ISO-8859-1");
			writer.write(data);
			writer.flush();
			writer.close();			
			String pathZip = ConfigDispatcher.getConfigFolder() + File.separator + ".." + File.separator + "logs" + File.separator + "ZippedCsvFiles.zip";
		    Path zipLocation = FileSystems.getDefault().getPath(pathZip).toAbsolutePath();
		    Path toBeAdded = FileSystems.getDefault().getPath(csvFilePath).toAbsolutePath();
		    createZip(zipLocation, toBeAdded, zipFolderName + "/" + filename);
		    Files.delete(toBeAdded);
		} catch (Throwable e) {
			String message = "handling" + label + "' throws exception";
			logger.error(message, e);
			throw e;
		} finally {		
		}
	}

	public void createZip(Path zipLocation, Path toBeAdded, String internalPath) throws Throwable {

	    URI zipUri = new URI("jar", zipLocation.toUri().toString(), null);
	    
	    Map<String, String> env = new HashMap<String, String>();
	    env.put("create", String.valueOf(!zipLocation.toFile().exists()));
	    
	    try (FileSystem fs = FileSystems.newFileSystem(zipUri, env)) {
	        URI root = fs.getPath("/").toUri();    
	    } 
 
	    try (FileSystem zipfs = FileSystems.newFileSystem(zipLocation, null)) {
	        Path internalTargetPath = zipfs.getPath(internalPath);
	        if (!Files.exists(internalTargetPath.getParent())) {
	            Files.createDirectory(internalTargetPath.getParent());
	        }
	        Files.copy(toBeAdded, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
	        logger.debug("Add file:\"{}\"",  internalPath);
	    }
        catch(Throwable e){
        	logger.error("Error when adding file to zip (FileSystemException?), run garbage collector, delay and try again:", e);
			try {
				System.gc();
				totalDelayInZipCreate += delayInZipCreate;
				if(totalDelayInZipCreate<maxTotalDelayInZipCreate)
					Thread.sleep(delayInZipCreate);
			} catch (InterruptedException ie) {
			}
    	    try (FileSystem zipfs = FileSystems.newFileSystem(zipLocation, null)) {
    	        Path internalTargetPath = zipfs.getPath(internalPath);
    	        if (!Files.exists(internalTargetPath.getParent())) {
    	            Files.createDirectory(internalTargetPath.getParent());
    	        }
    	        Files.copy(toBeAdded, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
    	    }        	
        }	    
	}
}
