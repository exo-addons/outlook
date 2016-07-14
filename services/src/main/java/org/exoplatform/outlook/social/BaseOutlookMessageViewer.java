
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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseOutlookMessageViewer.java 00000 Jul 12, 2016 pnedonosko $
 * 
 */
public class BaseOutlookMessageViewer extends UIAbstractManagerComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseOutlookMessageViewer.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIAbstractManager.class;
  }

  public Node getCurrentNode() throws Exception {
    Node node = currentNode();
    if (node == null) {
      throw new OutlookMessageViewException("Cannot find message node in component " + this + ", parent: " + getParent());
    }

    return node;
  }
  
  protected Node currentNode() throws Exception {
    Node node = null;

    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // when in document explorer
      node = uiExplorer.getCurrentNode();
    } else if (getParent() instanceof UIBaseNodePresentation) {
      // when in social activity stream (file view)
      UIBaseNodePresentation docViewer = getParent();
      node = docViewer.getNode();
    } else {
      WebuiRequestContext reqContext = WebuiRequestContext.getCurrentInstance();
      UIApplication uiApp = reqContext.getUIApplication();
      UIJcrExplorerContainer jcrExplorerContainer = uiApp.getChild(UIJcrExplorerContainer.class);
      if (jcrExplorerContainer != null) {
        UIJCRExplorer jcrExplorer = jcrExplorerContainer.getChild(UIJCRExplorer.class);
        node = jcrExplorer.getCurrentNode();
      }
      node = null;
    }

    return node;
  }

}
