#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8090',
    [switch]$Import
)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
if (-not ([uri]$BaseUrl).IsLoopback) { throw 'This synthetic demo importer supports a local loopback API only.' }
$data = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'demo-data.json') -Raw | ConvertFrom-Json -AsHashtable
if (-not $data.synthetic) { throw 'Synthetic confirmation missing from pack.' }

# Validation-only mode never saves portfolios or supporting data.
foreach ($portfolio in $data.portfolios) {
    $file = Get-Item -LiteralPath (Join-Path $PSScriptRoot $portfolio.file)
    $validation = Invoke-RestMethod -Uri "$BaseUrl/api/portfolios/validate" -Method Post -Headers @{'X-Demo-Role'='CONTRIBUTOR'} -Form @{file=$file;synthetic='true'}
    if ($validation.errors.Count -gt 0 -or $validation.warnings.Count -gt 0 -or $validation.rows.Count -ne 16) { throw "Validation failed for $($portfolio.file). Inspect errors/warnings before importing." }
    Write-Host "Validated $($portfolio.reportingDate): 16 loans; zero errors and warnings."
}
if (-not $Import) { Write-Host 'Validation only. To create the full synthetic demo, run this script with -Import.'; return }

$statePath = Join-Path $PSScriptRoot 'import-state.json'
if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json -AsHashtable
    if ($state.baseUrl -ne $BaseUrl -or $state.packId -ne $data.packId) { throw 'Existing import state belongs to another destination or pack.' }
} else {
    $indiaToday = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromMinutes(330)).Date
    $state = @{packId=$data.packId;baseUrl=$BaseUrl;runKey=[guid]::NewGuid().ToString();portfolios=@{};capital=@{};quality=@{};thresholds=@{};scenarios=@{};assessments=@{};followUps=@{};referenceDate=$indiaToday.ToString('yyyy-MM-dd');complete=$false}
}
function Save-State { $state | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $statePath -Encoding utf8 }
function Invoke-DemoJson {
    param([string]$Role,[string]$Path,[object]$Body,[string]$Method='Post')
    Invoke-RestMethod -Uri ($BaseUrl+$Path) -Method $Method -Headers @{'X-Demo-Role'=$Role} -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 25 -Compress)
}
Save-State
foreach ($portfolio in $data.portfolios) {
    $date=$portfolio.reportingDate
    if (-not $state.portfolios.ContainsKey($date)) {
        $file=Get-Item -LiteralPath (Join-Path $PSScriptRoot $portfolio.file)
        $saved=Invoke-RestMethod -Uri "$BaseUrl/api/portfolios" -Method Post -Headers @{'X-Demo-Role'='CONTRIBUTOR'} -Form @{file=$file;synthetic='true'}
        if ($saved.status -ne 'APPROVED') { throw "Portfolio $date was not approved." }
        $state.portfolios[$date]=$saved.id; Save-State
    }
}
foreach ($capital in $data.capital) {
    $date=$capital.reportingDate
    if (-not $state.capital.ContainsKey($date)) { $saved=Invoke-DemoJson CONTRIBUTOR '/api/portfolios/capital' $capital; $state.capital[$date]=$saved.id; Save-State }
}
foreach ($quality in $data.quality) {
    $key=$quality.reportingDate+'|'+$quality.nbfcId
    if (-not $state.quality.ContainsKey($key)) { $saved=Invoke-DemoJson CONTRIBUTOR '/api/portfolios/quality' $quality; $state.quality[$key]=$saved.id; Save-State }
}
foreach ($threshold in $data.thresholds) {
    if (-not $state.thresholds.ContainsKey($threshold.key)) {
        $body=@{bankId=$threshold.bankId;name=$threshold.name;mediumPct=$threshold.mediumPct;highPct=$threshold.highPct}
        $saved=Invoke-DemoJson SUPERVISOR '/api/portfolios/thresholds' $body; $state.thresholds[$threshold.key]=$saved.id; Save-State
    }
}
foreach ($scenario in $data.scenarios) {
    if (-not $state.scenarios.ContainsKey($scenario.key)) {
        $date=$scenario.reportingDate
        $body=@{snapshotId=$state.portfolios[$date];nbfcId=$scenario.nbfcId;capitalId=$state.capital[$date];qualityId=$state.quality[$date+'|'+$scenario.nbfcId];thresholdId=$state.thresholds[$scenario.thresholdKey];severityPct=$scenario.severityPct;recoveryPct=$scenario.recoveryPct;rationale=$scenario.rationale;idempotencyKey=$state.runKey+'-'+$scenario.key}
        $saved=Invoke-DemoJson ANALYST '/api/scenarios' $body
        if ($saved.status -ne 'COMPLETED') { throw "Scenario failed: $($scenario.name): $($saved.failureReason)" }
        if ([decimal]$saved.result.stressedLoss -ne [decimal]$scenario.expectedLoss -or [decimal]$saved.result.capitalImpactPct -ne [decimal]$scenario.expectedImpactPct -or $saved.result.alertSeverity -ne $scenario.expectedAlert) { throw "Unexpected saved result for $($scenario.name). Inspect it before continuing." }
        $state.scenarios[$scenario.key]=@{id=$saved.id;name=$scenario.name;loss=$saved.result.stressedLoss;impactPct=$saved.result.capitalImpactPct;alert=$saved.result.alertSeverity}; Save-State
    }
}
$reviews=@(
    @{key='scenario-1';target='SUBMITTED';note='Synthetic baseline submitted for review.'},
    @{key='scenario-2';target='RETURNED';note='Synthetic moderate case: add a more detailed recovery rationale.'},
    @{key='scenario-3';target='APPROVED';note='Synthetic severe scenario reviewed; arithmetic and recorded inputs checked.'}
)
foreach ($review in $reviews) {
    if (-not $state.assessments.ContainsKey($review.key)) {
        $saved=Invoke-DemoJson ANALYST '/api/governance/scenario-assessments' @{scenarioId=$state.scenarios[$review.key].id;note=$review.note}
        $state.assessments[$review.key]=@{id=$saved.id;status=$saved.status}; Save-State
    }
    $assessment=$state.assessments[$review.key]
    if ($review.target -ne 'SUBMITTED' -and $assessment.status -ne $review.target) {
        $saved=Invoke-DemoJson SUPERVISOR ('/api/governance/assessments/'+$assessment.id+'/decision') @{decision=$review.target;note=$review.note}
        $assessment.status=$saved.status; Save-State
    }
}
$referenceDate=[datetime]::ParseExact($state.referenceDate,'yyyy-MM-dd',[cultureinfo]::InvariantCulture)
$actions=@(
    @{key='overdue-open';assessment='scenario-1';title='Review synthetic exposure concentration';due=$referenceDate.AddDays(-1).ToString('yyyy-MM-dd');transitions=@()},
    @{key='overdue-progress';assessment='scenario-2';title='Document recovery assumption evidence';due=$referenceDate.AddDays(-1).ToString('yyyy-MM-dd');transitions=@('IN_PROGRESS')},
    @{key='closed';assessment='scenario-3';title='Verify severe-scenario calculation';due=$referenceDate.AddDays(7).ToString('yyyy-MM-dd');transitions=@('IN_PROGRESS','EVIDENCE_SUBMITTED','VERIFIED','CLOSED')}
)
foreach ($action in $actions) {
    if (-not $state.followUps.ContainsKey($action.key)) {
        $assessmentId=$state.assessments[$action.assessment].id
        $saved=Invoke-DemoJson SUPERVISOR ('/api/governance/assessments/'+$assessmentId+'/follow-ups') @{title=$action.title;owner='Demo Risk Analyst';dueDate=$action.due}
        $state.followUps[$action.key]=@{id=$saved.id;status=$saved.status;steps=0}; Save-State
    }
    $stored=$state.followUps[$action.key]
    for ($step=[int]$stored.steps; $step -lt $action.transitions.Count; $step++) {
        $next=$action.transitions[$step]
        $saved=Invoke-DemoJson SUPERVISOR ('/api/governance/follow-ups/'+$stored.id) @{status=$next;evidence='Synthetic evidence: verified the recorded EAD, capital, severity, recovery and threshold version.'} 'Patch'
        $stored.status=$saved.status; $stored.steps=$step+1; Save-State
    }
}
$state.complete=$true; Save-State
Write-Host 'Full synthetic demo imported. Select the September portfolio matching import-state.json.'
Write-Host 'Created: 3 portfolios, 3 capital versions, 24 quality versions, 2 thresholds, 7 scenarios, 3 assessments and 3 follow-ups.'
Write-Host 'Read import-state.json for the exact IDs. Existing records remain; dashboard counts may include them.'
