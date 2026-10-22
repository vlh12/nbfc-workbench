#requires -Version 7.0
[CmdletBinding()]
param([string]$BaseUrl='http://localhost:8090',[switch]$Import)
# The old header-based import cannot bypass the new Supervisor data-review step.
& (Join-Path $PSScriptRoot '../workflow-demo/Import-WorkflowData.ps1') -BaseUrl $BaseUrl -Import:$Import
