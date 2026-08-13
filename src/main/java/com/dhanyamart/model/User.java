package com.dhanyamart.model;

/**
 * Plain Java Bean (POJO) that represents one user row from users.xlsx.
 */
public class User {

    private int userId;
    private String name;
    private String email;
    private String password;      // holds the HASHED password when read from Excel
    private String phone;
    private String address;
    private String createdAt;

    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User [userId=" + userId + ", name=" + name + ", email=" + email
                + ", phone=" + phone + ", address=" + address + ", createdAt=" + createdAt + "]";
    }
}
