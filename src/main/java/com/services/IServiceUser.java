package com.services;

import java.util.List;
public interface IServiceUser<T> {
    // Méthodes communes
    void ajouterUser(T user);
    void modifierUser(T user);
    void supprimerUser(T user);
    List<T> rechercherTousUsers();
    T rechercherUserParId(int id);

    // Méthodes spécifiques médecins
    void ajouterMedecin(T medecin);
    void modifierMedecin(T medecin);
    void supprimerMedecin(int id);
    List<T> rechercherTousMedecins();
    T rechercherMedecinParId(int id);

    // Méthodes spécifiques patients
    void ajouterPatient(T patient);
    boolean modifierPatient(T patient);
    void supprimerPatient(int id);
    List<T> rechercherTousPatients();
    T rechercherPatientParId(int id);
}
