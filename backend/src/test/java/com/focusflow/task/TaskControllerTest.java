package com.focusflow.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focusflow.common.error.GlobalExceptionHandler;
import com.focusflow.security.FocusFlowUserDetailsService;
import com.focusflow.security.SecurityConfig;
import com.focusflow.task.dto.CreateTaskRequest;
import com.focusflow.task.dto.TaskResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TaskController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TaskService taskService;

	@MockBean
	private FocusFlowUserDetailsService userDetailsService;

	@Test
	void create_whenUnauthenticated_returns403() throws Exception {
		mockMvc.perform(
						post("/api/tasks")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "title": "Write tests"
										}
										"""))
				.andExpect(status().isForbidden());

		verify(taskService, never()).create(any(CreateTaskRequest.class));
	}

	@Test
	@WithMockUser
	void create_withBlankTitle_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/tasks")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "title": ""
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/tasks"))
				.andExpect(jsonPath("$.details.title").isArray());

		verify(taskService, never()).create(any(CreateTaskRequest.class));
	}

	@Test
	@WithMockUser
	void create_whenAuthenticated_returns201AndBody() throws Exception {
		when(taskService.create(any(CreateTaskRequest.class)))
				.thenReturn(
						new TaskResponse(
								1L,
								"Write tests",
								"TDD coverage",
								com.focusflow.task.TaskPriority.HIGH,
								com.focusflow.task.TaskStatus.OPEN,
								null,
								45));

		mockMvc.perform(
						post("/api/tasks")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "title": "Write tests",
										  "description": "TDD coverage",
										  "priority": "HIGH",
										  "estimatedMinutes": 45
										}
										"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Write tests"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	@WithMockUser
	void list_whenAuthenticated_returnsOnlyCurrentUserTasks() throws Exception {
		when(taskService.listForCurrentUser())
				.thenReturn(
						List.of(
								new TaskResponse(
										1L,
										"Task A",
										null,
										com.focusflow.task.TaskPriority.MEDIUM,
										com.focusflow.task.TaskStatus.OPEN,
										null,
										null),
								new TaskResponse(
										2L,
										"Task B",
										"Details",
										com.focusflow.task.TaskPriority.LOW,
										com.focusflow.task.TaskStatus.DONE,
										null,
										30)));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").value("Task A"))
				.andExpect(jsonPath("$[1].title").value("Task B"));
	}
}
