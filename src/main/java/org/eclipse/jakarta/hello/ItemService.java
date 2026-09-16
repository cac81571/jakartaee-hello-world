package org.eclipse.jakarta.hello;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ItemService {

    @PersistenceContext(unitName = "oraclePU")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Item> findAll() {
        return em.createNativeQuery(
                "SELECT ID, NAME, DESCRIPTION FROM ITEMS ORDER BY ID", Item.class)
                .getResultList();
    }

    public Item find(Long id) {
        @SuppressWarnings("unchecked")
        List<Item> found = em.createNativeQuery(
                "SELECT ID, NAME, DESCRIPTION FROM ITEMS WHERE ID = ?1", Item.class)
                .setParameter(1, id)
                .getResultList();
        if (found.isEmpty()) {
            throw new ItemNotFoundException(id);
        }
        return found.get(0);
    }

    public Item create(Item item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        em.createNativeQuery("INSERT INTO ITEMS (ID, NAME, DESCRIPTION) VALUES (?1, ?2, ?3)")
                .setParameter(1, item.getId())
                .setParameter(2, item.getName())
                .setParameter(3, item.getDescription())
                .executeUpdate();
        return item;
    }

    public Item update(Long id, Item incoming) {
        int updated = em.createNativeQuery(
                "UPDATE ITEMS SET NAME = ?1, DESCRIPTION = ?2 WHERE ID = ?3")
                .setParameter(1, incoming.getName())
                .setParameter(2, incoming.getDescription())
                .setParameter(3, id)
                .executeUpdate();
        if (updated == 0) {
            throw new ItemNotFoundException(id);
        }
        return find(id);
    }

    public void delete(Long id) {
        int deleted = em.createNativeQuery("DELETE FROM ITEMS WHERE ID = ?1")
                .setParameter(1, id)
                .executeUpdate();
        if (deleted == 0) {
            throw new ItemNotFoundException(id);
        }
        Item managed = em.find(Item.class, id);
        if (managed != null) {
            em.detach(managed);
        }
    }
}
