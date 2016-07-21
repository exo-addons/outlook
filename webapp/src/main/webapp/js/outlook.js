/**
 * Hide PLF's toolbar.
 */
require([ "SHARED/jquery" ], function($) {
	// do in dedicated require to do not wait for other scripts load
	$("#PlatformAdminToolbarContainer").css("display", "none");
	$(function() {
		$("#PlatformAdminToolbarContainer").css("display", "none");
	});
});

/**
 * Outlook read pane app script.
 */
require([ "SHARED/jquery", "SHARED/outlookFabricUI", "SHARED/outlookJqueryUI", "SHARED/juzu-ajax" ], function($, fabric) {

	function pageBaseUrl() {
		var theLocation = window.location;

		var theHostName = theLocation.hostname;
		var theQueryString = theLocation.search;

		if (theLocation.port) {
			theHostName += ":" + theLocation.port;
		}

		return theLocation.protocol + "//" + theHostName;
	}

	function getMessage(key) {
		if (outlookBundle) {
			return outlookBundle.messages[key];
		} else {
			return key;
		}
	}

	function fixId(msId) {
		return msId ? msId.replace(/\//g, "-") : msId;
	}
	
	function formatISODate(date) {
		if (date) {
			// adapted script from
			// http://stackoverflow.com/questions/17415579/how-to-iso-8601-format-a-date-with-timezone-offset-in-javascript
	    var tzo = -date.getTimezoneOffset(),
	        dif = tzo >= 0 ? '+' : '-',
	        pad2 = function(num) {
	            var norm = Math.abs(Math.floor(num));
	            return (norm < 10 ? '0' : '') + norm;
	        },
	        pad3 = function(num) {
	          var norm = Math.abs(Math.floor(num));
	          return (norm < 10 ? '00' : (norm < 100 ? '0' : '')) + norm;
	        };
	    return date.getFullYear() 
	        + '-' + pad2(date.getMonth()+1)
	        + '-' + pad2(date.getDate())
	        + 'T' + pad2(date.getHours())
	        + ':' + pad2(date.getMinutes()) 
	        + ':' + pad2(date.getSeconds()) 
	        + '.' + pad3(date.getMilliseconds())
	        + dif + pad2(tzo / 60) // + ':' 
	        + pad2(tzo % 60);
		} else {
			return null;
		}
	}

	/**
	 * Method adapted from org.exoplatform.services.cms.impl.Utils.fileSize().
	 */
	function sizeString(size) {
		var byteSize = size % 1024;
		var kbSize = (size % 1048576) / 1024;
		var mbSize = (size % 1073741824) / 1048576;
		var gbSize = size / 1073741824;

		if (gbSize >= 1) {
			return gbSize.toFixed(2) + " GB";
		} else if (mbSize >= 1) {
			return mbSize.toFixed(2) + " MB";
		} else if (kbSize > 1) {
			return kbSize.toFixed(2) + " KB";
		}
		if (byteSize > 0) {
			return byteSize + " B";
		} else {
			return "";
			// return empty not 1 KB as ECMS does
		}
	}

	Office.initialize = function(reason) {

		$(function() {
			// context data
			var serverUrl = pageBaseUrl(location);
			var userEmail = Office.context.mailbox.userProfile.emailAddress;
			var userName = Office.context.mailbox.userProfile.displayName;
			console.log("> user: " + userName + "<" + userEmail + ">");
			// The itemId property returns null in compose mode for items that have not been saved to
			// the server.
			var messageId = fixId(Office.context.mailbox.item.itemId);
			console.log("> messageId: " + messageId);
			// Internet message identifier for an email message. Read mode only.
			var internetMessageId = Office.context.mailbox.item.internetMessageId;
			console.log("> internetMessageId: " + internetMessageId);

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

				function showError(source, cause) {
					var message;
					// check if source is i18n key or jqXHR (of jQuery ajax request)
					if (typeof source === "string" && source.indexOf("Outlook.messages") === 0) {
						// interpret as i18n message
						message = getMessage(source);
					} else if (source && source.hasOwnProperty("responseText")) {
						// it's jqXHR
						var text = source.responseText;
						if (!text) {
							text = source.statusText;
						}
						message = text + " (" + source.status + ")";
					} else {
						message = source;
					}
					if (cause) {
						message += " " + cause;
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

				var $menu = $pane.find("#outlook-menu");
				var $container = $pane.find("#outlook-menu-container");
				function initNoSpacesLink() {
					var $noSpacesLink = $container.find(".noSpacesMessage .ms-MessageBar-text a");
					// should replace /portal/intranet/outlook to /portal/intranet/all-spaces
					var allSpacesPath = location.pathname.replace(/\/[^\/]*$/, "/all-spaces");
					$noSpacesLink.attr("href", allSpacesPath);
				}

				function homeInit() {
					// TODO something?
				}

				function saveAttachmentInit() {
					var $saveAttachment = $("#outlook-saveAttachment");
					var $form = $saveAttachment.find("form");
					var $attachments = $form.find("ul#attachments");
					var $comment = $form.find("textarea[name='comment']");

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
								showError("Outlook.messages.folderHasNoPath");
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
							process.reject(showError("Outlook.messages.spacePathNotFound"));
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
							groupRootPath = $space.data("rootpath");
							path = $space.data("path");
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
							    + "</span><span class='ms-ListItem-metaText attachmentSize'>" + sizeString(att.size) + "</span>"
							    + "<div class='ms-ListItem-selectionTarget js-toggleSelection'></div><input name='attachmentIds' type='hidden'></li>");
							$li.data("attachmentId", fixId(att.id));
							$li.appendTo($attachments);
							$li.ListItem(); // init FabricUI JS (for a case of some extra func)
							// then disable FabricUI's click for this case and add click for the whole list item
							$li.off("click", ".js-toggleSelection");
							$li.click(function() {
								$(this).toggleClass("is-selected");
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
						var $savingAttachment = $saveAttachment.find("#savingAttachment");
						var $savedAttachment = $saveAttachment.find("#savedAttachment");
						$cancelButton.click(function() {
							$cancelButton.data("cancel", true);
						});
						$form.submit(function(event) {
							event.preventDefault();
							clearError();
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
									$form.hide("blind");
									$savingAttachment.show("blind");
									var spinner = new fabric.Spinner($savingAttachment.find(".ms-Spinner").get(0));
									spinner.start();
									function cancelSave() {
										spinner.stop();
										$savingAttachment.hide("blind", {
											"direction" : "down"
										});
										$form.show("blind", {
											"direction" : "down"
										});
									}
									Office.context.mailbox.getCallbackTokenAsync(function(asyncResult) {
										if (asyncResult.status === "succeeded") {
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
											var $savedSpaceInfo = $savedAttachment.find(".savedSpaceInfo");
											$savedSpaceInfo.jzLoad("Outlook.saveAttachment()", {
											  groupId : groupId,
											  path : path,
											  comment : $comment.val(),
											  ewsUrl : ewsUrl,
											  userEmail : userEmail,
											  userName : userName,
											  messageId : messageId,
											  attachmentToken : attachmentToken,
											  attachmentIds : attachmentIds.join()
											}, function(response, status, jqXHR) {
												if (status == "error") {
													showError(jqXHR);
													cancelSave();
												} else {
													clearError();
													var $litems = $savedSpaceInfo.find("li.ms-ListItem");
													if ($litems.size() > 0) {
														var $savedSpaceTitle = $savedAttachment.find(".savedSpaceTitle");
														$savedSpaceTitle.text($savedSpaceTitle.text() + " " + groupTitle);
														$litems.each(function() {
															$(this).ListItem(); // init FabricUI JS
															$(this).click(function() {
																var fileUrl = $(this).data("portalurl");
																window.open(fileUrl, "_blank");
															});
														});
														spinner.stop();
														$savingAttachment.hide("blind");
														$savedAttachment.show("blind");
													} else {
														// nothing saved, stay in the form
														showError("Outlook.messages.nothingSavedTryAgain");
														cancelSave();
													}
												}
											});
										} else {
											console.log(">> Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: "
											    + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
											showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
											cancelSave();
										}
									});
								} else {
									showError("Outlook.messages.attachmentNotSelected");
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
												showError("Outlook.messages.folderNameRequired");
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

				function convertToStatusInit() {
					var $convertToStatus = $("#outlook-convertToStatus");
					var $title = $convertToStatus.find("input[name='activityTitle']");
					var $text = $convertToStatus.find("div.activityText");
					var $form = $convertToStatus.find("form");
					var $groupIdDropdown = $form.find(".ms-Dropdown");
					var $groupId = $groupIdDropdown.find("select[name='groupId']");
					// $groupId.combobox(); // jQueryUI combo w/ autocompletion
					var $convertButton = $form.find("button.convertButton");
					$convertButton.prop("disabled", true);
					var $cancelButton = $form.find("button.cancelButton");
					var $converting = $convertToStatus.find("#converting");
					var $converted = $convertToStatus.find("#converted");
					var $convertedInfo = $converted.find(".convertedInfo");
					$cancelButton.click(function() {
						$cancelButton.data("cancel", true);
					});
					$convertToStatus.find(".editActivityText>a").click(function(event) {
						event.preventDefault();
						$(this).parent().hide();
						// $text.attr("contenteditable", "true");
					});

					var subject = Office.context.mailbox.item.subject;
					$title.val(subject);

					// init spaces dropdown
					$groupId.val([]); // initially no spaces selected
					var groupId;
					$groupId.change(function() {
						var $space = $groupId.find("option:selected");
						if ($space.size() > 0) {
							groupId = $space.val();
						}
					});
					$groupIdDropdown.Dropdown();

					// FYI getTypeAsync available in compose mode only
					// Office.context.mailbox.item.body.getTypeAsync({}, function(asyncResult) {
					// if (asyncResult.status === "succeeded") {
					// var textType = asyncResult.value == "html" ? asyncResult.value : "text";
					var textType = "html";
					console.log(">> convertToStatus textType: " + textType);
					Office.context.mailbox.item.body.getAsync(textType, {}, function(asyncResult) {
						if (asyncResult.status === "succeeded") {
							groupId = groupId ? groupId : "";
							console.log(">> groupId: " + groupId);
							console.log(">> title: " + $title.val());
							// console.log(">> text: " + asyncResult.value);
							$text.html(asyncResult.value);
							$form.submit(function(event) {
								event.preventDefault();
								clearError();
								$form.hide("blind");
								$converting.show("blind");
								var spinner = new fabric.Spinner($converting.find(".ms-Spinner").get(0));
								spinner.start();
								if ($cancelButton.data("cancel")) {
									loadMenu("home");
								} else {
									var created = Office.context.mailbox.item.dateTimeCreated;
									var modified = Office.context.mailbox.item.dateTimeModified;
									// from and to (it's array) have following interesting fields: displayName,
									// emailAddress
									var from = Office.context.mailbox.item.from;
									// var to = Office.context.mailbox.item.to;
									
									$convertedInfo.jzLoad("Outlook.convertToStatus()", {
									  groupId : groupId,
									  messageId : messageId,
									  subject : $title.val(),
									  body : asyncResult.value,
									  created : formatISODate(created),
									  modified : formatISODate(modified),
									  userName : userName,
									  userEmail : userEmail,
									  fromName : from.displayName,
									  fromEmail : from.emailAddress
									}, function(response, status, jqXHR) {
										if (status == "error") {
											showError(jqXHR);
											spinner.stop();
											$converting.hide("blind", {
												"direction" : "down"
											});
											$form.show("blind", {
												"direction" : "down"
											});
										} else {
											clearError();
											spinner.stop();
											$converting.hide("blind");
											$converted.show("blind");
											// TODO
											// var $activityLink = $convertedInfo.find("a.ms-Link");
										}
									});
								}
							});
							$convertButton.prop("disabled", false);
						} else {
							console.log(">> Office.context.mailbox.item.body.getAsync() [" + asyncResult.status + "] error: " + JSON.stringify(asyncResult.error)
							    + " value: " + JSON.stringify(asyncResult.value));
							showError("Outlook.messages.messageItemReadError", asyncResult.error.message);
						}
					});
					// } else {
					// showError("Outlook.messages.messageItemReadError", asyncResult.error.message);
					// }
					// });
				}

				function loadMenu(menuName) {
					var process = $.Deferred();
					var newMenu;
					if (menuName) {
						console.log(">> loadMenu: " + menuName);
						newMenu = menuName != $container.data("menu-name");
					} else {
						menuName = $container.data("menu-name");
						console.log(">> loadMenu: " + menuName + " (new from container)");
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
										initNoSpacesLink();
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
							process.reject(showError("Outlook.messages.menuNameUndefined"));
						}
					} else {
						process.resolve();
					}
					return process.promise();
				}

				// init menu if it found
				if ($menu.size() > 0) {
					var $menuItems = $menu.find(".outlookMenu");
					var $menuGroups = $menu.find(".outlookGroupMenu");

					// special logic for saveAttachment: remove it when no attachment found in the message
					// or it's compose mode
					var $saveAttachment = $menuItems.filter(".saveAttachment");
					if ($saveAttachment.size() > 0
					    && (!(Office.context.mailbox.item.attachments && Office.context.mailbox.item.attachments.length > 0) || !internetMessageId)) {
						$saveAttachment.parent().remove();
					}
					// special login for item addAttachment - show it only in compose mode
					// FYI internetMessageId will be found for sent/received message
					var $addAttachment = $menuItems.filter(".addAttachment");
					if ($addAttachment.size() > 0 && internetMessageId) {
						$addAttachment.parent().remove();
					}

					// remove convert* menus for new messages (messageId not null for messages saved on
					// the server)
					var $convertTo = $menuGroups.filter(".convertTo");
					if ($convertTo.size() > 0 && !messageId) {
						$convertTo.parent().remove();
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
				}

				$menu.NavBar();

				// load first menu inside container (it is set as data attr of the container)
				loadMenu();
			} // end of pane init
		});

	};
});
