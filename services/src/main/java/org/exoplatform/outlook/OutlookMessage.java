
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

/**
 * Office user API.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookEmail.java 00000 JUn 14, 2016 pnedonosko $
 * 
 */
public class OutlookMessage {

  /** The Constant DATE_FORMAT. */
  public static final DateFormat DATE_FORMAT        = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

  /**
   * Date format was used when message was read in JS client.
   */
  @Deprecated
  public static final DateFormat DATE_FORMAT_CLIENT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /** The user. */
  protected final OutlookUser    user;

  /** The from. */
  protected OutlookEmail         from;

  /** The to. */
  protected List<OutlookEmail>   to                 = new ArrayList<OutlookEmail>();

  /** The id. */
  protected String               id;

  /** The title. */
  protected String               title;

  /** The subject. */
  protected String               subject;

  /** The body. */
  protected String               body;

  /** The type. */
  protected String               type;

  /** The created. */
  protected Calendar             created;

  /** The modified. */
  protected Calendar             modified;

  /** The file node. */
  protected Node                 fileNode;

  /**
   * Instantiates a new outlook message.
   *
   * @param user the user
   */
  protected OutlookMessage(OutlookUser user) {
    this.user = user;
    this.from = user;
  }

  /**
   * Sets the title.
   *
   * @param title the title to set
   */
  protected void setTitle(String title) {
    this.title = title;
  }

  /**
   * Set message subject.
   * 
   * @param subject {@link String}
   */
  protected void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Sets the body.
   *
   * @param body message body
   */
  protected void setBody(String body) {
    this.body = body;
  }

  /**
   * Sets the type.
   *
   * @param type the type to set
   */
  protected void setType(String type) {
    this.type = type;
  }

  /**
   * Sets the id.
   *
   * @param id the id to set
   */
  protected void setId(String id) {
    this.id = id;
  }

  /**
   * Sets the to.
   *
   * @param to the to to set
   */
  protected void setTo(List<OutlookEmail> to) {
    this.to = to;
  }

  /**
   * Sets the from.
   *
   * @param from the from to set
   */
  protected void setFrom(OutlookEmail from) {
    this.from = from;
  }

  /**
   * Sets the created.
   *
   * @param created the created to set
   */
  protected void setCreated(Calendar created) {
    this.created = created;
  }

  /**
   * Sets the modified.
   *
   * @param modified the modified to set
   */
  protected void setModified(Calendar modified) {
    this.modified = modified;
  }

  /**
   * Gets the file node.
   *
   * @return the fileNode
   */
  protected Node getFileNode() {
    return fileNode;
  }

  /**
   * Sets the file node.
   *
   * @param fileNode the fileNode to set
   */
  protected void setFileNode(Node fileNode) {
    this.fileNode = fileNode;
  }

  /**
   * Gets the user.
   *
   * @return the user
   */
  public OutlookUser getUser() {
    return user;
  }

  /**
   * Gets the from.
   *
   * @return the from
   */
  public OutlookEmail getFrom() {
    return from;
  }

  /**
   * Gets the to.
   *
   * @return the to
   */
  public List<OutlookEmail> getTo() {
    return Collections.unmodifiableList(to);
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Gets the body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the created.
   *
   * @return the created
   */
  public Calendar getCreated() {
    return created;
  }

  /**
   * Gets the modified.
   *
   * @return the modified
   */
  public Calendar getModified() {
    return modified;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("From ");
    str.append(user.toString());
    str.append(" at ");
    str.append(modified.getTime());
    return str.toString();
  }

}
