package com.example.yummies.Model;

public class User {
    private String Name;
    private String Password;
    private String Phone;
    private String secureCode;

    public User() {

    }

    public User(String name, String password, String secureCode) {
        Name = name;
        Password = password;
        this.secureCode = secureCode;
    }

    public String getSecureCode() {
        return secureCode;
    }

    public void setSecureCode(String secureCode) {
        this.secureCode = secureCode;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getName() {
        return Name;
    }

    public String getPassword() {
        return Password;
    }

    public void setName(String name) {
        Name = name;
    }

    public void setPassword(String password) {
        Password = password;
    }
}