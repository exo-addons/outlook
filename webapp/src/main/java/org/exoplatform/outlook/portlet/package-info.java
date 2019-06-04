/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * OutlookPortlet.
 */
@Application
@Portlet(name = "OutlookPortlet")
@Bindings({ @Binding(value = OutlookService.class), @Binding(value = OutlookTokenService.class),
    @Binding(value = CookieTokenService.class), @Binding(value = ContentLink.class) })

@Stylesheets({ @Stylesheet(id = "fabric.css", value = "skin/fabric.min.css", location = AssetLocation.SERVER),
    @Stylesheet(id = "fabric.components.css", value = "skin/fabric.components.min.css", location = AssetLocation.SERVER),
    @Stylesheet(id = "outlook.css", value = "skin/outlook.css", location = AssetLocation.SERVER),
    @Stylesheet(id = "jquery-ui.css", value = "skin/jquery-ui.min.css", location = AssetLocation.SERVER),
    @Stylesheet(id = "jquery-ui.structure.css", value = "skin/jquery-ui.structure.min.css", location = AssetLocation.SERVER),
    @Stylesheet(id = "jquery-ui.theme.css", value = "skin/jquery-ui.theme.min.css", location = AssetLocation.SERVER) })
@Scripts({
    @Script(id = "office", value = "https://appsforoffice.microsoft.com/lib/1/hosted/Office.js",
            location = AssetLocation.URL),
    @Script(id = "outlook", value = "js/outlook.js", location = AssetLocation.SERVER,
            depends = { "office", "jquery-ui.css", "jquery-ui.structure.css", "jquery-ui.theme.css",
                "fabric.css", "fabric.components.css", "outlook.css" }) })

@Assets({ "*" })

package org.exoplatform.outlook.portlet;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Scripts;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.asset.Stylesheets;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.outlook.OutlookService;
import org.exoplatform.outlook.jcr.ContentLink;
import org.exoplatform.outlook.security.OutlookTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
