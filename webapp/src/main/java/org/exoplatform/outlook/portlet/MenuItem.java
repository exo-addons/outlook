
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Menu.java 00000 Jun 7, 2016 pnedonosko $
 * 
 */
public class MenuItem {

  protected final String        name;

  protected String              title;

  @Deprecated
  protected String              link;

  protected final Set<MenuItem> submenu;

  public MenuItem(String name) {
    this(name, null, null, new HashSet<MenuItem>());
  }

  public MenuItem(String name, Set<MenuItem> submenu) {
    this(name, null, null, submenu);
  }

  public MenuItem(String name, String title) {
    this(name, title, null, new HashSet<MenuItem>());
  }

  public MenuItem(String name, String title, Set<MenuItem> submenu) {
    this(name, title, null, submenu);
  }

  @Deprecated
  protected MenuItem(String name, String title, String link) {
    this(name, title, link, new HashSet<MenuItem>());
  }

  protected MenuItem(String name, String title, String link, Set<MenuItem> submenu) {
    super();
    this.name = name;
    this.title = title;
    this.link = link;
    this.submenu = submenu;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MenuItem clone() {
    Set<MenuItem> submenu = new LinkedHashSet<MenuItem>();
    for (MenuItem sm : this.submenu) {
      submenu.add(sm.clone());
    }
    return new MenuItem(name, title, submenu);
  }

  public void addSubmenu(String name, String title, Set<MenuItem> submenu) {
    this.submenu.add(new MenuItem(name, title, submenu));
  }

  public void addSubmenu(String name, String title) {
    this.submenu.add(new MenuItem(name, title));
  }

  public void addSubmenu(String name) {
    this.submenu.add(new MenuItem(name));
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the link
   */
  @Deprecated
  public String getLink() {
    return link;
  }

  /**
   * @param link the link to set
   */
  @Deprecated
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * @return the submenu
   */
  public Set<MenuItem> getSubmenu() {
    return submenu;
  }

  public boolean hasSubmenu() {
    return submenu != null && submenu.size() > 0;
  }

}
