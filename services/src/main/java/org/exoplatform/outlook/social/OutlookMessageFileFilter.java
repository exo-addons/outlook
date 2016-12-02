
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

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.outlook.OutlookService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.ApplicationContext;
import org.exoplatform.services.rest.impl.ApplicationContextImpl;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageFileFilter.java 00000 Mar 2, 2016 pnedonosko $
 */
public class OutlookMessageFileFilter extends org.exoplatform.webui.ext.filter.impl.FileFilter {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    Node contextNode = (Node) context.get(Node.class.getName());
    if (contextNode == null) {
      UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
      if (uiExplorer != null) {
        contextNode = uiExplorer.getCurrentNode();
      }

      if (contextNode == null) {
        WebuiRequestContext reqContext = WebuiRequestContext.getCurrentInstance();
        UIApplication uiApp = reqContext.getUIApplication();
        UIJcrExplorerContainer jcrExplorerContainer = uiApp.getChild(UIJcrExplorerContainer.class);
        if (jcrExplorerContainer != null) {
          UIJCRExplorer jcrExplorer = jcrExplorerContainer.getChild(UIJCRExplorer.class);
          contextNode = jcrExplorer.getCurrentNode();
        }
      }

      if (contextNode == null) {
        // XXX in Platform 4.4 we rely on ContentViewerRESTService for activity preview in portal which does a
        // nasty thing inside:
        // it constructs a portal request objects and simulate with them a WebUI rendering on directly
        // instantiated UIDocViewer object, then return the markup in the response.
        // We cannot inject in any place here to add Node instance to the UIDocViewer object
        // context before its rendering call in the REST service.
        // Thus we'll check here isn't a GET call to this REST service using eXo WS internals
        ApplicationContext restContext = ApplicationContextImpl.getCurrent();
        if (restContext != null && restContext.getContainerRequest().getMethod().equals(HttpMethod.GET)
            && restContext.getPath().startsWith("/contentviewer")) {
          // we are in ContentViewerRESTService request here, get the request node and use it for test below
          MultivaluedMap<String, String> pathParams = restContext.getPathParameters();
          if (pathParams != null) {
            List<String> repoNameList = pathParams.get("repoName");
            List<String> workspaceNameList = pathParams.get("workspaceName");
            List<String> uuidList = pathParams.get("uuid");
            if (repoNameList != null && !repoNameList.isEmpty() && workspaceNameList != null && !workspaceNameList.isEmpty()
                && workspaceNameList != null && !workspaceNameList.isEmpty()) {
              String repoName = repoNameList.get(0);
              String workspaceName = workspaceNameList.get(0);
              String uuid = uuidList.get(0);
              // here we could check more precise about the REST method (via exact path order
              // repoName/workspace/uuid), but so far the service has
              // only a single method - an one we want detect here
              // if (restPath.startsWith(new StringBuilder("/contentviewer/").append(repoName).toString())) {
              RepositoryService repositoryService = CommonsUtils.getService(RepositoryService.class);
              ManageableRepository repository = repositoryService.getCurrentRepository();
              // obviously repoName should be the save as the current one
              if (repository.getConfiguration().getName().equals(repoName)) {
                SessionProviderService service = CommonsUtils.getService(SessionProviderService.class);
                SessionProvider sessionProvider = service.getSystemSessionProvider(null);
                Session session = sessionProvider.getSession(workspaceName, repository);
                contextNode = session.getNodeByUUID(uuid);
              }
            }
          }
        }
      }
    }

    if (contextNode != null && contextNode.isNodeType(OutlookService.MESSAGE_NODETYPE)) {
      context.put(Node.class.getName(), contextNode);
      return true;
    }
    return false;
  }

}
