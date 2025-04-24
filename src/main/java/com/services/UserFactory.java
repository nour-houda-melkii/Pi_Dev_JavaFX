package com.services;

import com.models.User;
import com.demo.enums.Role;
import com.demo.enums.Specialite;

import java.util.List;

public class UserFactory {

    public static User createMedecin(String email, String password, String firstName,
                                     String lastName, String address, String phoneNumber,
                                     int age, String numeroLicence, Specialite specialite) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        user.setAge(age);
        user.setRoles(List.of(User.ROLE_MEDECIN));
        user.setNumeroLicence(numeroLicence);
        user.setSpecialite(specialite);
        return user;
    }

    public static User createPatient(String email, String password, String firstName,
                                     String lastName, String address, String phoneNumber,
                                     int age) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        user.setAge(age);
        user.setRoles(List.of(User.ROLE_USER));
        return user;
    }
}