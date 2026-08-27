package com.focusflow.plan;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

	@EntityGraph(attributePaths = {"items", "items.task"})
	Optional<DailyPlan> findByOwner_IdAndId(Long ownerId, Long planId);

	@Query(
			value =
					"""
					SELECT p.id AS id, p.planDate AS planDate, p.createdAt AS createdAt,
					       p.availableMinutes AS availableMinutes,
					       CASE WHEN p.warning IS NOT NULL THEN true ELSE false END AS hasWarning,
					       (SELECT COUNT(i) FROM DailyPlanItem i WHERE i.dailyPlan.id = p.id) AS itemCount
					FROM DailyPlan p
					WHERE p.owner.id = :ownerId
					ORDER BY p.createdAt DESC, p.id DESC
					""",
			countQuery = "SELECT COUNT(p) FROM DailyPlan p WHERE p.owner.id = :ownerId")
	Page<DailyPlanSummaryProjection> findSummariesByOwner(
			@Param("ownerId") Long ownerId, Pageable pageable);

	@EntityGraph(attributePaths = {"items", "items.task"})
	Optional<DailyPlan> findFirstByOwner_IdAndPlanDateOrderByCreatedAtDescIdDesc(
			Long ownerId, LocalDate planDate);
}
