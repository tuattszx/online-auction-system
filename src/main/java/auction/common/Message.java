package auction.common;

import auction.common.model.users.User;

import java.io.Serializable;

public class Message implements Serializable {
    protected String type;
    protected User newuser;

    public Message(String type,User newUser) {
        this.newuser = newUser;
        this.type = type;
    }
}