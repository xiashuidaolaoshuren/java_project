package com.focusflow.testsupport;

import com.focusflow.plan.DailyPlan;
import com.focusflow.plan.DailyPlanItem;
import com.focusflow.task.Task;
import com.focusflow.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link DailyPlan} and ordered {@link DailyPlanItem} rows in tests.
 */
public final class DailyPlanTestBuilder {

	private final User owner;
	private final LocalDate planDate;
	private Instant createdAt = Instant.parse("1970-01-01T00:00:00Z");
	private final List<ItemSpec> items = new ArrayList<>();

	private DailyPlanTestBuilder(User owner, LocalDate planDate) {
		this.owner = owner;
		this.planDate = planDate;
	}

	public static DailyPlanTestBuilder plan(User owner, LocalDate planDate) {
		return new DailyPlanTestBuilder(owner, planDate);
	}

	public DailyPlanTestBuilder withCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public DailyPlanTestBuilder addItem(Task task, int position) {
		items.add(new ItemSpec(task, position));
		return this;
	}

	public DailyPlan build() {
		DailyPlan plan = new DailyPlan();
		plan.setOwner(owner);
		plan.setPlanDate(planDate);
		plan.setCreatedAt(createdAt);
		for (ItemSpec spec : items) {
			DailyPlanItem item = new DailyPlanItem();
			item.setTask(spec.task);
			item.setPosition(spec.position);
			plan.addItem(item);
		}
		return plan;
	}

	private record ItemSpec(Task task, int position) {}
}
