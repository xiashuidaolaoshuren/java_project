package com.focusflow.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

	@EntityGraph(attributePaths = {"items", "items.task"})
	Optional<DailyPlan> findByOwner_IdAndId(Long ownerId, Long planId);

	List<DailyPlan> findByOwner_IdAndPlanDateOrderByCreatedAtDesc(Long ownerId, LocalDate planDate);

	List<DailyPlan> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);
}
