package org.eclipse.jakarta.hello;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ItemService {

    @Inject
    private SqlSessionFactory sqlSessionFactory;

    public List<Item> findAll() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(ItemMapper.class).findAll();
        }
    }

    public Item find(Long id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Item item = session.getMapper(ItemMapper.class).findById(id);
            if (item == null) {
                throw new ItemNotFoundException(id);
            }
            return item;
        }
    }

    public Item create(Item item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(ItemMapper.class).insert(item);
            return item;
        }
    }

    public Item update(Long id, Item incoming) {
        incoming.setId(id);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int updated = session.getMapper(ItemMapper.class).update(incoming);
            if (updated == 0) {
                throw new ItemNotFoundException(id);
            }
        }
        return find(id);
    }

    public void delete(Long id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int deleted = session.getMapper(ItemMapper.class).delete(id);
            if (deleted == 0) {
                throw new ItemNotFoundException(id);
            }
        }
    }
}
