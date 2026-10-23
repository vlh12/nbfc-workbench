#requires -Version 7.0
[CmdletBinding()]
param([string]$BaseUrl='http://localhost:8090',[switch]$Import)
$ErrorActionPreference='Stop'
if(-not ([uri]$BaseUrl).IsLoopback){throw 'Use a local workbench URL for this synthetic data pack.'}
$BaseUrl=$BaseUrl.TrimEnd('/')
$data=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'demo-data.json') -Raw | ConvertFrom-Json
$credential=Get-Credential -Message 'Sign in with your existing Contributor account. Nothing is approved automatically.'
$web=New-Object Microsoft.PowerShell.Commands.WebRequestSession
function Send-Json([string]$Path,$Body){
 $token=Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $web
 Invoke-RestMethod -Uri ($BaseUrl+$Path) -Method Post -WebSession $web -Headers @{$token.headerName=$token.token} -ContentType 'application/json' -Body ($Body|ConvertTo-Json -Depth 15)
}
try{
 $login=Send-Json '/api/auth/login' @{username=$credential.UserName;password=$credential.GetNetworkCredential().Password;role='CONTRIBUTOR'}
 Write-Host "Local verification code: $($login.code)"
 $code=Read-Host 'Enter the displayed code to complete sign-in'
 $null=Send-Json '/api/auth/verify' @{code=$code}
 foreach($portfolio in $data.portfolios){
  $file=Get-Item -LiteralPath (Join-Path $PSScriptRoot $portfolio.file)
  $token=Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $web
  $result=Invoke-RestMethod -Uri "$BaseUrl/api/portfolios/validate" -Method Post -WebSession $web -Headers @{$token.headerName=$token.token} -Form @{file=$file;synthetic='true'}
  if($result.errors.Count -or $result.warnings.Count){throw "Validation needs correction for $($portfolio.name). Nothing from this file was saved."}
  Write-Host "Validated: $($portfolio.name), $($result.rows.Count) loans."
  if($Import){
   $token=Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $web
   $saved=Invoke-RestMethod -Uri "$BaseUrl/api/portfolios" -Method Post -WebSession $web -Headers @{$token.headerName=$token.token} -Form @{file=$file;synthetic='true';name=$portfolio.name}
   Write-Host "Saved for Supervisor review: $($portfolio.name), $($saved.status)."
  }
 }
 if($Import){
  foreach($capital in $data.capital){$entry=Send-Json '/api/portfolios/capital' $capital;Write-Host "Saved for review: $($entry.name)."}
  foreach($quality in $data.quality){$entry=Send-Json '/api/portfolios/quality' $quality;Write-Host "Saved for review: $($entry.name)."}
  Write-Host 'Contributor step complete. Supervisor must inspect and approve the named inputs, then save thresholds. Analyst calculations and final scenario approval happen in the website. See README.txt.'
 }else{Write-Host 'Validation only. No portfolio or supporting data was saved. Use -Import to submit all three periods for review.'}
}finally{
 try{$token=Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $web;$null=Invoke-RestMethod -Uri "$BaseUrl/api/auth/logout" -Method Post -WebSession $web -Headers @{$token.headerName=$token.token}}catch{}
 $credential=$null
}
