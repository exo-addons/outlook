
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.outlook.social;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletRequest;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageViewer.java 00000 Jul 12, 2016 pnedonosko $
 * 
 */
@ComponentConfig(template = "classpath:groovy/templates/OutlookMessageViewer.gtmpl")
public class OutlookMessageViewer extends BaseOutlookMessageViewer {

  protected static final Log                     LOG        = ExoLogger.getLogger(OutlookMessageViewer.class);

  public static final String                     EVENT_NAME = "ShowOutlookMessage";

  protected static final List<UIExtensionFilter> FILTERS    = Arrays.asList(new UIExtensionFilter[] {
      new OutlookMessageFileFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * Gets the webdav url (adopted code from FileUIActivity).
   * 
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavLink() throws Exception {
    final Node node = getCurrentNode();
    if (node != null) {
      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
      PortletRequest portletRequest = portletRequestContext.getRequest();
      String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
      String workspace = node.getSession().getWorkspace().getName();
      String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
          + String.format("%s", portletRequest.getServerPort());

      FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);

      StringBuilder link = new StringBuilder(baseURI);
      link.append('/');
      link.append(PortalContainer.getCurrentPortalContainerName());
      link.append('/');
      link.append(PortalContainer.getCurrentRestContextName());
      link.append("/jcr/");
      link.append(repository);
      link.append('/');
      link.append(workspace);

      if (node.isNodeType("nt:frozenNode")) {
        String uuid = node.getProperty("jcr:frozenUuid").getString();
        Node originalNode = node.getSession().getNodeByUUID(uuid);
        link.append(originalNode.getPath());
        link.append("?version=");
        link.append(node.getParent().getName());
      } else {
        link.append(node.getPath());
      }

      return friendlyService.getFriendlyUri(link.toString());
    } else {
      return "#"; // should not happen here
    }
  }

}
