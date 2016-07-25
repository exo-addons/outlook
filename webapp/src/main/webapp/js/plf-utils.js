require([ "SHARED/jquery" ], function($) {
	// Hide PLF's toolbar: do in dedicated require to do not wait for other scripts load
	$("#PlatformAdminToolbarContainer").css("display", "none");
	$(function() {
		$("#PlatformAdminToolbarContainer").css("display", "none");
	});
});