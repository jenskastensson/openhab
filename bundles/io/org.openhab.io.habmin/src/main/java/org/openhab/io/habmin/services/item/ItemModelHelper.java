/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.item;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.LabelSplitHelper;
import org.openhab.model.core.ModelRepository;
import org.openhab.model.items.ItemModel;
import org.openhab.model.items.ModelBinding;
import org.openhab.model.items.ModelGroupItem;
import org.openhab.model.items.ModelItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Chris Jackson
 * @since 1.3.0
 * 
 */
public class ItemModelHelper {
	private static final Logger logger = LoggerFactory.getLogger(ItemModelHelper.class);

	public ItemModelHelper() {
	}

	private String getItemConfigString(ModelItem item) {
		String config = "";

		if (item instanceof ModelGroupItem) {
			ModelGroupItem gItem = (ModelGroupItem) item;
			config = "Group";

			// Check if this is an active group
			if (item.getType() != null) {
				config += ":" + gItem.getType();
				if (gItem.getFunction() != null) {
					config += ":" + gItem.getFunction();
					
					if(gItem.getArgs() != null) {
						config +="(";
						boolean first = true;
						for(String arg : gItem.getArgs()) {
							if(first == false)
								config += ",";
							first = false;
							config += arg;
						}
						config +=")";
					}
				}
			}
		} else {
			config = item.getType();
		}

		config += "\t" + item.getName();

		if (item.getLabel() != null)
			config += "\t\"" + item.getLabel() + "\"";

		if (item.getIcon() != null && !item.getIcon().isEmpty())
			config += "\t<" + item.getIcon() + ">";

		if (item.getGroups() != null) {
			boolean first = true;
			for (String group : item.getGroups()) {
				if (group != null && !group.isEmpty()) {
					if (first == true)
						config += "\t(";
					else
						config += ",";
					config += group;
					first = false;
				}
			}
			if (first != true)
				config += ")\t";
		}

		if (item.getBindings().size() != 0) {
			config += "\t{ ";
			boolean first = true;
			for (ModelBinding binding : item.getBindings()) {
				if (binding.getType() == null || binding.getConfiguration() == null)
					continue;

				if (first == false)
					config += ", ";

				config += binding.getType() + "=\"" + binding.getConfiguration() + "\"";
				first = false;
			}
			config += " }";
		}

		return config;
	}

	private String getItemConfigString(ItemConfigBean item) {
		String config = "";

		config = item.type.substring(0, item.type.indexOf("Item"));

		config += "\t" + item.name;

		if (item.label != null) {
			LabelSplitHelper label = new LabelSplitHelper(item.label, item.format, item.units, item.translateService,
					item.translateRule);
			config += "\t\"" + label.getLabelString() + "\"";
		}

		if (item.icon != null && !item.icon.isEmpty())
			config += "\t<" + item.icon + ">";

		if (item.groups != null) {
			boolean first = true;
			for (String group : item.groups) {
				if (group != null && !group.isEmpty()) {
					if (first == true)
						config += "\t(";
					else
						config += ",";
					config += group;
					first = false;
				}
			}
			if (first != true)
				config += ")\t";
		}

		// Write out the binding configs
		if (item.bindings != null && item.bindings.size() != 0) {
			config += "\t{ ";
			boolean first = true;
			for (ItemBindingBean binding : item.bindings) {
				if (binding.binding == null || binding.config == null)
					continue;

				if (first == false)
					config += ", ";

				config += binding.binding + "=\"" + binding.config + "\"";
				first = false;
			}
			config += " }";
		}

		return config;
	}

	// Save an item
	public boolean updateItem(String itemname, ItemConfigBean itemUpdate, boolean deleteItem) {

		ModelRepository repo = HABminApplication.getModelRepository();
		if (repo == null)
			return false;

		String modelName = itemUpdate.model + ".items";

		String orgName = "configurations/items/" + itemUpdate.model + ".items";
		String newName = "configurations/items/" + itemUpdate.model + ".items.new";
		String bakName = "configurations/items/" + itemUpdate.model + ".items.bak";

		ItemModel items = (ItemModel) repo.getModel(modelName);

		try {
			boolean itemSaved = deleteItem;

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newName),"UTF-8"));
	
			// Are there any items in this model?
			if (items != null) {
				// Loop through all items in the model and write them to the new
				// file
				EList<ModelItem> modelList = items.getItems();
				for (ModelItem item : modelList) {
					if (item.getName() == null)
						continue;
					if (item.getName().equals(itemUpdate.name)) {
						// Write out the new data
						if (deleteItem == false)
							out.write(getItemConfigString(itemUpdate) + "\r\n");
						itemSaved = true;
					} else {
						// Write out the old data
						out.write(getItemConfigString(item) + "\r\n");
					}
				}
			}

			// If this is a new item, then save it at the end of the file
			if (itemSaved == false)
				out.write(getItemConfigString(itemUpdate) + "\r\n");

			out.close();
