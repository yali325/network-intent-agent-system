package com.yali.mactav.execution.safety;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Rejects parameter shapes that imply generic shell or forbidden CLI execution.
 */
public class ExecutionCommandClassifier {

    public void rejectForbiddenParameters(String itemId, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            validateKey(itemId, entry.getKey());
            validateValue(itemId, entry.getValue());
        }
    }

    private void validateKey(String itemId, String key) {
        if (key == null) {
            return;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.equals("command")
                || normalized.equals("cmd")
                || normalized.equals("shell")
                || normalized.equals("script")
                || normalized.equals("rawcommand")
                || normalized.equals("huawei_cli")
                || normalized.equals("huawei-cli")
                || normalized.equals("cli")) {
            throw forbidden(itemId, "Forbidden generic execution parameter key: " + key);
        }
    }

    private void validateValue(String itemId, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            validateText(itemId, text);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                validateKey(itemId, entry.getKey() == null ? null : entry.getKey().toString());
                validateValue(itemId, entry.getValue());
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                validateValue(itemId, item);
            }
        }
    }

    private void validateText(String itemId, String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        if (normalized.startsWith("rm ")
                || normalized.equals("rm")
                || normalized.startsWith("curl ")
                || normalized.equals("curl")
                || normalized.startsWith("powershell")
                || normalized.startsWith("cmd")
                || normalized.startsWith("bash -c")
                || normalized.startsWith("sh -c")
                || normalized.contains(" huawei ")
                || normalized.startsWith("huawei ")
                || normalized.contains("system-view")
                || normalized.contains("display current-configuration")) {
            throw forbidden(itemId, "Forbidden generic shell or Huawei CLI content");
        }
    }

    private BusinessException forbidden(String itemId, String message) {
        return new BusinessException(
                ErrorCode.EXECUTION_FORBIDDEN_COMMAND,
                "[" + itemId + "] " + message);
    }
}
