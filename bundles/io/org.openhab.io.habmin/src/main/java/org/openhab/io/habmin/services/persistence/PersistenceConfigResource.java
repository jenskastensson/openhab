/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.MediaTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.json.JSONWithPadding;

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
@Path(PersistenceConfigResource.PATH_PERSISTENCE)
public class PersistenceConfigResource {

	private static final Logger logger = LoggerFactory.getLogger(PersistenceConfigResource.class);

	/** The URI path to this resource */
	public static final String PATH_PERSISTENCE = "config/persistence";

	@Context
	UriInfo uriInfo;

	@GET
	@Path("/services")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceServices(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

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
	@Path("/item/{itemname: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpGetPersistenceItems(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getPersistenceItems(itemName), callback) : getPersistenceItems(itemName);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@PUT
	@Path("/item/{itemname: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response httpPutPersistenceItems(@Context HttpHeaders headers, @QueryParam("type") String type,
			@PathParam("itemname") String itemName,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback, ItemPersistenceListBean persistence) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putPersistenceItems(itemName, persistence), callback) : putPersistenceItems(itemName, persistence);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
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

	private List<PersistenceServiceBean> getPersistenceServiceList() {
		List<PersistenceServiceBean> beanList = new ArrayList<PersistenceServiceBean>();

		for (Map.Entry<String, PersistenceService> service : HABminApplication.getPersistenceServices().entrySet()) {
			PersistenceServiceBean serviceBean = new PersistenceServiceBean();

			serviceBean.name = service.getKey();
			serviceBean.actions = new ArrayList<String>();

			serviceBean.actions.add("Create");
			if (service.getValue() instanceof QueryablePersistenceService)
				serviceBean.actions.add("Read");

			PersistenceModelHelper helper = new PersistenceModelHelper(service.getKey());
			serviceBean.strategies = helper.getPersistenceStrategies();

			beanList.add(serviceBean);
		}

		return beanList;
	}

	private ItemPersistenceListBean putPersistenceItems(String itemName, ItemPersistenceListBean persistence) {
		for(ItemPersistenceBean service : persistence.entries) {
			PersistenceModelHelper helper = new PersistenceModelHelper(service.service);

			helper.setItemPersistence(itemName, service);
		}
		return getPersistenceItems(itemName);
	}

	private ItemPersistenceListBean getPersistenceItems(String itemName) {
		List<ItemPersistenceBean> list = new ArrayList<ItemPersistenceBean>();

		if (HABminApplication.getItemUIRegistry() == null)
			return null;

		Item item = null;
		try {
			item = HABminApplication.getItemUIRegistry().getItem(itemName);
		} catch (ItemNotFoundException e) {
			return null;
		}

		for (Map.Entry<String, PersistenceService> service : HABminApplication.getPersistenceServices().entrySet()) {
			PersistenceModelHelper helper = new PersistenceModelHelper(service.getKey());
			ItemPersistenceBean p = helper.getItemPersistence(itemName, item.getGroupNames());
			if (p != null)
				list.add(p);
		}

		ItemPersistenceListBean bean = new ItemPersistenceListBean();
		bean.item = itemName;
		if(list.size() > 0)
			bean.entries.addAll(list);
		return bean;
	}
}
