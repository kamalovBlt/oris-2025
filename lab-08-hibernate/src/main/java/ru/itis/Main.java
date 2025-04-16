package ru.itis;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {

    public static void main(String[] args) {

        try (EntityManagerFactory entityManagerFactory =
                     Persistence.createEntityManagerFactory("lab-08-hibernate")) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            entityManager.close();

        }

    }

}

