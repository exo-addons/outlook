
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

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.outlook.social.SharedOutlookMessageActivity.ViewDocumentActionListener;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.ext.component.activity.UIDocumentPreview;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SharedOutlookMessageActivity.java 00000 Jul 12, 2016 pnedonosko $
 * 
 */
@ComponentConfigs({ @ComponentConfig(lifecycle = UIFormLifecycle.class,
                                     // template =
                                     // "classpath:groovy/ecm/social-integration/UISharedFile.gtmpl",
                                     template = "classpath:groovy/templates/SharedOutlookMessageActivity.gtmpl",
                                     events = { @EventConfig(listeners = ViewDocumentActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) }) })
public class SharedOutlookMessageActivity extends FileUIActivity {

  public static final String             ACTIVITY_TYPE   = "outlook:sharemessage";

  @Deprecated
  private static final ThreadLocal<Node> viewContextNode = new ThreadLocal<Node>();

  public static class ViewDocumentActionListener extends EventListener<SharedOutlookMessageActivity> {
    @Override
    public void execute(Event<SharedOutlookMessageActivity> event) throws Exception {
      // code adapted from FileUIActivity but using OutlookMessageDocumentPreview instead of UIDocumentPreview
      SharedOutlookMessageActivity activity = event.getSource();
      UIActivitiesContainer uiActivitiesContainer = activity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();

      OutlookMessageDocumentPreview preview = uiPopupContainer.createUIComponent(OutlookMessageDocumentPreview.class,
                                                                                 null,
                                                                                 "UIDocumentPreview");
      preview.setBaseUIActivity(activity);
      preview.setContentInfo(activity.docPath, activity.repository, activity.workspace, activity.getContentNode());

      uiPopupContainer.activate(preview, 0, 0, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }

    // TODO clenaup
    // /**
    // * {@inheritDoc}
    // */
    // @Override
    // public void execute(Event<FileUIActivity> event) throws Exception {
    // try {
    // viewContextNode.set(event.getSource().getContentNode());
    // super.execute(event);
    // } finally {
    // viewContextNode.set(null);
    // }
    // }

  }

  @Deprecated
  protected static Node getViewDocumentNode() {
    return viewContextNode.get();
  }

  public SharedOutlookMessageActivity() throws Exception {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFileSupportPreview(Node data) throws Exception {
    if (data != null) {
      // code adapted from the super's method but with adding a node in to context
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(Utils.MIME_TYPE, data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString());
      // add node in the context to help view filter recognize Outlook Message file
      context.put(Node.class.getName(), data);

      for (UIExtension extension : extensions) {
        if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, extension.getName(), context)
            && !"Text".equals(extension.getName())) {
          return true;
        }
      }
    }
    return super.isFileSupportPreview(data);
  }

  public void renderContentPresentation() throws Exception {
    OutlookMessagePresentation uicontentpresentation = addChild(OutlookMessagePresentation.class, null, null);
    uicontentpresentation.setNode(getContentNode());
    UIComponent fileComponent = uicontentpresentation.getUIComponent(getMimeType());
    uicontentpresentation.renderUIComponent(fileComponent);
  }

}
