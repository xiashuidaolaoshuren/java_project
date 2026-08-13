package com.focusflow.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

	@EntityGraph(attributePaths = {"items", "items.task"})
	Optional<DailyPlan> findByOwner_IdAndId(Long ownerId, Long planId);

	@EntityGraph(attributePaths = {"items", "items.task"})
	@Query(
			"SELECT DISTINCT p FROM DailyPlan p WHERE p.owner.id = :ownerId AND p.planDate = :planDate ORDER BY p.createdAt DESC")
	List<DailyPlan> findByOwner_IdAndPlanDateOrderByCreatedAtDesc(
			@Param("ownerId") Long ownerId, @Param("planDate") LocalDate planDate);

	@EntityGraph(attributePaths = {"items", "items.task"})
	@Query(
			"SELECT DISTINCT p FROM DailyPlan p WHERE p.owner.id = :ownerId ORDER BY p.createdAt DESC")
	List<DailyPlan> findAllByOwner_IdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
}
