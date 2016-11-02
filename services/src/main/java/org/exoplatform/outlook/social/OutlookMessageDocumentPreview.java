
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

import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.wcm.ext.component.activity.UIDocumentPreview;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;

import java.io.Writer;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageDocumentPreview.java 00000 Jul 14, 2016 pnedonosko $
 * 
 */
@ComponentConfig(template = "classpath:groovy/ecm/social-integration/UIDocumentPreview.gtmpl",
                 events = { @EventConfig(listeners = UIDocumentPreview.CloseActionListener.class) })
public class OutlookMessageDocumentPreview extends UIDocumentPreview {

  public OutlookMessageDocumentPreview() throws Exception {
    super();
    // replace original UIDocViewer with an one that uses UI extension context with content node
    this.removeChild(UIDocViewer.class);
    this.addChild(OutlookMessageDocViewer.class, null, "UIDocViewer");
  }

  protected boolean isWebContent() throws Exception {
    return true;
  }

  /**
   * Check if a node is media/image.
   * 
   * @param data {@link Node}
   * @return boolean
   * @throws Exception when error
   */
  protected boolean isMediaFile(Node data) throws Exception {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    Writer writer = context.getWriter();
    // we add this extra div with a class for CSS that will align Outlook Message preview using standard view
    // from social integration (see outlook-view.css)
    writer.write("<div class=\"outlookMessagePreview\">");
    super.processRender(context);
    writer.write("</div>");
  }

}
