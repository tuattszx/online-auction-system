package auction.common.model.users;

import auction.common.model.BaseEntity;

import java.time.LocalDateTime;


public class Account extends BaseEntity {
    private static final long serialVersionUID = 1L;
    public static Account loggedInAccount;
    private String username;
    private String password;
    private String role;
    private String email;
    private String displayName;
    private LocalDateTime creatTime;

    public Account(){
        super();
    }
    public Account(String username, String password){
        this.username = username;
        this.password = password;
    }
    public Account(int id, String username, String password, String role,String email){
        super(id);
        this.username=username;
        this.password=password;
        this.role=role;
        this.email=email;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public String getDisplayName() {return displayName;}
    public LocalDateTime getCreatTime(){ return creatTime;}

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role=role;}
    public void setDisplayName(String displayName) {this.displayName=displayName;}
    public void setCreatTime(LocalDateTime localDateTime) {this.creatTime=localDateTime;}
}