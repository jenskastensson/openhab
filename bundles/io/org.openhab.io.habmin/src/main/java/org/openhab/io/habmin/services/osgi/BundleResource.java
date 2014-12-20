/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.osgi;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.io.habmin.HABminApplication;
import org.openhab.io.habmin.internal.resources.MediaTypeHelper;
import org.openhab.ui.items.ItemUIRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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
@Path(BundleResource.PATH_BUNDLE)
public class BundleResource {

	private static final Logger	logger		= LoggerFactory.getLogger(BundleResource.class);

	/** The URI path to this resource */
	public static final String	PATH_BUNDLE = "bundle";


	@Context UriInfo uriInfo;

	@GET
    @Produces( { MediaType.WILDCARD })
    public Response getItems(
    		@Context HttpHeaders headers,
    		@QueryParam("type") String type, 
    		@QueryParam("jsoncallback") @DefaultValue("callback") String callback) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type );

		String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
		if(responseType!=null) {
	    	Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ?
	    			new JSONWithPadding(new BundleListBean(getBundles(uriInfo.getPath())), callback) : new BundleListBean(getBundles(uriInfo.getPath()));
	    	return Response.ok(responseObject, responseType).build();
		} else {
			return Response.notAcceptable(null).build();
		}
    }
	
/*
	@GET
	@Path("/{bundlename: [a-zA-Z_0-9]*}")
	@Produces({ MediaType.WILDCARD })
	public SuspendResponse<Response> getItemData(@Context HttpHeaders headers, @PathParam("bundlename") String bundlename,
			@QueryParam("type") String type, @QueryParam("jsoncallback") @DefaultValue("callback") String callback,
			@HeaderParam(HeaderConfig.X_ATMOSPHERE_TRANSPORT) String atmosphereTransport,
			@Context AtmosphereResource resource) {
		logger.debug("Received HTTP GET request at '{}' for media type '{}'.", uriInfo.getPath(), type );

		if (atmosphereTransport == null || atmosphereTransport.isEmpty()) {
			final String responseType = MediaTypeHelper.getResponseMediaType(headers.getAcceptableMediaTypes(), type);
			if (responseType != null) {
				final Object responseObject = responseType.equals(MediaTypeHelper.APPLICATION_X_JAVASCRIPT) ? new JSONWithPadding(
						getBundleBean(bundlename, true), callback) : getBundleBean(bundlename, true);
				throw new WebApplicationException(Response.ok(responseObject, responseType).build());
			} else {
				throw new WebApplicationException(Response.notAcceptable(null).build());
			}
		}
		GeneralBroadcaster itemBroadcaster = (GeneralBroadcaster) BroadcasterFactory.getDefault().lookup(
				GeneralBroadcaster.class, resource.getRequest().getPathInfo(), true);
		return new SuspendResponse.SuspendResponseBuilder<Response>().scope(SCOPE.REQUEST)
				.resumeOnBroadcast(!ResponseTypeHelper.isStreamingTransport(resource.getRequest()))
				.broadcaster(itemBroadcaster).outputComments(true).build();
	}
*/
	public static BundleBean createBundleBean(Bundle bundle, String uriPath, boolean detail) {
		BundleBean bean = new BundleBean();

		bean.name = bundle.getSymbolicName();
		bean.version = bundle.getVersion().toString();
		bean.modified = bundle.getLastModified();
		bean.id = bundle.getBundleId();
		bean.state = bundle.getState();
		bean.link = uriPath;

		return bean;
	}

	static public Item getBundle(String itemname, String uriPath) {
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
/*
	private ItemBean getBundleBean(String bundlename, String uriPath) {

		Item item = getItem(itemname);
		if (item != null) {
			return createBundleBean(item, uriInfo.getBaseUri().toASCIIString(), true);
		} else {
			logger.info("Received HTTP GET request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
			throw new WebApplicationException(404);
		}
	}
*/
	private List<BundleBean> getBundles(String uriPath) {
		List<BundleBean> beans = new LinkedList<BundleBean>();

		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();

		for (Bundle bundle : bundleContext.getBundles()) {
			logger.debug(bundle.toString());
			BundleBean bean = (BundleBean)createBundleBean(bundle, uriPath, false);

			if(bean != null)
				beans.add(bean);
		}
		return beans;
	}

}
