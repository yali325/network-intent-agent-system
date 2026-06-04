package com.yali.mactav.modelcore.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.workspace.AgentExecutionRecord;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.entity.AgentExecutionRecordEntity;
import com.yali.mactav.modelcore.entity.NetworkArtifactEntity;
import com.yali.mactav.modelcore.entity.NetworkTaskEntity;
import com.yali.mactav.modelcore.entity.WorkspaceChangeRecordEntity;
import com.yali.mactav.modelcore.entity.WorkspaceEventEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between shared DTOs and MyBatis persistence rows.
 */
public class MyBatisModelCoreAssembler {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public MyBatisModelCoreAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NetworkTaskEntity toEntity(NetworkTask task) {
        NetworkTaskEntity entity = new NetworkTaskEntity();
        entity.setTaskId(task.getTaskId());
        entity.setRawText(task.getRawText());
        entity.setDescription(task.getDescription());
        entity.setTaskStatus(name(task.getTaskStatus()));
        entity.setCurrentStage(name(task.getCurrentStage()));
        entity.setCreatedBy(task.getCreatedBy());
        entity.setCreateTime(task.getCreateTime());
        entity.setUpdateTime(task.getUpdateTime());
        return entity;
    }

    public NetworkTask toTask(NetworkTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return NetworkTask.builder()
                .taskId(entity.getTaskId())
                .rawText(entity.getRawText())
                .description(entity.getDescription())
                .taskStatus(enumValue(TaskStatus.class, entity.getTaskStatus()))
                .currentStage(enumValue(WorkflowStage.class, entity.getCurrentStage()))
                .createdBy(entity.getCreatedBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public NetworkArtifactEntity toEntity(NetworkArtifact artifact) {
        NetworkArtifactEntity entity = new NetworkArtifactEntity();
        entity.setArtifactId(artifact.getArtifactId());
        entity.setTaskId(artifact.getTaskId());
        entity.setArtifactType(name(artifact.getArtifactType()));
        entity.setVersion(artifact.getVersion());
        entity.setStage(name(artifact.getStage()));
        entity.setStatus(name(artifact.getStatus()));
        entity.setPayloadType(artifact.getPayloadType());
        entity.setPayloadJson(artifact.getPayloadJson());
        entity.setPayloadSummary(artifact.getPayloadSummary());
        entity.setTraceRefsJson(writeJson(artifact.getTraceRefs()));
        entity.setCreatedBy(artifact.getCreatedBy());
        entity.setCreateTime(artifact.getCreateTime());
        return entity;
    }

    public NetworkArtifact toArtifact(NetworkArtifactEntity entity) {
        if (entity == null) {
            return null;
        }
        return NetworkArtifact.builder()
                .artifactId(entity.getArtifactId())
                .taskId(entity.getTaskId())
                .artifactType(enumValue(ArtifactType.class, entity.getArtifactType()))
                .version(entity.getVersion())
                .stage(enumValue(WorkflowStage.class, entity.getStage()))
                .status(enumValue(ArtifactStatus.class, entity.getStatus()))
                .payloadType(entity.getPayloadType())
                .payloadJson(entity.getPayloadJson())
                .payloadSummary(entity.getPayloadSummary())
                .traceRefs(readTraceRefs(entity.getTraceRefsJson()))
                .createdBy(entity.getCreatedBy())
                .createTime(entity.getCreateTime())
                .build();
    }

    public WorkspaceEventEntity toEntity(WorkspaceEvent event) {
        WorkspaceEventEntity entity = new WorkspaceEventEntity();
        entity.setEventId(event.getEventId());
        entity.setTaskId(event.getTaskId());
        entity.setEventType(event.getEventType());
        entity.setStage(name(event.getStage()));
        entity.setEventTime(event.getEventTime());
        entity.setSeverity(event.getSeverity());
        entity.setTitle(event.getTitle());
        entity.setMessage(event.getMessage());
        entity.setRelatedArtifactId(event.getRelatedArtifactId());
        entity.setRelatedRecordId(event.getRelatedRecordId());
        entity.setTraceId(event.getTraceId());
        entity.setPayloadSummary(event.getPayloadSummary());
        return entity;
    }

    public WorkspaceEvent toEvent(WorkspaceEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkspaceEvent.builder()
                .eventId(entity.getEventId())
                .taskId(entity.getTaskId())
                .eventType(entity.getEventType())
                .stage(enumValue(WorkflowStage.class, entity.getStage()))
                .eventTime(entity.getEventTime())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .relatedArtifactId(entity.getRelatedArtifactId())
                .relatedRecordId(entity.getRelatedRecordId())
                .traceId(entity.getTraceId())
                .payloadSummary(entity.getPayloadSummary())
                .build();
    }

    public WorkspaceChangeRecordEntity toEntity(WorkspaceChangeRecord change) {
        WorkspaceChangeRecordEntity entity = new WorkspaceChangeRecordEntity();
        entity.setChangeId(change.getChangeId());
        entity.setTaskId(change.getTaskId());
        entity.setStage(name(change.getStage()));
        entity.setChangeType(change.getChangeType());
        entity.setFromArtifactId(change.getFromArtifactId());
        entity.setToArtifactId(change.getToArtifactId());
        entity.setReason(change.getReason());
        entity.setCreatedBy(change.getCreatedBy());
        entity.setCreateTime(change.getCreateTime());
        return entity;
    }

    public WorkspaceChangeRecord toChange(WorkspaceChangeRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkspaceChangeRecord.builder()
                .changeId(entity.getChangeId())
                .taskId(entity.getTaskId())
                .stage(enumValue(WorkflowStage.class, entity.getStage()))
                .changeType(entity.getChangeType())
                .fromArtifactId(entity.getFromArtifactId())
                .toArtifactId(entity.getToArtifactId())
                .reason(entity.getReason())
                .createdBy(entity.getCreatedBy())
                .createTime(entity.getCreateTime())
                .build();
    }

    public AgentExecutionRecordEntity toEntity(AgentExecutionRecord record) {
        AgentExecutionRecordEntity entity = new AgentExecutionRecordEntity();
        entity.setRecordId(record.getRecordId());
        entity.setTaskId(record.getTaskId());
        entity.setTraceId(record.getTraceId());
        entity.setAgentName(record.getAgentName());
        entity.setTargetAgentName(record.getTargetAgentName());
        entity.setRemoteCallType(record.getRemoteCallType());
        entity.setAgentCardVersion(record.getAgentCardVersion());
        entity.setStage(name(record.getStage()));
        entity.setStageStatus(name(record.getStageStatus()));
        entity.setInputArtifactIdsJson(writeJson(record.getInputArtifactIds()));
        entity.setOutputArtifactIdsJson(writeJson(record.getOutputArtifactIds()));
        entity.setToolCallSummariesJson(writeJson(record.getToolCallSummaries()));
        entity.setMcpCallSummariesJson(writeJson(record.getMcpCallSummaries()));
        entity.setA2aCallSummariesJson(writeJson(record.getA2aCallSummaries()));
        entity.setModelCallCount(record.getModelCallCount());
        entity.setStartTime(record.getStartTime());
        entity.setFinishTime(record.getFinishTime());
        entity.setDurationMs(record.getDurationMs());
        entity.setInputSummary(record.getInputSummary());
        entity.setOutputSummary(record.getOutputSummary());
        entity.setErrorCode(record.getErrorCode());
        entity.setErrorMessage(record.getErrorMessage());
        entity.setMessage(record.getMessage());
        return entity;
    }

    public AgentExecutionRecord toRecord(AgentExecutionRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return AgentExecutionRecord.builder()
                .recordId(entity.getRecordId())
                .taskId(entity.getTaskId())
                .traceId(entity.getTraceId())
                .agentName(entity.getAgentName())
                .targetAgentName(entity.getTargetAgentName())
                .remoteCallType(entity.getRemoteCallType())
                .agentCardVersion(entity.getAgentCardVersion())
                .stage(enumValue(WorkflowStage.class, entity.getStage()))
                .stageStatus(enumValue(StageStatus.class, entity.getStageStatus()))
                .inputArtifactIds(readStringList(entity.getInputArtifactIdsJson()))
                .outputArtifactIds(readStringList(entity.getOutputArtifactIdsJson()))
                .toolCallSummaries(readStringList(entity.getToolCallSummariesJson()))
                .mcpCallSummaries(readStringList(entity.getMcpCallSummariesJson()))
                .a2aCallSummaries(readStringList(entity.getA2aCallSummariesJson()))
                .modelCallCount(entity.getModelCallCount())
                .startTime(entity.getStartTime())
                .finishTime(entity.getFinishTime())
                .durationMs(entity.getDurationMs())
                .inputSummary(entity.getInputSummary())
                .outputSummary(entity.getOutputSummary())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .message(entity.getMessage())
                .build();
    }

    public String artifactRefsJson(Map<ArtifactType, String> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        refs.forEach((type, artifactId) -> {
            if (type != null && artifactId != null) {
                values.put(type.name(), artifactId);
            }
        });
        return writeJson(values);
    }

    public Map<ArtifactType, String> artifactRefs(String json) {
        Map<ArtifactType, String> refs = new EnumMap<>(ArtifactType.class);
        if (json == null || json.isBlank()) {
            return refs;
        }
        Map<String, String> values = readJson(json, STRING_MAP);
        values.forEach((type, artifactId) -> {
            ArtifactType artifactType = enumValue(ArtifactType.class, type);
            if (artifactType != null && artifactId != null) {
                refs.put(artifactType, artifactId);
            }
        });
        return refs;
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        return readJson(json, STRING_LIST);
    }

    private TraceRefs readTraceRefs(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return readJson(json, TraceRefs.class);
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, value);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "Failed to write persistence JSON", ex);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "Failed to read persistence JSON", ex);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "Failed to read persistence JSON", ex);
        }
    }
}
