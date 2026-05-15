package com.yali.mactav.configuration.impl;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.configuration.agent.ConfigurationAgent;
import com.yali.mactav.configuration.agent.MockConfigurationAgent;
import com.yali.mactav.configuration.service.ConfigurationService;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.plan.NetworkPlan;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ConfigurationServiceImpl implements ConfigurationService {

    private final ConfigurationAgent configurationAgent;

    public ConfigurationServiceImpl() {
        this(new MockConfigurationAgent());
    }

    public ConfigurationServiceImpl(ConfigurationAgent configurationAgent) {
        this.configurationAgent = configurationAgent;
    }

    @Override
    public ConfigSet generateConfig(NetworkPlan plan) {
        AgentContext context = AgentContext.of(plan == null ? null : plan.getTaskId(), null);
        AgentResult<ConfigSet> result = configurationAgent.execute(context, plan);
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getMessage());
        }
        return result.getData();
    }
}
