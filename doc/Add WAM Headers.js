// Normally, BenMAP is running behind the EPA WAM Webgate proxy
// Webgate requires authentication before allowing the user to pass through
// Once authenticated, the Webgate passes "uid" and "ismemberof" headers
// that BenMAP uses to identify the user's identity and roles

// This script assumes the BenMAP API is being accessed directly
// and NOT through the Webgate. We add the uid and ismemberof headers
// directly to simulate an authenticated user
function sendingRequest(msg, initiator, helper) {
	msg.getRequestHeader().setHeader("uid", "test@test.com");
	msg.getRequestHeader().setHeader("ismemberof", "BenMAP_Users");
}

function responseReceived(msg, initiator, helper) {
	// Nothing to do here
}