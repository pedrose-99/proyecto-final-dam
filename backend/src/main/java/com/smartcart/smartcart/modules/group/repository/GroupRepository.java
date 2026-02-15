package com.smartcart.smartcart.modules.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.group.entity.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {

    Optional<Group> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);
}
