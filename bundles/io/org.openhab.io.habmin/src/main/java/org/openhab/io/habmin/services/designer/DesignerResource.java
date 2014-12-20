/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.designer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
import org.openhab.io.habmin.services.designer.blocks.DesignerRuleCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.json.JSONWithPadding;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * @author Chris Jackson
 * @since 1.5.0
 */
@Path(DesignerResource.PATH)
public class DesignerResource {

	private static final Logger logger = LoggerFactory.getLogger(DesignerResource.class);

	protected static final String DESIGN_FILE = "designer.xml";

	public static final String PATH = "config/designer";

	@Context
	UriInfo uriInfo;

	@GET
	@Produces({ MediaType.WILDCARD })
	public Response getDesigns(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type);

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getDesignBeans(), callback) : getDesignBeans();
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	@GET
	@Path("/{designref: [0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response getDesignRef(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@PathParam("designref") Integer designref
			) {
		logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());
		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					getDesignBean(designref), callback) : getDesignBean(designref);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}
	
	@DELETE
	@Path("/{designref: [0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response deleteDesignRef(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@PathParam("designref") Integer designref
			) {
		logger.debug("Received HTTP DELETE request at '{}'.", uriInfo.getPath());
		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					deleteDesignBean(designref), callback) : deleteDesignBean(designref);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}
	
	@PUT
	@Path("/{designref: [0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public Response putDesignRef(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@PathParam("designref") Integer designref, 
			DesignerBean updatedDesign
			) {
		logger.debug("Received HTTP PUT request at '{}'.", uriInfo.getPath());
		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putDesignBean(designref, updatedDesign), callback) : putDesignBean(designref, updatedDesign);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}
	
	@POST
	@Produces({ MediaType.WILDCARD })
	public Response postDesignRef(@Context HttpHeaders headers, @QueryParam("type") String type,
			@QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			DesignerBean updatedDesign
			) {
		logger.debug("Received HTTP POST request at '{}' for media type '{}'.", uriInfo.getPath(), type);
		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if (responseType != null) {
			Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
					putDesignBean(0, updatedDesign), callback) : putDesignBean(0, updatedDesign);
			return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
	}

	private DesignerListBean getDesignBeans() {
		DesignerListBean designs = loadDesigns();
		DesignerListBean newList = new DesignerListBean();

		// We only want to return the id and name
		for (DesignerBean i : designs.entries) {
			DesignerBean newDesign = new DesignerBean();
			newDesign.id = i.id;
			newDesign.name = i.name;

			newList.entries.add(newDesign);
		}

		return newList;
	}

	private DesignerBean getDesignBean(Integer designRef) {
		DesignerListBean designs = loadDesigns();

		for (DesignerBean i : designs.entries) {
			if(i.id.intValue() == designRef) {
				i.source = DesignerRuleCreator.loadSource(i.id, i.name);
				return i;
			}
		}

		return null;
	}

	private DesignerListBean deleteDesignBean(Integer designRef) {
		DesignerListBean designs = loadDesigns();

		DesignerBean foundDesign = null;
		// Loop through the designs list
		for(DesignerBean i : designs.entries) {
			if(i.id.intValue() == designRef) {
				// If it was found in the list, remember it...
				foundDesign = i;
			}
		}

		// If it was found in the list, remove it...
		if(foundDesign != null)
			designs.entries.remove(foundDesign);

		saveDesigns(designs);

		return getDesignBeans();
	}

	private DesignerListBean loadDesigns() {
		DesignerListBean designs = null;

		logger.debug("Loading Designs.");
		FileInputStream fin;
		try {
			long timerStart = System.currentTimeMillis();

			fin = new FileInputStream(HABminApplication.HABMIN_DATA_DIR + DESIGN_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("designlist", DesignerListBean.class);
			xstream.alias("field", DesignerFieldBean.class);
			xstream.alias("mutation", DesignerMutationBean.class);
			xstream.alias("child", DesignerChildBean.class);
			xstream.processAnnotations(DesignerListBean.class);
			xstream.processAnnotations(DesignerMutationBean.class);
			xstream.processAnnotations(DesignerBean.class);
			xstream.processAnnotations(DesignerBlockBean.class);
			xstream.processAnnotations(DesignerChildBean.class);
			xstream.processAnnotations(DesignerCommentBean.class);
			xstream.processAnnotations(DesignerFieldBean.class);

			designs = (DesignerListBean) xstream.fromXML(fin);

			fin.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Designs loaded in {}ms.", timerStop - timerStart);

		} catch (FileNotFoundException e) {
			designs = new DesignerListBean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(designs.entries == null)
			designs.entries = new ArrayList<DesignerBean>();

		return designs;
	}

	/**
	 * 
	 * @param designRef The reference for this design. This should be 0 to create a new design.
	 * @param bean
	 * @return
	 */
	private DesignerBean putDesignBean(Integer designRef, DesignerBean bean) {
		// Sanity check.
		// designRef is 0 for a new design
		// if it's not 0, then bean.id must either be missing, or it must be the same as designRef
		if(designRef != 0 && bean.id != null && bean.id.intValue() != designRef.intValue()) {
			logger.error("Inconsistent id in HTTP call '{}' and structure '{}'", designRef, bean.id);
			return null;
		}
		
		// Load the existing list
		DesignerListBean list = loadDesigns();

		int high = 0;

		DesignerBean foundDesign = null;
		// Loop through the designs list
		for(DesignerBean i : list.entries) {
			if(i.id > high)
				high = i.id;
			if(i.id.intValue() == designRef) {
				// If it was found in the list, remember it...
				foundDesign = i;
			}
		}

		// If it was found in the list, remove it...
		if(foundDesign != null) {
			list.entries.remove(foundDesign);
		}
		
		// Set id if this is a new design
		if(bean.id == null) {
			bean.id = high + 1;
		}
		
		// Sanity check the name
		if(bean.name == null) {
			bean.name = "";
		}

		// Now save the updated version
		list.entries.add(bean);
		saveDesigns(list);
		
		bean.source = DesignerRuleCreator.saveRule(bean.id, bean.name, bean.block);

		return bean;
	}

	private boolean saveDesigns(DesignerListBean designs) {
		File folder = new File(HABminApplication.HABMIN_DATA_DIR);
		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", HABminApplication.HABMIN_DATA_DIR);
			folder.mkdirs();
		}
		
		FileOutputStream fout;
		try {
			long timerStart = System.currentTimeMillis();

			fout = new FileOutputStream(HABminApplication.HABMIN_DATA_DIR + DESIGN_FILE);

			XStream xstream = new XStream(new StaxDriver());
			xstream.alias("designlist", DesignerListBean.class);
			xstream.alias("field", DesignerFieldBean.class);
			xstream.alias("mutation", DesignerMutationBean.class);
			xstream.alias("child", DesignerChildBean.class);
			xstream.processAnnotations(DesignerListBean.class);
			xstream.processAnnotations(DesignerMutationBean.class);
			xstream.processAnnotations(DesignerBean.class);
			xstream.processAnnotations(DesignerBlockBean.class);
			xstream.processAnnotations(DesignerChildBean.class);
			xstream.processAnnotations(DesignerCommentBean.class);
			xstream.processAnnotations(DesignerFieldBean.class);

			xstream.toXML(designs, fout);

			fout.close();

			long timerStop = System.currentTimeMillis();
			logger.debug("Designs saved in {}ms.", timerStop - timerStart);
		} catch (FileNotFoundException e) {
			logger.debug("Unable to open Designs for SAVE - ", e);

			return false;
		} catch (IOException e) {
			logger.debug("Unable to write Designs for SAVE - ", e);

			return false;
		}

		return true;
	}
}
