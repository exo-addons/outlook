/**
 * Outlook read pane app script.
 */
require(
    [ "SHARED/jquery", "SHARED/outlookFabricUI", "SHARED/outlookJqueryUI", "SHARED/outlookJqueryValidate", "SHARED/juzu-ajax" ],
    function($, fabric) {

	    $(function() {
		    // hide PLF's toolbar
		    $("#PlatformAdminToolbarContainer").css("display", "none");
	    });

	    Office.initialize = function(reason) {

		    $(function() {
			    // init main pane page
			    var $pane = $("#outlook-pane");
			    if ($pane.size() > 0) {
				    var $error = $pane.find("#outlook-error");
				    var $messageBanner = $error.find(".ms-MessageBanner");
				    var $popup = $pane.find("#outlook-popup");
				    var messageBanner;
				    if ($messageBanner.size() > 0) {
					    messageBanner = new fabric.MessageBanner($messageBanner.get(0));
				    }
				    var $errorText = $error.find(".ms-MessageBanner-clipper");

				    function showError(source) {
					    var message;
					    // check if source is jqXHR of jQuery ajax request
					    if (source && source.hasOwnProperty("responseText")) {
						    var text = source.responseText;
						    if (!text) {
							    text = source.statusText;
						    }
						    message = text + " (" + source.status + ")";
					    } else {
						    message = source;
					    }
					    console.log("ERROR: " + message + ". ");
					    $errorText.empty();
					    $("<i class='uiIconError'></i><span>" + message + "</span>").appendTo($errorText);
					    messageBanner.showBanner();
					    $error.show("blind", {
						    "direction" : "down"
					    });
					    return message;
				    }
				    function clearError() {
					    $error.hide("blind");
					    $errorText.empty();
				    }

				    function fixId(msId) {
					    return msId ? msId.replace(/\//g, "-") : msId;
				    }

				    var $menu = $pane.find("#outlook-menu");
				    var $container = $pane.find("#outlook-menu-container");

				    function homeInit() {
					    // TODO something?
				    }

				    function saveAttachmentInit() {
					    var $saveAttachment = $("#outlook-saveAttachment");
					    var $form = $saveAttachment.find("form");
					    var $attachments = $form.find("ul#attachments");

					    // init spaces dropdown
					    var $groupIdDropdown = $form.find(".ms-Dropdown");
					    var $groupIdSelect = $groupIdDropdown.find("select[name='groupId']");
					    $groupIdSelect.val([]); // initially no spaces selected
					    var $groupPath = $form.find(".group-path");
					    var $folders = $groupPath.find("ul#folders");
					    var $path = $groupPath.find("input[name='path']");
					    var $pathInfo = $groupPath.find(".pathInfo");
					    var $pathUp = $groupPath.find(".pathUp");
					    var $saveButton = $form.find("button.saveButton");
					    var $cancelButton = $form.find("button.cancelButton");

					    // space folders loading
					    var groupId, groupTitle, groupRootPath;
					    var path, pathLabel, portalUrl;
					    function showFolder() {
						    // set path of loaded folder in form input
						    var $parentFolder = $groupPath.find(".parentFolder");
						    path = $parentFolder.data("path");
						    pathLabel = $parentFolder.data("pathlabel");
						    portalUrl = $parentFolder.data("portalurl");
						    if (groupRootPath == path) {
							    $pathUp.attr("disabled", "true");
						    } else {
							    $pathUp.removeAttr("disabled");
						    }
						    $path.val(path);
						    $pathInfo.val(pathLabel);
						    var $litems = $folders.find("li.ms-ListItem");
						    $litems.ListItem(); // init FabricUI JS
						    $litems.click(function() {
							    var childPath = $(this).data("path");
							    if (childPath) {
								    path = childPath;
								    pathLabel = $(this).data("pathlabel");
								    portalUrl = $(this).data("portalurl");
								    loadFolder();
							    } else {
								    showError("Error selecting folder - path not found. Please reload page.");
							    }
						    });
					    }
					    function loadFolder() {
						    var process = $.Deferred();
						    if (groupId && path) {
							    console.log(">> loadFolder: " + groupId + " >> " + path);
							    $folders.jzLoad("Outlook.folders()", {
							      groupId : groupId,
							      path : path
							    }, function(response, status, jqXHR) {
								    if (status == "error") {
									    process.reject(showError(jqXHR));
								    } else {
									    clearError();
									    showFolder();
									    $saveButton.prop("disabled", false);
									    process.resolve(path);
								    }
							    });
						    } else {
							    console.log(">> loadFolder: groupId and/or path not found");
							    showError("Space and/or path not found");
							    process.reject("Space and/or path not found");
						    }
						    return process.promise();
					    }

					    $groupIdSelect.change(function() {
						    var $space = $groupIdSelect.find("option:selected");
						    if ($space.size() > 0) {
							    if (!$groupPath.is(":visible")) {
								    $groupPath.show("blind");
							    }
							    groupId = $space.val();
							    groupTitle = $space.text();
							    groupRootPath = path = $space.data("path");
							    loadFolder();
						    }
					    });
					    $groupIdDropdown.Dropdown();

					    var item = Office.context.mailbox.item;
					    if (item.attachments.length > 0) {
						    console.log("Attachments: ");
						    for (i = 0; i < item.attachments.length; i++) {
							    var att = item.attachments[i];
							    var outputString = "";
							    outputString += i + ". Name: " + att.name + " ID: " + att.id;
							    outputString += " contentType: " + att.contentType;
							    outputString += " size: " + att.size;
							    outputString += " attachmentType: " + att.attachmentType;
							    outputString += " isInline: " + att.isInline;
							    console.log(outputString);
							    var $li = $("<li class='ms-ListItem is-selectable'><span class='ms-ListItem-primaryText'>" + att.name
							        + "</span>" // + "<span class='ms-ListItem-secondaryText'>" + att.contentType
							        // + "</span>"
							        + "<span class='ms-ListItem-metaText attachmentSize'>" + att.size + "</span>"
							        + "<div class='ms-ListItem-selectionTarget js-toggleSelection'></div>"
							        + "<input name='attachmentIds' type='hidden'></li>");
							    $li.data("attachmentId", fixId(att.id));
							    $li.appendTo($attachments);
							    $li.ListItem(); // init FabricUI JS
							    $li.click(function() {
								    $(this).toggleClass('is-selected');
								    if ($(this).hasClass("is-selected")) {
									    // $(this).find(".js-toggleSelection").click();
									    var attachmentId = $(this).data("attachmentId");
									    $(this).find("input[name='attachmentIds']").val(attachmentId);
								    } else {
									    $(this).find("input[name='attachmentIds']").val("");
								    }
							    });
						    }
						    var $attachmentIds = $attachments.find("input[name='attachmentIds']");
						    var $savingAttachment = $saveAttachment.find(".savingAttachment");
						    var $savedAttachment = $saveAttachment.find(".savedAttachment");
						    $cancelButton.click(function() {
							    $cancelButton.data("cancel", true);
						    });
						    $form.submit(function(event) {
							    event.preventDefault();
							    clearError();
							    $form.hide("fade");
							    if ($cancelButton.data("cancel")) {
								    loadMenu("home");
							    } else {
								    var attachmentIds = [];
								    $attachmentIds.each(function(i) {
									    var aid = $(this).val();
									    if (aid) {
										    attachmentIds.push(aid);
									    }
								    });
								    if (attachmentIds.length > 0) {
									    $savingAttachment.show("blind");
									    var spinner = new fabric.Spinner($savingAttachment.find(".ms-Spinner").get(0));
									    spinner.start();
									    function cancelSave() {
										    spinner.stop();
										    $savingAttachment.hide("fade");
										    $form.show("blind", {
											    "direction" : "down"
										    });
									    }
									    Office.context.mailbox.getCallbackTokenAsync(function(asyncResult) {
										    if (asyncResult.status === "succeeded") {
											    var userEmail = Office.context.mailbox.userProfile.emailAddress;
											    console.log(">> userEmail: " + userEmail);
											    var messageId = fixId(Office.context.mailbox.item.itemId);
											    console.log(">> messageId: " + messageId);
											    var attachmentToken = asyncResult.value;
											    console.log(">> attachmentToken: " + attachmentToken);
											    var ewsUrl = Office.context.mailbox.ewsUrl;
											    console.log(">> ewsUrl: " + ewsUrl);
											    // TODO do we need identity token? Is it OAuth2 bearer token?
											    // Office.context.mailbox.getUserIdentityTokenAsync(function(asyncResult)
											    // {
											    // if (asyncResult.status === "succeeded") {
											    // var identityToken = asyncResult.value;
											    // console.log(">> identityToken: " + identityToken);
											    // } else {
											    // showError("Could not get identity token: " +
											    // asyncResult.error.message);
											    // }
											    // });

											    console.log(">> savingAttachment: " + JSON.stringify(attachmentIds));
											    var $savedAttachment = $saveAttachment.find(".savedAttachment");
											    var $savedAttachmentList = $savedAttachment.find("ul#savedAttachmentList");
											    $savedAttachmentList.jzLoad("Outlook.saveAttachment()", {
											      groupId : groupId,
											      path : path,
											      ewsUrl : ewsUrl,
											      userEmail : userEmail,
											      messageId : messageId,
											      attachmentToken : attachmentToken,
											      attachmentIds : attachmentIds.join()
											    }, function(response, status, jqXHR) {
												    if (status == "error") {
													    showError(jqXHR);
													    cancelSave();
												    } else {
													    clearError();
													    var $litems = $savedAttachmentList.find("li.ms-ListItem");
													    if ($litems.size() > 0) {
														    var $savedSpaceInfo = $savedAttachment.find(".savedSpaceInfo");
														    $savedSpaceInfo.text($savedSpaceInfo.text() + " " + groupTitle);
														    $litems.each(function() {
															    $(this).ListItem(); // init FabricUI JS
															    $(this).click(function() {
																    var fileUrl = $(this).data("portalurl");
																    window.open(fileUrl, "_blank");
															    });
														    });
														    spinner.stop();
														    $savingAttachment.hide("fade");
														    $savedAttachment.show("blind");
													    } else {
														    // nothing saved, stay in the form
														    showError("Nothing was saved. Please submit form again or contact administrator.");
														    cancelSave();
													    }
												    }
											    });
										    } else {
											    showError("Error getting access token for mail server: " + asyncResult.error.message);
											    cancelSave();
										    }
									    });
								    } else {
									    showError("Attachment not selected. Select at least an one and then submit form again.");
									    $form.animate({
										    scrollTop : $attachments.offset().top - $form.offset().top + $form.scrollTop()
									    });
								    }
							    }
						    });

						    // pathInfo controls
						    $groupPath.find("#path-info>li").ListItem();
						    $pathUp.click(function() {
							    if (!$pathUp.attr("disabled")) {
								    var lastElemIndex = path.lastIndexOf("/");
								    if (lastElemIndex > 0) {
									    var origPath = path;
									    path = path.substring(0, lastElemIndex);
									    var process = loadFolder();
									    process.fail(function() {
										    path = origPath;
									    });
								    }
							    }
						    });
						    $groupPath.find(".pathOpen").click(function() {
							    if (portalUrl) {
								    window.open(portalUrl);
							    }
						    });
						    $groupPath.find(".pathAdd").click(function() {
							    // show dialog for new folder name, then create this folder
							    $popup.jzLoad("Outlook.addFolderDialog()", {}, function(response, status, jqXHR) {
								    if (status == "error") {
									    showError(jqXHR);
								    } else {
									    clearError();
									    $popup.show();
									    var $dialog = $popup.find(".addFolderDialog");
									    var $newFolderName = $dialog.find("input[name='newFolderName']");
									    var $addFolderButton = $dialog.find("button.addFolder");
									    $newFolderName.change(function() {
										    if ($(this).val()) {
											    $addFolderButton.prop("disabled", false);
										    } else {
											    $addFolderButton.prop("disabled", true);
										    }
									    });
									    var $dialogForm = $dialog.find("form");
									    $dialogForm.submit(function(event) {
										    event.preventDefault();
										    if (!$dialogForm.data("cancel")) {
											    var newFolderName = $newFolderName.val();
											    if (newFolderName) {
												    $folders.jzLoad("Outlook.addFolder()", {
												      groupId : groupId,
												      path : path,
												      name : newFolderName
												    }, function(response, status, jqXHR) {
													    if (status == "error") {
														    showError(jqXHR);
													    } else {
														    clearError();
														    $popup.hide();
														    $newFolderName.val("");
														    showFolder();
													    }
												    });
											    } else {
												    showError("Folder name required");
											    }
										    }
									    });
									    $dialog.find("button.cancelFolder").click(function() {
										    $newFolderName.val("");
										    $dialogForm.data("cancel", true);
										    $popup.hide();
									    });
								    }
							    });
						    });
					    }
				    }

				    function loadMenu(menuName) {
					    var process = $.Deferred();
					    var newMenu;
					    if (menuName) {
						    newMenu = menuName != $container.data("menu-name");
					    } else {
						    menuName = $container.data("menu-name");
						    newMenu = true;
					    }
					    // load only if not already loaded
					    if (newMenu) {
						    if (menuName) {
							    var cursorCss = $container.css("cursor");
							    $container.css("cursor", "wait");
							    $container.jzLoad("Outlook." + menuName + "Form()", {}, function(response, status, jqXHR) {
								    $container.css("cursor", cursorCss);
								    if (status == "error") {
									    showError(jqXHR);
									    process.reject(response, status, jqXHR);
								    } else {
									    clearError();
									    $container.data("menu-name", menuName); // know last loaded
									    try {
										    var commandFunc = menuName + "Init";
										    if (eval("typeof " + commandFunc + " === 'function'")) {
											    // safe to use the function
											    eval(commandFunc + "()");
										    }
										    process.resolve(response, status, jqXHR);
									    } catch(e) {
										    console.log(e);
										    console.log(e.stack);
										    var initError = e.message;
										    process.reject(initError);
									    }
								    }
							    });
						    } else {
							    showError("Menu name undefined");
							    process.reject("Menu name undefined");
						    }
					    } else {
						    process.resolve();
					    }
					    return process.promise();
				    }

				    // init menu if it found
				    if ($menu.size() > 0) {
					    var $menuItems = $menu.find(".outlookMenu");

					    var internetMessageId = Office.context.mailbox.item.internetMessageId;
					    console.log(">> internetMessageId: " + internetMessageId);
					    
					    // special logic for saveAttachment: remove it when no attachment found in the message or it's compose mode
					    var $saveAttachment = $menuItems.filter(".saveAttachment");
					    if ($saveAttachment.size() > 0
					        && (!(Office.context.mailbox.item.attachments && Office.context.mailbox.item.attachments.length > 0) || !internetMessageId)) {
						    // $saveAttachment.click(function() {
						    // var $mi = loadMenu("saveAttachment");
						    // $mi.done(function() {
						    // // saveAttachmentInit();
						    // });
						    // $mi.fail(function() {
						    // // TODO something here?
						    // });
						    // });
						    // } else {
						    $saveAttachment.parent().remove();
					    }
					    // special login for item addAttachment - show it only in compose mode
					    var $addAttachment = $menuItems.filter(".addAttachment");
					    if ($addAttachment.size() > 0 && internetMessageId) {
						    $addAttachment.parent().remove();
					    }

					    $menuItems.each(function(i, m) {
						    var $m = $(m);
						    $m.click(function() {
							    var name = $m.data("name");
							    if (name) {
								    var $mi = loadMenu(name);
								    $mi.done(function() {
									    // TODO what could be here in extra to *Init function called by loadMenu()?
								    });
								    $mi.fail(function() {
									    // TODO something additional here?
								    });
							    } else {
								    console.log("WARN: Skipped menu item without command name: " + $m.html());
							    }
						    });
					    });

					    // var $home = $menuItems.find(".home");
					    // $home.click(function() {
					    // loadMenu("home");
					    // });
				    }

				    $menu.NavBar();

				    // load first menu inside container (it is set as data attr of the container)
				    loadMenu();
			    } // end of pane init
		    });

	    };
    });
