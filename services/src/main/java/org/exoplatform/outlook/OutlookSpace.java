
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
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.mow.api.Page;

import java.util.Collection;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookSpace.java 00000 May 28, 2016 pnedonosko $
 * 
 */
public abstract class OutlookSpace {

  protected final String groupId;

  protected final String title;

  protected final String shortName;

  protected final String prettyName;

  public OutlookSpace(String groupId, String title, String shortName, String prettyName) {
    this.groupId = groupId;
    this.title = title;
    this.shortName = shortName;
    this.prettyName = prettyName;
  }

  /**
   * @return group id
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * @return title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the shortName
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * @return the prettyName
   */
  public String getPrettyName() {
    return prettyName;
  }

  public abstract Folder getFolder(String path) throws OutlookException, RepositoryException;

  public abstract Folder getRootFolder() throws OutlookException, RepositoryException;

  public abstract ExoSocialActivity postActivity(OutlookMessage message) throws Exception;

  public abstract ExoSocialActivity postActivity(OutlookUser user, String title, String body) throws Exception;

  public abstract ExoSocialActivity postActivity(OutlookUser user, String text) throws Exception;

  public abstract Page addWikiPage(OutlookMessage message) throws Exception;

  public abstract Topic addForumTopic(OutlookMessage message) throws Exception;

  public abstract Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException;

}
