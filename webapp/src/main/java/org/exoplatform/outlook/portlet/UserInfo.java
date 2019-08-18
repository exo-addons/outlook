/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.model.Identity;

import java.util.List;

public class UserInfo extends IdentityInfo {

  private final Identity           identity;

  private final List<ActivityInfo> activities;

  private final List<IdentityInfo> connections;

  public UserInfo(Identity identity, List<ActivityInfo> activities, List<IdentityInfo> connections) {
    super(identity);
    this.identity = identity;
    this.activities = activities;
    this.connections = connections;
  }

  public List<ActivityInfo> getActivities() {
    return activities;
  }

  public List<IdentityInfo> getConnections() {
    return connections;
  }

}
