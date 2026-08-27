package com.focusflow.task;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

	Optional<Task> findByOwner_IdAndId(Long ownerId, Long taskId);

	List<Task> findByOwner_IdOrderByDueDateAsc(Long ownerId);

	List<Task> findByOwner_IdAndStatusOrderByDueDateAsc(Long ownerId, TaskStatus status);

	List<Task> findByOwner_IdAndStatusInOrderByDueDateAsc(
			Long ownerId, Collection<TaskStatus> statuses);

	List<Task> findByOwner_IdAndDueDateBetween(Long ownerId, LocalDate startInclusive, LocalDate endInclusive);

	List<Task> findByOwner_IdAndIdIn(Long ownerId, Collection<Long> taskIds);
}
