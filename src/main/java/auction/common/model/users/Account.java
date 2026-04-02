package auction.common.model.users;

import auction.common.model.BaseEntity;


public abstract class Account extends BaseEntity {
    private String username;
    private String password;
    private String role;
    private String email;

    public Account(){
        super();
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

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    protected void setRole(String role) { this.role=role;}

}