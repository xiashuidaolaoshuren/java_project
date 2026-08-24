package com.focusflow.plan;

import com.focusflow.ai.AiPlanItem;
import com.focusflow.ai.AiProviderException;
import com.focusflow.task.Task;
import com.focusflow.task.TaskStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DailyPlanRankingValidator {

	public void validate(
			List<Task> candidates,
			LocalDate planDate,
			int availableMinutes,
			List<AiPlanItem> aiItems) {
		Map<Long, Task> candidateById = new HashMap<>();
		Map<Long, Integer> blockOf = new HashMap<>();
		for (Task candidate : candidates) {
			long id = candidate.getId() != null ? candidate.getId() : 0L;
			candidateById.put(id, candidate);
			blockOf.put(id, classifyBlock(candidate, planDate));
		}

		Set<Long> seenTaskIds = new HashSet<>();
		int highestBlockSeen = 0;
		for (AiPlanItem aiItem : aiItems) {
			if (!candidateById.containsKey(aiItem.taskId())) {
				throw new AiProviderException(
						"invalid task id in AI response: " + aiItem.taskId());
			}
			int block = blockOf.get(aiItem.taskId());
			if (block < highestBlockSeen) {
				throw new AiProviderException("block order violated in AI response");
			}
			highestBlockSeen = Math.max(highestBlockSeen, block);
			seenTaskIds.add(aiItem.taskId());
		}

		for (Map.Entry<Long, Integer> entry : blockOf.entrySet()) {
			if (entry.getValue() == 1 && !seenTaskIds.contains(entry.getKey())) {
				throw new AiProviderException(
						"missing must-continue task in AI response: " + entry.getKey());
			}
			if (entry.getValue() == 2 && !seenTaskIds.contains(entry.getKey())) {
				throw new AiProviderException(
						"missing due-or-overdue task in AI response: " + entry.getKey());
			}
		}

		int requiredMinutes = 0;
		for (Task candidate : candidates) {
			int block = blockOf.get(candidate.getId() != null ? candidate.getId() : 0L);
			if (block == 1 || block == 2) {
				requiredMinutes += knownEstimate(candidate);
			}
		}
		int leftover = Math.max(0, availableMinutes - requiredMinutes);

		int optionalMinutes = 0;
		for (AiPlanItem aiItem : aiItems) {
			if (blockOf.get(aiItem.taskId()) == 3) {
				optionalMinutes += knownEstimate(candidateById.get(aiItem.taskId()));
			}
		}
		if (optionalMinutes > leftover) {
			throw new AiProviderException(
					"optional work exceeds leftover minutes in AI response");
		}
	}

	private int knownEstimate(Task task) {
		Integer estimate = task.getEstimatedMinutes();
		return estimate != null ? estimate : 0;
	}

	private int classifyBlock(Task task, LocalDate planDate) {
		if (task.getStatus() == TaskStatus.IN_PROGRESS) {
			return 1;
		}
		if (task.getStatus() == TaskStatus.OPEN) {
			LocalDate dueDate = task.getDueDate();
			if (dueDate != null && !dueDate.isAfter(planDate)) {
				return 2;
			}
			return 3;
		}
		throw new IllegalArgumentException("unexpected task status: " + task.getStatus());
	}
}
