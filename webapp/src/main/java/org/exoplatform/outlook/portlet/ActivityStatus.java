
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
package org.exoplatform.outlook.portlet;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ActivityStatus.java 00000 Jul 11, 2016 pnedonosko $
 */
public class ActivityStatus {

  /** The user name. */
  final String userName;

  /** The space name. */
  final String spaceName;

  /** The link. */
  final String link;

  /**
   * Instantiates a new activity status.
   *
   * @param userName the user name
   * @param spaceName the space name
   * @param link the link
   */
  public ActivityStatus(String userName, String spaceName, String link) {
    this.userName = userName;
    this.spaceName = spaceName;
    this.link = link;
  }

  /**
   * Gets the link.
   *
   * @return the link
   */
  public String getLink() {
    return link;
  }

  /**
   * Gets the target name.
   *
   * @return the target name
   */
  public String getTargetName() {
    return isSpace() ? spaceName : userName;
  }

  /**
   * Checks if is space.
   *
   * @return true, if is space
   */
  public boolean isSpace() {
    return spaceName != null;
  }

}
