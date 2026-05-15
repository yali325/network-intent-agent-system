package com.yali.mactav.configuration.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.plan.NetworkPlan;

public interface ConfigurationService {

    ConfigSet generateConfig(NetworkPlan plan);
}
