/**
 * Outlook read pane app script.
 */
require(["SHARED/jquery", "SHARED/outlookFabricUI", "SHARED/outlookJqueryUI", "SHARED/juzu-ajax"], function ($, fabric) {

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

  function formatISODate(date) {
    if (date) {
      // adapted script from
      // http://stackoverflow.com/questions/17415579/how-to-iso-8601-format-a-date-with-timezone-offset-in-javascript
      var tzo = -date.getTimezoneOffset(), dif = tzo >= 0 ? '+' : '-', pad2 = function (num) {
        var norm = Math.abs(Math.floor(num));
        return (norm < 10 ? '0' : '') + norm;
      }, pad3 = function (num) {
        var norm = Math.abs(Math.floor(num));
        return (norm < 10 ? '00' : (norm < 100 ? '0' : '')) + norm;
      };
      return date.getFullYear() //
        + '-' + pad2(date.getMonth() + 1) //
        + '-' + pad2(date.getDate()) //
        + 'T' + pad2(date.getHours()) + ':' + pad2(date.getMinutes()) + ':' + pad2(date.getSeconds())
        // + '.' + pad3(date.getMilliseconds())
        + dif + pad2(tzo / 60)// + ':'
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

  /**
   * Returns the version of Windows Internet Explorer or a -1 (indicating the use of another browser).
   */
  function getIEVersion() {
    var rv = -1;
    // Return value assumes failure.
    if (navigator.appName == "Microsoft Internet Explorer") {
      var ua = navigator.userAgent;
      var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
        rv = parseFloat(RegExp.$1);
    }
    return rv;
  }

  /**
   * Add style to the given document (to the end of head).
   */
  function loadStyle(cssUrl, theDocument) {
    if (theDocument.createStyleSheet) {
      theDocument.createStyleSheet(cssUrl); // IE way
    } else {
      if ($("head", theDocument).find("link[href='" + cssUrl + "']").length == 0) {
        var headElems = theDocument.getElementsByTagName("head");
        var style = theDocument.createElement("link");
        style.type = "text/css";
        style.rel = "stylesheet";
        style.href = cssUrl;
        headElems[headElems.length - 1].appendChild(style);
      } // else, already added
    }
  }

  var isIOS = /iPhone|iPod|iPad/.test(navigator.userAgent);

  Office.initialize = function (reason) {

    $(function () {
      // context data
      var serverUrl = pageBaseUrl(location);
      var userEmail = Office.context.mailbox.userProfile.emailAddress;
      var userName = Office.context.mailbox.userProfile.displayName;
      console.log("Url - " + serverUrl + " user: " + userName + " < " + userEmail + " > ");

      var from = Office.context.mailbox.item.from;
      var internetMessageId = Office.context.mailbox.item.internetMessageId;

      // init main pane page
      var $pane = $("#outlook-pane");
      if ($pane.length > 0) {
        // TODO Hide eXo Tribe's Feedback widget
        // $("#btnFeedback").hide();

        var $error = $pane.find("#outlook-error");
        var $messageBanner = $error.find(".ms-MessageBanner");
        var $popup = $pane.find("#outlook-popup");
        var messageBanner;
        if ($messageBanner.length > 0) {
          messageBanner = new fabric.MessageBanner($messageBanner.get(0));
        }
        var $errorText = $error.find(".ms-MessageBanner-clipper");

        var showError = function (source, cause) {
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
          $("<i class='ms-Icon ms-Icon--alert ms-font-m ms-fontColor-error'></i><span>" + message + "</span>").appendTo($errorText);
          messageBanner.showBanner();
          $error.show("blind", {
            "direction": "down"
          });
          return message;
        };
        var clearError = function () {
          $error.hide("blind");
          $errorText.empty();
        };

        var messageId;
        var internetMessageId;
        var readMessageId = function (force) {
          var process = $.Deferred();
          if (force) {
            // Get the currently selected item's ID
            var ewsId = Office.context.mailbox.item.itemId;
            if (ewsId) {
              // Convert to a REST ID for the v2.0 version of the Outlook Mail API
              messageId = Office.context.mailbox.convertToRestId(ewsId, Office.MailboxEnums.RestVersion.v2_0);
              console.log("messageId: " + messageId);
              internetMessageId = Office.context.mailbox.item.internetMessageId;
              console.log("internetMessageId: " + internetMessageId);
            }
          }
          if (messageId) {
            process.resolve(messageId);
          } else if (!internetMessageId) {
            // If in compose form: save (as draft) and then get message ID
            Office.context.mailbox.item.saveAsync(function (asyncResult) {
              if (asyncResult.status === "succeeded") {
                messageId = Office.context.mailbox.convertToRestId(asyncResult.value, Office.MailboxEnums.RestVersion.v2_0);
                internetMessageId = Office.context.mailbox.item.internetMessageId;
                process.resolve(messageId);
              } else {
                console.log("Office.context.mailbox.item.saveAsync() [" + asyncResult.status + "] error: " //
                  + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                showError("Outlook.messages.savingMessageError", asyncResult.error.message);
                process.reject();
              }
            });
          } else {
            console.log("itemId not found for " + internetMessageId);
            showError("Outlook.messages.messageIdNotFound", internetMessageId);
            process.reject();
          }
          return process.promise();
        };
        readMessageId(true);

        var $menu = $pane.find("#outlook-menu");
        var $container = $pane.find("#outlook-menu-container");

        var initRefresh = function ($form, refreshFunc) {
          var $refresh = $form.find(".menuRefresh>a");
          $refresh.click(function () {
            var $textFieldLabel = $refresh.parent().parent();
            var $spaces = $form.find(".spacesDropdown");
            var cursorCss = $container.css("cursor");
            $container.css("cursor", "wait");
            try {
              // XXX warm up the portal to avoid 302 response and loading the whole page
              // if user has used the portal externally (e.g. joined a space)
              $spaces.jzAjax("Outlook.userSpaces()");
              // Do actual request waiting a bit for the portal server
              setTimeout(function () {
                $spaces.jzLoad("Outlook.userSpaces()", {}, function (response, status, jqXHR) {
                  $container.css("cursor", cursorCss);
                  if (status == "error") {
                    showError(jqXHR);
                  } else {
                    clearError();
                    if (refreshFunc) {
                      refreshFunc();
                    }
                  }
                });
              }, 750);
            } catch (e) {
              console.log("Error loading user spaces", e);
            }
          });
        };

        var initNoSpacesLink = function ($message) {
          if ($message.length > 0) {
            var $noSpacesLink = $message.find(".ms-MessageBar-text a.joinSpacesLink");
            // should replace /portal/intranet/outlook to /portal/intranet/all-spaces
            var allSpacesPath = location.pathname.replace(/\/[^\/]*$/, "/all-spaces");
            $noSpacesLink.attr("href", allSpacesPath);
          }
        };

        var setDropdownSize = function ($dropdown) {
          // set dropdown items height exact to what it contains (not 100% for block element)
          var $items = $dropdown.find(".ms-Dropdown-items");
          var $itemsList = $items.find(".ms-Dropdown-item");
          if ($itemsList.length > 0) {
            $items.height(1 + $itemsList.first().height() * $itemsList.length);
          }
        };

        var initSpacesDropdown = function ($form, value, onChangeFunc) {
          function createDropdown(value) {
            var $dropdown = $form.find(".ms-Dropdown");
            var $select = $dropdown.find("select[name='groupId']");
            var inOptions = false;
            $select.find("option").each(function () {
              if (!inOptions) {
                inOptions = $(this).val() == value;
              }
            });
            var selected;
            if (inOptions) {
              $select.val(value);
              selected = value;
            } else {
              $select.val([]);
              selected = null;
            }
            $dropdown.Dropdown();
            // $select.combobox(); // TODO jQueryUI combo w/ autocompletion
            setDropdownSize($dropdown);
            $select.change(function () {
              var $space = $select.find("option:selected");
              selected = $space.val();
              onChangeFunc($space, $select);
            });
            var $description = $form.find(".spaceDescription");
            var $message = $form.find(".noSpacesMessage");
            if ($dropdown.data("spacesnumber") > 0) {
              $message.hide();
              $description.show();
            } else {
              initNoSpacesLink($message);
              $description.hide();
              $message.show();
            }
            return {
              component: function () {
                console.log("Start initSpacesDropdown component function");
                return $dropdown;
              },
              value: function () {
                console.log("Start initSpacesDropdown selected function");
                return selected;
              }
            };
          }

          var dropdown = createDropdown(value);

          initRefresh($form, function () {
            var selected = dropdown.value();
            var newDropdown = createDropdown(selected);
            dropdown.component = newDropdown.component;
            dropdown.value = newDropdown.value;
          });

          return dropdown;
        };

        function homeInit() {
          // TODO something?
        }

// These are common features for userInfo in compose and read mode
        function getConnections(messageType = Office.context.mailbox.item.to, isComposeMode = false) {
          var $userInfo = $("#outlook-userInfo");
          var $users = $userInfo.find(".compose-Persona");
          var presentUsers = "";
          var $overlay = $("#otherConnection");

          for (var i = 0; i < $users.length; i++) {
            var names = $users[i].getAttribute("id").split(",");
            presentUsers += names[2] + ","
          }
          console.log("Prezent user in compose mode - "+presentUsers);
          $overlay.jzLoad("Outlook.userInfoConnections()", {presentUsers:presentUsers},
            function (response, status, jqXHR) {
              if (status === "error") {
                showError(jqXHR);
              } else {
                if ($.fn.PersonaCard) {
                  $userInfo.find(".ms-PersonaCard").PersonaCard();
                }
                $overlay.find(".letter-btn").hide();
                $searchInput = $overlay.find(".ms-CommandBarSearch-input");

                $overlay.find(".menu-btn").click(function () {
                  $(this).toggleClass("activeMenu-btn");
                  if ($(this).hasClass("activeMenu-btn")){
                    $userDetails = $overlay.find("#user-details-" + $(this).attr("id"));
                    showUserDetails($(this).attr("id"), $userDetails);
                    $userDetails.css("max-height","none");
                  } else {
                    $userDetails.css("max-height","0");
                  }
                });

                $overlay.find(".add-btn").click(function () {
                  if (isComposeMode){
                    addRecipients($(this).attr("id"),messageType);
                    $(this).closest(".compose-Persona").hide();
                  } else {
                    var recipient = $(this).attr("id");
                    Office.context.mailbox.displayNewMessageForm(
                      {
                        toRecipients: [recipient],
                      });
                  }
                });

                $searchInput.change(function () {
                  var $users = $overlay.find(".compose-Persona");
                  console.log($users);
                  for (var i = 0; i < $users.length; i++) {
                    var names = $users[i].getAttribute("id").split(",");
                    if (names[0].toLowerCase().startsWith($(this).val().toLowerCase()) ||
                      names[1].toLowerCase().startsWith($(this).val().toLowerCase()) ||
                      names[2].toLowerCase().startsWith($(this).val().toLowerCase())) {
                      $($users[i]).show();
                    } else {
                      $($users[i]).hide();
                    }
                  }
                });
              }
            });
        }

        function showUserDetails(user, $ob) {
          $ob.jzLoad("Outlook.userInfoDetails()", {user: user},
            function (response, status, jqXHR) {
              if (status === "error") {
                showError(jqXHR);
              } else {
              }
            });
        }

        function addRecipients(emailAddress,messageType) {
          if(!messageType){
            messageType = item.to;
          }
          messageType.addAsync(
            [{
              "displayName": emailAddress,
              "emailAddress": emailAddress
            }],
            function (asyncResult) {
              if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                write(asyncResult.error.message);
              } else {
              }
            });
        }

        function userInfoComposeInit() {
          var item = Office.context.mailbox.item;

          var presentEmail = "";
          var typeOfMessage = [];

          var to = item.to;
          var cc = item.cc;
          var bcc = item.bcc;

          to.getAsync(function (asyncResult) {
            if (asyncResult.status === Office.AsyncResultStatus.Failed) {
              console.log(asyncResult.error.message);
            } else {
              if (asyncResult.value.length > 0) {
                typeOfMessage.push(to);
                for (var i = 0; i < asyncResult.value.length; i++) {
                  if (!presentEmail.includes(asyncResult.value[i].emailAddress.toLowerCase())) {
                    presentEmail += asyncResult.value[i].emailAddress.toLowerCase() + ",";
                  }
                }
              }
              cc.getAsync(function (asyncResult) {
                if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                  console.log(asyncResult.error.message);
                } else {
                  if (asyncResult.value.length > 0) {
                    typeOfMessage.push(cc);
                    for (var i = 0; i < asyncResult.value.length; i++) {
                      if (!presentEmail.includes(asyncResult.value[i].emailAddress.toLowerCase())) {
                        presentEmail += asyncResult.value[i].emailAddress.toLowerCase() + ",";
                      }
                    }
                  }
                  bcc.getAsync(function (asyncResult) {
                    if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                      console.log(asyncResult.error.message);
                    } else {
                      if (asyncResult.value.length > 0) {
                        typeOfMessage.push(bcc);
                        for (var i = 0; i < asyncResult.value.length; i++) {
                          if (!presentEmail.includes(asyncResult.value[i].emailAddress.toLowerCase())) {
                            presentEmail += asyncResult.value[i].emailAddress.toLowerCase() + ",";
                          }
                        }
                      }
                      if (typeOfMessage.length > 1) {
                        typeOfMessage[0] = item.to;
                      }
                      loadRecipients(presentEmail,typeOfMessage[0]);
                    }
                  });
                }
              });
            }
          });

          function loadRecipients(presentEmail,messageType) {
            var $userInfo = $("#outlook-userInfo");
            $userInfo.jzLoad("Outlook.userInfoRecipients()", {
                presentEmail:presentEmail
              },
              function (response, status, jqXHR) {
                if (status === "error") {
                  showError(jqXHR);
                } else {
                  clearError();
                  $(function () {
                    var $userDetails;
                    $bigPlus = $userInfo.find(".bigPlus");
                    $userInfo.find(".createMessage-btn").hide();

                    if ($.fn.PersonaCard) {
                      $userInfo.find(".ms-PersonaCard").PersonaCard();
                    }

                    $bigPlus.click(function () {
                      $(this).toggleClass("activeBigPlus");
                      if ($(this).hasClass("activeBigPlus")){
                        getConnections(messageType, true);
                        $("#recipientForm").hide();
                      } else {
                        $("#otherConnection").empty();
                        $("#recipientForm").show();
                        userInfoComposeInit();
                      }
                    });


                    if(presentEmail.length < 5){
                      $bigPlus.trigger("click");
                    }

                    $userInfo.find(".remove-btn").click(function () {
                      removeRecipients($(this).attr("id"));
                      $(this).closest(".compose-Persona").remove();
                    });

                    $userInfo.find(".menu-btn").click(function () {
                      $(this).toggleClass("activeMenu-btn");
                      if ($(this).hasClass("activeMenu-btn")){
                        $userDetails = $userInfo.find("#user-details-" + $(this).attr("id"));
                        showUserDetails($(this).attr("id"), $userDetails);
                        $userDetails.css("max-height","none");
                      } else {
                        $userDetails.css("max-height","0");
                      }
                    });

                    function removeRecipients(emailAddress) {
                      messageType.getAsync(function (asyncResult) {
                        if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                          write(asyncResult.error.message);
                        } else {
                          var list = [];
                          for (var i = 0; i < asyncResult.value.length; i++) {
                            if (emailAddress === asyncResult.value[i].emailAddress.toLowerCase()) {
                            } else {
                              list.push({
                                "displayName": asyncResult.value[i].emailAddress.toLowerCase(),
                                "emailAddress": asyncResult.value[i].emailAddress.toLowerCase()
                              });
                            }
                          }

                          messageType.setAsync(list, function (asyncResult) {
                            if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                              write(asyncResult.error.message);
                            } else {
                              // $(document.getElementById(emailAddress)).removeClass("activeAdd-btn");
                            }
                          });
                        }
                      });
                    }
                  });
                }
              });
          }
        }

        function userInfoReadInit() {
          var item = Office.context.mailbox.item;

          function loadUserInfo(byEmail) {
            var $userInfo = $("#outlook-userInfo");
            $userInfo.jzLoad("Outlook.userInfoRecipients()", {
              presentEmail: byEmail
            }, function (response, status, jqXHR) {
              if (status == "error") {
                showError(jqXHR);
              } else {
                clearError();
                $(function () {
                  if ($.fn.PersonaCard) {
                    $userInfo.find(".ms-PersonaCard").PersonaCard();
                  }

                  var $bigPlus = $userInfo.find(".bigPlus");
                  $bigPlus.click(function () {
                    $(this).toggleClass("activeBigPlus");
                    if ($(this).hasClass("activeBigPlus")){
                      getConnections();
                      $("#recipientForm").hide();
                    } else {
                      $("#otherConnection").empty();
                      $("#recipientForm").show();
                    }
                  });

                  $userInfo.find(".remove-btn").click( function () {
                    var recipients = byEmail.split(",");
                    recipients.splice(recipients.indexOf($(this).attr("id")),1);

                    var list = [];
                    for (i = 0; i<recipients.length; i++) {
                      list.push({
                        "displayName": recipients[i].toLowerCase(),
                        "emailAddress": recipients[i].toLowerCase()
                      });
                    }

                    function set(list){
                      Office.context.mailbox.item.to.setAsync(list, function (asyncResult) {
                        if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                          write(asyncResult.error.message);
                        } else {

                        }
                      });
                    }

                    Office.context.mailbox.item.displayReplyAllForm(
                      { 'callback' : function (asyncResult) {
                          console.log("REZULT");
                          console.log(asyncResult);
                          console.log(Office.context.mailbox.item.to);
                          // var list = [];
                          // for (i = 0; i<recipients.length; i++) {
                          //   list.push({
                          //     "displayName": recipients[i].toLowerCase(),
                          //     "emailAddress": recipients[i].toLowerCase()
                          //   });
                          // }
                          console.log(list);
                          console.log(item.to);

                          set(list);

                          // Office.context.mailbox.item.to.setAsync(list, function (asyncResult) {
                          //   if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                          //     write(asyncResult.error.message);
                          //   } else {
                          //     $(document.getElementById(emailAddress)).removeClass("activeAdd-btn");
                          //   }
                          // });

                        }},
                      );
                  });

                  $userInfo.find(".menu-btn").click(function () {
                    $(this).toggleClass("activeMenu-btn");
                    if ($(this).hasClass("activeMenu-btn")){
                      $userDetails = $userInfo.find("#user-details-" + $(this).attr("id"));
                      showUserDetails($(this).attr("id"), $userDetails);
                      $userDetails.css("max-height","none");
                    } else {
                      $userDetails.css("max-height","0");
                    }
                  });

                });
              }
            });
          }

          var recipientEmails = "";

          function addEmailsIfNotUser(recipients) {
            if (recipients != null) {
              for (var i = 0; i < recipients.length; i++) {
                if (recipients[i].emailAddress != userEmail) {
                  recipientEmails += recipients[i].emailAddress + ",";
                }
              }
            }
          }
          if (Office.context.mailbox.item.conversationId) {
            if (from.emailAddress !== userEmail) {
              recipientEmails += from.emailAddress + ",";
            }
            addEmailsIfNotUser(from);
            var toCopy = Office.context.mailbox.item.to;
            addEmailsIfNotUser(toCopy);
            var carbonCopy = Office.context.mailbox.item.cc;
            addEmailsIfNotUser(carbonCopy);
            loadUserInfo(recipientEmails.toLowerCase());
          } else {
            var $userInfo = $("#outlook-userInfo");
            $userInfo.jzLoad("Outlook.userInfoConnections()", {
              presentEmail: ""
            }, function (response, status, jqXHR) {
              if (status === "error") {
                showError(jqXHR);
              } else {
                $userInfo.find(".add-btn").hide();
                if ($.fn.PersonaCard) {
                  $userInfo.find(".ms-PersonaCard").PersonaCard();
                }
                $userInfo.find(".letter-btn").click( function () {
                  var recipient = $(this).attr("id");
                  Office.context.mailbox.displayNewMessageForm(
                    {
                      toRecipients: [recipient],
                    });
                });

                $searchInput =  $userInfo.find(".ms-CommandBarSearch-input");
                $searchInput.change(function () {
                  var $users = $overlay.find(".compose-Persona");
                  console.log($users);
                  for (var i = 0; i < $users.length; i++) {
                    var names = $users[i].getAttribute("id").split(",");
                    if (names[0].toLowerCase().startsWith($(this).val().toLowerCase()) ||
                      names[1].toLowerCase().startsWith($(this).val().toLowerCase()) ||
                      names[2].toLowerCase().startsWith($(this).val().toLowerCase())) {
                      $($users[i]).show();
                    } else {
                      $($users[i]).hide();
                    }
                  }
                });

                $userInfo.find(".menu-btn").click(function () {
                  $(this).toggleClass("activeMenu-btn");
                  if ($(this).hasClass("activeMenu-btn")){
                    $userDetails = $userInfo.find("#user-details-" + $(this).attr("id"));
                    showUserDetails($(this).attr("id"), $userDetails);
                    $userDetails.css("max-height","none");
                  } else {
                    $userDetails.css("max-height","0");
                  }
                });
              }
            });
          }
        }

        function saveAttachmentInit() {
          var $saveAttachment = $("#outlook-saveAttachment");
          var $form = $saveAttachment.find("form");
          var $attachments = $form.find("ul.attachments");
          var $comment = $form.find("textarea[name='comment']");
          var $groupPath = $form.find(".groupPath");
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
            $litems.click(function () {
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
              // console.log(">> loadFolder: " + groupId + " >> " + path);
              $folders.jzLoad("Outlook.folders()", {
                groupId: groupId,
                path: path
              }, function (response, status, jqXHR) {
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
              console.log("loadFolder: groupId and/or path not found");
              process.reject(showError("Outlook.messages.spacePathNotFound"));
            }
            return process.promise();
          }

          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
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

          var item = Office.context.mailbox.item;
          if (item.attachments.length > 0) {
            for (i = 0; i < item.attachments.length; i++) {
              var att = item.attachments[i];
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
              $li.click(function () {
                $(this).toggleClass("is-selected");
                if ($(this).hasClass("is-selected")) {
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
            $cancelButton.click(function () {
              $cancelButton.data("cancel", true);
            });
            $form.submit(function (event) {
              event.preventDefault();
              clearError();
              if ($cancelButton.data("cancel")) {
                loadMenu("home");
              } else {
                var attachmentIds = [];
                $attachmentIds.each(function (i) {
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
                      "direction": "down"
                    });
                    $form.show("blind", {
                      "direction": "down"
                    });
                  }

                  Office.context.mailbox.getCallbackTokenAsync(function (asyncResult) {
                    if (asyncResult.status === "succeeded") {
                      var attachmentToken = asyncResult.value;
                      var ewsUrl = Office.context.mailbox.ewsUrl;
                      // console.log(">> attachmentToken: " + attachmentToken + ", ewsUrl: " + ewsUrl);
                      // console.log(">> savingAttachment: " + JSON.stringify(attachmentIds));
                      var $savedSpaceInfo = $savedAttachment.find(".savedSpaceInfo");
                      $savedSpaceInfo.jzLoad("Outlook.saveAttachment()", {
                        groupId: groupId,
                        path: path,
                        comment: $comment.val(),
                        ewsUrl: ewsUrl,
                        userEmail: userEmail,
                        userName: userName,
                        messageId: messageId,
                        attachmentToken: attachmentToken,
                        attachmentIds: attachmentIds.join()
                      }, function (response, status, jqXHR) {
                        if (status == "error") {
                          showError(jqXHR);
                          cancelSave();
                        } else {
                          clearError();
                          var $litems = $savedSpaceInfo.find("li.ms-ListItem");
                          if ($litems.length > 0) {
                            var $savedSpaceTitle = $savedAttachment.find(".savedSpaceTitle");
                            $savedSpaceTitle.text($savedSpaceTitle.text() + " " + groupTitle);
                            $litems.each(function () {
                              $(this).ListItem();
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
                      console.log("Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " //
                        + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                      showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
                      cancelSave();
                    }
                  });
                } else {
                  showError("Outlook.messages.attachmentNotSelected");
                  $form.animate({
                    scrollTop: $attachments.offset().top - $form.offset().top + $form.scrollTop()
                  });
                }
              }
            });

            // currentFolder controls
            $groupPath.find("ul.currentFolder>li").ListItem();
            $pathUp.click(function () {
              if (!$pathUp.attr("disabled")) {
                var lastElemIndex = path.lastIndexOf("/");
                if (lastElemIndex > 0) {
                  var origPath = path;
                  path = path.substring(0, lastElemIndex);
                  var process = loadFolder();
                  process.fail(function () {
                    path = origPath;
                  });
                }
              }
            });
            $groupPath.find(".pathOpen").click(function () {
              if (portalUrl) {
                window.open(portalUrl);
              }
            });
            $groupPath.find(".pathAdd").click(function () {
              // show dialog for new folder name, then create this folder
              $popup.jzLoad("Outlook.addFolderDialog()", {}, function (response, status, jqXHR) {
                if (status == "error") {
                  showError(jqXHR);
                } else {
                  clearError();
                  $popup.show();
                  var $dialog = $popup.find(".addFolderDialog");
                  var $newFolderName = $dialog.find("input[name='newFolderName']");
                  var $addFolderButton = $dialog.find("button.addFolder");
                  $newFolderName.change(function () {
                    if ($(this).val()) {
                      $addFolderButton.prop("disabled", false);
                    } else {
                      $addFolderButton.prop("disabled", true);
                    }
                  });
                  var $dialogForm = $dialog.find("form");
                  $dialogForm.submit(function (event) {
                    event.preventDefault();
                    if (!$dialogForm.data("cancel")) {
                      var newFolderName = $newFolderName.val();
                      if (newFolderName) {
                        $folders.jzLoad("Outlook.addFolder()", {
                          groupId: groupId,
                          path: path,
                          name: newFolderName
                        }, function (response, status, jqXHR) {
                          if (status == "error") {
                            $popup.hide();
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
                  $dialog.find("button.cancelFolder").click(function () {
                    $newFolderName.val("");
                    $dialogForm.data("cancel", true);
                    $popup.hide();
                  });
                }
              });
            });
          } else {
            var $notAttachment = $("#notAttachment");
            $saveAttachment.hide();
            $notAttachment.show();
          }
        }

        function convertToStatusInit() {
          var $convertToStatus = $("#outlook-convertToStatus");
          var $title = $convertToStatus.find("textarea[name='activityTitle']");
          var $viewer = $convertToStatus.find("div.messageViewerContainer");
          var $subject = $viewer.find("div.messageSubject");
          var $textFrame = $viewer.find("div.messageText>iframe");
          var $text = $textFrame.contents().find("html");
          $textFrame.on("load", function () {
            // XXX do it again for FF
            $text = $textFrame.contents().find("html");
          });
          var $editor = $convertToStatus.find("div.messageEditorContainer");
          var $editorSubject, $editorText;
          var $form = $convertToStatus.find("form");
          var $convertButton = $form.find("button.convertButton");
          $convertButton.prop("disabled", true);
          var $cancelButton = $form.find("button.cancelButton");
          var $converting = $convertToStatus.find("#converting");
          var $converted = $convertToStatus.find("#converted");
          var $convertedInfo = $converted.find(".convertedInfo");
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });
          $convertToStatus.find(".editMessageText>a").click(function (event) {
            event.preventDefault();
            $(this).parent().hide();
            $editorSubject = $editor.find("input[name='messageSubject']");
            $editorSubject.val($subject.text());
            var $editorFrame = $editor.find("div.messageEditor>iframe");
            $editorFrame.contents().find("html").html($text.html());
            var $content = $editorFrame.contents().find("html");
            var $contentBody = $content.find("body");
            if ($contentBody.length > 0) {
              $content = $contentBody;
            }
            $editorFrame.contents().find("html, body").css({"margin": "0px", "padding": "0px"});
            $editorText = $("<div contenteditable=\"true\"></div>");
            $editorText.append($content.children());
            $content.append($editorText);
            $text = $editorText;
            $viewer.hide();
            $editor.show();
          });

          var groupId;
          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
              groupId = $space.val();
            }
          });

          var subject = Office.context.mailbox.item.subject;
          if (internetMessageId) {
            $subject.text(subject);
          } else {
            Office.context.mailbox.item.subject.getAsync(function callback(asyncResult) {
              if (asyncResult.status === "succeeded") {
                $subject.text(asyncResult.value);
              } else {
                console.log("Office.context.mailbox.item.subject.getAsync() [" + asyncResult.status + "] error: " //
                  + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                showError("Outlook.messages.gettingSubjectError", asyncResult.error.message);
              }
            });
          }

          // get a token to read message from server side
          Office.context.mailbox.getCallbackTokenAsync(function (asyncResult) {
            if (asyncResult.status === "succeeded") {
              var messageToken = asyncResult.value;
              var midProcess = readMessageId();
              midProcess.done(function (mid) {
                // console.log("getMessage(): " + mid + " token:" + messageToken);
                if (mid) {
                  var ewsUrl = Office.context.mailbox.ewsUrl;
                  // console.log(">> ewsUrl: " + ewsUrl);
                  // get the message content to temp div and then move it to iframe
                  var $tempText = $("<div style='display:none'></div>");
                  $textFrame.append($tempText);
                  $tempText.jzLoad("Outlook.getMessage()", {
                    ewsUrl: ewsUrl,
                    userEmail: userEmail,
                    userName: userName,
                    messageId: mid,
                    messageToken: messageToken
                  }, function (response, status, jqXHR) {
                    if (status == "error") {
                      showError(jqXHR);
                    } else {
                      clearError();
                      groupId = groupId ? groupId : "";
                      var textType = jqXHR.getResponseHeader("X-MessageBodyContentType");
                      textType = textType ? textType : "html";
                      $text.html($tempText.html());
                      $tempText.remove();
                      $convertButton.prop("disabled", false);
                      $form.submit(function (event) {
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
                          var from = Office.context.mailbox.item.from;
                          $convertedInfo.jzLoad("Outlook.convertToStatus()", {
                            groupId: groupId,
                            messageId: mid,
                            title: $title.val(),
                            subject: $editorSubject ? $editorSubject.val() : $subject.text(),
                            body: $text.html(),
                            created: formatISODate(created),
                            modified: formatISODate(modified),
                            userName: userName,
                            userEmail: userEmail,
                            fromName: from.displayName,
                            fromEmail: from.emailAddress
                          }, function (response, status, jqXHR) {
                            if (status == "error") {
                              showError(jqXHR);
                              spinner.stop();
                              $converting.hide("blind", {
                                "direction": "down"
                              });
                              $form.show("blind", {
                                "direction": "down"
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
              midProcess.fail(function () {
                console.log("getMessage() failed to read messageId ");
              });
            } else {
              console.log("Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " //
                + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
              showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
            }
          });
        }

        function postStatusInit() {
          var $postStatus = $("#outlook-postStatus");
          var $statusField = $postStatus.find("div.statusField");
          var $statusPlaceholder = $statusField.find(".ms-Label");
          var $text = $statusField.find("div.statusText");
          if ($text.length > 0) {
            // XXX http://stackoverflow.com/questions/2388164/set-focus-on-div-contenteditable-element
            $text.get(0).focus();
          }
          $statusField.TextField();
          var $form = $postStatus.find("form");
          var $postButton = $form.find("button.postButton");
          $postButton.prop("disabled", true);
          var $cancelButton = $form.find("button.cancelButton");
          var $posting = $postStatus.find("#posting");
          var $posted = $postStatus.find("#posted");
          var $postedInfo = $posted.find(".postedInfo");
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });

          var groupId;
          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
              groupId = $space.val();
            }
          });

          $text.on("blur paste input", null, function () {
            // if "blur" doesn't work well, also add on ""
            var content = $text.text().trim();
            if (content.length > 0) {
              $postButton.prop("disabled", false);
              if ($statusPlaceholder.is(":visible")) {
                $statusPlaceholder.hide();
              }
            } else {
              $postButton.prop("disabled", true);
              if (!$statusPlaceholder.is(":visible")) {
                $statusPlaceholder.show();
              }
            }
          });

          $form.submit(function (event) {
            event.preventDefault();
            clearError();
            // console.log(">> postStatus groupId: " + groupId + " message: " + $text.html());
            $form.hide("blind");
            $posting.show("blind");
            var spinner = new fabric.Spinner($posting.find(".ms-Spinner").get(0));
            spinner.start();
            if ($cancelButton.data("cancel")) {
              loadMenu("home");
            } else {
              $postedInfo.jzLoad("Outlook.postStatus()", {
                groupId: groupId ? groupId : "",
                message: $text.html(),
                userName: userName,
                userEmail: userEmail
              }, function (response, status, jqXHR) {
                if (status == "error") {
                  showError(jqXHR);
                  spinner.stop();
                  $posting.hide("blind", {
                    "direction": "down"
                  });
                  $form.show("blind", {
                    "direction": "down"
                  });
                } else {
                  clearError();
                  spinner.stop();
                  $posting.hide("blind");
                  $posted.show("blind");
                }
              });
            }
          });
        }

        function convertToWikiInit() {
          // FYI this method adapted from convertToStatusInit(), consider for code reuse
          var $convertToWiki = $("#outlook-convertToWiki");
          var $title = $convertToWiki.find("input[name='wikiTitle']");
          var $viewer = $convertToWiki.find("div.messageText");
          var $textFrame = $viewer.find("iframe");
          var $text = $textFrame.contents().find("html");
          $textFrame.on("load", function () {
            // XXX do it again for FF
            $text = $textFrame.contents().find("html");
          });
          var $editor = $convertToWiki.find("div.messageEditor");
          var $form = $convertToWiki.find("form");
          var $convertButton = $form.find("button.convertButton");
          $convertButton.prop("disabled", true);
          var $cancelButton = $form.find("button.cancelButton");
          var $converting = $convertToWiki.find("#converting");
          var $converted = $convertToWiki.find("#converted");
          var $convertedInfo = $converted.find(".convertedInfo");
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });
          $convertToWiki.find(".editMessageText>a").click(function (event) {
            event.preventDefault();
            $(this).parent().hide();
            var $editorFrame = $editor.find("iframe");
            $editorFrame.contents().find("html").html($text.html());
            var $content = $editorFrame.contents().find("html");
            var $contentBody = $content.find("body");
            if ($contentBody.length > 0) {
              $content = $contentBody;
            }
            $editorFrame.contents().find("html, body").css({"margin": "0px", "padding": "0px"});
            $editorText = $("<div contenteditable='true'></div>");
            $editorText.append($content.children());
            $content.append($editorText);
            $text = $editorText;
            $viewer.hide();
            $editor.show();
          });

          var groupId;
          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
              groupId = $space.val();
            }
          });

          var cleanWikiTitle = function (title) {
            if (title) {
              // Not allowed:
              // % = : @ / \ | ^ # ; [ ] { } < > * ' " + ? &
              return title.replace(/[%=:@\/\\\|\^#;\[\]{}<>\*'"\+\?&]/g, " ");
            } else {
              return title;
            }
          };

          var subject = Office.context.mailbox.item.subject;
          if (internetMessageId) {
            $title.val(cleanWikiTitle(subject));
          } else {
            Office.context.mailbox.item.subject.getAsync(function (asyncResult) {
              if (asyncResult.status === "succeeded") {
                $title.val(cleanWikiTitle(asyncResult.value));
              } else {
                console.log("Office.context.mailbox.item.subject.getAsync() [" + asyncResult.status + "] error: " //
                  + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                showError("Outlook.messages.gettingSubjectError", asyncResult.error.message);
              }
            });
          }

          // get a token to read message from server side
          Office.context.mailbox.getCallbackTokenAsync(function (asyncResult) {
            if (asyncResult.status === "succeeded") {
              var messageToken = asyncResult.value;
              var midProcess = readMessageId();
              midProcess.done(function (mid) {
                if (mid) {
                  var ewsUrl = Office.context.mailbox.ewsUrl;
                  var $tempText = $("<div style='display:none'></div>");
                  $textFrame.append($tempText);
                  $tempText.jzLoad("Outlook.getMessage()", {
                    ewsUrl: ewsUrl,
                    userEmail: userEmail,
                    userName: userName,
                    messageId: mid,
                    messageToken: messageToken
                  }, function (response, status, jqXHR) {
                    if (status == "error") {
                      showError(jqXHR);
                    } else {
                      clearError();
                      groupId = groupId ? groupId : "";
                      var textType = jqXHR.getResponseHeader("X-MessageBodyContentType");
                      textType = textType ? textType : "html";
                      $text.html($tempText.html());
                      $tempText.remove();
                      $convertButton.prop("disabled", false);
                      $form.submit(function (event) {
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
                          $convertedInfo.jzLoad("Outlook.convertToWiki()", {
                            groupId: groupId,
                            messageId: mid,
                            subject: $title.val(),
                            body: $text.html(),
                            created: formatISODate(created),
                            modified: formatISODate(modified),
                            userName: userName,
                            userEmail: userEmail,
                            fromName: from.displayName,
                            fromEmail: from.emailAddress
                          }, function (response, status, jqXHR) {
                            if (status == "error") {
                              showError(jqXHR);
                              spinner.stop();
                              $converting.hide("blind", {
                                "direction": "down"
                              });
                              $form.show("blind", {
                                "direction": "down"
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
              midProcess.fail(function () {
                console.log("getMessage() failed to read messageId ");
              });
            } else {
              console.log("Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " //
                + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
              showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
            }
          });
        }

        function convertToForumInit() {
          // FYI this method adapted from convertToWikiInit(), consider for code reuse
          var $convertToForum = $("#outlook-convertToForum");
          var $topicName = $convertToForum.find("input[name='topicName']");
          var $viewer = $convertToForum.find("div.messageText");
          var $textFrame = $viewer.find("iframe");
          var $text = $textFrame.contents().find("html");
          $textFrame.on("load", function () {
            // XXX do it again for FF
            $text = $textFrame.contents().find("html");
          });
          var $editor = $convertToForum.find("div.messageEditor");
          var $form = $convertToForum.find("form");
          var $groupIdDropdown = $form.find(".ms-Dropdown");
          var $groupId = $groupIdDropdown.find("select[name='groupId']");
          var $convertButton = $form.find("button.convertButton");
          $convertButton.prop("disabled", true);
          var $cancelButton = $form.find("button.cancelButton");
          var $converting = $convertToForum.find("#converting");
          var $converted = $convertToForum.find("#converted");
          var $convertedInfo = $converted.find(".convertedInfo");
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });
          $convertToForum.find(".editMessageText>a").click(function (event) {
            event.preventDefault();
            $(this).parent().hide();
            var $editorFrame = $editor.find("iframe");
            $editorFrame.contents().find("html").html($text.html());
            var $content = $editorFrame.contents().find("html");
            var $contentBody = $content.find("body");
            if ($contentBody.length > 0) {
              $content = $contentBody;
            }
            $editorFrame.contents().find("html, body").css({"margin": "0px", "padding": "0px"});
            $editorText = $("<div contenteditable='true'></div>");
            $editorText.append($content.children());
            $content.append($editorText);
            $text = $editorText;
            $viewer.hide();
            $editor.show();
          });

          var groupId;
          var textReady = false;
          var checkCanConvert = function () {
            if (textReady && groupId) {
              $convertButton.prop("disabled", false);
            } else {
              $convertButton.prop("disabled", true);
            }
          };
          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
              groupId = $space.val();
              checkCanConvert();
            }
          });

          var subject = Office.context.mailbox.item.subject;
          if (internetMessageId) {
            $topicName.val(subject);
          } else {
            Office.context.mailbox.item.subject.getAsync(function (asyncResult) {
              if (asyncResult.status === "succeeded") {
                $topicName.val(asyncResult.value);
              } else {
                console.log("Office.context.mailbox.item.subject.getAsync() [" + asyncResult.status + "] error: " //
                  + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                showError("Outlook.messages.gettingSubjectError", asyncResult.error.message);
              }
            });
          }

          // get a token to read message from server side
          Office.context.mailbox.getCallbackTokenAsync(function (asyncResult) {
            if (asyncResult.status === "succeeded") {
              var messageToken = asyncResult.value;
              var midProcess = readMessageId();
              midProcess.done(function (mid) {
                if (mid) {
                  var ewsUrl = Office.context.mailbox.ewsUrl;
                  var $tempText = $("<div style='display:none'></div>");
                  $textFrame.append($tempText);
                  $tempText.jzLoad("Outlook.getMessage()", {
                    ewsUrl: ewsUrl,
                    userEmail: userEmail,
                    userName: userName,
                    messageId: mid,
                    messageToken: messageToken
                  }, function (response, status, jqXHR) {
                    if (status == "error") {
                      showError(jqXHR);
                    } else {
                      clearError();
                      groupId = groupId ? groupId : "";
                      var textType = jqXHR.getResponseHeader("X-MessageBodyContentType");
                      textType = textType ? textType : "html";
                      $text.html($tempText.html());
                      $tempText.remove();
                      textReady = true;
                      checkCanConvert();
                      $form.submit(function (event) {
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
                          $convertedInfo.jzLoad("Outlook.convertToForum()", {
                            groupId: groupId,
                            messageId: mid,
                            subject: $topicName.val(),
                            body: $text.html(),
                            created: formatISODate(created),
                            modified: formatISODate(modified),
                            userName: userName,
                            userEmail: userEmail,
                            fromName: from.displayName,
                            fromEmail: from.emailAddress
                          }, function (response, status, jqXHR) {
                            if (status == "error") {
                              showError(jqXHR);
                              spinner.stop();
                              $converting.hide("blind", {
                                "direction": "down"
                              });
                              $form.show("blind", {
                                "direction": "down"
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
              midProcess.fail(function () {
                console.log("getMessage() failed to read messageId ");
              });
            } else {
              console.log("Office.context.mailbox.getCallbackTokenAsync() [" + asyncResult.status + "] error: " //
                + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
              showError("Outlook.messages.gettingTokenError", asyncResult.error.message);
            }
          });
        }

        function startDiscussionInit() {
          // FYI this method adapted from convertToForumInit() and postStatusInit(), consider for code reuse
          var $startDiscussion = $("#outlook-startDiscussion");

          var $topicNameField = $startDiscussion.find("div.topicNameField");
          $topicNameField.TextField();
          var $topicNamePlaceholder = $topicNameField.find(".ms-Label");
          var $topicName = $topicNameField.find("input[name='topicName']");

          var $topicTextField = $startDiscussion.find("div.topicTextField");
          $topicTextField.TextField();
          var $topicTextPlaceholder = $topicTextField.find(".ms-Label");
          var $topicText = $topicTextField.find("div.topicText");

          var $form = $startDiscussion.find("form");
          var $startButton = $form.find("button.startButton");
          $startButton.prop("disabled", true);
          var $cancelButton = $form.find("button.cancelButton");
          var $starting = $startDiscussion.find("#starting");
          var $started = $startDiscussion.find("#started");
          var $startedInfo = $started.find(".startedInfo");
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });

          var groupId;
          var hasName = false;
          var hasText = false;
          var checkCanStart = function () {
            if (hasName && hasText && groupId) {
              $startButton.prop("disabled", false);
            } else {
              $startButton.prop("disabled", true);
            }
          };
          // init spaces dropdown: initially no spaces selected
          initSpacesDropdown($form, [], function ($space) {
            if ($space.length > 0) {
              groupId = $space.val();
              checkCanStart();
            }
          });

          // init text placeholders and start-button enabler
          $topicName.on("blur paste input", null, function () {
            var content = $topicName.val().trim();
            if (content.length > 0) {
              hasName = true;
              checkCanStart();
              if ($topicNamePlaceholder.is(":visible")) {
                $topicNamePlaceholder.hide();
              }
            } else {
              hasName = false;
              checkCanStart();
              if (!$topicNamePlaceholder.is(":visible")) {
                $topicNamePlaceholder.show();
              }
            }
          });
          $topicText.on("blur paste input", null, function () {
            var content = $topicText.text().trim();
            if (content.length > 0) {
              hasText = true;
              checkCanStart();
              if ($topicTextPlaceholder.is(":visible")) {
                $topicTextPlaceholder.hide();
              }
            } else {
              hasText = false;
              checkCanStart();
              if (!$topicTextPlaceholder.is(":visible")) {
                $topicTextPlaceholder.show();
              }
            }
          });

          $form.submit(function (event) {
            event.preventDefault();
            clearError();
            // console.log(">> startDiscussionInit groupId: " + groupId + " name: " + $topicName.val() + " text: " +
            // $topicText.html());
            $form.hide("blind");
            $starting.show("blind");
            var spinner = new fabric.Spinner($starting.find(".ms-Spinner").get(0));
            spinner.start();
            if ($cancelButton.data("cancel")) {
              loadMenu("home");
            } else {
              $startedInfo.jzLoad("Outlook.startDiscussion()", {
                groupId: groupId ? groupId : "",
                name: $topicName.val(),
                text: $topicText.html(),
                userName: userName,
                userEmail: userEmail
              }, function (response, status, jqXHR) {
                if (status == "error") {
                  showError(jqXHR);
                  spinner.stop();
                  $starting.hide("blind", {
                    "direction": "down"
                  });
                  $form.show("blind", {
                    "direction": "down"
                  });
                } else {
                  clearError();
                  spinner.stop();
                  $starting.hide("blind");
                  $started.show("blind");
                }
              });
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

          var findByPath = function ($items, fpath) {
            return $items.filter(function () {
              return $(this).data("path") == fpath;
            });
          };

          var isSelected = function (fpath) {
            return findByPath($documents.find("li.ms-ListItem"), fpath).filter(".is-selected").length > 0;
          };

          var initFiles = function ($files, loadChildred) {
            // FYI loadChildred is optional and required for explorer only
            var $litems = $files.find("li.ms-ListItem");
            $litems.ListItem();
            $litems.each(function (i, li) {
              var $li = $(li);
              var fpath = $li.data("path");
              if (fpath && isSelected(fpath)) {
                $li.addClass("is-selected");
              }
              $li.find(".size").each(function (i, se) {
                var size = parseInt($(se).text());
                if (size) {
                  var sizeText = sizeString(size);
                  $(se).text(sizeText);
                }
              });
            });

            // init files
            $litems.click(function () {
              var $child = $(this);
              var fpath = $child.data("path");
              var isFolder = $child.data("isfolder");
              if (isFolder) {
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
                    $documents.find("li.ms-ListItem").each(function (i, li) {
                      var $li = $(li);
                      if ($li.data("path") == fpath) {
                        $li.remove();
                      }
                    });
                    // if clicked in Explorer tab: uncheck in Search tab also
                    // otherwise it was clicked in Search tab: uncheck in Explorer tab
                    var $shown = (loadChildred ? $documentSearchResults : $folderFiles).children();
                    findByPath($shown, fpath).removeClass("is-selected");
                  } else {
                    var $selected = findByPath($documents.find("li.ms-ListItem"), fpath).not(".is-selected");
                    if ($selected.length == 0) {
                      $selected = $child.clone();
                      // clone w/o data/events
                      $selected.data("path", fpath);
                      $selected.ListItem();
                      $selected.click(function () {
                        $selected.toggleClass("is-selected");
                        // here also check/uncheck in $folderFiles
                        if ($selected.hasClass("is-selected")) {
                          findByPath($documentSearchResults.children().add($folderFiles.children()), fpath).addClass("is-selected");
                        } else {
                          findByPath($documentSearchResults.children().add($folderFiles.children()), fpath).removeClass("is-selected");
                        }
                      });
                      $selected.find(".pathControls").click(function (event) {
                        event.stopPropagation();
                      });
                      // add to selected documents
                      $selected.appendTo($documents);
                    }
                    // and preselect it
                    $selected.click();
                  }
                } else {
                  showError("Outlook.messages.fileHasNoPath");
                }
                $attachButton.prop("disabled", $documents.find("li.ms-ListItem.is-selected").length === 0);
              }
            });
            $litems.find(".pathControls").click(function (event) {
              event.stopPropagation();
            });
          };

          var searchFiles = function (text) {
            $documentSearchResults.jzLoad("Outlook.searchFiles()", {
              sourceId: sourceId,
              text: text
            }, function (response, status, jqXHR) {
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
          $source.change(function () {
            var $s = $source.find("option:selected");
            if ($s.length > 0) {
              clearError();
              sourceId = $s.val();
              path = sourceRootPath = $s.data("rootpath");
              portalUrl = $s.data("portalurl");
              // pre-load source files:
              // for All spaces try gather last used files ordered by access/modification date first
              // for Personal Docs gather last used from user's documents
              // for a space gather last used from that space
              // Having last used files (up to 20 items), prefill the search pane results with it and show the pane
              // for Personal Docs and space make Explorer tab visible, when user click it then load root folder files.
              searchFiles("");
              // when source changed - show its search tab
              $searchTab.click();
              // also clear what have in Explorer
              $folderFiles.empty();
            }
            $explorerTab.prop("disabled", sourceId == "*");
          });
          $sourceDropdown.Dropdown();
          setDropdownSize($sourceDropdown);

          // init Search Tab
          $searchTab.click(function () {
            clearError();
            $explorerTab.removeClass("ms-Button--primary");
            $searchTab.addClass("ms-Button--primary");
            $documentExplorer.hide();
            $documentSearch.show();
          });

          // init search form
          $documentSearchForm.find(".ms-SearchBox").SearchBox();
          $documentSearchForm.submit(function (event) {
            event.preventDefault();
            clearError();
            searchFiles($documentSearchInput.val());
          });

          // init Explore Tab
          var loadChildred = function () {
            var process = $.Deferred();
            if (sourceId && path) {
              // console.log(">> loadChildred: " + sourceId + " >> " + path);
              $folderFiles.jzLoad("Outlook.exploreFiles()", {
                sourceId: sourceId,
                path: path
              }, function (response, status, jqXHR) {
                if (status == "error") {
                  process.reject();
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
                  process.resolve();
                }
              });
            } else {
              process.reject();
              console.log("loadChildred: sourceId and/or path not found");
              showError("Outlook.messages.sourcePathNotFound");
            }
            return process.promise();
          };
          $explorerTab.click(function () {
            clearError();
            $explorerTab.addClass("ms-Button--primary");
            $searchTab.removeClass("ms-Button--primary");
            if ($folderFiles.children().length == 0) {
              loadChildred();
            }
            $documentSearch.hide();
            $documentExplorer.show();
          });
          // init Explorer pathInfo
          $pathUp.click(function () {
            if (!$pathUp.attr("disabled")) {
              var lastElemIndex = path.lastIndexOf("/");
              if (lastElemIndex > 0) {
                var origPath = path;
                path = path.substring(0, lastElemIndex);
                var process = loadChildred();
                process.fail(function () {
                  path = origPath;
                });
              }
            }
          });
          $pathOpen.click(function () {
            if (portalUrl) {
              window.open(portalUrl);
            }
          });

          // init Cancel button
          $cancelButton.click(function () {
            $cancelButton.data("cancel", true);
          });

          // init Attach button (as form submit)
          $attach.find("form").submit(function (event) {
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
              var files = [];
              var $attachedDocuments = $attached.find(".documents");
              $documents.find("li.ms-ListItem.is-selected").each(function (i, li) {
                var $selected = $(li);
                // var title = $selected.find(".ms-ListItem-primaryText").text();
                // XXX we cannot use WebDAV link as it requires authentication in eXo
                // var downloadUrl = $selected.data("downloadurl");
                var fpath = $selected.data("path");
                var $fileLink = $selected.jzAjax("Outlook.fileLink()", {
                  type: 'POST',
                  data: {
                    nodePath: fpath
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

                var $fileProcess = $.Deferred();
                files.push($fileProcess);

                $fileLink.done(function (response, status, jqXHR) {
                  var link = response.link;
                  var title = response.name;
                  Office.context.mailbox.item.addFileAttachmentAsync(link, title, {}, function (asyncResult) {
                    if (asyncResult.status === "succeeded") {
                      $docName.prepend("<i class='ms-Icon ms-Icon--checkbox ms-font-m ms-fontColor-green'>");
                      $fileProcess.resolve();
                    } else {
                      console.log("Office.context.mailbox.item.addFileAttachmentAsync() [" + asyncResult.status + "] error: "//
                        + JSON.stringify(asyncResult.error) + " value: " + JSON.stringify(asyncResult.value));
                      $fileProcess.reject();
                      $attachedDoc.addClass("ms-bgColor-error");
                      $docName.prepend("<i class='ms-Icon ms-Icon--alert ms-font-m ms-fontColor-error'></i>");
                      $docName.after("<div class='ms-ListItem-tertiaryText addedError'>" + asyncResult.error.message + "</div>");
                    }
                  });
                });
                $fileLink.fail(function (jqXHR, textStatus, errorThrown) {
                  console.log("Outlook.fileLink() [" + textStatus + "]: "//
                    + errorThrown + " response: " + jqXHR.responseText);
                  $fileProcess.reject();
                  $attachedDoc.addClass("ms-bgColor-error");
                  $docName.prepend("<i class='ms-Icon ms-Icon--alert ms-font-m ms-fontColor-error'></i>");
                  $docName.after("<div class='ms-ListItem-tertiaryText linkError'>" + jqXHR.responseText + "</div>");
                });

                $attachedDoc.appendTo($attachedDocuments);
              });

              $.when.apply($, files).then(function () {
                // all successful - do nothing
              }, function () {
                // some failed
                $attached.find(".attachedAllMessage").hide();
                $attached.find(".attachedSomeMessage").show();
                showError("Outlook.messages.addingAttachmentError");
              });

              $attaching.hide("blind");
              $attached.show("blind");
            }
          });

          // do initial search for better UX (first should be 'All Spaces')
          $source.val($source.find("option:first").val());
          $source.change();
        }

        function searchInit() {
          var $search = $("#outlook-search");
          var $searchContainer = $search.find(".searchContainer");
          var $searchFrame = $searchContainer.find("iframe");
          $searchFrame.height($searchContainer.height());
          var ieVersion = getIEVersion();
          // TODO do we need DOMSubtreeModified also?
          var domEvent = ieVersion > 0 && ieVersion < 9.0 ? "onpropertychange" : "DOMNodeInserted";
          var searchWindow = $searchFrame.get(0).contentWindow;

          $searchFrame.on("load", function () {
            // XXX it is a hack for quicksearch.js's generateAllResultsURL()
            var outlookSiteName;
            var portalName = searchWindow.eXo.env.portal.portalName;
            if (portalName) {
              outlookSiteName = portalName + "/outlook";
              searchWindow.eXo.env.portal.portalName = outlookSiteName;
            }

            function fixPortalName(url) {
              if (outlookSiteName) {
                return url.replace(outlookSiteName, portalName);
              } else {
                return url;
              }
            }

            // TODO not used, see commented code below
            function outlookSiteUrl(url) {
              if (outlookSiteName) {
                var portalPath;
                if (portalName.indexOf("/") != 0) {
                  portalPath = "/" + portalName;
                } else {
                  portalPath = portalName;
                }
                var outlookPath = "/" + outlookSiteName;
                return url.replace(portalPath, outlookPath);
              } else {
                return url;
              }
            }

            // load CSS to align the search UI to Outlook add-in style
            var searchDocument = searchWindow.document;
            loadStyle("/outlook/skin/fabric.min.css", searchDocument);
            loadStyle("/outlook/skin/fabric.components.min.css", searchDocument);

            // make search results open in new window
            var $searchPortlet = $searchFrame.contents().find("#ToolBarSearch");
            if ($searchPortlet.length > 0) {
              // it's Quick Search portlet
              $searchPortlet.find("i.uiIconPLF24x24Search").parent().remove();
              var $keyword = $searchPortlet.find("input[name='adminkeyword']");
              $keyword.addClass("ms-SearchBox-field");
              $keyword.show();
              var $searchResult = $searchPortlet.find(".uiQuickSearchResult");
              $searchResult.on(domEvent, "table", function (event) { // .quickSearchResult
                var $table = $(event.target);
                $table.addClass("ms-font-m");
                $table.find(".quickSearchResult>a").each(function () {
                  var $a = $(this);
                  $a.attr("target", "_blank");
                  $a.attr("href", fixPortalName($a.attr("href")));
                });
                // TODO do we want show unified (full) search in the add-in?
                // $table.find("td.message>a").each(function() {
                // var $a = $(this);
                // //$a.attr("href", outlookSiteUrl($a.attr("href")));
                // $(searchWindow.document).on("click", $a.attr("id"), function() {
                // window.location.href = generateAllResultsURL(); //open the main search page
                // //$(quickSearchResult_id).hide();
                // });
                // });
                return true;
              });
            } else {
              $searchPortlet = $searchFrame.contents().find("#searchPortlet");
              if ($searchPortlet.length > 0) {
                // it's Unified Search portlet
                $searchPortlet.addClass("ms-font-m");
                var $resultPage = $searchPortlet.find("#resultPage");
                $resultPage.on(domEvent, "div.resultBox", function (event) {
                  $(event.target).find("a").each(function () {
                    var $a = $(this);
                    $a.attr("target", "_blank");
                    $a.attr("href", fixPortalName($a.attr("href")));
                  });
                  return true;
                });
              }
            }
          });
        }

        function loadMenu(menuName) {
          var process = $.Deferred();
          var newMenu;
          if (menuName) {
            console.log("loadMenu: " + menuName);
            newMenu = menuName != $container.data("menu-name");
          } else {
            menuName = $container.data("menu-name");
            console.log("loadMenu: " + menuName + " (new from container)");
            newMenu = true;
          }
          // load only if not already loaded
          if (newMenu) {
            if (menuName) {
              var cursorCss = $container.css("cursor");
              $container.css("cursor", "wait");
              $container.jzLoad("Outlook." + menuName + "Form()", {}, function (response, status, jqXHR) {
                $container.css("cursor", cursorCss);
                if (status == "error") {
                  showError(jqXHR);
                  process.reject(response, status, jqXHR);
                } else {
                  clearError();
                  // do autofocus (except of postStatus w/ contenteditbale)
                  $container.find("textarea[autofocus], input[autofocus]").focus();
                  // know last loaded
                  $container.data("menu-name", menuName);
                  // XXX force iOS don't use native style for inputs (shadow on upper border)
                  if (isIOS) {
                    $container.find("input[type='text'], textarea").css({
                      "-webkit-appearance": "none"
                    });
                  }
                  try {
                    var commandFunc = menuName + "Init";
                    if (eval("typeof " + commandFunc + " === 'function'")) {
                      // safe to use the function
                      eval(commandFunc + "()");
                    }
                    process.resolve(response, status, jqXHR);
                  } catch (e) {
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
        if ($menu.length > 0) {
          var $menuItems = $menu.find(".outlookMenu");
          var $menuGroups = $menu.find(".outlookGroupMenu");

          // special logic for saveAttachment: remove it when no attachment found in the message
          // or it's compose mode
          var $saveAttachment = $menuItems.filter(".saveAttachment");
          if ($saveAttachment.length > 0 &&
            (!(Office.context.mailbox.item.attachments && Office.context.mailbox.item.attachments.length > 0) || !internetMessageId)) {
            $saveAttachment.parent().remove();
          }
          // special logic for item addAttachment - show it only in compose mode
          // FYI internetMessageId will be found for sent/received message
          var $addAttachment = $menuItems.filter(".addAttachment");
          if ($addAttachment.length > 0 && internetMessageId) {
            $addAttachment.parent().remove();
          }
          // remove convert* menus for compose form (internetMessageId will be null)
          var $convertTo = $menuGroups.filter(".convertTo");
          if ($convertTo.length > 0 && !internetMessageId) {
            $convertTo.parent().remove();
          }

          $menuItems.each(function (i, m) {
            var $m = $(m);
            $m.click(function () {
              var name = $m.data("name");
              if (name) {
                loadMenu(name);
              } else {
                console.log("WARN: Skipped menu item without command name: " + $m.html());
              }
            });
          });
        } else {
          // otherwise we hide "Cancel" button to do not confuse by showing Home page
          $pane.find("form button.cancelButton").hide();
        }

        $menu.NavBar();
        // set menu items height exact to what it contains (not 100% for block element)
        var $menuItems = $menu.find(".ms-NavBar-items");
        var $menuItemsList = $menuItems.find(".ms-NavBar-item");
        if ($menuItemsList.length > 0 && $menuItems.height() > 0) {
          $menuItems.height(10 + $menuItemsList.first().height() * $menuItemsList.length);
        }

        // load first menu inside container (it is set as data attr of the container)
        loadMenu();

        // init brand bar (logout)
        var $brandBar = $("#outlook-brand-bar");
        var $logoutDialog = $brandBar.find(".logoutDialog");
        $logoutDialog.Dialog();
        $logoutDialog.find("form").submit(function (event) {
          event.preventDefault();
          $logoutDialog.hide();
          if (!$logoutDialog.data("cancel")) {
            $popup.jzLoad("Outlook.logout()", {}, function (response, status, jqXHR) {
              if (status == "error") {
                showError(jqXHR);
              } else {
                // refresh the page
                window.location.reload();
              }
            });
          }
        });
        $logoutDialog.find("button.cancel").click(function () {
          $logoutDialog.data("cancel", true);
        });
        $brandBar.find("a.showLogoutLink").click(function () {
          clearError();
          $logoutDialog.removeData("cancel");
          $logoutDialog.show();
        });
      } // end of pane init
    });
  };
});

