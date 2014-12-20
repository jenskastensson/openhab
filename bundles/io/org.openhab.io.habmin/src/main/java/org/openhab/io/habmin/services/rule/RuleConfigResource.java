/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.rule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.types.State;
import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.MediaTypeHelper;
import org.openhab.io.habmin.services.item.ItemConfigBean;
import org.openhab.io.habmin.services.item.ItemModelHelper;
import org.openhab.model.core.ModelRepository;
import org.openhab.model.rule.rules.ChangedEventTrigger;
import org.openhab.model.rule.rules.CommandEventTrigger;
import org.openhab.model.rule.rules.EventTrigger;
import org.openhab.model.rule.rules.Import;
import org.openhab.model.rule.rules.Rule;
import org.openhab.model.rule.rules.RuleModel;
import org.openhab.model.rule.rules.SystemOnShutdownTrigger;
import org.openhab.model.rule.rules.SystemOnStartupTrigger;
import org.openhab.model.rule.rules.TimerTrigger;
import org.openhab.model.rule.rules.UpdateEventTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.json.JSONWithPadding;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * <p>
 * This class acts as a REST resource for history data and provides different
 * methods to interact with the rule system
 * 
 * <p>
 * The typical content types are plain text for status values and XML or JSON(P)
 * for more complex data structures
 * </p>
 * 
 * <p>
 * This resource is registered with the Jersey servlet.
 * </p>
 * 
 * @author Chris Jackson
 * @since 1.4.0
 */
@Path(RuleConfigResource.PATH_RULES)
public class RuleConfigResource {

	private static final Logger logger = LoggerFactory.getLogger(RuleConfigResource.class);

	private static String RULE_FILE = "rules.xml";

	private final String HABMIN_RULES = "habmin-autorules";
	private final String HABMIN_RULES_LIBRARY = "rules_library.xml";

	/** The URI path to this resource */
	public static final String PATH_RULES = "config/rules";

	protected static final String RULE_FILEEXT = ".rules";

	@Context
	UriInfo uriInfo;

/*	@GET
	@Path("/library/list/{itemname: .+}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetTemplateTypeList(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getRuleTemplateList(itemName), callback) : getRuleTemplateList(itemName);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
//		}
	}*/

