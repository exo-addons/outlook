
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

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookActivitySupport.java 00000 Nov 24, 2016 pnedonosko $
 */
public class OutlookActivitySupport {

  /** The Constant LOG. */
  protected static final Log           LOG = ExoLogger.getLogger(OutlookActivitySupport.class);
  
  /** The Constant CONTEXT_INITIALIZED. */
  public static final String CONTEXT_INITIALIZED = "outlookView_initialized";

  /** The document service. */
  protected final DocumentService      documentService;

  /** The repository. */
  protected final ManageableRepository repository;

  /**
   * Instantiates a new outlook activity support.
   *
   * @param documentService the document service
   * @param repository the repository
   */
  protected OutlookActivitySupport(DocumentService documentService, ManageableRepository repository) {
    super();
    this.repository = repository;
    this.documentService = documentService;
  }

  /**
   * Gets the doc open uri.
   *
   * @param node the node
   * @return the doc open uri
   */
  String getDocOpenUri(Node node) {
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node);
    String nodePath = nodeLocation.getPath();
    String uri;
    try {
      DriveData docDrive = documentService.getDriveOfNode(nodePath);
      uri = documentService.getLinkInDocumentsApp(nodePath, docDrive);
    } catch (Exception e) {
      LOG.error("Cannot get document open URI of node " + nodePath + " : " + e.getMessage(), e);
      uri = "";
    }
    return uri;
  }

  /**
   * Gets the activity status.
   *
   * @param activity the activity
   * @return the activity status
   */
  public String getActivityStatus(ExoSocialActivity activity) {
    String message = activity.getTemplateParams().get(FileUIActivity.MESSAGE);
    if (message == null) {
      return activity.getTemplateParams().get(FileUIActivity.ACTIVITY_STATUS);
    } else {
      return null;
    }
  }

  /**
   * Gets the preview link.
   *
   * @param ctx the ctx
   * @param uiActivity the ui activity
   * @param node the node
   * @return the preview link
   * @throws Exception the exception
   */
  public String getPreviewLink(WebuiBindingContext ctx, BaseUIActivity uiActivity, Node node) throws Exception {
    Identity ownerIdentity = uiActivity.getOwnerIdentity();

    String docDownloadUrl = org.exoplatform.wcm.webui.Utils.getDownloadLink(node);
    String docOpenUri = getDocOpenUri(node);
    String ownerName = ownerIdentity.getProfile().getFullName();
    String escapedOwnerName = ownerName.replace("'", "\\'").replace("&#39;", "\\&#39;");
    ExoSocialActivity activity = uiActivity.getActivity();

    String activityPostedTime = uiActivity.getPostedTimeString(ctx, activity.getPostedTime());
    String escapedActivityPostedTime = activityPostedTime.replace("'", "\\'").replace("&#39;", "\\&#39;");

    String activityStatus = getActivityStatus(activity);
    String escapedActivityStatus = activityStatus != null
                                                          ? StringEscapeUtils.escapeHtml(activityStatus.replace("'", "\\'")
                                                                                                       .replace("&#39;",
                                                                                                                "\\&#39;")
                                                                                                       .replace("\n", "")
                                                                                                       .replace("\r", ""))
                                                          : "";
    String ownerUri = LinkProvider.getUserProfileUri(ownerIdentity.getRemoteId());
    String ownerAvatar = ownerIdentity.getProfile().getAvatarUrl();
    if (ownerAvatar == null || ownerAvatar.length() == 0) {
      ownerAvatar = LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
    }

    String[] displayedIdentityLikes = uiActivity.getDisplayedIdentityLikes();
    int identityLikesNum = displayedIdentityLikes != null ? displayedIdentityLikes.length : 0;
    String docPreviewUri =
                         "javascript:require(['SHARED/social-ui-activity'], function(activity) {activity.previewDoc({doc: {id:'"
                             + node.getUUID() + "', repository:'" + repository.getConfiguration().getName()
                             + "', workspace:'" + node.getSession().getWorkspace().getName() + "', path:'" + node.getPath()
                             + "', title:'" + node.getName() + "', downloadUrl:'" + docDownloadUrl + "', openUrl:'"
                             + docOpenUri + "'}, author: {username:'" + ownerIdentity.getRemoteId() + "', fullname:'"
                             + escapedOwnerName + "', avatarUrl:'" + ownerAvatar + "', profileUrl:'" + ownerUri
                             + "'}, activity: {id: '" + activity.getId() + "', postTime:'" + escapedActivityPostedTime
                             + "', status: '" + escapedActivityStatus + "', liked: " + uiActivity.isLiked() + ", likes: "
                             + identityLikesNum + "}})})";
    return docPreviewUri;
  }

}
