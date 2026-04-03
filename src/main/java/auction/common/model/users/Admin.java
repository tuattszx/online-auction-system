package auction.common.model.users;


public class Admin extends Account{
    private String adminCode;

    public Admin(){
        super();
        this.setRole("ADMIN");
    }

    public Admin(int id,String username, String password,String email,String adminCode){
        super(id, username, password, "ADMIN", email);
        this.adminCode=adminCode;
    }

    public String getAdminCode(){
        return adminCode;
    }
    public void setAdminCode(String adminCode){
        this.adminCode=adminCode;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                ", adminCode='" + adminCode + '\'' +
                '}';
    }
}