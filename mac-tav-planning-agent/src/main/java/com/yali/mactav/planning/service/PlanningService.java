package com.yali.mactav.planning.service;

import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;

public interface PlanningService {

    NetworkPlan createPlan(NetworkIntent intent);
}
