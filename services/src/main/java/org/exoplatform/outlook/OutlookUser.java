
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
package org.exoplatform.outlook;

import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.mow.api.Page;

import java.net.URI;

/**
 * Office user API.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookUser.java 00000 JUn 14, 2016 pnedonosko $
 * 
 */
public abstract class OutlookUser extends OutlookEmail {

  protected final String localUser;

  protected URI          mailServerUrl;

  protected OutlookUser(String email, String displayName, String localUser) {
    super(email, displayName);
    this.localUser = localUser;
  }

  /**
   * @return the localUser
   */
  public String getLocalUser() {
    return localUser;
  }

  /**
   * @return the mailServerUrl
   */
  public URI getMailServerUrl() {
    return mailServerUrl;
  }

  protected void setMailServerUrl(URI mailServerUrl) {
    this.mailServerUrl = mailServerUrl;
  }

  // ****** abstract *****

  public abstract ExoSocialActivity postActivity(OutlookMessage message) throws OutlookException;

  public abstract ExoSocialActivity postActivity(String title, String body) throws Exception;
  
  public abstract ExoSocialActivity postActivity(String text) throws Exception;

  public abstract Page addWikiPage(OutlookMessage message) throws Exception;

  public abstract Topic addForumTopic(String categoryId, String forumId, OutlookMessage message) throws Exception;
}
