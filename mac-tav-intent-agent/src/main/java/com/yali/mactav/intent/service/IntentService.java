package com.yali.mactav.intent.service;

import com.yali.mactav.model.intent.NetworkIntent;

public interface IntentService {

    NetworkIntent parseIntent(String taskId, String rawText);
}
