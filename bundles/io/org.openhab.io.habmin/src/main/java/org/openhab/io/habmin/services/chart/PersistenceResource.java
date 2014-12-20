/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.chart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.emf.common.util.EList;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;

import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.LabelSplitHelper;
import org.openhab.io.habmin.internal.resources.MediaTypeHelper;
import org.openhab.io.habmin.services.persistence.ItemPersistenceBean;
import org.openhab.io.habmin.services.persistence.PersistenceBean;
import org.openhab.io.habmin.services.persistence.PersistenceModelHelper;
import org.openhab.io.habmin.services.persistence.PersistenceServiceBean;
import org.openhab.model.core.ModelRepository;
import org.openhab.model.items.ItemModel;
import org.openhab.model.items.ModelItem;
import org.openhab.ui.items.ItemUIRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.json.JSONWithPadding;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * <p>
 * This class acts as a REST resource for history data and provides different
 * methods to interact with the, persistence store
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
 * @since 1.3.0
 */
@Path(PersistenceResource.PATH)
public class PersistenceResource {

	private static String CHART_FILE = "charts.xml";

	private static final Logger logger = LoggerFactory.getLogger(PersistenceResource.class);

	/** The URI path to this resource */
	public static final String PATH = "persistence";

	@Context
	UriInfo uriInfo;

