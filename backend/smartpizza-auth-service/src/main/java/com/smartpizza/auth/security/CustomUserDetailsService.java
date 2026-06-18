package com.smartpizza.auth.security;

import com.smartpizza.auth.entity.User;
import com.smartpizza.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor  //lombok automatically generate required constructor with this annotation
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; //this object is passed from security config when spring created this service

    @Override     //tells the spring to automatically find a required bean and inject it into the class(config file)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { //passes the email entered by user

        User user = userRepository.findByEmail(email) //search the db 
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        
    //spring convert this as user detail object, used by spring security to autheticate the user.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())  //disable login if user is inactive
                .disabled(!user.getActive())
                .build();                       //build and return the user details
    } 
}