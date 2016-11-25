/**
 * Client Javascript support for Outlook components in eXo Platform.
 */

(function($) {

	var isIOS = /iPhone|iPod|iPad/.test(navigator.userAgent);
	var previewListener = false;
	var previewWorker = null;

	function initIOSPreviewFrame() {
		if (!previewListener && isIOS) {
			$("body").on("DOMNodeInserted", "div#documentPreviewContainer", function(event) {
				//console.log("DOMNodeInserted " + event.type + " " + event.target);
				if (!previewWorker) {
					// XXX We need special behaviour for iOS devices about iframe scrolling, as suggested in
					// http://stackoverflow.com/questions/4599153/iframes-and-the-safari-on-the-ipad-how-can-the-user-scroll-the-content
					previewWorker = setTimeout(function() {
						//console.log("messageBody fixed " + event.target);
						$(event.target).find("#uiDocumentPreview .outlookMessageViewer .messageBody").css({
						  // width : $this.attr('width'),
						  // height : $this.attr('height'),
						  "overflow" : "auto",
						  "-webkit-overflow-scrolling" : "touch"
						});
						previewWorker = null;
					}, 500);
				}
			});
			previewListener = true;
		}
	}

	$(function() {
		try {
			setTimeout(function() {
				// in activity stream
				$(".outlookMessageActivityContent").each(function(ai, activity) {
					var $activity = $(activity);
					$activity.find(".outlookMessageIframe").each(function(ii, iframe) {
						var $iframe = $(iframe);
						$iframe.ready(function() {
							var $body = $iframe.contents().find("body,div:first");
							$body.first().css("overflow", "hidden");
						});
						if (isIOS) {
							// XXX We need special behaviour for iOS devices about iframe overlapping the activity
							// stream outside the iframe area
							$iframe.parents(".messageBody").each(function(ii, messageBody) {
								var $messageBody = $(messageBody);
								$messageBody.css({
									"overflow-x" : "auto",
								  "overflow-y" : "hidden"
								});
								$messageBody.click(function(event) {
									event.stopPropagation();
									// TODO cleanup
									// var activitylink = $activity.data('activitylink');
									// eval(activitylink);
									$activity.click();
								});
							});
						}
					});
					$activity.click(function(event) {
						// here we want wait for the preview request done, as we cannot chain its methods - no
						// promise or like that there, we will wait for some time, then do our work.
						initIOSPreviewFrame();
					});
				});
			}, 500);
		} catch(e) {
			log("Error configuring Outlook View components.", e);
		}
	});

})($);
