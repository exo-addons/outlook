
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
import org.exoplatform.webui.core.UIComponentDecorator;

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

  protected static final Log           LOG         = ExoLogger.getLogger(OutlookLifecycle.class);

  protected final ThreadLocal<Boolean> navRendered = new ThreadLocal<Boolean>();

  /**
   * 
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
    if (LOG.isDebugEnabled()) {
      LOG.debug("> onStartRequest: " + app + " " + context.getRequestContextPath() + " " + context.getSessionId());
    }
    UIContainer navPortlet = findNavigationPortlet(app, context);
    if (navPortlet != null && navPortlet.isRendered()) {
      navRendered.set(true);
      navPortlet.setRendered(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    UIContainer navPortlet = findNavigationPortlet(app, context);
    if (navPortlet != null) {
      Boolean render = navRendered.get();
      if (render != null && render.booleanValue()) {
        // restore rendered if was rendered and set hidden explicitly
        navPortlet.setRendered(true);
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

  protected UIContainer findNavigationPortlet(Application app, WebuiRequestContext context) throws Exception {
    ExoContainer container = app.getApplicationServiceContainer();
    if (container != null) {
      UIApplication uiApp = context.getUIApplication();
      UIComponentDecorator uiViewWS = uiApp.findComponentById(UIPortalApplication.UI_VIEWING_WS_ID);
      if (uiViewWS != null) {
        UIContainer uiContainer = (UIContainer) uiViewWS.getUIComponent();
        if (uiContainer != null) {
          return uiContainer.getChildById("NavigationPortlet");
        }
      }
    }
    return null;
  }

}
