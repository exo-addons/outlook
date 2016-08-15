/**
 * Outlook read pane app script.
 */
require(["SHARED/jquery", "SHARED/outlookFabricUI", "SHARED/outlookJqueryUI", "SHARED/juzu-ajax"], function($, fabric) {

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
			var tzo = -date.getTimezoneOffset(), dif = tzo >= 0 ? '+' : '-', pad2 = function(num) {
				var norm = Math.abs(Math.floor(num));
				return (norm < 10 ? '0' : '') + norm;
			}, pad3 = function(num) {
				var norm = Math.abs(Math.floor(num));
				return (norm < 10 ? '00' : (norm < 100 ? '0' : '')) + norm;
			};
			return date.getFullYear() //
				+ '-' + pad2(date.getMonth() + 1) //
				+ '-' + pad2(date.getDate()) //
				+ 'T' + pad2(date.getHours()) + ':' + pad2(date.getMinutes()) + ':' + pad2(date.getSeconds())
				// +  '.' + pad3(date.getMilliseconds())
				+ dif + pad2(tzo / 60)// +  ':'
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

				var showError = function(source, cause) {
					var message;
					// check if source is i18n key or jqXHR (of jQuery ajax request)
					if ( typeof source === "string" && source.indexOf("Outlook.messages") === 0) {
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
				};
				var clearError = function() {
					$error.hide("blind");
					$errorText.empty();
				};

				var messageId;
				var internetMessageId;
				var readMessageId = function(force) {
					var process = $.Deferred();
					if (force) {
						// Get the currently selected item's ID
						var ewsId = Office.context.mailbox.item.itemId;
						if (ewsId) {
							// Convert to a REST ID for the v2.0 version of the Outlook Mail API
							messageId = Office.context.mailbox.convertToRestId(ewsId, Office.MailboxEnums.RestVersion.v2_0);
							//messageId = fixId(Office.context.mailbox.item.itemId);
							console.log("> messageId: " + messageId);
							internetMessageId = Office.context.mailbox.item.internetMessageId;
							console.log("> internetMessageId: " + internetMessageId);
						}
					}
					if (messageId) {
						process.resolve(messageId);
					} else if (!internetMessageId) {
						// If in compose form: save (as draft) and then get message ID
						Office.context.mailbox.item.saveAsync(function(asyncResult) {
							if (asyncResult.status === "succeeded") {
								messageId = Office.context.mailbox.convertToRestId(asyncResult.value, Office.MailboxEnums.RestVersion.v2_0);
								console.log(">> messageId: " + messageId);
								internetMessageId = Office.context.mailbox.item.internetMessageId;
								console.log(">> internetMessageId: " + internetMessageId);
								process.resolve(messageId);
							} else {
								console.log(">> Office.context.mailbox.item.saveAsync() [" + asyncResult.status + "] error: " //
									+ JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
								showError("Outlook.messages.savingMessageError", asyncResult.error.message);
								process.reject();
							}
						});
					} else {
						console.log("> itemId not found for " + internetMessageId);
						showError("Outlook.messages.messageIdNotFound", internetMessageId);
						process.reject();
					}
					return process.promise();
				};
				readMessageId(true);

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
					var $attachments = $form.find("ul.attachments");
					var $comment = $form.find("textarea[name='comment']");

					// init spaces dropdown
					var $groupIdDropdown = $form.find(".ms-Dropdown");
					var $groupIdSelect = $groupIdDropdown.find("select[name='groupId']");
					$groupIdSelect.val([]);
					// initially no spaces selected
					var $groupPath = $form.find(".group-path");
					var $folders = $groupPath.find("ul.folders");
					var $path = $groupPath.find("input[name='path']");
					var $pathInfo = $groupPath.find("input.pathInfo");
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
						$litems.ListItem();
						// init FabricUI JS
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
						for ( i = 0; i < item.attachments.length; i++) {
							var att = item.attachments[i];
							// var outputString = "";
							// outputString += i + ". Name: " + att.name + " ID: " + att.id;
							// outputString += " contentType: " + att.contentType;
							// outputString += " size: " + att.size;
							// outputString += " attachmentType: " + att.attachmentType;
							// outputString += " isInline: " + att.isInline;
							// console.log(outputString);
							var $li = $("<li class='ms-ListItem is-selectable'><span class='ms-ListItem-primaryText'>" 
								+ att.name + "</span><span class='ms-ListItem-metaText attachmentSize'>" // 
								+ sizeString(att.size) + "</span>" //
								+ "<div class='ms-ListItem-selectionTarget js-toggleSelection'></div><input name='attachmentIds' type='hidden'></li>");
							$li.data("attachmentId", Office.context.mailbox.convertToRestId(att.id, Office.MailboxEnums.RestVersion.v2_0));
							$li.appendTo($attachments);
							$li.ListItem();
							// init FabricUI JS (for a case of some extra func)
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
							// add preselect it by default
							$li.click();
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
															$(this).ListItem();
															// init FabricUI JS
															// $(this).click(function() {
															// var fileUrl = $(this).data("portalurl");
															// window.open(fileUrl, "_blank");
															// });
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
											console.log(">> Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " // 
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

						// currentFolder controls
						$groupPath.find("ul.currentFolder>li").ListItem();
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
					var $editor = $convertToStatus.find("div.activityEditor");
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
						$editor.append($text.children());
						$text.hide();
						$editor.show();
						$text = $editor;
					});

					// init spaces dropdown
					$groupId.val([]);
					// initially no spaces selected
					var groupId;
					$groupId.change(function() {
						var $space = $groupId.find("option:selected");
						if ($space.size() > 0) {
							groupId = $space.val();
						}
					});
					$groupIdDropdown.Dropdown();

					var subject = Office.context.mailbox.item.subject;
					if (internetMessageId) {
						$title.val(subject);
					} else {
						Office.context.mailbox.item.subject.getAsync(function callback(asyncResult) {
							if (asyncResult.status === "succeeded") {
								$title.val(asyncResult.value);
							} else {
								console.log(">> Office.context.mailbox.item.subject.getAsync() [" + asyncResult.status + "] error: " //
									+ JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
								showError("Outlook.messages.gettingSubjectError", asyncResult.error.message);
							}
						});
					}

					// get a token to read message from server side
					Office.context.mailbox.getCallbackTokenAsync(function(asyncResult) {
						if (asyncResult.status === "succeeded") {
							var messageToken = asyncResult.value;
							var midProcess = readMessageId();
							midProcess.done(function(mid) {
								console.log(">> getMessage(): " + mid + " token:" + messageToken);
								if (mid) {
									var ewsUrl = Office.context.mailbox.ewsUrl;
									console.log(">> ewsUrl: " + ewsUrl);
									$text.jzLoad("Outlook.getMessage()", {
										ewsUrl : ewsUrl,
										userEmail : userEmail,
										userName : userName,
										messageId : mid,
										messageToken : messageToken
									}, function(response, status, jqXHR) {
										if (status == "error") {
											showError(jqXHR);
										} else {
											clearError();
											groupId = groupId ? groupId : "";
											console.log(">> groupId: " + groupId);
											console.log(">> title: " + $title.val());
											var textType = jqXHR.getResponseHeader("X-MessageBodyContentType");
											textType = textType ? textType : "html";
											console.log(">> convertToStatus textType: " + textType);
											$convertButton.prop("disabled", false);
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
													$convertedInfo.jzLoad("Outlook.convertToStatus()", {
														groupId : groupId,
														messageId : mid,
														subject : $title.val(),
														body : $text.html(),
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
														}
													});
												}
											});
										}
									});
								} else {
									showError("Outlook.messages.messageIdNotFound", internetMessageId);
								}
							});
							midProcess.fail(function() {
								console.log(">> getMessage() failed to read messageId ");
							});
						} else {
							console.log(">> Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " // 
								+ JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
							showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
						}
					});
				}

				function addAttachmentInit() {
					var $addAttachment = $("#outlook-addAttachment");
					var $documentSelector = $addAttachment.find("#documentSelector");
					var $sourceDropdown = $documentSelector.find(".sourceDropdown");
					var $source = $sourceDropdown.find("select[name='source']");
					// $source.combobox(); // jQueryUI combo w/ autocompletion
					var $searchTab = $documentSelector.find("button.searchTab");
					var $explorerTab = $documentSelector.find("button.explorerTab");
					var $documentSearch = $documentSelector.find(".documentSearch");
					var $documentSearchForm = $documentSearch.find("form");
					var $documentSearchInput = $documentSearch.find("input");
					var $documentSearchResults = $documentSearch.find("ul.documentSearchResults");
					var $documentExplorer = $documentSelector.find(".documentExplorer");
					var $currentFolder = $documentExplorer.find("ul.currentFolder");
					var $folderFiles = $documentExplorer.find("ul.folderFiles");
					var $pathInfo = $currentFolder.find(".pathInfo");
					var $pathUp = $currentFolder.find(".pathControls .pathUp");
					var $pathOpen = $currentFolder.find(".pathControls .pathOpen");
					
					var $attach = $addAttachment.find("#attach");
					var $documents = $attach.find("ul.documents");
					var $attachButton = $attach.find("button.attachButton");
					$attachButton.prop("disabled", true);
					var $cancelButton = $attach.find("button.cancelButton");
					
					var $attaching = $addAttachment.find("#attaching");
					var $attached = $addAttachment.find("#attached");
					
					// initially no source selected
					var sourceId, sourceTitle, sourceRootPath;
					var path, pathLabel, portalUrl;
					
					var isSelected = function(fpath) {
						return $documents.find("li.ms-ListItem").filter(function() {
							return $(this).data("path") == fpath;
						}).size() > 0;
					};
					
					var initFiles = function($files, loadChildred) {
						// FYI loadChildred is optional and required for explorer only
						var $litems = $files.find("li.ms-ListItem");
						$litems.ListItem();
						$litems.each(function(i, li) {
							var $li = $(li);
							var fpath = $li.data("path");
							if (fpath && isSelected(fpath)) {
								$li.addClass("is-selected");
							}
							$li.find(".size").each(function(i, se) {
								var size = parseInt($(se).text());
								if (size) {
									var sizeText = sizeString(size);
									$(se).text(sizeText);
								}
							});
						});

						// init files
						$litems.click(function() {
							var $child = $(this);
							var fpath = $child.data("path");
							var isFolder = $child.data("isfolder");
							if (isFolder === "true") {
								// navigate into this folder
								if (fpath) {
									path = fpath;
									pathLabel = $child.data("pathlabel");
									portalUrl = $child.data("portalurl");
									if (loadChildred) {
										loadChildred();
									} else {
										showError("Navigation not possible for this item.");
									}
								} else {
									showError("Outlook.messages.folderHasNoPath");
								}
							} else {
								// add to selected documents
								if (fpath) {
									if (isSelected(fpath)) {
										$child.removeClass("is-selected");
										$documents.find("li.ms-ListItem").each(function(i, li) {
											var $li = $(li);
											if ($li.data("path") == fpath) {
												$li.remove();
											}
										});
									} else {
										var $selected = $child.clone();
										// clone w/o data/events
										$selected.data("path", fpath);
										$selected.ListItem();
										$selected.click(function() {
											$selected.toggleClass("is-selected");
											// here also check/uncheck in $folderFiles
											if ($selected.hasClass("is-selected")) {
												$child.addClass("is-selected");
											} else {
												$child.removeClass("is-selected");
											}
										});
										$selected.find(".pathControls").click(function(event) {
											event.stopPropagation();
										});
										// add preselect it
										$selected.click();
										// add to selected documents
										$selected.appendTo($documents);
									}
								} else {
									showError("Outlook.messages.fileHasNoPath");
								}
								$attachButton.prop("disabled", $documents.find("li.ms-ListItem.is-selected").size() === 0);
							}
						});
						$litems.find(".pathControls").click(function(event) {
							event.stopPropagation();
						});
					};

					var searchFiles = function(text) {
						var process = $.Deferred();
						$documentSearchResults.jzLoad("Outlook.searchFiles()", {
							sourceId : sourceId,
							text : text
						}, function(response, status, jqXHR) {
							if (status == "error") {
								showError(jqXHR);
							} else {
								clearError();
								// init results
								initFiles($documentSearchResults);
							}
						}); 
					};
					
					// init sources dropdown
					//$source.val([]);
					$source.change(function() {
						var $s = $source.find("option:selected");
						if ($s.size() > 0) {
							clearError();
							sourceId = $s.val();
							path = sourceRootPath = $s.data("rootpath");
							portalUrl = $s.data("portalurl");
							// TODO pre-load source files:
							// for All spaces try gather last used files ordered by access/modification date first
							// for Personal Docs gather last used from user's documents
							// for a space gather last used from that space
							// Having last used files (up to 20 items), prefill the search pane results with it and show the pane
							// for Personal Docs and space make Explorer tab visible, when user click it then load root folder files.
							searchFiles("");
							// TODO When user will click a file in search or explorer pane, check the file checkbox and add it to the selected
						}
						$explorerTab.prop("disabled", sourceId == "*");
					});
					$sourceDropdown.Dropdown();
					
					// init Search Tab
					$searchTab.click(function() {
						clearError();
						$explorerTab.toggleClass("ms-Button--primary");
						$searchTab.toggleClass("ms-Button--primary");
						$documentExplorer.hide();
						$documentSearch.show();
					});
					
					// init search form
					$documentSearchForm.find(".ms-SearchBox").SearchBox();
					$documentSearchForm.submit(function(event) {
						event.preventDefault();
						clearError();
						searchFiles($documentSearchInput.val());
					});
					
					// init Explore Tab
					$explorerTab.click(function() {
						clearError();
						$explorerTab.toggleClass("ms-Button--primary");
						$searchTab.toggleClass("ms-Button--primary");
						$documentSearch.hide();
						$documentExplorer.show();
						var loadChildred = function() {
							if (sourceId && path) {
								console.log(">> loadChildred: " + sourceId + " >> " + path);
								$folderFiles.jzLoad("Outlook.exploreFiles()", {
									sourceId : sourceId,
									path : path
								}, function(response, status, jqXHR) {
									if (status == "error") {
										showError(jqXHR);
									} else {
										clearError();
										// show children
										var $parentFolder = $folderFiles.find(".parentFolder");
										path = $parentFolder.data("path");
										pathLabel = $parentFolder.data("pathlabel");
										portalUrl = $parentFolder.data("portalurl");
										if (sourceRootPath == path) {
											$pathUp.attr("disabled", "true");
										} else {
											$pathUp.removeAttr("disabled");
										}
										$pathInfo.val(pathLabel);
										initFiles($folderFiles, loadChildred);
									}
								});
							} else {
								console.log(">> loadChildred: sourceId and/or path not found");
								showError("Outlook.messages.sourcePathNotFound");
							}
						};
						loadChildred(); 
					});
					
					// init Cancel button
					$cancelButton.click(function() {
						$cancelButton.data("cancel", true);
					});
					
					// init Attach button (as form submit)
					$attach.find("form").submit(function(event) {
						event.preventDefault();
						clearError();
						$documentSelector.hide();
						$attach.hide("blind");
						$attaching.show("blind");
						var spinner = new fabric.Spinner($attaching.find(".ms-Spinner").get(0));
						spinner.start();
						if ($cancelButton.data("cancel")) {
							loadMenu("home");
						} else {
							var hasError = false;
							var $attachedDocuments = $attached.find(".documents");
							$documents.find("li.ms-ListItem.is-selected").each(function(i, li) {
								var $selected = $(li);
								var title = $selected.find(".ms-ListItem-primaryText").text();
								// XXX we cannot use WebDAV link as it requires authentication in eXo
								//var downloadUrl = $selected.data("downloadurl");
								var fpath = $selected.data("path");
								var $fileLink = $selected.jzAjax("Outlook.fileLink()", {
									type : 'POST',
									data : {
										nodePath : fpath
									}
								});
								
								var $attachedDoc = $selected.clone();
								$attachedDoc.find(".lastModified").remove();
								$attachedDoc.find(".size").remove();
								$attachedDoc.find(".pathControls").remove();
								$attachedDoc.find(".ms-ListItem-selectionTarget").remove();
								$attachedDoc.removeClass("is-selected");
								$attachedDoc.removeClass("selectableItem");
								var $docName = $attachedDoc.find(".ms-ListItem-primaryText");
								
								$fileLink.done(function(response, status, jqXHR) {
									var link = response.link;
									Office.context.mailbox.item.addFileAttachmentAsync(link, title, {}, function(asyncResult) {
										if (asyncResult.status === "succeeded") {
											$docName.prepend("<i class='ms-Icon ms-Icon--checkbox ms-font-m ms-fontColor-green'>");
										} else {
											hasError = true;
											console.log(">> Office.context.mailbox.item.addFileAttachmentAsync() [" + asyncResult.status + "] error: "//
											+ JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
											// TODO show error state in added pane within a document styled in red
											$attachedDoc.addClass("ms-bgColor-error");
											$docName.prepend("<i class='ms-Icon ms-Icon--alert ms-font-m ms-fontColor-error'>");
											$docName.after("<div class='ms-ListItem-tertiaryText addedError'>" + asyncResult.error.message + "</div>");
										}
									});
								});
								$fileLink.fail(function(jqXHR, textStatus, errorThrown) {
									hasError = true;
									console.log(">> Outlook.fileLink() [" + textStatus + "]: "//
											+ errorThrown + " response: " + jqXHR.responseText);
									$attachedDoc.addClass("ms-bgColor-error");
									$docName.prepend("<i class='ms-Icon ms-Icon--alert ms-font-m ms-fontColor-error'>");
									$docName.after("<div class='ms-ListItem-tertiaryText linkError'>" + jqXHR.responseText + "</div>");
								});
								
								$attachedDoc.appendTo($attachedDocuments);
							}); 

							if (hasError) {
								showError("Outlook.messages.addingAttachmentError");
							}
							
							$attaching.hide("blind");
							$attached.show("blind");
						}
					});
					
					// do initial search for better UX (first should be 'All Spaces')
					$source.val($source.find("option:first").val());
					$source.change();
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
									$container.data("menu-name", menuName);
									// know last loaded
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
					if ($saveAttachment.size() > 0 && 
							(!(Office.context.mailbox.item.attachments && Office.context.mailbox.item.attachments.length > 0) || !internetMessageId)) {
						$saveAttachment.parent().remove();
					}
					// special logic for item addAttachment - show it only in compose mode
					// FYI internetMessageId will be found for sent/received message
					var $addAttachment = $menuItems.filter(".addAttachment");
					if ($addAttachment.size() > 0 && internetMessageId) {
						$addAttachment.parent().remove();
					}
					// remove convert* menus for compose form (internetMessageId will be null)
					var $convertTo = $menuGroups.filter(".convertTo");
					if ($convertTo.size() > 0 && !internetMessageId) {
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

