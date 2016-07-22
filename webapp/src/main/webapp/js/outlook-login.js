/**
 * Outlook Login and Welcome screen.
 */
(function() {
	Office.initialize = function(reason) {
		function getRequestParameter(name) {
			var url = window.location.href;
			name = name.replace(/[\[\]]/g, "\\$&");
			var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"), results = regex.exec(url);
			if (!results)
				return null;
			if (!results[2])
				return '';
			return decodeURIComponent(results[2].replace(/\+/g, " "));
		}

		function getCookie(name, fromDocument) {
			var nameEQ = name + "=";
			var ca = (fromDocument ? fromDocument : document).cookie.split(';');
			for (var i = 0; i < ca.length; i++) {
				var c = ca[i];
				while (c.charAt(0) == ' ') {
					c = c.substring(1, c.length);
				}
				if (c.indexOf(nameEQ) == 0) {
					var v = c.substring(nameEQ.length, c.length);
					// clean value from leading quotes (actual if set via eXo WS)
					return decodeURIComponent(v.match(/([^\"]+)/g));
				}
			}
			return null;
		}

		function setCookie(name, value, millis, toDocument, toPath, toDomain) {
			var expires;
			if (millis) {
				var date = new Date();
				date.setTime(date.getTime() + millis);
				expires = "; expires=" + date.toGMTString();
			} else {
				expires = "";
			}
			(toDocument ? toDocument : document).cookie = name + "=" + encodeURIComponent(value) + expires + "; path=" + (toPath ? toPath : "/")
			    + (toDomain ? "; domain=" + toDomain : "");
		}

		var $welcome = $("#welcomePage");
		var $signInProgress = $("#signInProgress");
		var spinner = new fabric.Spinner($signInProgress.find(".ms-Spinner").get(0));
		var $error = $("#outlook-error");

		var $messageBanner = $error.find(".ms-MessageBanner");
		var messageBanner;
		if ($messageBanner.size() > 0) {
			messageBanner = new fabric.MessageBanner($messageBanner.get(0));
		}
		var $errorText = $error.find(".ms-MessageBanner-clipper");

		function showError(message) {
			console.log("ERROR: " + message + ". ");

			if ($signInProgress.is(":visible")) {
				$signInProgress.hide("blind");
				spinner.stop();
			}

			$errorText.empty();
			$("<i class='uiIconError'></i><span>" + message + "</span>").appendTo($errorText);
			$error.scrollTop();
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

		// handle SignIn
		if ($welcome.size() > 0) {
			var $signInDialog = $("#signInDialog");
			var $signInExoButton = $welcome.find("button.signInExo");
			if ($signInDialog.size() > 0) {
				$signInDialog.Dialog();
				var $signInButton = $signInDialog.find("button.signIn");
				var $userName = $signInDialog.find("input[name='userName']");
				var $password = $signInDialog.find("input[name='password']");
				$userName.change(function() {
					if ($(this).val()) {
						$signInButton.prop("disabled", false);
					} else {
						$signInButton.prop("disabled", true);
					}
				});
				$signInDialog.submit(function(event) {
					event.preventDefault();
					$signInDialog.hide();
					if ($signInDialog.data("cancel")) {
						$userName.val("");
						$password.val("");
					} else {
						// check username not empty (password can be empty)
						var userName = $userName.val();
						if (userName && userName.length > 0) {
							var password = $password.val();
							var initialURI = getRequestParameter("initialURI");

							spinner.start();
							$welcome.hide();
							$signInProgress.show("blind");

							var $portalLogin = $.ajax({
							  async : true,
							  type : "POST",
							  url : "/portal/login",
							  data : {
							    initialURI : initialURI,
							    username : userName,
							    password : password,
							    rememberme : true
							  }
							});
							$portalLogin.done(function(data, textStatus, jqXHR) {
								console.log("[" + jqXHR.status + "] " + textStatus);
								var $data = $(data);
								var signinFailMessage = $data.find(".signinFail").text();
								if (signinFailMessage) {
									// FYI wrong user/pwd will come as 200 with message in the body html
									showError(signinFailMessage);
									$welcome.show();
								} else {
									setCookie("remembermeoutlook", "_init_me", 120000, document, "/portal/intranet/outlook");
									window.location = initialURI;
								}
							});
							$portalLogin.fail(function(jqXHR, textStatus, errorThrown) {
								// it's system/net error
								console.log("[" + jqXHR.status + "] " + errorThrown);
								// TODO i18n here
								showError("Portal login failed (" + errorThrown + "). Contact your administrator.");
								$welcome.show();
							});
						} // else, stay in the form (Login button disabled)
					}
				});
				$signInDialog.find("button.cancel").click(function() {
					$signInDialog.data("cancel", true);
				});
				$signInExoButton.click(function() {
					clearError();
					$signInDialog.removeData("cancel");
					$signInDialog.show();
				});
			} else {
				$signInExoButton.prop("disabled", false);
			}
		}
	};
})();
