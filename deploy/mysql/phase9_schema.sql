-- MAC-TAV Phase 9 manual MySQL schema.
-- Execute manually after creating the mac_tav database. No foreign keys are
-- defined here; consistency is enforced later by Repository / Service logic.

CREATE TABLE IF NOT EXISTS network_task (
  task_id varchar(64) NOT NULL,
  raw_text longtext NOT NULL,
  description varchar(512) DEFAULT NULL,
  task_status varchar(32) NOT NULL,
  current_stage varchar(32) NOT NULL,
  created_by varchar(128) DEFAULT NULL,
  create_time datetime NOT NULL,
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS network_workspace_state (
  task_id varchar(64) NOT NULL,
  workspace_status varchar(32) NOT NULL,
  current_intent_version int DEFAULT NULL,
  current_plan_version int DEFAULT NULL,
  current_config_version int DEFAULT NULL,
  current_execution_version int DEFAULT NULL,
  current_validation_version int DEFAULT NULL,
  current_repair_version int DEFAULT NULL,
  current_artifact_refs_json longtext,
  update_time datetime NOT NULL,
  PRIMARY KEY (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS network_artifact (
  artifact_id varchar(64) NOT NULL,
  task_id varchar(64) NOT NULL,
  artifact_type varchar(64) NOT NULL,
  version int NOT NULL,
  stage varchar(32) NOT NULL,
  status varchar(32) NOT NULL,
  payload_type varchar(256) DEFAULT NULL,
  payload_json longtext,
  payload_summary varchar(1024) DEFAULT NULL,
  trace_refs_json longtext,
  created_by varchar(128) DEFAULT NULL,
  create_time datetime NOT NULL,
  PRIMARY KEY (artifact_id),
  UNIQUE KEY uk_artifact_task_type_version (task_id, artifact_type, version),
  KEY idx_artifact_task_type (task_id, artifact_type),
  KEY idx_artifact_task_stage (task_id, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_execution_record (
  record_id varchar(64) NOT NULL,
  task_id varchar(64) NOT NULL,
  trace_id varchar(128) DEFAULT NULL,
  agent_name varchar(128) DEFAULT NULL,
  target_agent_name varchar(128) DEFAULT NULL,
  remote_call_type varchar(64) DEFAULT NULL,
  agent_card_version varchar(64) DEFAULT NULL,
  stage varchar(32) DEFAULT NULL,
  stage_status varchar(32) DEFAULT NULL,
  input_artifact_ids_json longtext,
  output_artifact_ids_json longtext,
  tool_call_summaries_json longtext,
  mcp_call_summaries_json longtext,
  a2a_call_summaries_json longtext,
  model_call_count int DEFAULT NULL,
  start_time datetime DEFAULT NULL,
  finish_time datetime DEFAULT NULL,
  duration_ms bigint DEFAULT NULL,
  input_summary varchar(1024) DEFAULT NULL,
  output_summary varchar(1024) DEFAULT NULL,
  error_code varchar(128) DEFAULT NULL,
  error_message varchar(1024) DEFAULT NULL,
  message varchar(1024) DEFAULT NULL,
  PRIMARY KEY (record_id),
  KEY idx_agent_record_task_stage (task_id, stage),
  KEY idx_agent_record_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workspace_event (
  event_id varchar(64) NOT NULL,
  task_id varchar(64) NOT NULL,
  event_type varchar(128) NOT NULL,
  stage varchar(32) DEFAULT NULL,
  event_time datetime NOT NULL,
  severity varchar(32) DEFAULT NULL,
  title varchar(256) DEFAULT NULL,
  message varchar(1024) DEFAULT NULL,
  related_artifact_id varchar(64) DEFAULT NULL,
  related_record_id varchar(64) DEFAULT NULL,
  trace_id varchar(128) DEFAULT NULL,
  payload_summary varchar(1024) DEFAULT NULL,
  PRIMARY KEY (event_id),
  KEY idx_workspace_event_task_time (task_id, event_time),
  KEY idx_workspace_event_task_type (task_id, event_type),
  KEY idx_workspace_event_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workspace_change_record (
  change_id varchar(64) NOT NULL,
  task_id varchar(64) NOT NULL,
  stage varchar(32) DEFAULT NULL,
  change_type varchar(128) NOT NULL,
  from_artifact_id varchar(64) DEFAULT NULL,
  to_artifact_id varchar(64) DEFAULT NULL,
  reason varchar(1024) DEFAULT NULL,
  created_by varchar(128) DEFAULT NULL,
  create_time datetime NOT NULL,
  PRIMARY KEY (change_id),
  KEY idx_workspace_change_task_time (task_id, create_time),
  KEY idx_workspace_change_task_type (task_id, change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workflow_job (
  job_id varchar(64) NOT NULL,
  task_id varchar(64) NOT NULL,
  requested_stage varchar(32) DEFAULT NULL,
  job_type varchar(64) NOT NULL,
  job_status varchar(32) NOT NULL,
  requested_by varchar(128) DEFAULT NULL,
  request_payload_json longtext,
  start_time datetime DEFAULT NULL,
  finish_time datetime DEFAULT NULL,
  error_code varchar(128) DEFAULT NULL,
  error_message varchar(1024) DEFAULT NULL,
  trace_id varchar(128) DEFAULT NULL,
  create_time datetime NOT NULL,
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (job_id),
  KEY idx_workflow_job_task_status (task_id, job_status),
  KEY idx_workflow_job_trace (trace_id),
  KEY idx_workflow_job_status_create_time (job_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
