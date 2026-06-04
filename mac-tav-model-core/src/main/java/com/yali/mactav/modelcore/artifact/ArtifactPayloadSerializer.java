package com.yali.mactav.modelcore.artifact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;

/**
 * Serializes stage DTO payloads into the NetworkArtifact payloadJson contract.
 *
 * <p>This helper belongs to model-core artifact handling. It must not preserve
 * live Object payloads as a cross-module contract or store sensitive content.</p>
 */
public class ArtifactPayloadSerializer {

    private final ObjectMapper objectMapper;

    public ArtifactPayloadSerializer() {
        this(defaultObjectMapper());
    }

    public ArtifactPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object payloadDto) {
        if (payloadDto == null) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "payloadDto must not be null");
        }
        try {
            return objectMapper.writeValueAsString(payloadDto);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "Failed to serialize artifact payload", ex);
        }
    }

    public String payloadType(Object payloadDto) {
        if (payloadDto == null) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "payloadDto must not be null");
        }
        return payloadDto.getClass().getName();
    }

    public <T> T deserialize(String payloadJson, Class<T> targetType) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "payloadJson must not be blank");
        }
        try {
            return objectMapper.readValue(payloadJson, targetType);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "Failed to deserialize artifact payload", ex);
        }
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
