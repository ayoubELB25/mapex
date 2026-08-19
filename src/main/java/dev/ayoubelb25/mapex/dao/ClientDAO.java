package dev.ayoubelb25.mapex.dao;

import dev.ayoubelb25.mapex.model.Client;
import dev.ayoubelb25.mapex.util.DBConfig;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import java.util.List;

@Component
public class ClientDAO {

    @Autowired
    private DBConfig dbConfig;

    public void save(Client c) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(c);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Client> findAll() {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Client c ORDER BY c.id DESC", Client.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Client findById(Long id) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.find(Client.class, id);
        } finally {
            em.close();
        }
    }
}