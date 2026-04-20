package auction.common.message;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private String command; // Ví dụ: "LOGIN", "REGISTER", "BID"
    private Object data;    // Chứa User object hoặc String, List...
    private String status;  // "SUCCESS" hoặc "FAILED"

    public Message(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    // Getters và Setters
    public String getCommand() { return command; }
    public Object getData() { return data; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}