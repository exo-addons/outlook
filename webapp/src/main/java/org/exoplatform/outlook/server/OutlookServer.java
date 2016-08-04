/*
 * Copyright (C) 2012 eXo Platform SAS.
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

package org.exoplatform.outlook.server;

import juzu.MimeType;
import juzu.Path;
import juzu.Response;
import juzu.Route;
import juzu.View;
import juzu.request.RequestContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Used for function file page in Outlook. TODO doesn't used for the moment.
 */
@Singleton
public class OutlookServer {
  private static final Logger                    LOG = Logger.getLogger(OutlookServer.class.getSimpleName());

  @Inject
  @Path("index.gtmpl")
  org.exoplatform.outlook.server.templates.index index;

  @Inject
  @Path("login.gtmpl")
  org.exoplatform.outlook.server.templates.login login;

  public OutlookServer() {
  }

  @View
  @MimeType.HTML
  @Route("/menu")
  public Response index() throws IOException {
    return index.ok();
  }

  @View
  @MimeType.HTML
  @Route("/")
  public Response login(RequestContext context) throws IOException {
    Charset charset;
    try {
      charset = Charset.forName("UTF-8");
    } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
      charset = Charset.defaultCharset();
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Encoding not supported: UTF-8. " + e.getMessage() + ". Will use " + charset);
      }
    }
    ResourceBundle i18n = context.getApplicationContext().resolveBundle(context.getUserContext().getLocale());
    String title = i18n.getString("Outlook.welcome.title");
    if (title == null || title.length() == 0) {
      title = "Welcome to eXo Platform Add-in";
    }
    return login.ok().withCharset(charset).withTitle(title);
  }

}
