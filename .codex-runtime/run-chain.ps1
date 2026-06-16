$ErrorActionPreference='Stop'
$base='http://127.0.0.1:18080/api/v1'
function PostJson($url,$body=$null){
  if($null -eq $body){ Invoke-RestMethod -Method Post $url }
  else { Invoke-RestMethod -Method Post $url -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 20) }
}
function WaitJob($base,$jobId,[int]$timeoutSec=260){
  $deadline=(Get-Date).AddSeconds($timeoutSec)
  do {
    Start-Sleep -Seconds 3
    $job=Invoke-RestMethod "$base/workflows/jobs/$jobId"
    $s=$job.data.jobStatus
    Write-Host "JOB $jobId $s $($job.data.errorCode) $($job.data.errorMessage)"
    if($s -in @('SUCCESS','FAILED','ERROR','CANCELLED','INTERRUPTED')){ return $job.data }
  } while((Get-Date) -lt $deadline)
  throw "Timeout waiting job $jobId"
}
$task=PostJson "$base/tasks" @{rawText='办公区与访客区隔离，访客区不能访问服务器区；办公区允许访问服务器区，并生成配置、执行验证。'; createdBy='codex-dynamic-review'}
$taskId=$task.data.taskId
Write-Host "TASK $taskId"
$stages=@(
  @{Name='run'; Url="$base/workflows/$taskId/run"},
  @{Name='plan'; Url="$base/workflows/$taskId/plan"},
  @{Name='config'; Url="$base/workflows/$taskId/config"},
  @{Name='execution'; Url="$base/executions/$taskId/run"},
  @{Name='validation'; Url="$base/validations/$taskId/run"}
)
$result=[ordered]@{taskId=$taskId; stages=@()}
foreach($stage in $stages){
  Write-Host "STAGE $($stage.Name)"
  $submit=PostJson $stage.Url
  $jobId=$submit.data.jobId
  $job=WaitJob $base $jobId
  $result.stages += [ordered]@{name=$stage.Name; jobId=$jobId; status=$job.jobStatus; errorCode=$job.errorCode; errorMessage=$job.errorMessage}
  if($job.jobStatus -ne 'SUCCESS'){ break }
}
$arts=Invoke-RestMethod "$base/artifacts/$taskId?size=50"
$result.artifacts=$arts.data.items | ForEach-Object { [ordered]@{type=$_.artifactType; version=$_.version; id=$_.artifactId} }
$result | ConvertTo-Json -Depth 8
