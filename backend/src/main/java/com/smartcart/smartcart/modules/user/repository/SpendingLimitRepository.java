package com.smartcart.smartcart.modules.user.repository;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.user.entity.LimitType;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;

@Repository
public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, Long> {
    Optional<SpendingLimit> findByIdUserAndType(Long idUser, LimitType type);
    List<SpendingLimit> findByIdUserAndIsActiveTrue(Long idUser);
}


