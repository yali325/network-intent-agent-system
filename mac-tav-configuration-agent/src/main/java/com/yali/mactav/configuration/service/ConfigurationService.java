package com.yali.mactav.configuration.service;

import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;

/**
 * Internal ConfigurationAgent service for schema parsing and output validation.
 */
public interface ConfigurationService {

    ConfigSet parse(ConfigurationResponseSchema schema, ConfigurationAgentInvokePayload payload);
}
