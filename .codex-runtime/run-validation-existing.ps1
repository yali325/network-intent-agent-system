$ErrorActionPreference='Stop'
$base='http://127.0.0.1:18080/api/v1'
$taskId='task-ad2a4a3d-94c3-4582-bf0c-991fabcd76e9'
function WaitJob($jobId,[int]$timeoutSec=220){
  $deadline=(Get-Date).AddSeconds($timeoutSec)
  do { Start-Sleep -Seconds 3; $job=Invoke-RestMethod "$base/workflows/jobs/$jobId"; $s=$job.data.jobStatus; Write-Host "JOB $jobId $s $($job.data.errorCode) $($job.data.errorMessage)"; if($s -in @('SUCCESS','FAILED','ERROR','CANCELLED','INTERRUPTED')){ return $job.data } } while((Get-Date) -lt $deadline)
  throw "Timeout waiting $jobId"
}
$submit=Invoke-RestMethod -Method Post "$base/validations/$taskId/run"
$job=WaitJob $submit.data.jobId
$arts=Invoke-RestMethod "$base/artifacts/$taskId"
[ordered]@{taskId=$taskId; validationJob=$job; artifacts=($arts.data.items | ForEach-Object { [ordered]@{type=$_.artifactType; version=$_.version; id=$_.artifactId} })} | ConvertTo-Json -Depth 8
