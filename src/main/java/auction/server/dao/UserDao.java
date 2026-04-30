package auction.server.dao;

import auction.common.model.users.User;

public interface UserDao extends GenericDAO<User, Integer> {
    User CheckLogin(String userName, String password);
    long getBalance(int id);
    boolean isUsernameExists(String username);
    boolean isEmailExists(String email);
    boolean updatePassword(int userId, String newPassword);
    boolean updateBalance(int id, long amount);
    boolean updateProfile(int userId, String newName, String newEmail, String newAddress, String newPhoneNumber);
}