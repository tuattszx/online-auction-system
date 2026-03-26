package auction.common.model.users;

public class User extends Account{
    private long balance;
    private String address;
    public User (){}
    public User(int id,String username,String password,String role,String email,String address,long balance){
        super(id, username, password, role, email);
        this.balance=0;
        this.address=address;
        this.balance=balance;
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
}