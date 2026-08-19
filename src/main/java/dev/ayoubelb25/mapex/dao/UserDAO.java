package dev.ayoubelb25.mapex.dao;

import dev.ayoubelb25.mapex.model.User;
import dev.ayoubelb25.mapex.util.DBConfig;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import java.util.List;

@Component
public class UserDAO {

    @Autowired
    private DBConfig dbConfig;

    public User findByUsername(String username) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            List<User> results = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    public void save(User u) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(u);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}