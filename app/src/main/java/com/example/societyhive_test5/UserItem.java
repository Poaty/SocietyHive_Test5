package com.example.societyhive_test5;

import java.util.List;

public class UserItem {
    private String id;
    private String fullName;
    private String email;
    private String role;
    private List<String> adminOf;

    public UserItem() {}

    public UserItem(String id, String fullName, String email, String role, List<String> adminOf) {
        this.id = id;
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.role = role != null ? role : "member";
        this.adminOf = adminOf;
    }

    public String getId() { return id != null ? id : ""; }
    public String getFullName() { return fullName != null ? fullName : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getRole() { return role != null ? role : "member"; }
    public List<String> getAdminOf() { return adminOf; }
    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
}