	@GET
	@Path("/services")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceServices(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getPersistenceServices(), callback) : getPersistenceServices();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/items")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceItems(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getPersistenceItems(), callback) : getPersistenceItems();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/charts")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceCharts(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getPersistenceChartList(), callback) : getPersistenceChartList();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@POST
	@Path("/charts")
	@Produces({ MediaType.WILDCARD })
	public Response httpPostPersistenceCharts(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, ChartConfigBean chart) {
		logger.trace("Received HTTP POST request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putChartBean(0, chart), callback) : putChartBean(0, chart);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@PUT
	@Path("/charts/{chartid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPutPersistenceCharts(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@PathParam("chartid") Integer chartId, ChartConfigBean chart) {
		logger.trace("Received HTTP PUT request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putChartBean(chartId, chart), callback) : putChartBean(chartId, chart);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@DELETE
	@Path("/charts/{chartid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpDeletePersistenceCharts(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, @PathParam("chartid") Integer chartId) {
		logger.trace("Received HTTP DELETE request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					deleteChart(chartId), callback) : deleteChart(chartId);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/charts/{chartid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceCharts(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, @PathParam("chartid") Integer chartId) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getChart(chartId), callback) : getChart(chartId);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/services/{servicename: [a-zA-Z_0-9]*}/{itemname: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceItemData(@Context HttpHeaders headers,
			@PathParam("servicename") String serviceName, @PathParam("itemname") String itemName,
			@QueryParam("starttime") String startTime, @QueryParam("endtime") String endTime,
			@QueryParam("page") long pageNumber, @QueryParam("pagelength") long pageLength,
			@QueryParam("type") String type, @QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		final String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			final Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getItemHistoryBean(serviceName, itemName, startTime, endTime), callback) : getItemHistoryBean(
					serviceName, itemName, startTime, endTime);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	/*
	 * @POST
	 * 
	 * @Path("/{servicename: [a-zA-Z_0-9]*}/{itemname: [a-zA-Z_0-9]*}")
	 * 
	 * @Produces({ MediaType.WILDCARD }) public Response
	 * httpPostPersistenceItemData(@Context HttpHeaders headers,
	 * 
	 * @PathParam("servicename") String serviceName, @PathParam("itemname")
	 * String itemName,
	 * 
	 * @QueryParam("time") String time, @QueryParam("state") String state,
	 * 
	 * @QueryParam("type") String type,
	 * 
	 * @QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
	 * logger.debug("Received HTTP GET request at '{}' for media type '{}'.",
	 * uriInfo.getPath(), type);
	 * 
	 * final String responseType =
	 * MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(),
	 * type); if (responseType != null) { final Object responseObject =
	 * responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new
	 * JSONWithPadding( updateItemHistory(serviceName, itemName, time, state),
	 * callback) : updateItemHistory(serviceName, itemName, time, state); return
	 * Response.ok(responseObject, responseType).build(); } else { return
	 * Response.notAcceptable(null).build(); } }
	 * 
	 * @DELETE
	 * 
	 * @Path("/{servicename: [a-zA-Z_0-9]*}/{itemname: [a-zA-Z_0-9]*}}")
	 * 
	 * @Produces({ MediaType.WILDCARD }) public Response
	 * httpDeleteItemData(@Context HttpHeaders headers, @PathParam("serviceame")
	 * String serviceName,
	 * 
	 * @PathParam("itemname") String itemName, @QueryParam("time") String time,
	 * 
	 * @QueryParam("type") String type,
	 * 
	 * @QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
	 * logger.debug("Received HTTP GET request at '{}' for media type '{}'.",
	 * uriInfo.getPath(), type);
	 * 
	 * final String responseType =
	 * MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(),
	 * type); if (responseType != null) { final Object responseObject =
	 * responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new
	 * JSONWithPadding( deleteItemHistory(serviceName, itemName, time),
	 * callback) : deleteItemHistory(serviceName, itemName, time); return
	 * Response.ok(responseObject, responseType).build(); } else { return
	 * Response.notAcceptable(null).build(); } }
	 */

	Date convertTime(String sTime) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

		// replace with your start date string
		Date dateTime;
		try {
			dateTime = df.parse(sTime);
		} catch (ParseException e) {
			// Time doesn't parse as string - try long
			long lTime = Long.parseLong(sTime, 10);
			dateTime = new Date(lTime);
		}

		return dateTime;
	}

	static public Item getItem(String itemname) {
		ItemUIRegistry registry = HABminApplication.getItemUIRegistry();
		if (registry != null) {
			try {
				Item item = registry.getItem(itemname);
				return item;
			} catch (ItemNotFoundException e) {
				logger.debug(e.getMessage());
			}
		}
		return null;
	}

	// start PROSERV
	static public String getTransformedLabel(String label) {
		ItemUIRegistry registry = HABminApplication.getItemUIRegistry();
		if (registry != null) {
			String transformedLabel = registry.transformLabel(label);
			return transformedLabel;
		}
		return null;
	}
	// end PROSERV

	private ItemHistoryBean getItemHistoryBean(String serviceName, String itemName, String timeBegin, String timeEnd) {
		PersistenceService service = (PersistenceService) HABminApplication.getPersistenceServices().get(serviceName);

		long timerStart = System.currentTimeMillis();

		if (service == null) {
			logger.debug("Persistence service not found '{}'.", serviceName);
			throw new WebApplicationException(404);
		}

		if (!(service instanceof QueryablePersistenceService)) {
			logger.debug("Persistence service not queryable '{}'.", serviceName);
			throw new WebApplicationException(404);
		}

		Item item = getItem(itemName);
		if (item == null) {
			logger.info("Received HTTP GET request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemName);
			throw new WebApplicationException(404);
		}

		QueryablePersistenceService qService = (QueryablePersistenceService) service;

		Date dateTimeBegin = new Date();
		Date dateTimeEnd = dateTimeBegin;
		if (timeBegin != null)
			dateTimeBegin = convertTime(timeBegin);

		if (timeEnd != null)
			dateTimeEnd = convertTime(timeEnd);

		// End now...
		if (dateTimeEnd.getTime() == 0)
			dateTimeEnd = new Date();
		if (dateTimeBegin.getTime() == 0)
			dateTimeBegin = new Date(dateTimeEnd.getTime() - 86400000);

		// Default to 1 days data if the times are the same
		if (dateTimeBegin.getTime() >= dateTimeEnd.getTime())
			dateTimeBegin = new Date(dateTimeEnd.getTime() - 86400000);

		FilterCriteria filter = new FilterCriteria();
		Iterable<HistoricItem> result;
		org.openhab.core.types.State state = null;

		Long quantity = 0l;
		double average = 0;
		Double minimum = null;
		Double maximum = null;
		Date timeMinimum = null;
		Date timeMaximum = null;

		ItemHistoryBean bean = null;
		bean = new ItemHistoryBean();

		bean.name = item.getName();

		// First, get the value at the start time.
		// This is necessary for values that don't change often otherwise data
		// will start
		// after the start of the graph (or not at all if there's no change
		// during the graph period)
		filter = new FilterCriteria();
		filter.setEndDate(dateTimeBegin);
		filter.setItemName(item.getName());
		filter.setPageSize(1);
		filter.setOrdering(Ordering.DESCENDING);
		result = qService.query(filter);
		if (result.iterator().hasNext()) {
			HistoricItem historicItem = result.iterator().next();

			state = historicItem.getState();
			double value = bean.addData(dateTimeBegin.getTime(), state);

			average += value;
			quantity++;

			minimum = value;
			timeMinimum = historicItem.getTimestamp();

			maximum = value;
			timeMaximum = historicItem.getTimestamp();
		}

		filter.setBeginDate(dateTimeBegin);
		filter.setEndDate(dateTimeEnd);
		filter.setOrdering(Ordering.ASCENDING);
		filter.setPageSize(Integer.MAX_VALUE);
		
		result = qService.query(filter);
		Iterator<HistoricItem> it = result.iterator();

		// Iterate through the data
		while (it.hasNext()) {
			HistoricItem historicItem = it.next();
			state = historicItem.getState();

			// For 'binary' states, we need to replicate the data
			// to avoid diagonal lines
			if(state instanceof OnOffType || state instanceof OpenClosedType) {
				bean.addData(historicItem.getTimestamp().getTime(), state);
			}

			double value = bean.addData(historicItem.getTimestamp().getTime(), state);

			average += value;
			quantity++;

			if (minimum == null || value < minimum) {
				minimum = value;
				timeMinimum = historicItem.getTimestamp();
			}

			if (maximum == null || value > maximum) {
				maximum = value;
				timeMaximum = historicItem.getTimestamp();
			}
		}

		// Add the last value again at the end time
		average += bean.addData(dateTimeEnd.getTime(), state);
		quantity++;

		bean.datapoints = Long.toString(quantity);
		if (quantity > 0)
			bean.stateavg = Double.toString(average / quantity);

		if (minimum != null) {
			bean.statemin = minimum.toString();
			bean.timemin = Long.toString(timeMinimum.getTime());
		}

		if (maximum != null) {
			bean.statemax = maximum.toString();
			bean.timemax = Long.toString(timeMaximum.getTime());
		}

		bean.type = item.getClass().getSimpleName();

		long timerStop = System.currentTimeMillis();
		logger.debug("CHART: returned {} rows in {}ms", bean.datapoints, timerStop - timerStart);

		return bean;
	}

	/**
	 * Read through an items model. Get all the items and provide the
	 * information that's of use for graphing/stats etc. Only items with
	 * persistence services configured are returned.
	 * 
	 * @param modelItems
	 *            the item model
	 * @param modelName
	 *            the model name
	 * @return
	 */
	private List<ItemHistoryBean> readItemModel(ItemModel modelItems, String modelName) {
		List<ItemHistoryBean> beanList = new ArrayList<ItemHistoryBean>();
		if(modelItems == null)
			return beanList;

		EList<ModelItem> modelList = modelItems.getItems();
		for (ModelItem item : modelList) {
			ItemHistoryBean bean = new ItemHistoryBean();

			if (item.getLabel() != null) {
				LabelSplitHelper label = new LabelSplitHelper(item.getLabel());

				// start PROSERV
				String labelRaw = label.getLabel();
				bean.label = getTransformedLabel(labelRaw);
				//bean.label = label.getLabel();
				// end PROSERV

				bean.format = label.getFormat();
				bean.units = label.getUnit();
			}

			bean.icon = item.getIcon();
			bean.name = item.getName();
			if (item.getType() == null)
				bean.type = "GroupItem";
			else
				bean.type = item.getType() + "Item";

			bean.groups = new ArrayList<String>();
			EList<String> groupList = item.getGroups();
			for (String group : groupList) {
				bean.groups.add(group.toString());
			}

			ModelRepository repo = HABminApplication.getModelRepository();
			if (repo == null)
				return null;

			// Loop through all the registered persistence models and read their
			// data...
			bean.services = new ArrayList<String>();
			for (Map.Entry<String, PersistenceService> service : HABminApplication.getPersistenceServices().entrySet()) {
				PersistenceModelHelper helper = new PersistenceModelHelper(service.getKey());
				ItemPersistenceBean p = helper.getItemPersistence(item.getName(), item.getGroups());
				if (p != null)
					bean.services.add(p.service);
			}

			// We're only interested in items with persistence enabled
			if (bean.services.size() > 0)
				beanList.add(bean);
		}

		return beanList;
	}

	/**
	 * Gets a list of persistence services currently configured in the system
	 * 
	 * @return
	 */
	private PersistenceBean getPersistenceServices() {
		PersistenceBean bean = new PersistenceBean();

		bean.serviceEntries.addAll(getPersistenceServiceList());

		return bean;
	}

	private PersistenceBean getPersistenceItems() {
		PersistenceBean bean = new PersistenceBean();

		for (Map.Entry<String, PersistenceService> service : HABminApplication.getPersistenceServices().entrySet()) {
			PersistenceServiceBean serviceBean = new PersistenceServiceBean();

			serviceBean.name = service.getKey();
			serviceBean.actions = new ArrayList<String>();

			serviceBean.actions.add("Create");
			if (service.getValue() instanceof QueryablePersistenceService)
				serviceBean.actions.add("Read");
			// if (service.getValue() instanceof CRUDPersistenceService) {
			// serviceBean.actions.add("Update");
			// serviceBean.actions.add("Delete");
			// }
		}

		bean.itemEntries = new ArrayList<ItemHistoryBean>();
		bean.itemEntries.addAll(getHistory());
		return bean;
	}

	private List<PersistenceServiceBean> getPersistenceServiceList() {
		List<PersistenceServiceBean> beanList = new ArrayList<PersistenceServiceBean>();

		for (Map.Entry<String, PersistenceService> service : HABminApplication.getPersistenceServices().entrySet()) {
			PersistenceServiceBean serviceBean = new PersistenceServiceBean();

			serviceBean.name = service.getKey();
			serviceBean.actions = new ArrayList<String>();

			serviceBean.actions.add("Create");
			if (service.getValue() instanceof QueryablePersistenceService)
				serviceBean.actions.add("Read");
			// if (service.getValue() instanceof CRUDPersistenceService) {
			// serviceBean.actions.add("Update");
			// serviceBean.actions.add("Delete");
			// }

			beanList.add(serviceBean);
		}

		return beanList;
	}

	private List<ItemHistoryBean> getHistory() {
		List<ItemHistoryBean> beanList = new ArrayList<ItemHistoryBean>();

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
				List<ItemHistoryBean> beans = readItemModel(items,
						listOfFiles[i].getName().substring(0, listOfFiles[i].getName().indexOf('.')));
				if (beans != null)
					beanList.addAll(beans);
			}
		}

		return beanList;
	}

	private ChartConfigBean putChartBean(Integer chartRef, ChartConfigBean bean) {
		if (chartRef == 0) {
			bean.id = null;
		} else {
			bean.id = chartRef;
		}

		// Load the existing list
		ChartListBean list = loadCharts();

		int high = 0;

		ChartConfigBean foundChart = null;
		// Loop through the interface list
		for (ChartConfigBean i : list.entries) {
			if (i.id > high)
				high = i.id;
			if (i.id.intValue() == chartRef) {
				// If it was found in the list, remember it...
				foundChart = i;
			}
		}

		// If it was found in the list, remove it...
		if (foundChart != null) {
			list.entries.remove(foundChart);
		}

		// Set defaults if this is a new chart
		if (bean.id == null) {
			bean.id = high + 1;
		}

		// Now save the updated version
		list.entries.add(bean);
		saveCharts(list);

		return bean;
	}

	private ChartListBean getPersistenceChartList() {
		ChartListBean charts = loadCharts();
		ChartListBean newList = new ChartListBean();

		// We only want to return the id and name
		for (ChartConfigBean i : charts.entries) {
			ChartConfigBean newChart = new ChartConfigBean();
			newChart.id = i.id;
			newChart.name = i.name;
			newChart.icon = i.icon;

			newList.entries.add(newChart);
		}

		return newList;
	}

	private ChartConfigBean getChart(Integer chartRef) {
		ChartListBean charts = loadCharts();

		for (ChartConfigBean i : charts.entries) {
			if (i.id.intValue() == chartRef)
				return i;
		}

		return null;
	}

	private ChartListBean deleteChart(Integer chartRef) {
		ChartListBean charts = loadCharts();

		ChartConfigBean foundChart = null;
		for (ChartConfigBean i : charts.entries) {
			if (i.id.intValue() == chartRef) {
				// If it was found in the list, remember it...
				foundChart = i;
				break;
			}
		}

		// If it was found in the list, remove it...
		if (foundChart != null)
			charts.entries.remove(foundChart);

		saveCharts(charts);

		return getPersistenceChartList();
	}

	private boolean saveCharts(ChartListBean chart) {
		File folder = new File(HABminApplication.HABMIN_DATA_DIR);
		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", HABminApplication.HABMIN_DATA_DIR);
			folder.mkdirs();
		}

		FileOutputStream fout;
		try {
			long timerStart = System.currentTimeMillis();

			fout = new FileOutputStream(HABminApplication.HABMIN_DATA_DIR + CHART_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("charts", ChartListBean.class);
			xstream.alias("chart", ChartConfigBean.class);
			xstream.alias("item", ChartItemConfigBean.class);
			xstream.alias("axis", ChartAxisConfigBean.class);
			xstream.processAnnotations(ChartListBean.class);

			xstream.toXML(chart, fout);

			fout.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Chart list saved in {}ms.", timerStop - timerStart);
		} catch (FileNotFoundException e) {
			logger.debug("Unable to open Chart list for SAVE - ", e);

			return false;
		} catch (IOException e) {
			logger.debug("Unable to write Chart list for SAVE - ", e);

			return false;
		}

		return true;
	}

	private ChartListBean loadCharts() {
		ChartListBean charts = null;

		FileInputStream fin;
		try {
			long timerStart = System.currentTimeMillis();

			fin = new FileInputStream(HABminApplication.HABMIN_DATA_DIR + CHART_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("charts", ChartListBean.class);
			xstream.alias("chart", ChartConfigBean.class);
			xstream.alias("item", ChartItemConfigBean.class);
			xstream.alias("axis", ChartAxisConfigBean.class);
			xstream.processAnnotations(ChartListBean.class);

			charts = (ChartListBean) xstream.fromXML(fin);

			fin.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Charts loaded in {}ms.", timerStop - timerStart);

		} catch (FileNotFoundException e) {
			charts = new ChartListBean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return charts;
	}

	/*
	 * private boolean updateItemHistory(String serviceName, String itemName,
	 * String time, String state) { State nState = new StringType(state); Date
	 * recordTime = convertTime(time);
	 * 
	 * if(recordTime.getTime() == 0) return false;
	 * 
	 * PersistenceService service =
	 * HABminApplication.getPersistenceServices().get(serviceName); if (service
	 * instanceof CRUDPersistenceService) { CRUDPersistenceService qService =
	 * (CRUDPersistenceService) service; FilterCriteria filter = new
	 * FilterCriteria(); filter.setBeginDate(recordTime);
	 * filter.setItemName(itemName); return qService.update(filter, nState); }
	 * else { logger.warn("The persistence service does not support UPDATE.");
	 * return false; } }
	 * 
	 * private boolean deleteItemHistory(String serviceName, String itemName,
	 * String time) { Date recordTime = convertTime(time);
	 * 
	 * if(recordTime.getTime() == 0) return false;
	 * 
	 * PersistenceService service =
	 * HABminApplication.getPersistenceServices().get(serviceName); if (service
	 * instanceof CRUDPersistenceService) { CRUDPersistenceService qService =
	 * (CRUDPersistenceService) service; FilterCriteria filter = new
	 * FilterCriteria(); filter.setBeginDate(recordTime);
	 * filter.setItemName(itemName); return qService.delete(filter); } else {
	 * logger.warn("The persistence service does not support DELETE."); return
	 * false; } }
	 */
}
