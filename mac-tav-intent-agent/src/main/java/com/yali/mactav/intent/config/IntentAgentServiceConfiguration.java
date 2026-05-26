package com.yali.mactav.intent.config;

import com.yali.mactav.intent.a2a.IntentAgentInvokePayloadMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for IntentAgent service-level helpers.
 *
 * <p>Official Spring AI Alibaba A2A server and Nacos registry are configured by
 * starter auto-configuration. This class only keeps local mapping helpers and
 * must not publish Agent Cards through custom Nacos Config code.</p>
 */
@Configuration(proxyBeanMethods = false)
public class IntentAgentServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntentAgentInvokePayloadMapper intentAgentInvokePayloadMapper() {
        return new IntentAgentInvokePayloadMapper();
    }
}
