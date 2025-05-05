package com.services;
import java.util.List;

public interface CrudService <T>{
    void ajouter(T t);
    void modifer (T t);
    void supprimer(int id);
    List<T> afficher();
}