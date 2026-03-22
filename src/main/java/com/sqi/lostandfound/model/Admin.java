package com.sqi.lostandfound.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admins")
public class Admin {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    private AdminRole role;

    // null = super admin (all branches), set = branch admin
    private String branch;

    public enum AdminRole {
        SUPER_ADMIN,
        BRANCH_ADMIN
    }
}