
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
package org.exoplatform.outlook.webui;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

import javax.servlet.http.HttpServletRequest;

/**
 * Prepare Outlook site for showing without navigation bar from Platform's shared layout.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookLifecycle.java 00000 Oct 10, 2016 pnedonosko $
 * 
 */
public class OutlookLifecycle implements ApplicationLifecycle<WebuiRequestContext> {

  /** The Constant LOG. */
  protected static final Log           LOG             = ExoLogger.getLogger(OutlookLifecycle.class);

  /** The Constant NAVBAR_COMPONENT_ID. */
  protected static final String TOPBAR_COMPONENT_ID = "UITopBarContainerParent";
  
  /** The Constant TOOLBAR_COMPONENT_ID. */
  protected static final String TOOLBAR_COMPONENT_ID = "UIToolbarContainer";
  
  /** The toolbar rendered. */
  protected final ThreadLocal<Boolean> toolbarRendered = new ThreadLocal<Boolean>();

  /**
   * Instantiates a new outlook lifecycle.
   */
  public OutlookLifecycle() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInit(Application app) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStartRequest(Application app, WebuiRequestContext context) throws Exception {
    UIComponent navbar = findNavbarComponent(app, context);
    if (navbar != null && navbar.isRendered()) {
      toolbarRendered.set(true);
      navbar.setRendered(false);
    }
    // XXX add WCMUtils and Bootsrap-Dropdown Javascript which is required by UnifiedSearch portlet (it
    // doesn't depend on WCMUtils as QuicksearchPortlet does)
    if (context.<HttpServletRequest> getRequest().getRequestURI().indexOf("/outlook/search") > 0) {
      context.getJavascriptManager().require("SHARED/bts_dropdown");
      context.getJavascriptManager().require("SHARED/wcm-utils", "wcm_utils");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    UIComponent navbar = findNavbarComponent(app, context);
    if (navbar != null) {
      Boolean render = toolbarRendered.get();
      if (render != null && render.booleanValue()) {
        // restore rendered if was rendered and set hidden explicitly
        navbar.setRendered(true);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDestroy(Application app) throws Exception {
    // nothing
  }

  // ******* internals ******

  /**
   * Find navbar component.
   *
   * @param app the app
   * @param context the context
   * @return the UI component
   * @throws Exception the exception
   */
  protected UIComponent findNavbarComponent(Application app, WebuiRequestContext context) throws Exception {
    ExoContainer container = app.getApplicationServiceContainer();
    if (container != null) {
      UIApplication uiApp = context.getUIApplication();
      UIComponentDecorator uiViewWS = uiApp.findComponentById(UIPortalApplication.UI_VIEWING_WS_ID);
      if (uiViewWS != null) {
        UIContainer viewContainer = (UIContainer) uiViewWS.getUIComponent();
        if (viewContainer != null) {
          // Platform navbar lies inside UIPinToolbarContainer.gtmpl container (in HTML #PlatformAdminToolbarContainer)
          // and all this managed by sharedlayout.xml of PLF extension. Thus we need return a parent of found toolbar.
          // attempt #2
          UIComponent navbar = viewContainer.findComponentById(TOPBAR_COMPONENT_ID);
          if (navbar != null) {
            // for Platform 6.0.0-M24 and later
            return navbar;
          }
          navbar = viewContainer.findComponentById(TOOLBAR_COMPONENT_ID);
          if (navbar != null) {
            // for Platform 6.0.0-M23 and older
            return navbar.getParent();
          }
        }
      }
    }
    return null;
  }

}
