package gov.epa.bencloud.api.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationMessage {
	public Boolean success;
	public List<Message> messages;
	
	public ValidationMessage() {
		success = true;
		messages = new ArrayList<ValidationMessage.Message>();
	}
	
	public static class Message{
		public String type = "";
		public String message = "";		
		
		public Message() {

		}
		public Message(String type, String message) {
			this.type = type;
			this.message = message;

		}

	}
	
	
	
}
