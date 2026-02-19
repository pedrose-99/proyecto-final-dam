package com.smartcart.smartcart.modules.user.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.user.entity.BillsHistory;

@Repository
public interface BillsHistoryRepository extends JpaRepository<BillsHistory, Long>
{

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

    @Query(value = """
        SELECT
            CAST(EXTRACT(MONTH FROM bh.recorded_at) AS INTEGER) AS period1,
            CAST(EXTRACT(YEAR FROM bh.recorded_at) AS INTEGER) AS period2,
            COALESCE(SUM(bh.total_amount), 0) AS totalAmount,
            CAST(COUNT(*) AS INTEGER) AS billCount,
            CAST(COALESCE(SUM(CASE WHEN bh.exceeded_limit = true THEN 1 ELSE 0 END), 0) AS INTEGER) AS exceededCount
        FROM bills_history bh
        WHERE bh.id_user = :userId
        AND bh.recorded_at >= :since AND bh.recorded_at < :until
        GROUP BY EXTRACT(YEAR FROM bh.recorded_at), EXTRACT(MONTH FROM bh.recorded_at)
        ORDER BY period2 ASC, period1 ASC
        """, nativeQuery = true)
    List<Object[]> findMonthlySummaryRaw(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );

    @Query(value = """
        SELECT
            CAST(EXTRACT(WEEK FROM bh.recorded_at) AS INTEGER) AS period1,
            CAST(EXTRACT(YEAR FROM bh.recorded_at) AS INTEGER) AS period2,
            COALESCE(SUM(bh.total_amount), 0) AS totalAmount,
            CAST(COUNT(*) AS INTEGER) AS billCount,
            CAST(COALESCE(SUM(CASE WHEN bh.exceeded_limit = true THEN 1 ELSE 0 END), 0) AS INTEGER) AS exceededCount
        FROM bills_history bh
        WHERE bh.id_user = :userId
        AND bh.recorded_at >= :since AND bh.recorded_at < :until
        GROUP BY EXTRACT(YEAR FROM bh.recorded_at), EXTRACT(WEEK FROM bh.recorded_at)
        ORDER BY period2 ASC, period1 ASC
        """, nativeQuery = true)
    List<Object[]> findWeeklySummaryRaw(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );

    @Query(value = """
        SELECT
            CAST(EXTRACT(YEAR FROM bh.recorded_at) AS INTEGER) AS period1,
            COALESCE(SUM(bh.total_amount), 0) AS totalAmount,
            CAST(COUNT(*) AS INTEGER) AS billCount,
            CAST(COALESCE(SUM(CASE WHEN bh.exceeded_limit = true THEN 1 ELSE 0 END), 0) AS INTEGER) AS exceededCount
        FROM bills_history bh
        WHERE bh.id_user = :userId
        AND bh.recorded_at >= :since AND bh.recorded_at < :until
        GROUP BY EXTRACT(YEAR FROM bh.recorded_at)
        ORDER BY period1 ASC
        """, nativeQuery = true)
    List<Object[]> findYearlySummaryRaw(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );
}
