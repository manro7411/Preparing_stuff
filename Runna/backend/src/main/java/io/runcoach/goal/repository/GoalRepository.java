package io.runcoach.goal.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.goal.entity.GoalEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GoalRepository implements PanacheRepositoryBase<GoalEntity, UUID> {
    public List<GoalEntity> findByUserId(UUID userId, int page, int size) {
        return find("userId = ?1 order by createdAt desc, id", userId).page(page, size).list();
    }
}