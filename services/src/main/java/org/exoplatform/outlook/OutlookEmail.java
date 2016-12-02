
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

/**
 * Outlook Email API.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookEmail.java 00000 JUn 14, 2016 pnedonosko $
 * 
 */
public class OutlookEmail {

  /** The display name. */
  protected final String displayName;

  /** The email. */
  protected String       email;

  /**
   * Instantiates a new outlook email.
   *
   * @param email the email
   * @param displayName the display name
   */
  protected OutlookEmail(String email, String displayName) {
    this.email = email;
    this.displayName = displayName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return displayName != null ? new StringBuilder(displayName).append('<').append(email).append('>').toString() : email;
  }

  /**
   * Gets the email.
   *
   * @return the userEmail
   */
  public String getEmail() {
    return email;
  }

  /**
   * Gets the display name.
   *
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the email.
   *
   * @param email the email to set
   */
  protected void setEmail(String email) {
    this.email = email;
  }
  
}
