#Microsoft Outlook add-on

This add-on allows use eXo Platform collaborative tools inside Microsoft Outlook. It embeds as an add-in into Outlook for Web, Mac and Windows and offer following features:
* Save attachments from email message to a collaborative space inside portal
* Attach documents from collaborative spaces to email message
* Convert email message to activity status, wiki page or forum post (forum TODO) 
* Post to activity streams, create forum posts (TODO) and wiki pages (TODO) from message window
* Search in collaborative spaces directly from message window (TODO)

##Introduction

Outlook integration consists of two parts: add-on for eXo Platform and add-in for Microsoft Outlook.

eXo Platform add-on adds pages to intranet portal that will handle requests from Outlook application. These pages live at path /portal/intranet/outlook. There is also a web page (servlet) at path /outlook in your eXo Platform server. Both portal and servlet pages should not be used directly, only via Outlook add-in.

Outlook add-in defines user interface (aka commands and panes) in its manifest. This manifest should be used within your Office365 or instllation of Microsoft Exchange. Add-in commands implemented by pages of eXo Platform add-on.

##Usage

###Convert Message To Activity

Each message can be converted to a post in intranet activity stream. You can choose also for a space stream where you want share the message.

![Convert To Status](https://raw.github.com/exo-addons/outlook/master/documentation/images/convert_to_status.png)

Message status will look as follows.

![Converted To Status](https://raw.github.com/exo-addons/outlook/master/documentation/images/converted_to_status.png)

###Convert Message To Wiki

Messages can be converted to Wiki pages in eXo Platform. You can choose intranet or one of your spaces wiki.

![Convert To Wiki](https://raw.github.com/exo-addons/outlook/master/documentation/images/convert_to_wiki.png)

Wiki page will contain quoted message body with date and sender information.

![Convert To Wiki](https://raw.github.com/exo-addons/outlook/master/documentation/images/converted_to_wiki.png)

###Save Attachment

You can save attachments of your messages in eXo Platform documents.

![Save Attachment](https://raw.github.com/exo-addons/outlook/master/documentation/images/save_attachment.png)

After this saved files will appear in selected space documents and others will see such activity status.

![Saved Attachment Activity](https://raw.github.com/exo-addons/outlook/master/documentation/images/saved_attachment_activity.png)

For several saved files you'll see activity similar to the following.

![Saved Attachments Activity](https://raw.github.com/exo-addons/outlook/master/documentation/images/saved_attachments_activity.png)

###Add Attachment

It's possible to attach documents from eXo Platform to your messages. You can search for or explore intranet documents directly from Outlook and then attach selected files to the message.

![Add Attachment](https://raw.github.com/exo-addons/outlook/master/documentation/images/add_attachment.png)

Attached documents appear in message immediatelly.

![Added Attachment](https://raw.github.com/exo-addons/outlook/master/documentation/images/added_attachment.png)


##Installation

Install eXo Platform add-on from catalog:

    ./addon install exo-outlook
  
To install latest development version use:

    ./addon install --snapshots exo-outlook  
  
After completing the add-on installation you need take a Outlook [manifest](https://raw.github.com/exo-addons/outlook/master/add-in/exo-outlook-manifest.xml) and modify it to point to your eXo Platform server in its links. Then install the manifest to user's Outlook account.

![Outlook Configuration](https://raw.github.com/exo-addons/outlook/master/documentation/images/outlook_configuration.png)

Manifest can be uploaded from local file or via URL. 

Add-in manifest also can be installed in your Microsoft Exchange instance, via admin center, to organization add-ins. After that it can be enabled for all or group of users without need to install for each of them.

