package com.smartcart.smartcart.modules.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.user.entity.BillsHistory;

@Repository
public interface BillsHistoryRepository extends JpaRepository<BillsHistory, Long> {

    @Query(value = """
        SELECT * FROM bills_history bh
        WHERE bh.id_user = :userId
        AND (
            :filter IS NULL OR 
            bh.name ILIKE CONCAT('%', :filter, '%') OR 
            CAST(bh.items_summary AS TEXT) ILIKE CONCAT('%', :filter, '%')
        )
        AND (:month IS NULL OR EXTRACT(MONTH FROM bh.recorded_at) = :month)
        AND (:year IS NULL OR EXTRACT(YEAR FROM bh.recorded_at) = :year)
        ORDER BY bh.recorded_at DESC
        """, nativeQuery = true)
    List<BillsHistory> findAdvancedHistory(
            @Param("userId") Long userId,
            @Param("filter") String filter,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}
