
/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.outlook.rest;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.net.URI;
import java.util.Scanner;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ContentService.java 00000 Aug 14, 2016 pnedonosko $
 */
@Path("/outlook")
public class InfoService extends RESTServiceBase {

  /** The Constant LOG. */
  protected static final Log LOG                  = ExoLogger.getLogger(InfoService.class);

  /** The Constant DEFAULT_DISPLAY_NAME. */
  public static final String DEFAULT_DISPLAY_NAME = "eXo Platform";

  /** The anonymous user ID. */
  public static final String ANONYMOUS_USER       = "__anonim";

  /**
   * Instantiates a new info service.
   *
   */
  public InfoService() {
  }

  /**
   * Outlook add-in manifest for registration in Microsoft Office365 or Exchange server. <br>
   * 
   * @param uriInfo - request info
   * @param request {@link HttpServletRequest}
   * @param guid {@link String} existing add-in GUID or {@code null}
   * @param hostName {@link String} with a host name (and optionally port) or {@code null}
   * @param displayName the display name
   * @return {@link Response}
   */
  @GET
  @Path("/manifest")
  @Produces("text/xml")
  public Response getManifest(@Context UriInfo uriInfo,
                           @Context HttpServletRequest request,
                           @QueryParam("guid") String guid,
                           @QueryParam("hostName") String hostName,
                           @QueryParam("displayName") String displayName) {
    String clientHost = getClientHost(request);

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Outlook manifest for " + clientHost + " as host:" + hostName + " with guid:" + guid);
    }

    URI requestURI = uriInfo.getRequestUri();
    StringBuilder serverHostBuilder = new StringBuilder();
    serverHostBuilder.append(requestURI.getScheme());
    serverHostBuilder.append("://");
    if (hostName != null && hostName.length() > 0) {
      serverHostBuilder.append(hostName);
    } else {
      serverHostBuilder.append(requestURI.getHost());
      int serverPort = requestURI.getPort();
      if (serverPort >= 0 && (serverPort != 80 || serverPort != 443)) {
        serverHostBuilder.append(':');
        serverHostBuilder.append(serverPort);
      }
    }
    String serverURL = serverHostBuilder.toString();

    ResponseBuilder resp;
    try (Scanner mScanner = new Scanner(getClass().getResourceAsStream("/manifest/exo-outlook-manifest.template.xml"),
                                        "UTF-8").useDelimiter("\\A")) {
      String mTemplate = mScanner.next();
      String manifest = mTemplate.replaceAll("\\$BASE_URL", serverURL);

      if (guid == null || (guid = guid.trim()).length() == 0) {
        // Generate RFC4122 version 4 UUID (as observed in https://github.com/OfficeDev/generator-office)
        guid = UUID.randomUUID().toString();
      }
      manifest = manifest.replaceAll("\\$GUID", guid);

      if (displayName == null || (displayName = displayName.trim()).length() == 0) {
        displayName = DEFAULT_DISPLAY_NAME;
      }
      manifest = manifest.replaceAll("\\$DISPLAY_NAME", displayName);

      resp = Response.ok().entity(manifest);
    } catch (Throwable e) {
      LOG.error("Error while generating manifest for " + clientHost, e);
      resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Cannot generate manifest");
    }
    return resp.build();
  }

  /**
   * User information (is authenticated and user name).
   *
   * @return the response
   * @throws Exception the exception
   */
  @GET
  @Path("userinfo")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserInfo() throws Exception {
    int status;
    StringBuilder info = new StringBuilder();
    info.append("{");
    try {
      final String userId = ConversationState.getCurrent().getIdentity().getUserId();
      if (userId != null && !userId.equals(ANONYMOUS_USER)) {
        info.append("\"authenticated\": true,");
        info.append("\"userId\": \"");
        info.append(userId);
        info.append('"');
      } else {
        info.append("\"authenticated\": false");
      }
      status = HTTPStatus.OK;
    } catch (Throwable e) {
      LOG.error("Error getting user information", e);
      status = HTTPStatus.INTERNAL_ERROR;
      info.append("\"error\": \"Error getting user information\"");
    }
    info.append("}");
    return Response.status(status).entity(info.toString()).build();
  }

}
