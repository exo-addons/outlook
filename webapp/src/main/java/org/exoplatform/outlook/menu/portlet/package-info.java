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
 * OutlookMenuPortlet.
 */
@Application
@Portlet(name = "OutlookMenuPortlet")

@Scripts({
    @Script(id = "outlook.js", value = "https://appsforoffice.microsoft.com/lib/1/hosted/Office.js",
            location = AssetLocation.URL),
    @Script(id = "outlook-menu", value = "js/outlook-menu.js", depends = { "outlook.js" },
            location = AssetLocation.SERVER) })

@Assets({ "*" })

package org.exoplatform.outlook.menu.portlet;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Scripts;
import juzu.plugin.portlet.Portlet;
