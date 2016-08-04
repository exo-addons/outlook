
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
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.outlook.OutlookService;
import org.exoplatform.outlook.social.OutlookMessageActivity.ViewDocumentActionListener;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.portlet.PortletRequest;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageActivity.java 00000 Jul 12, 2016 pnedonosko $
 * 
 */
@ComponentConfigs({ @ComponentConfig(lifecycle = UIFormLifecycle.class,
                                     // FYI original template:
                                     // "classpath:groovy/ecm/social-integration/UISharedFile.gtmpl",
                                     template = "classpath:groovy/templates/OutlookMessageActivity.gtmpl",
                                     events = { @EventConfig(listeners = ViewDocumentActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) }) })
public class OutlookMessageActivity extends FileUIActivity {

  public static final String ACTIVITY_TYPE       = "outlook:message";

  public static final String FILE_UUID           = "fileUUID";

  public static final String REPOSITORY          = "repository";

  public static final String WORKSPACE           = "workspace";

  public static final String AUTHOR              = "author";

  public static final String DATE_CREATED        = "dateCreated";

  public static final String DATE_LAST_MODIFIED  = "lastModified";

  public static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";

  public static final String DEFAULT_TIME_FORMAT = "HH:mm";

  protected static final Log LOG                 = ExoLogger.getLogger(OutlookMessageActivity.class);

  public static class ViewDocumentActionListener extends EventListener<OutlookMessageActivity> {
    @Override
    public void execute(Event<OutlookMessageActivity> event) throws Exception {
      // code adapted from FileUIActivity but using OutlookMessageDocumentPreview instead of UIDocumentPreview
      OutlookMessageActivity activity = event.getSource();
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
  }

  public OutlookMessageActivity() throws Exception {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSummary(Node node) {
    try {
      if (node.isNodeType(OutlookService.MESSAGE_NODETYPE)) {
        // TODO use eXo's formats or Java's ones
        // ForumService forumService = getApplicationComponent(ForumService.class);
        // try {
        // UserProfile forumProfile = forumService.getUserInfo(context.getRemoteUser());
        // userDateFormat = forumProfile.getLongDateFormat();
        // } catch (Exception e) {
        // LOG.warn("Error getting current user forum profile", e);
        // userDateFormat = userTimeFormat = null;
        // }
        //
        // if (userDateFormat == null) {
        // userDateFormat = DEFAULT_DATE_FORMAT;
        // }
        // DateFormat dateFormat = new SimpleDateFormat(userDateFormat, context.getLocale());

        String fromEmail = node.getProperty("mso:fromEmail").getString();
        String fromName = node.getProperty("mso:fromName").getString();
        Date time = node.getProperty("mso:created").getDate().getTime();

        Locale userLocale = null;
        RequestContext context = RequestContext.getCurrentInstance();
        OrganizationService orgService = getApplicationComponent(OrganizationService.class);
        try {
          UserProfile userProfile = orgService.getUserProfileHandler().findUserProfileByName(context.getRemoteUser());
          if (userProfile != null) {
            String lang = userProfile.getUserInfoMap().get(Constants.USER_LANGUAGE);
            if (lang != null) {
              userLocale = LocaleContextInfo.getLocale(lang);
            }
          } else {
            LOG.warn("User profile not found for " + context.getRemoteUser());
          }
        } catch (Exception e) {
          LOG.warn("Error getting user profile for " + context.getRemoteUser(), e);
        }

        if (userLocale == null) {
          // try find locale from user request
          if (PortletRequestContext.class.isAssignableFrom(context.getClass())) {
            userLocale = ((PortalRequestContext) PortletRequestContext.class.cast(context)
                                                                            .getParentAppRequestContext()).getRequest()
                                                                                                          .getLocale();
          } else if (PortalRequestContext.class.isAssignableFrom(context.getClass())) {
            userLocale = PortalRequestContext.class.cast(context).getRequest().getLocale();
          }
          if (userLocale == null) {
            // it's server locale in most cases
            userLocale = context.getLocale();
            if (userLocale == null) {
              userLocale = Locale.ENGLISH;
            }
          }
        }

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, userLocale);
        // TODO we could find and use user's timezone: dateFormat.setTimeZone(zone);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, userLocale);
        // TODO we could find and use user's timezone: timeFormat.setTimeZone(zone);

        ResourceBundle res = context.getApplicationResourceBundle();

        StringBuilder fromLine = new StringBuilder();
        fromLine.append(fromName);
        fromLine.append('<');
        fromLine.append(fromEmail);
        fromLine.append('>');

        StringBuilder summary = new StringBuilder();
        summary.append(res.getString("Outlook.activity.from"));
        summary.append(": <a href='mailto:");
        summary.append(fromEmail);
        summary.append("' target='_top'>");
        summary.append(ContentReader.simpleEscapeHtml(fromLine.toString()));
        summary.append("</a> ");
        summary.append(res.getString("Outlook.activity.on"));
        summary.append(' ');
        summary.append(dateFormat.format(time));
        summary.append(' ');
        summary.append(res.getString("Outlook.activity.at"));
        summary.append(' ');
        summary.append(timeFormat.format(time));

        return summary.toString();
      }
    } catch (RepositoryException e) {
      LOG.warn("Error generating info for Outlook message activity node " + node, e);
    }

    return super.getSummary(node);
  }
  
  public void renderContentPresentation() throws Exception {
    OutlookMessagePresentation uicontentpresentation = addChild(OutlookMessagePresentation.class, null, null);
    uicontentpresentation.setNode(getContentNode());
    UIComponent fileComponent = uicontentpresentation.getUIComponent(getMimeType());
    uicontentpresentation.renderUIComponent(fileComponent);
  }

}