	@GET
	@Path("/model/list")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetModelList(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getRuleModelList(null), callback) : getRuleModelList(null);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/model/source/{modelname: .+}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetModelSource(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("modelname") String modelName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getRuleModelSource(modelName), callback) : getRuleModelSource(modelName);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@PUT
	@Path("/model/source/{modelname: .+}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPutModelSource(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("modelname") String modelName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, RuleModelBean rule) {
		logger.debug("Received HTTP PUT request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putRuleModelSource(modelName, rule), callback) : putRuleModelSource(modelName, rule);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

/*	@GET
	@Path("/list")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetList(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getRuleList(), callback) : getRuleList();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
//		}
	}

	@GET
	@Path("/item/{itemname: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetItem(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getRuleTemplateItemList(itemName), callback) : getRuleTemplateItemList(itemName);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
//		}
	}
*/
	@PUT
	@Path("/item/{itemname: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPutItem(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, RuleListBean ruleData) {
		logger.debug("Received HTTP PUT request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putItemRules(itemName, ruleData), callback) : putItemRules(itemName, ruleData);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}
/*
	@POST
	@Path("/item/{itemname: [a-zA-Z_0-9]*}/{rulename: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPostItemRule(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName, @PathParam("rulename") String ruleName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, RuleTemplateBean ruleData) {
		logger.debug("Received HTTP POST request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					postRule(itemName, ruleName, ruleData), callback) : postRule(itemName, ruleName, ruleData);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
//		}
	}

	@DELETE
	@Path("/item/{itemname: [a-zA-Z_0-9]*}/{rulename: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpDeleteItemRule(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName, @PathParam("rulename") String ruleName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP DELETE request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					deleteRule(itemName, ruleName), callback) : deleteRule(itemName, ruleName);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
//		}
	}
*/
	private RuleListBean getRuleTemplateList(String itemName) {
		Item item = null;
		try {
			item = HABminApplication.getItemUIRegistry().getItem(itemName);
		} catch (ItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(item == null) {
			return null;
		}

		FileInputStream fin;
		try {
			fin = new FileInputStream("webapps/habmin/openhab/" + HABMIN_RULES_LIBRARY);

			// Load the rule library
			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("rules", RuleListBean.class);
			xstream.alias("variable", RuleVariableBean.class);
			xstream.processAnnotations(RuleListBean.class);
			xstream.processAnnotations(RuleVariableBean.class);

			RuleListBean ruleList = (RuleListBean) xstream.fromXML(fin);
			fin.close();

			// If itemName is specified, then we only return rules applicable to this type
			if(itemName != null) {
				List<RuleTemplateBean> resultList = new ArrayList<RuleTemplateBean>();
				
				// Loop through all rules and filter out any that aren't applicable
				// for this item type
				for (RuleTemplateBean rule : ruleList.rule) {
					boolean applicable = false;
					if (rule.applicableType != null) {
						for (String type : rule.applicableType) {
							for(Class<? extends State> itemType : item.getAcceptedDataTypes()) {
								String[] splitter = itemType.getName().split("\\.");
								if(type.equalsIgnoreCase(splitter[splitter.length-1])) {
									applicable = true;
									break;
								}
							}
						}
					}
					if (applicable == true) {
						resultList.add(rule);
					}
				}
				
				ruleList.rule.clear();
				ruleList.rule.addAll(resultList);
			}
			
			return ruleList;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private RuleModelListBean getRuleModelList(String type) {
		Collection<RuleModelBean> beans = new LinkedList<RuleModelBean>();

		ModelRepository modelRepository = HABminApplication.getModelRepository();
		File[] files = new File("configurations/rules").listFiles();
		
		for (File file : files) {
			String modelName = file.getName();
			
			if(!modelName.endsWith(RULE_FILEEXT))
				continue;

			RuleModelBean model = new RuleModelBean();
			model.rules = new ArrayList<RuleBean>();
			model.model = StringUtils.removeEnd(modelName, RULE_FILEEXT);

			RuleModel ruleModel = (RuleModel) modelRepository.getModel(modelName);
			if (ruleModel != null) {
				model.imports = new ArrayList<String>();
				for (Import importString : ruleModel.getImports()) {
					model.imports.add(importString.getImportedNamespace());
				}

				for (Rule rule : ruleModel.getRules()) {
					RuleBean bean = new RuleBean();
					bean.triggers = new ArrayList<RuleTriggerBean>();
					bean.name = rule.getName();
					bean.ruleContent = getScript(PATH_RULES + "/" + modelName, bean.name);

					for (EventTrigger trigger : rule.getEventtrigger()) {
						RuleTriggerBean triggerbean = new RuleTriggerBean();
						if (trigger instanceof SystemOnStartupTrigger) {
							triggerbean.type = "System started";
							bean.triggers.add(triggerbean);
						}
						if (trigger instanceof ChangedEventTrigger) {
							triggerbean.type = "Change";
							bean.triggers.add(triggerbean);
						}
						if (trigger instanceof UpdateEventTrigger) {
							triggerbean.type = "Update";
							bean.triggers.add(triggerbean);
						}
						if (trigger instanceof SystemOnShutdownTrigger) {
							triggerbean.type = "Shutdown";
							bean.triggers.add(triggerbean);
						}
						if (trigger instanceof CommandEventTrigger) {
							triggerbean.type = "Command";
							bean.triggers.add(triggerbean);
						}
						if (trigger instanceof TimerTrigger) {
							triggerbean.type = "Timer";
							bean.triggers.add(triggerbean);
						}

					}
					model.rules.add(bean);
				}
			}
			beans.add(model);
		}

		RuleModelListBean beanlist = new RuleModelListBean();
		beanlist.rule = new ArrayList<RuleModelBean>();
		beanlist.rule.addAll(beans);

		return beanlist;
	}
	
	/**
	 * Produces a lit of rules that are applicable to the item. Rules are
	 * filtered out based on attributes in the rule template file
	 * 
	 * @param itemName
	 *            the item name to return the list of rules
	 * @return returns a list of rules applicable and configured for this item
	 */
	private RuleListBean getRuleTemplateItemList(String itemName) {
		RuleListBean newRules = getRuleTemplateList(itemName);
		if(newRules == null)
			return null;

		// Loop through the rules and add any relevant config from the item
		// config database
		for (RuleTemplateBean rulelist : newRules.rule) {
			RuleTemplateBean rule = getItemRule(itemName, rulelist.name);
			if (rule != null) {
				for (RuleVariableBean vars : rule.variable) {
					// Correlate the values
					for (RuleVariableBean x : rulelist.variable) {
						if (x.name.equals(vars.name)) {
							x.value = vars.value;
							break;
						}
					}

					if (vars.name.equals("DerivedItem")) {
						rulelist.linkeditem = vars.value;
					}
				}
			}
		}

		return newRules;
	}

	private RuleTemplateBean getRuleTemplate(String ruleName) {
		// Get the item from the itemName
		// This is used so we can get the type, and filter only relevant rules

		FileInputStream fin;
		try {
			fin = new FileInputStream("webapps/habmin/openhab/" + HABMIN_RULES_LIBRARY);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("rules", RuleListBean.class);
			xstream.processAnnotations(RuleListBean.class);

			RuleListBean newRules = (RuleListBean) xstream.fromXML(fin);
			fin.close();

			// Loop through the rules and find the one we're looking for
			for (RuleTemplateBean rule : newRules.rule) {
				if (rule.name.equals(ruleName))
					return rule;
			}

			return null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private String resolveVariable(String line, List<RuleVariableBean> variables) {
		for (RuleVariableBean variable : variables) {
			line = line.replace("%%" + variable.name + "%%", variable.value);
		}

		return line;
	}

	private String writeRule(String itemName, List<RuleVariableBean> ruleVariables, RuleTemplateBean rule) {
		String ruleString = new String();

		// Create a new copy of the variables so we can add the ItemName without
		// messing with the rules variables
		List<RuleVariableBean> variables = new ArrayList<RuleVariableBean>(ruleVariables);
		RuleVariableBean thisItem = new RuleVariableBean();
		thisItem.name = "ItemName";
		thisItem.value = itemName;
		variables.add(thisItem);

		ruleString += "// " + rule.description + "\r\n";

		ruleString += "rule \"" + itemName + ": " + rule.label + "\"\r\n";
		ruleString += "when\r\n";
		for (String line : rule.trigger) {
			ruleString += "  " + resolveVariable(line, variables) + "\r\n";
		}
		ruleString += "then\r\n";
		for (String line : rule.action) {
			ruleString += "  " + resolveVariable(line, variables) + "\r\n";
		}
		ruleString += "end\r\n";

		return ruleString;
	}

	/**
	 * Writes the rules configured with all items to the habmin derived rules
	 * file
	 * 
	 * @return true if successful
	 */
	boolean writeRules() {
		ModelRepository repo = HABminApplication.getModelRepository();
		if (repo == null)
			return false;

		String orgName = "configurations/rules/" + HABMIN_RULES + ".rules";
		String newName = "configurations/rules/" + HABMIN_RULES + ".rules.new";
		String bakName = "configurations/rules/" + HABMIN_RULES + ".rules.bak";

		List<String> importList = new ArrayList<String>();
		List<String> ruleList = new ArrayList<String>();

		// Load the HABmin database so we have all the rules for all items
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newName),"UTF-8"));

			RuleListBean rules = loadRules();

			// Loop through all the rules for this item
			for (RuleTemplateBean rule : rules.rule) {
				RuleTemplateBean template = getRuleTemplate(rule.name);
				if (template.imports != null) {
					for (String i : template.imports) {
						if (importList.indexOf(i) == -1)
							importList.add(i);
					}
				}

				ruleList.add(writeRule(rule.item, rule.variable, template));
			}

			// Write a warning!
			out.write("// This rule file is autogenerated by HABmin.\r\n");
			out.write("// Any changes made manually to this file will be overwritten next time HABmin rules are saved.");
			out.write("\r\n");

			// Loop though all the rules and write each rule
			for (String s : importList)
				out.write("import " + s + "\r\n");
			out.write("\r\n");

			for (String s : ruleList)
				out.write(s + "\r\n");
			out.write("\r\n");

			out.close();

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
				repo.addOrRefreshModel(HABMIN_RULES, inFile);
			} catch (FileNotFoundException e) {
				logger.error("Error refreshing habmin rule file :", e);
			}
		} catch (IOException e) {
			logger.error("Error writing habmin rule file :", e);
		}

		return true;
	}

	/**
	 * Save a new rule for an item
	 * 
	 * @param itemName
	 *            the item name
	 * @param ruleName
	 *            the name of the rule
	 * @param ruleData
	 *            rule data
	 * @return returns a list of rules applicable and configured for this item
	 */
	private RuleListBean postRule(String itemName, String ruleName, RuleTemplateBean ruleData) {
		// Add the rule into the database
		updateItemRule(itemName, ruleData);

		// Check if there is an item to create
		for (RuleVariableBean variable : ruleData.variable) {
			if (!variable.itemtype.isEmpty()) {
				// Create a new item
				ItemModelHelper itemHelper = new ItemModelHelper();

				ItemConfigBean item = new ItemConfigBean();
				item.name = variable.value;
				RuleTemplateBean template = getRuleTemplate(ruleData.name);

				// Set the label here in case we don't find the parent bean
				// later.
				if (template != null)
					item.label = itemName + " " + template.name;

				// Put the new item in the "habmin.items" model file
				item.model = "habmin";

				// Get the parent item so that we know type etc.
				ItemModelHelper modelHelper = new ItemModelHelper();
				ItemConfigBean itemBean = modelHelper.getItemConfigBean(itemName);
				if (itemBean != null) {
					item.label = itemBean.label + " " + template.name;
					item.icon = itemBean.icon;
					item.format = itemBean.format;
					item.units = itemBean.units;
					item.translateRule = itemBean.translateRule;
					item.translateService = itemBean.translateService;
					item.type = variable.itemtype;

					// Save
					itemHelper.updateItem(item.name, item, false);
				}
			}
		}

		// Update the rules for openHAB
		writeRules();

		// Return the list of rules for this item
		return getRuleTemplateItemList(itemName);
	}

	/**
	 * Delete a rule from an item (not the rule library)
	 * 
	 * @param itemName
	 *            item to remove the rule from
	 * @param ruleName
	 *            name of the rule to remove
	 * @return list of rules configured for the item
	 */
	private RuleListBean deleteRule(String itemName, String ruleName) {
		removeItemRule(itemName, ruleName);

		// Update the rules for openHAB
		writeRules();

		// Return the list of rules for this item
		return getRuleTemplateItemList(itemName);
	}

	/**
	 * Save rules for a particular item. This can't create new rules, so it is
	 * effectively updating variable values
	 * 
	 * @param itemName
	 * @param ruleData
	 * @return
	 */
	private RuleListBean putItemRules(String itemName, RuleListBean ruleData) {
		// Loop through all the rules
		for (RuleTemplateBean rule : ruleData.rule) {
			// Make sure there are variables in this rule
			if (rule.variable == null)
				continue;

			// Get the rule from the config database
			RuleTemplateBean bean = getItemRule(itemName, rule.name);
			if (bean == null)
				continue;

			// Loop through all the variables in this rule
			for (RuleVariableBean varBean : bean.variable) {
				// Don't allow changing of "Setup" scoped variables
				if (varBean.scope.equalsIgnoreCase("Setup"))
					continue;

				for (RuleVariableBean varIn : rule.variable) {
					if (varIn.name.equals(varBean.name)) {
						// We have a match - update the value
						varBean.value = varIn.value;
					}
				}
			}
		}

		RuleListBean ruleList = new RuleListBean();
		// Write the config database
		saveRules(ruleList);

		// Update the rules for openHAB
		writeRules();

		// Return the list of rules for this item
		return getRuleTemplateItemList(itemName);
	}

	private RuleListBean getRuleList() {
		RuleListBean rules = loadRules();

		FileInputStream fin;
		try {
			fin = new FileInputStream("webapps/habmin/openhab/" + HABMIN_RULES_LIBRARY);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("rules", RuleListBean.class);
			xstream.processAnnotations(RuleListBean.class);

			RuleListBean ruleTemplates = (RuleListBean) xstream.fromXML(fin);
			fin.close();

			List<RuleTemplateBean> itemRuleList = new ArrayList<RuleTemplateBean>();

			// Loop through the items
			for (RuleTemplateBean rule : rules.rule) {
				// And finally find the template for this rule
				for (RuleTemplateBean template : ruleTemplates.rule) {
					if (rule.name.equals(template.name)) {
						//
						RuleTemplateBean newRule = new RuleTemplateBean();
						newRule.item = rule.item;
						newRule.label = template.label;
						newRule.name = template.name;
						newRule.description = template.description;

						// Add the new rule to the list
						itemRuleList.add(newRule);
					}
				}
			}

			RuleListBean listBean = new RuleListBean();
			listBean.rule = itemRuleList;

			return listBean;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private String getScript(String fileName, String ruleName) {

		return null;
	}

	private RuleModelBean putRuleModelSource(String modelName, RuleModelBean rule) {
		String orgName = "configurations/rules/" + modelName + RULE_FILEEXT;
		String newName = "configurations/rules/" + modelName + RULE_FILEEXT + ".new";
		String bakName = "configurations/rules/" + modelName + RULE_FILEEXT + ".bak";

        try {
          BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newName),"UTF-8"));

          out.write(rule.source);
          out.close();
        } catch ( IOException e ) {
        	// TODO: update
           e.printStackTrace();
        }
		
		
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
		ModelRepository repo = HABminApplication.getModelRepository();
		if (repo == null)
			return null;
		InputStream inFile;
		try {
			inFile = new FileInputStream(orgName);
			repo.addOrRefreshModel(modelName, inFile);
		} catch (FileNotFoundException e) {
			logger.error("Error refreshing habmin rule file :", e);
		}

		return getRuleModelSource(modelName);
	}

	private RuleModelBean getRuleModelSource(String modelName) {
		RuleModelBean model = new RuleModelBean();
		model.model = modelName;

		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream("configurations/rules/" + modelName + RULE_FILEEXT);
			model.source = IOUtils.toString(inputStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return model;
	}
	
	
	
	
	
	
	
	
	
	synchronized public boolean removeItemRule(String itemName, String ruleName) {
		logger.debug("HABmin database: Removing rule {}", ruleName);

		RuleListBean rules = loadRules();
		if(rules == null) {
			return false;
		}

		// Are there any rules?
		if(rules.rule == null)
			return false;

		// See if the rule exists
		for(RuleTemplateBean iRule : rules.rule) {
			if(iRule.name.equals(ruleName)) {
				// Remove from the list
				rules.rule.remove(iRule);
				break;
			}
		}

		// Save the database to disk
		saveRules(rules);
		
		return true;
	}

	synchronized public RuleTemplateBean getItemRule(String itemName, String ruleName) {
/*		HabminItemBean item = xxx.getItemConfig(itemName);
		if(item == null) {
			return null;
		}

		// See if the rule already exists
		for(RuleTemplateBean iRule : item.rules) {
			if(iRule.name.equals(ruleName)) {
				return iRule;
			}
		}
		*/
		return null;
	}

	synchronized static public boolean updateItemRule(String itemName, RuleTemplateBean rule) {
		logger.debug("HABmin database: Adding rule {}", rule.name);
/*
		HabminItemBean item = xxx.getItemConfig(itemName);
		if(item == null) {
			return false;
		}

		// Make sure rules is initialised
		if(item.rules == null)
			item.rules = new ArrayList<RuleTemplateBean>();
		if(item.rules == null)
			return false;

		// See if the rule already exists
		for(RuleTemplateBean iRule : item.rules) {
			if(iRule.name.equals(rule.name)) {
				// Remove from the list
				item.rules.remove(iRule);
				break;
			}
		}

		// We now know the rule isn't in the list so it can simply be added
		item.rules.add(rule);

		// Save the database to disk
//		saveRules();
		*/
		return true;
	}

	private boolean saveRules(RuleListBean rule) {
		File folder = new File(HABminApplication.HABMIN_DATA_DIR);
		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", HABminApplication.HABMIN_DATA_DIR);
			folder.mkdirs();
		}

		FileOutputStream fout;
		try {
			long timerStart = System.currentTimeMillis();

			fout = new FileOutputStream(HABminApplication.HABMIN_DATA_DIR + RULE_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("rules", RuleListBean.class);
			xstream.processAnnotations(RuleListBean.class);

			xstream.toXML(rule, fout);

			fout.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Rule list saved in {}ms.", timerStop - timerStart);
		} catch (FileNotFoundException e) {
			logger.debug("Unable to open Rule list for SAVE - ", e);

			return false;
		} catch (IOException e) {
			logger.debug("Unable to write Rule list for SAVE - ", e);

			return false;
		}

		return true;
	}

	private RuleListBean loadRules() {
		RuleListBean rules = null;

		FileInputStream fin;
		try {
			long timerStart = System.currentTimeMillis();

			fin = new FileInputStream(HABminApplication.HABMIN_DATA_DIR + RULE_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("rules", RuleListBean.class);
			xstream.processAnnotations(RuleListBean.class);

			rules = (RuleListBean) xstream.fromXML(fin);

			fin.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Charts loaded in {}ms.", timerStop - timerStart);

		} catch (FileNotFoundException e) {
			rules = new RuleListBean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rules;
	}
}
