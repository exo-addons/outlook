
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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.outlook.OutlookService;
import org.exoplatform.outlook.social.OutlookMessageActivity.ViewDocumentActionListener;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
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

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageActivity.java 00000 Jul 12, 2016 pnedonosko $
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
                                         @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
                                         @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) }) })
public class OutlookMessageActivity extends FileUIActivity {

  /** The Constant ACTIVITY_TYPE. */
  public static final String ACTIVITY_TYPE       = "outlook:message";

  /** The Constant FILE_UUID. */
  public static final String FILE_UUID           = "fileUUID";

  /** The Constant REPOSITORY. */
  public static final String REPOSITORY          = "repository";

  /** The Constant WORKSPACE. */
  public static final String WORKSPACE           = "workspace";

  /** The Constant AUTHOR. */
  public static final String AUTHOR              = "author";

  /** The Constant DATE_CREATED. */
  public static final String DATE_CREATED        = "dateCreated";

  /** The Constant DATE_LAST_MODIFIED. */
  public static final String DATE_LAST_MODIFIED  = "lastModified";

  /** The Constant DEFAULT_DATE_FORMAT. */
  public static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";

  /** The Constant DEFAULT_TIME_FORMAT. */
  public static final String DEFAULT_TIME_FORMAT = "HH:mm";

  /** The Constant FAKE_TITLE. */
  public static final String FAKE_TITLE          = "SocialIntegration.messages.createdBy";

  /** The Constant LOG. */
  protected static final Log LOG                 = ExoLogger.getLogger(OutlookMessageActivity.class);

  /**
   * The listener interface for receiving viewDocumentAction events.
   * The class that is interested in processing a viewDocumentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addViewDocumentActionListener</code> method. When
   * the viewDocumentAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  @Deprecated // TODO not used
  public static class ViewDocumentActionListener extends EventListener<OutlookMessageActivity> {

    /**
     * {@inheritDoc}
     */
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

  /** The script initialized. */
  protected static ThreadLocal<Boolean>  scriptInitialized = new ThreadLocal<Boolean>();

  /** The document service. */
  protected DocumentService              documentService;

  /** The activity status. */
  protected String                       message, activityStatus;

  /** The util. */
  protected final OutlookActivitySupport util;

  /**
   * Instantiates a new outlook message activity.
   *
   * @throws Exception the exception
   */
  public OutlookMessageActivity() throws Exception {
    super();
    RepositoryService repositoryService = CommonsUtils.getService(RepositoryService.class);
    this.util = new OutlookActivitySupport(CommonsUtils.getService(DocumentService.class),
                                           repositoryService.getCurrentRepository());
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
          } else if (LOG.isDebugEnabled()) {
            LOG.debug("User profile not found for " + context.getRemoteUser());
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
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, userLocale);

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

  /**
   * Render content presentation.
   *
   * @throws Exception the exception
   */
  public void renderContentPresentation() throws Exception {
    OutlookMessagePresentation uicontentpresentation = addChild(OutlookMessagePresentation.class, null, null);
    uicontentpresentation.setNode(getContentNode());
    UIComponent fileComponent = uicontentpresentation.getUIComponent(getMimeType());
    uicontentpresentation.renderUIComponent(fileComponent);

    // init script for UI support once
    WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
    Object init = rcontext.getAttribute(OutlookActivitySupport.CONTEXT_INITIALIZED);
    if (init == null || Boolean.FALSE.equals(init)) {
      rcontext.setAttribute(OutlookActivitySupport.CONTEXT_INITIALIZED, Boolean.TRUE);
      JavascriptManager jsManager = rcontext.getJavascriptManager();
      jsManager.require("SHARED/outlookView", "outlookView");
    }
  }

  /**
   * Get a link to open the activity in new window.<br>
   * Method existed in the parent class in PLF 4.3, but removed in 4.4, thus we keep it here.
   * 
   * @return {@link String}
   */
  @Deprecated
  public String getViewLink() {
    try {
      Node data = getContentNode();
      if (isFileSupportPreview(data)) {
        return this.event("ViewDocument", this.getId(), "");
      } else {
        return org.exoplatform.wcm.webui.Utils.getEditLink(data, false, false);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return StringUtils.EMPTY;
    }
  }

  /**
   * Gets the preview link.
   *
   * @param ctx the ctx
   * @return the preview link
   */
  public String getPreviewLink(WebuiBindingContext ctx) {
    Node node = null;
    try {
      node = getContentNode();
      if (node != null) {
        if (isFileSupportPreview(node)) {
          return util.getPreviewLink(ctx, OutlookMessageActivity.this, node);
        } else {
          return org.exoplatform.wcm.webui.Utils.getEditLink(node, false, false);
        }
      }
    } catch (Exception e) {
      LOG.error("Error getting document preview link " + node, e);
    }
    return StringUtils.EMPTY;
  }

  /**
   * {@inheritDoc}
   */
  public String getActivityStatus() {
    if (message == null || message.length() == 0) {
      return activityStatus;
    } else if (!FAKE_TITLE.equals(message)) {
      return message;
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setUIActivityData(Map<String, String> activityParams) {
    this.message = activityParams.get(FileUIActivity.MESSAGE);
    this.activityStatus = activityParams.get(FileUIActivity.ACTIVITY_STATUS);
    super.setUIActivityData(activityParams);
  }

}
