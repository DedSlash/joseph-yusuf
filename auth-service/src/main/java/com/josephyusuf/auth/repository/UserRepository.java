package com.josephyusuf.auth.repository;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByPlan(Plan plan);

    long countByEnabled(boolean enabled);

    @Query("""
            SELECT u FROM User u
            WHERE (:plan IS NULL OR u.plan = :plan)
              AND (:enabled IS NULL OR u.enabled = :enabled)
              AND (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> searchUsers(@Param("plan") Plan plan,
                           @Param("enabled") Boolean enabled,
                           @Param("search") String search,
                           Pageable pageable);
}
