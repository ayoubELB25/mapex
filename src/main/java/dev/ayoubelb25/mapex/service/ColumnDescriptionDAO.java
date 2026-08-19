package dev.ayoubelb25.mapex.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.ayoubelb25.mapex.util.DBConfig;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

@Component
public class ColumnDescriptionDAO {

    @Autowired
    private DBConfig dbConfig;


    /**
     * Returns column comments for the specified table.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getColumnDescriptions(String tableName) {
        EntityManager em = dbConfig.createEntityManager();
        Map<String, String> result = new LinkedHashMap<>();

        try {
            String sql
                    = "SELECT c.column_name, pgd.description "
                    + "FROM information_schema.columns c "
                    + "JOIN pg_catalog.pg_class pgc ON pgc.relname = c.table_name "
                    + "JOIN pg_catalog.pg_description pgd "
                    + "     ON pgd.objoid = pgc.oid AND pgd.objsubid = c.ordinal_position "
                    + "WHERE c.table_name = :tableName "
                    + "  AND c.column_name NOT IN ('id') "
                    + "  AND pgd.description IS NOT NULL "
                    + "ORDER BY c.ordinal_position";

            List<Object[]> rows = em.createNativeQuery(sql)
                    .setParameter("tableName", tableName)
                    .getResultList();
            for (Object[] row : rows) {
                result.put((String) row[0], (String) row[1]);
            }
        } finally {
            em.close();
        }

        System.out.println(">>> [" + tableName + "] columns: " + result);

        return result;
    }
}