//			fw.close();

			// Rename the files.
			File bakFile = new File(bakName);
			File orgFile = new File(orgName);
			File newFile = new File(newName);

			// Delete any existing .bak file
			if (bakFile.exists())
				bakFile.delete();

			// Rename the existing item file to backup
			orgFile.renameTo(bakFile);

			// Rename the new file to the item file
			newFile.renameTo(orgFile);

			// Update the model repository
			InputStream inFile;
			try {
				inFile = new FileInputStream(orgName);
				repo.addOrRefreshModel(modelName, inFile);
			} catch (FileNotFoundException e) {
				logger.error("Error refreshing item file " + modelName + ":", e);
			}
		} catch (IOException e) {
			logger.error("Error writing item file " + modelName + ":", e);
		}

		return true;
	}

	/**
	 * 
	 * @param items
	 * @param model
	 * @return
	 */
	public List<ItemConfigBean> readItemModel(ItemModel items, String model) {
		List<ItemConfigBean> beanList = new ArrayList<ItemConfigBean>();

		if(items == null)
			return beanList;

		EList<ModelItem> modelList = items.getItems();
		for (ModelItem item : modelList) {
			ItemConfigBean bean = new ItemConfigBean();
			bean.model = model;

			if (item.getLabel() != null) {
				LabelSplitHelper label = new LabelSplitHelper(item.getLabel());
				if (label != null) {
					bean.label = label.getLabel();
					bean.units = label.getUnit();
					bean.translateService = label.getTranslationService();
					bean.translateRule = label.getTranslationRule();
					bean.format = label.getFormat();
				}
			}

			bean.icon = item.getIcon();
			bean.name = item.getName();
			if (item.getType() == null)
				bean.type = "GroupItem";
			else
				bean.type = item.getType() + "Item";

			bean.bindings = new ArrayList<ItemBindingBean>();
			EList<ModelBinding> bindingList = item.getBindings();
			for (ModelBinding binding : bindingList) {
				ItemBindingBean bindingBean = new ItemBindingBean();
				bindingBean.binding = binding.getType();
				bindingBean.config = binding.getConfiguration();
				bean.bindings.add(bindingBean);
			}

			bean.groups = new ArrayList<String>();
			EList<String> groupList = item.getGroups();
			for (String group : groupList) {
				bean.groups.add(group.toString());
			}

			/*
			 * bean.persistence = new ArrayList<ItemPersistenceBean>(); for
			 * (Map.Entry<String, PersistenceService> service :
			 * RESTApplication.getPersistenceServices().entrySet()) {
			 * PersistenceModelHelper helper = new
			 * PersistenceModelHelper(service.getKey()); ItemPersistenceBean p =
			 * helper.getItemPersistence(item); if (p != null)
			 * bean.persistence.add(p); }
			 */

			beanList.add(bean);
		}

		return beanList;
	}

	public ItemConfigBean getItemConfigBean(String itemname) {
		ModelRepository repo = HABminApplication.getModelRepository();
		if (repo == null)
			return null;

		File folder = new File("configurations/items/");
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles == null)
			return null;

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() & listOfFiles[i].getName().endsWith(".items")) {
				ItemModel items = (ItemModel) repo.getModel(listOfFiles[i].getName());
				if (items != null) {
					List<ItemConfigBean> beans = readItemModel(items,
							listOfFiles[i].getName().substring(0, listOfFiles[i].getName().indexOf('.')));

					// Search for the requested item
					for (ItemConfigBean bean : beans) {
						if (bean.name == null)
							continue;

						if (bean.name.equals(itemname))
							return bean;
					}
				}
			}
		}

		return null;
	}

}
