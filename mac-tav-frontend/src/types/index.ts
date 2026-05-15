export interface ApiResult<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface NetworkWorkspace {
  task?: NetworkTask;
  currentIntentVersion?: number;
  currentPlanVersion?: number;
  currentConfigVersion?: number;
  currentExecutionVersion?: number;
  currentValidationVersion?: number;
  intent?: NetworkIntent;
  plan?: NetworkPlan;
  configSet?: ConfigSet;
  executionReport?: ExecutionReport;
  validationReport?: ValidationReport;
  agentLogs?: AgentStepLog[];
}

export interface NetworkTask {
  taskId: string;
  rawText: string;
  taskStatus: string;
  currentStage: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentStepLog {
  stepId?: string;
  taskId?: string;
  agentName?: string;
  stage?: string;
  stageStatus?: string;
  message?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface NetworkIntent {
  taskId?: string;
  intentVersion?: number;
  rawText?: string;
  semanticIntentGraph?: SemanticIntentGraph;
  assumptions?: Assumption[];
  stageStatus?: string;
}

export interface SemanticIntentGraph {
  nodes?: IntentNode[];
  relations?: IntentRelation[];
}

export interface IntentNode {
  id: string;
  name?: string;
  type?: string;
  description?: string;
}

export interface IntentRelation {
  id: string;
  type?: string;
  source?: string;
  target?: string;
  action?: string;
  service?: string;
  description?: string;
  explicit?: boolean;
}

export interface Assumption {
  id?: string;
  description?: string;
  confidence?: string;
}

export interface NetworkPlan {
  taskId?: string;
  intentVersion?: number;
  planVersion?: number;
  planSummary?: string;
  selectedArchitecture?: Record<string, unknown>;
  topology?: Topology;
  zones?: NetworkZone[];
  addressPlan?: AddressPlanItem[];
  vlanPlan?: VlanPlanItem[];
  routingPlan?: RoutingPlan;
  securityPolicyPlan?: SecurityPolicyPlanItem[];
  natPlan?: NatPlan;
  targetEnvironment?: TargetEnvironment;
  stageStatus?: string;
}

export interface Topology {
  nodes?: TopologyNode[];
  links?: TopologyLink[];
}

export interface TopologyNode {
  id: string;
  name?: string;
  nodeType?: string;
  deviceType?: string;
  hostType?: string;
  role?: string;
  vendor?: string;
  zoneId?: string;
}

export interface TopologyLink {
  id: string;
  sourceNode: string;
  sourceInterface?: string;
  targetNode: string;
  targetInterface?: string;
  linkType?: string;
}

export interface NetworkZone {
  zoneId?: string;
  zoneName?: string;
  zoneType?: string;
  description?: string;
  [key: string]: unknown;
}

export interface AddressPlanItem {
  id?: string;
  zoneId?: string;
  cidr?: string;
  gateway?: string;
  usage?: string;
  [key: string]: unknown;
}

export interface VlanPlanItem {
  id?: string;
  zoneId?: string;
  vlanId?: number;
  vlanName?: string;
  [key: string]: unknown;
}

export interface RoutingPlan {
  routingProtocol?: string;
  routers?: Record<string, unknown>[];
  defaultRoutes?: Record<string, unknown>[];
  [key: string]: unknown;
}

export interface SecurityPolicyPlanItem {
  id?: string;
  name?: string;
  sourceZone?: string;
  targetZone?: string;
  action?: string;
  service?: string;
  [key: string]: unknown;
}

export interface NatPlan {
  enabled?: boolean;
  [key: string]: unknown;
}

export interface TargetEnvironment {
  environmentType?: string;
  vendor?: string;
  platform?: string;
  [key: string]: unknown;
}

export interface ConfigSet {
  taskId?: string;
  planVersion?: number;
  configVersion?: number;
  targetEnvironment?: TargetEnvironment;
  generationSummary?: string;
  generationSources?: Record<string, unknown>[];
  deviceConfigs?: DeviceConfig[];
  endpointConfigs?: EndpointConfig[];
  rollbackPlan?: Record<string, unknown>;
  warnings?: ConfigWarning[];
  stageStatus?: string;
}

export interface DeviceConfig {
  deviceId: string;
  deviceName?: string;
  deviceType?: string;
  vendor?: string;
  configText?: string;
  commandBlocks?: CommandBlock[];
}

export interface CommandBlock {
  blockId: string;
  blockType?: string;
  order?: number;
  title?: string;
  commands?: string[];
  explanation?: string;
  rollbackCommands?: string[];
  dependsOn?: string[];
  traceRefs?: TraceRefs;
}

export interface TraceRefs {
  intentRelationIds?: string[];
  planElementIds?: string[];
}

export interface EndpointConfig {
  endpointId?: string;
  endpointName?: string;
  ipAddress?: string;
  gateway?: string;
  [key: string]: unknown;
}

export interface ConfigWarning {
  warningId?: string;
  level?: string;
  message?: string;
  [key: string]: unknown;
}

export interface ExecutionReport {
  taskId?: string;
  planVersion?: number;
  configVersion?: number;
  executionVersion?: number;
  executionMode?: string;
  executionPlan?: Record<string, unknown>;
  runtimeState?: RuntimeState;
  testResult?: TestResult;
  stageStatus?: string;
}

export interface RuntimeState {
  environmentStatus?: string;
  nodes?: RuntimeNodeState[];
  links?: RuntimeLinkState[];
  controllerConnected?: boolean;
}

export interface RuntimeNodeState {
  nodeId?: string;
  status?: string;
}

export interface RuntimeLinkState {
  linkId?: string;
  status?: string;
}

export interface TestResult {
  connectivityTests?: TestCaseResult[];
  policyTests?: TestCaseResult[];
  rawLogs?: string[];
}

export interface TestCaseResult {
  testId: string;
  source?: string;
  target?: string;
  expected?: string;
  actual?: string;
  success?: boolean;
  rawOutput?: string;
}

export interface ValidationReport {
  taskId?: string;
  intentVersion?: number;
  planVersion?: number;
  configVersion?: number;
  executionVersion?: number;
  validationVersion?: number;
  overallStatus?: string;
  summary?: string;
  items?: ValidationItem[];
  suggestions?: string[];
  stageStatus?: string;
}

export interface ValidationItem {
  itemId: string;
  name?: string;
  type?: string;
  expected?: string;
  actual?: string;
  passed?: boolean;
  relatedIntentRelationId?: string;
  relatedPlanElementIds?: string[];
  relatedConfigBlockIds?: string[];
  relatedTestId?: string;
  message?: string;
}
