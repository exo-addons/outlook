
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

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

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
public abstract class OutlookUser {

  protected final String userName;

  protected String       email;

  protected URI          mailServerUrl;

  protected OutlookUser(String userName) {
    this.userName = userName;
  }
  
  protected void setMailServerUrl(URI mailServerUrl) {
    this.mailServerUrl = mailServerUrl;
  }
  
  protected void setEmail(String email) {
    this.email = email;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return email;
  }

  /**
   * @return the userEmail
   */
  public String getEmail() {
    return email;
  }

  /**
   * @return the mailServerUrl
   */
  public URI getMailServerUrl() {
    return mailServerUrl;
  }

  /**
   * @return the userName
   */
  public String getUserName() {
    return userName;
  }
  
  // ****** abstract *****
  
  public abstract ExoSocialActivity postActivity(String userEmail, String messageId, String title, String text) throws Exception;
}
