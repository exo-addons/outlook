
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

import org.exoplatform.wcm.ext.component.activity.ContentPresentation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessagePresentation.java 00000 Jul 14, 2016 pnedonosko $
 */
@ComponentConfig(lifecycle = Lifecycle.class)
public class OutlookMessagePresentation extends ContentPresentation {

  /**
   * {@inheritDoc}
   */
  @Override
  public UIComponent getUIComponent(String mimeType) throws Exception {
    // code adapted from the super method, but with node in the context
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(org.exoplatform.ecm.webui.utils.Utils.FILE_VIEWER_EXTENSION_TYPE);

    Map<String, Object> context = new HashMap<String, Object>();
    context.put(org.exoplatform.ecm.webui.utils.Utils.MIME_TYPE, mimeType);
    // add node in the context to help view filter recognize Outlook Message file
    context.put(Node.class.getName(), getNode());

    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, this);
      if (uiComponent != null && !"Text".equals(extension.getName())) {
        return uiComponent;
      }
    }

    return null;
  }
}
