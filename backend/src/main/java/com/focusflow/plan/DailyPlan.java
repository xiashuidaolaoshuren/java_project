package com.focusflow.plan;

import com.focusflow.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "daily_plans")
public class DailyPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@Column(name = "plan_date", nullable = false)
	private LocalDate planDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(
			mappedBy = "dailyPlan",
			cascade = CascadeType.ALL,
			orphanRemoval = true)
	@OrderBy("position ASC")
	private final List<DailyPlanItem> items = new ArrayList<>();

	@Column(name = "available_minutes")
	private Integer availableMinutes;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "warning")
	private DailyPlanWarningSnapshot warning;

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public LocalDate getPlanDate() {
		return planDate;
	}

	public void setPlanDate(LocalDate planDate) {
		this.planDate = planDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<DailyPlanItem> getItems() {
		return items;
	}

	public Integer getAvailableMinutes() {
		return availableMinutes;
	}

	public void setAvailableMinutes(Integer availableMinutes) {
		this.availableMinutes = availableMinutes;
	}

	public DailyPlanWarningSnapshot getWarning() {
		return warning;
	}

	public void setWarning(DailyPlanWarningSnapshot warning) {
		this.warning = warning;
	}

	public void addItem(DailyPlanItem item) {
		items.add(item);
		item.setDailyPlan(this);
	}
}
