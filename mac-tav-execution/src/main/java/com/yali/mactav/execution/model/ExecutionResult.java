package com.yali.mactav.execution.model;

import com.yali.mactav.model.execution.ExecutionError;
import com.yali.mactav.model.execution.ExecutionStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional adapter-internal execution summary.
 *
 * <p>This type does not replace the core {@code ExecutionReport} stage
 * artifact. It is available for future adapter internals only.</p>
 */
public class ExecutionResult {

    private String adapterId;

    private ExecutionStatus status;

    private String summary;

    private List<ExecutionError> errors = new ArrayList<>();

    public ExecutionResult() {
    }

    public ExecutionResult(
            String adapterId,
            ExecutionStatus status,
            String summary,
            List<ExecutionError> errors) {
        this.adapterId = adapterId;
        this.status = status;
        this.summary = summary;
        this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ExecutionError> getErrors() {
        return errors;
    }

    public void setErrors(List<ExecutionError> errors) {
        this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }
}
