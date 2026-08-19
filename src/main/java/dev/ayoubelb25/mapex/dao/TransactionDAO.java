package dev.ayoubelb25.mapex.dao;

import dev.ayoubelb25.mapex.model.Client;
import dev.ayoubelb25.mapex.model.Transaction;
import dev.ayoubelb25.mapex.model.TransactionView;
import dev.ayoubelb25.mapex.util.DBConfig;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionDAO {

    @Autowired
    private DBConfig dbConfig;

    public void save(Transaction t) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Transaction> findAll() {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT t FROM Transaction t ORDER BY t.id DESC", Transaction.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    @SuppressWarnings("unchecked")
    public List<TransactionView> findAllResolved() {
        EntityManager em = dbConfig.createEntityManager();
        try {
            String sql = """
                SELECT
                    t.id,
                    t.isin,
                    COALESCE(sl_side.name,  t.side)       AS side,
                    t.quantity,
                    t.price,
                    COALESCE(sl_ord.name,   t.order_type) AS order_type,
                    COALESCE(sl_stat.name,  t.status)     AS status
                FROM transaction t
                LEFT JOIN data_mapping dm_side ON dm_side.source_id = t.side
                LEFT JOIN systemlist   sl_side ON sl_side.id = dm_side.system_id
                LEFT JOIN data_mapping dm_ord  ON dm_ord.source_id  = t.order_type
                LEFT JOIN systemlist   sl_ord  ON sl_ord.id  = dm_ord.system_id
                LEFT JOIN data_mapping dm_stat ON dm_stat.source_id = t.status
                LEFT JOIN systemlist   sl_stat ON sl_stat.id = dm_stat.system_id
                ORDER BY t.id DESC
                """;

            List<Object[]> rows = em.createNativeQuery(sql).getResultList();
            List<TransactionView> result = new ArrayList<>();

            for (Object[] row : rows) {
                result.add(new TransactionView(
                        ((Number)     row[0]).longValue(),
                        (String)      row[1],
                        (String)      row[2],
                        ((Number)     row[3]).intValue(),
                        (BigDecimal)  row[4],
                        (String)      row[5],
                        (String)      row[6]
                ));
            }
            return result;

        } finally {
            em.close();
        }
    }

    public Transaction findById(Long id) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.find(Transaction.class, id);
        } finally {
            em.close();
        }
    }

    public void update(Transaction t) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            Transaction t = em.find(Transaction.class, id);
            if (t != null) em.remove(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Client> findAllClients() {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Client c ORDER BY c.name", Client.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Client findClientById(Long id) {
        EntityManager em = dbConfig.createEntityManager();
        try {
            return em.find(Client.class, id);
        } finally {
            em.close();
        }
    }
}
