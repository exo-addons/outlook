#Microsoft Outlook add-on

This add-on allows use eXo Platform collaborative tools inside Microsoft Outlook. It embeds as an add-in into Outlook for Web, Mac and Windows and offer following features:
* Save attachments from email message to a collaborative space inside the eXo portal
* Attach documents from collaborative spaces to an email message
* Convert email message to activity status, wiki page or discussion (aka forum topics) 
* Post to activity streams or create discussion from a message window
* Search in collaborative spaces directly from message window (TODO)
* Show message collaborators information and activities from the portal (TODO)

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
  
After completing the add-on installation you need to generate an Outlook [manifest](https://your.exoplatform.server/portal/rest/outlook/manifest) and install it to user Outlook account. 

![Outlook Configuration](https://raw.github.com/exo-addons/outlook/master/documentation/images/outlook_configuration.png)

Manifest can be uploaded from local file or via URL. 

Add-in manifest also can be installed in your Microsoft Exchange instance, via admin center, to organization add-ins. After that it can be enabled for all or group of users without need to install for each of them.

To keep manifest up-to-date you can point your Office365/Exchange to the add-on manifest generator [URL](https://your.exoplatform.server/portal/rest/outlook/manifest) (as shown above). But then you'll need also keep the add-in GUID the same throug several requests to the generator - how this can be possible see below in the Manifest Generator paragraph.

##Manifest Generator

Outlook add-on offers a service to generate Add-in manifests for Office365/Exchange services using your eXo Platform server host name. A new manifest can be obtained at such URL: [https://your.exoplatform.server/portal/rest/outlook/manifest](https://your.exoplatform.server/portal/rest/outlook/manifest), where you need replace "your.exoplatform.server" with actual host name of your server. The generator returns a manifest with newly generated add-in ID and all links based on the URL host name.

If it happens that your eXo Platform server will be available to Office365/Exchange under another host name, then you can point that name explicitly via `hostName` query parameter: 
`https://your.exoplatform.server/portal/rest/outlook/manifest?hostName=acme.com`

Another helpful parameter can be used `displayName`. Thansk to this you can install several eXo add-ins for Outlook into Office365/Exchange catalog.
`https://your.exoplatform.server/portal/rest/outlook/manifest?hostName=acme.com&displayName=eXo%20Platform`

Since manifest installed into Office365/Exchange service, it may be required to keep the add-in ID the same as first time it was. The generator supports use of provided GUID via `guid` query parameter: 
`https://your.exoplatform.server/portal/rest/outlook/manifest?guid=05425534-77d2-4997-9856-52a13927452e` 
You can save this URL for use between updates of the add-on, thus next time you'll request the generator it will return a manifest with given ID of your add-in.

##Security

###Add-in pages access

Add-in consists of several pages in eXo Platform server. Main functionality provided by _/portal/intranet/outlook_ site, it handles various add-in commands for authenticated portal users. This site will be available if an user already logged in the eXo portal. 
When user not authenitcated in eXo portal, the Outlook add-in will show a login page where user will need input its username and password. Add-on offers user login persistence between requests based on portal session tokens stored on the server side with validity time configurred by _OutlookTokenService_ component (by default it's 90 days to expire add-in session). Users also can force logout in the add-in by using "Log out" button on the bottom brand bar. 

###User content access

Outlook add-in needs provide an access to user's content in eXo portal to Office365/Exchange services for attaching documents to messages. As this content will be requested directly by the office server from outside the eXo Platform server, a special short living links offered by the add-on service. These links are randomly generated and exists per-user in eXo Platform. Each such link wil be generated by the add-in when user will choose to attach his document in eXo to currently open message in Outlook. The link will be active for about a minute and can be consumed only once. In case of a failure or expiration, a new link should be requested by the add-in - it means that user has to repeat an adding attachment operation.



