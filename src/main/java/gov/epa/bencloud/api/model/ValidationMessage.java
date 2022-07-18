package gov.epa.bencloud.api.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Representation of a validation messages
 */

public class ValidationMessage {
	public Boolean success;
	public List<Message> messages;
	
	/**
	 * Default constructor
	 */
	public ValidationMessage() {
		success = true;
		messages = new ArrayList<ValidationMessage.Message>();
	}
	

	/*
	 * Representation of a message, including the type of message and the contents of the message
	 */
	public static class Message{
		public String type = "";
		public String message = "";		
		
		/**
		 * Default constructor
		 */
		public Message() {

		}

		/**
		 * Creates a validation message with the given type and message contents.
		 * @param type
		 * @param message
		 */
		public Message(String type, String message) {
			this.type = type;
			this.message = message;

		}

	}
	
	
	
}
