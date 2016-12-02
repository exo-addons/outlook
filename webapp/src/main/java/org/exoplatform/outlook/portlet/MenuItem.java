
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Menu.java 00000 Jun 7, 2016 pnedonosko $
 */
public class MenuItem {

  /** The name. */
  protected final String        name;

  /** The title. */
  protected String              title;

  /** The link. */
  @Deprecated
  protected String              link;

  /** The submenu. */
  protected final Set<MenuItem> submenu;

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   */
  public MenuItem(String name) {
    this(name, null, null, new LinkedHashSet<MenuItem>());
  }

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   * @param submenu the submenu
   */
  public MenuItem(String name, Set<MenuItem> submenu) {
    this(name, null, null, submenu);
  }

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   * @param title the title
   */
  public MenuItem(String name, String title) {
    this(name, title, null, new LinkedHashSet<MenuItem>());
  }

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   * @param title the title
   * @param submenu the submenu
   */
  public MenuItem(String name, String title, Set<MenuItem> submenu) {
    this(name, title, null, submenu);
  }

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   * @param title the title
   * @param link the link
   */
  @Deprecated
  protected MenuItem(String name, String title, String link) {
    this(name, title, link, new LinkedHashSet<MenuItem>());
  }

  /**
   * Instantiates a new menu item.
   *
   * @param name the name
   * @param title the title
   * @param link the link
   * @param submenu the submenu
   */
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

  /**
   * Adds the submenu.
   *
   * @param name the name
   * @param title the title
   * @param submenu the submenu
   */
  public void addSubmenu(String name, String title, Set<MenuItem> submenu) {
    this.submenu.add(new MenuItem(name, title, submenu));
  }

  /**
   * Adds the submenu.
   *
   * @param name the name
   * @param title the title
   */
  public void addSubmenu(String name, String title) {
    this.submenu.add(new MenuItem(name, title));
  }

  /**
   * Adds the submenu.
   *
   * @param name the name
   */
  public void addSubmenu(String name) {
    this.submenu.add(new MenuItem(name));
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
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
   * Sets the title.
   *
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the link.
   *
   * @return the link
   */
  @Deprecated
  public String getLink() {
    return link;
  }

  /**
   * Sets the link.
   *
   * @param link the link to set
   */
  @Deprecated
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * Gets the submenu.
   *
   * @return the submenu
   */
  public Set<MenuItem> getSubmenu() {
    return submenu;
  }

  /**
   * Checks for submenu.
   *
   * @return true, if successful
   */
  public boolean hasSubmenu() {
    return submenu != null && submenu.size() > 0;
  }

}
