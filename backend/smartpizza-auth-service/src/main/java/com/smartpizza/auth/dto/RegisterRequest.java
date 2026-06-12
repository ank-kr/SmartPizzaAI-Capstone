package com.smartpizza.auth.dto;

import com.smartpizza.auth.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String fullName;

    private String email;

    private String password;

    private String phone;

    private String address;

    private Role role;
}