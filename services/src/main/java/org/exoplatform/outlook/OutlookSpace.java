
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

import java.util.Collection;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookSpace.java 00000 May 28, 2016 pnedonosko $
 */
public abstract class OutlookSpace {

  /** The group id. */
  protected final String groupId;

  /** The title. */
  protected final String title;

  /** The short name. */
  protected final String shortName;

  /** The pretty name. */
  protected final String prettyName;

  /**
   * Instantiates a new outlook space.
   *
   * @param groupId the group id
   * @param title the title
   * @param shortName the short name
   * @param prettyName the pretty name
   */
  public OutlookSpace(String groupId, String title, String shortName, String prettyName) {
    this.groupId = groupId;
    this.title = title;
    this.shortName = shortName;
    this.prettyName = prettyName;
  }

  /**
   * Gets the group id.
   *
   * @return group id
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Gets the title.
   *
   * @return title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the short name.
   *
   * @return the shortName
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Gets the pretty name.
   *
   * @return the prettyName
   */
  public String getPrettyName() {
    return prettyName;
  }

  /**
   * Gets the folder.
   *
   * @param path the path
   * @return the folder
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
  public abstract Folder getFolder(String path) throws OutlookException, RepositoryException;

  /**
   * Gets the root folder.
   *
   * @return the root folder
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
  public abstract Folder getRootFolder() throws OutlookException, RepositoryException;

  /**
   * Post activity.
   *
   * @param message the message
   * @return the exo social activity
   * @throws Exception the exception
   */
  public abstract ExoSocialActivity postActivity(OutlookMessage message) throws Exception;

  /**
   * Post activity.
   *
   * @param user the user
   * @param title the title
   * @param body the body
   * @return the exo social activity
   * @throws Exception the exception
   */
  public abstract ExoSocialActivity postActivity(OutlookUser user, String title, String body) throws Exception;

  /**
   * Post activity.
   *
   * @param user the user
   * @param text the text
   * @return the exo social activity
   * @throws Exception the exception
   */
  public abstract ExoSocialActivity postActivity(OutlookUser user, String text) throws Exception;

  /**
   * Adds the forum topic.
   *
   * @param message the message
   * @return the topic
   * @throws Exception the exception
   */
  public abstract Topic addForumTopic(OutlookMessage message) throws Exception;
  
  /**
   * Adds the forum topic.
   *
   * @param user the user
   * @param name the name
   * @param text the text
   * @return the topic
   * @throws Exception the exception
   */
  public abstract Topic addForumTopic(OutlookUser user, String name, String text) throws Exception;

  /**
   * Find last documents.
   *
   * @param text the text
   * @return the collection
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public abstract Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException;

}
