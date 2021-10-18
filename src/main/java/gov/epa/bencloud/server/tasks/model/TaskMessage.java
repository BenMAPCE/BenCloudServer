package gov.epa.bencloud.server.tasks.model;

public class TaskMessage {
	private String status;
	private String message;
	
	public TaskMessage(String status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public void update(String status, String message) {
		this.status = status;
		this.message = message;	
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
