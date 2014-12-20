/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.repository;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openhab.io.habmin.services.rule.RuleTemplateBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * 
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public class HabminConfigDatabase {
	private static final Logger logger = LoggerFactory.getLogger(HabminConfigDatabase.class);

	static private final String HABMIN_DATABASE_FILE = "webapps/habmin/openhab/configuration_database.xml";
	
	static private HabminConfigDatabaseBean configDb = null;

	/**
	 * Loads the database into memory. Once in memory, it isn't re-read - this
	 * speeds up subsequent operations.
	 * 
	 * @return true if the database is open
	 */
	static private boolean loadDatabase() {
		if (configDb != null)
			return true;

		logger.debug("Loading HABmin database.");
		FileInputStream fin;
		try {
			long timerStart = System.currentTimeMillis();

			fin = new FileInputStream(HABMIN_DATABASE_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("HABminDatabase", HabminConfigDatabaseBean.class);
			xstream.processAnnotations(HabminConfigDatabaseBean.class);

			configDb = (HabminConfigDatabaseBean) xstream.fromXML(fin);

			fin.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("HABmin database loaded in {}ms.", timerStop - timerStart);

			return true;
		} catch (FileNotFoundException e) {
			logger.debug("HABmin database not found - reinitialising.");

			configDb = new HabminConfigDatabaseBean();
			if(configDb == null)
				return false;
			configDb.items = new ArrayList<HabminItemBean>();
			if(configDb.items == null) {
				configDb = null;
				return false;
			}
			
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		}
	}

	static public boolean saveDatabase() {
		// This possibly should be moved to a separate thread so that it can be
		// deferred to avoid multiple calls
		if (loadDatabase() == false)
			return false;

		FileOutputStream fout;
		try {
			long timerStart = System.currentTimeMillis();

			fout = new FileOutputStream(HABMIN_DATABASE_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("HABminDatabase", HabminConfigDatabaseBean.class);
			xstream.processAnnotations(HabminConfigDatabaseBean.class);

			xstream.toXML(configDb, fout);

			fout.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("HABmin database saved in {}ms.", timerStop - timerStart);
		} catch (FileNotFoundException e) {
			logger.debug("Unable to open HABmin database for SAVE - ", e);

			return false;
		} catch (IOException e) {
			logger.debug("Unable to write HABmin database for SAVE - ", e);

			return false;
		}

		return true;
	}

	/**
	 * Reads the Habmin configuration "database" and returns the item requested
	 * by the item parameter.
	 * 
	 * @param item
	 *            name of the item to return
	 * @return HabminItemBean or null if not found
	 */
	synchronized static public HabminItemBean getItemConfig(String itemName) {
		if (loadDatabase() == false)
			return null;

		if(configDb.items == null)
			return null;
		
		HabminItemBean item = null;
		for (HabminItemBean bean : configDb.items) {
			if (bean.name == null)
				continue;
			if (bean.name.equals(itemName)) {
				item = bean;
				break;
			}
		}

		// If not found, create the new item
		if(item == null) {
			item = new HabminItemBean();
			item.name = itemName;
		
			configDb.items.add(item);
		}
		
		// Make sure rules is initialised
		if(item.rules == null)
			item.rules = new ArrayList<RuleTemplateBean>();
		if(item.rules == null)
			return null;
		
		// Not found
		return item;
	}

	/**
	 * Reads the Habmin configuration "database" and returns the item requested
	 * by the item parameter.
	 * 
	 * @param item
	 *            name of the item to return
	 * @return HabminItemBean or null if not found
	 */
	synchronized static public boolean existsItemConfig(String item) {
		if (loadDatabase() == false)
			return false;

		if(configDb.items == null)
			return false;
		
		for (HabminItemBean bean : configDb.items) {
			if (bean.name == null)
				continue;
			if (bean.name.equals(item))
				return true;
		}

		// Not found
		return false;
	}

	/**
	 * Saves item data in the Habmin "database" file. This will overwrite the
	 * existing data for this item
	 * 
	 * @param item
	 *            HabminItemBean with the item data
	 * @return true if item data saved successfully
	 */
	synchronized static public boolean setItemConfig(HabminItemBean item) {
		if (loadDatabase() == false)
			return false;

		HabminItemBean foundItem = null;

		// Find the required item
		for (HabminItemBean bean : configDb.items) {
			if (bean.name == null)
				continue;
			if (bean.name.equals(item))
				foundItem = bean;
		}

		if (foundItem != null) {
			// Found this item in the database, remove it
			configDb.items.remove(foundItem);
		}

		// Add the new data into the database
		configDb.items.add(item);

		saveDatabase();

		// Success
		return true;
	}

	synchronized static public List<HabminItemBean> getItems() {
		if (loadDatabase() == false)
			return null;
		
		return configDb.items;
	}

}
