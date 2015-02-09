/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.dashboard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.MediaTypeHelper;

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
 * @since 1.7.0
 */
@Path(DashboardResource.PATH)
public class DashboardResource {

	private static String CFG_FILE = "dashboard.xml";

	private static final Logger logger = LoggerFactory.getLogger(DashboardResource.class);

	/** The URI path to this resource */
	public static final String PATH = "dashboard";

	@Context
	UriInfo uriInfo;

	@GET
	@Path("/dashboard")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetDashboards(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getDashboardList(), callback) : getDashboardList();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@POST
	@Path("/dashboard")
	@Produces({ MediaType.WILDCARD })
	public Response httpPostDashboards(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, DashboardConfigBean dashboard) {
		logger.trace("Received HTTP POST request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putDashboardBean(0, dashboard), callback) : putDashboardBean(0, dashboard);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@PUT
	@Path("/dashboard/{dashboardid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPutDashboard(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@PathParam("dashboardid") Integer dashboardId, DashboardConfigBean dashboard) {
		logger.trace("Received HTTP PUT request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putDashboardBean(dashboardId, dashboard), callback) : putDashboardBean(dashboardId, dashboard);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@DELETE
	@Path("/dashboard/{dashboardid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpDeleteDashboard(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, @PathParam("dashboardid") Integer dashboardId) {
		logger.trace("Received HTTP DELETE request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					deleteDashboard(dashboardId), callback) : deleteDashboard(dashboardId);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/dashboard/{dashboardid: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetDashboard(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, @PathParam("dashboardid") Integer dashboardId) {
		logger.trace("Received HTTP GET request at '{}'.", uriInfo.getPath());

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getDashboard(dashboardId), callback) : getDashboard(dashboardId);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	private DashboardConfigBean putDashboardBean(Integer dashRef, DashboardConfigBean bean) {
		if (dashRef == 0) {
			bean.id = null;
		} else {
			bean.id = dashRef;
		}

		// Load the existing list
		DashboardListBean list = loadDashboards();

		int high = 0;

		DashboardConfigBean foundDash = null;
		// Loop through the interface list
		for (DashboardConfigBean i : list.entries) {
			if (i.id > high)
				high = i.id;
			if (i.id.intValue() == dashRef) {
				// If it was found in the list, remember it...
				foundDash = i;
			}
		}

		// If it was found in the list, remove it...
		if (foundDash != null) {
			list.entries.remove(foundDash);
		}

		// Set defaults if this is a new dashboard
		if (bean.id == null) {
			bean.id = high + 1;
		}

		// Now save the updated version
		list.entries.add(bean);
		saveDashboards(list);

		return bean;
	}

	private DashboardListBean getDashboardList() {
		DashboardListBean dashboards = loadDashboards();
		DashboardListBean newList = new DashboardListBean();

		// We only want to return the id and name
		for (DashboardConfigBean i : dashboards.entries) {
			DashboardConfigBean newDash = new DashboardConfigBean();
			newDash.id = i.id;
			newDash.name = i.name;
			newDash.icon = i.icon;

			newList.entries.add(newDash);
		}

		return newList;
	}

	private DashboardConfigBean getDashboard(Integer dashRef) {
		DashboardListBean dashboards = loadDashboards();

		for (DashboardConfigBean i : dashboards.entries) {
			if (i.id.intValue() == dashRef)
				return i;
		}

		return null;
	}

	private DashboardListBean deleteDashboard(Integer dashRef) {
		DashboardListBean dashboards = loadDashboards();

		DashboardConfigBean foundDash = null;
		for (DashboardConfigBean i : dashboards.entries) {
			if (i.id.intValue() == dashRef) {
				// If it was found in the list, remember it...
				foundDash = i;
				break;
			}
		}

		// If it was found in the list, remove it...
		if (foundDash != null)
			dashboards.entries.remove(foundDash);

		saveDashboards(dashboards);

		return getDashboardList();
	}

	private boolean saveDashboards(DashboardListBean dashboard) {
		File folder = new File(HABminApplication.HABMIN_DATA_DIR);
		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", HABminApplication.HABMIN_DATA_DIR);
			folder.mkdirs();
		}

		try {
			long timerStart = System.currentTimeMillis();
			
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(HABminApplication.HABMIN_DATA_DIR + CFG_FILE),"UTF-8"));

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("dashboards", DashboardListBean.class);
			xstream.alias("dashboard", DashboardConfigBean.class);
			xstream.alias("widget", DashboardWidgetBean.class);
			xstream.processAnnotations(DashboardListBean.class);

			xstream.toXML(dashboard, out);

			out.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("DAshboard list saved in {}ms.", timerStop - timerStart);
		} catch (FileNotFoundException e) {
			logger.debug("Unable to open Dashboard list for SAVE - ", e);

			return false;
		} catch (IOException e) {
			logger.debug("Unable to write Dashboard list for SAVE - ", e);

			return false;
		}

		return true;
	}

	private DashboardListBean loadDashboards() {
		DashboardListBean dashboards = null;

		FileInputStream fin;
		try {
			long timerStart = System.currentTimeMillis();

			fin = new FileInputStream(HABminApplication.HABMIN_DATA_DIR + CFG_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("dashboards", DashboardListBean.class);
			xstream.alias("dashboard", DashboardConfigBean.class);
			xstream.alias("widget", DashboardWidgetBean.class);
			xstream.processAnnotations(DashboardListBean.class);

			dashboards = (DashboardListBean) xstream.fromXML(fin);

			fin.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Dashboards loaded in {}ms.", timerStop - timerStart);

		} catch (FileNotFoundException e) {
			dashboards = new DashboardListBean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dashboards;
	}
}
