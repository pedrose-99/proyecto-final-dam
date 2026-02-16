package com.smartcart.smartcart.modules.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>
{
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<User> findByRole_NameOrderByCreatedAtDesc(String roleName, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY u.createdAt DESC")
    Page<User> searchByEmailOrUsername(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role.name = :role AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY u.createdAt DESC")
    Page<User> searchByEmailOrUsernameAndRole(@Param("search") String search, @Param("role") String role, Pageable pageable);

    long countByRole_Name(String roleName);
}
