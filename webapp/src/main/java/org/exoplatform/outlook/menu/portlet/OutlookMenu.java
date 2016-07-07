/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.outlook.menu.portlet;

import juzu.Response;
import juzu.View;
import juzu.request.RequestContext;

import javax.inject.Singleton;

/**
 * Juzu controller for Outlook app functions (UI-less page for commands menu, e.g. in ribbon bar).<br>
 * 
 * Created by The eXo Platform SAS<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMenu.java 00000 Jun 3, 2016 pnedonosko $
 * 
 */
@Singleton
@Deprecated
public class OutlookMenu {

  @View
  public Response index(RequestContext resourceContext) {
    return Response.ok();
  }
}
