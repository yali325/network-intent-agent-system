package com.yali.mactav.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    private static final String RAW_TEXT = "构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，"
            + "办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkWorkspaceService networkWorkspaceService;

    @Test
    void postDemoTaskReturnsFullWorkspaceAndArtifactsCanBeQueried() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/demo/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rawText", RAW_TEXT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.task").exists())
                .andExpect(jsonPath("$.data.intent").exists())
                .andExpect(jsonPath("$.data.plan").exists())
                .andExpect(jsonPath("$.data.configSet").exists())
                .andExpect(jsonPath("$.data.executionReport").exists())
                .andExpect(jsonPath("$.data.validationReport").exists())
                .andExpect(jsonPath("$.data.agentLogs").isArray())
                .andExpect(jsonPath("$.data.task.taskStatus").value("PASSED"))
                .andReturn();

        String taskId = taskId(result);

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.task.taskId").value(taskId));
        mockMvc.perform(get("/api/tasks/{taskId}/intent", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semanticIntentGraph").exists());
        mockMvc.perform(get("/api/tasks/{taskId}/plan", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topology").exists());
        mockMvc.perform(get("/api/tasks/{taskId}/config", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceConfigs").isArray())
                .andExpect(jsonPath("$.data.deviceConfigs[0].commandBlocks").isArray());
        mockMvc.perform(get("/api/tasks/{taskId}/execution", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionMode").value("DRY_RUN"));
        mockMvc.perform(get("/api/tasks/{taskId}/validation", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallStatus").value("PASSED"));
        mockMvc.perform(get("/api/tasks/{taskId}/logs", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void postDemoTaskRejectsBlankRawText() throws Exception {
        mockMvc.perform(post("/api/demo/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rawText", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void missingTaskReturnsTaskNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", "missing-task"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void missingStageArtifactReturnsStageNotReady() throws Exception {
        String taskId = networkWorkspaceService.createTask("pending task").getTask().getTaskId();

        mockMvc.perform(get("/api/tasks/{taskId}/intent", taskId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STAGE_NOT_READY"));
    }

    private String taskId(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("task").path("taskId").asText();
    }
}
