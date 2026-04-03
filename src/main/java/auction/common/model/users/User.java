package auction.common.model.users;

public class User extends Account{
    private long balance;
    private String address;
    public User (){
        super();
        this.setRole("USER");
    }
    public User(int id,String username,String password,String email,String address,long balance){
        super(id, username, password, "USER", email);
        this.address=address;
        this.setBalance(balance);
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public long getBalance() {
        return balance;
    }
    public void setBalance(long balance) {
        if (balance >= 0) {
            this.balance = balance;
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                ", address='" + address + '\'' +
                ", balance=" + balance +
                '}';
    }
}