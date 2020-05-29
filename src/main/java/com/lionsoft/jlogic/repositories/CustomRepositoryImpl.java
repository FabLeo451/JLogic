package com.lionsoft.jlogic;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import javax.persistence.EntityManager;
import java.io.Serializable;

public class CustomRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements CustomRepository<T, ID> {
    private final EntityManager entityManager;
    public CustomRepositoryImpl(JpaEntityInformation entityInformation, 
                                EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }
    @Override
    @Transactional
    public void refresh(T t) {
        entityManager.refresh(t);
    }
}
