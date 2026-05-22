package auction.common.model.users;

public class User extends Account{
    private static final long serialVersionUID = 1L;
    private long balance;
    private String address;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String country;
    private String shippingPhone;
    private String cardHolderName;
    private String cardNumber;

    public User (){
        super();
        this.setRole("USER");
    }
    public User(int id,String username,String password,String email,String address,long balance, String shippingPhone){
        super(id, username, password, "USER", email);
        this.address=address;
        this.setBalance(balance);
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
        this.shippingPhone=shippingPhone;
        }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

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

    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}
    public String getPhoneNumber() {return phoneNumber;}

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
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