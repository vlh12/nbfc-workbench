const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const Module = require('node:module');
const ts = require('../frontend/node_modules/typescript');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE_PATH || 'playwright');
const helpers = new Module(path.join(__dirname, '../frontend/src/riskExploration.js'));
helpers.filename = path.join(__dirname, '../frontend/src/riskExploration.js'); helpers.paths = module.paths;
// Load the two pure TypeScript helper modules without adding a test dependency.
const money = new Module('dashboardMetrics');
money._compile(ts.transpileModule(fs.readFileSync(path.join(__dirname, '../frontend/src/dashboardMetrics.ts'), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, 'dashboardMetrics.js');
helpers.require = name => name === './dashboardMetrics' ? money.exports : require(name);
helpers._compile(ts.transpileModule(fs.readFileSync(path.join(__dirname, '../frontend/src/riskExploration.ts'), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, helpers.filename);
const { stressPreview, latestNpaPeriods } = helpers.exports;
const worked = stressPreview('12000000000','50000000000',60,30,'5','10');
assert.equal(worked.lossCents, 504000000000n); assert.equal(worked.impact, '10.080000'); assert.equal(worked.alert, 'HIGH');
assert.equal(stressPreview('100','100',5,0,'5','10').alert,'MEDIUM');
assert.equal(stressPreview('100','100',10,0,'5','10').alert,'HIGH');
assert.equal(stressPreview('100','100',100,100,'5','10').lossCents,0n);
assert.equal(stressPreview('0.01','1',20,0,'5','10').impact,'0.200000');
assert.throws(()=>stressPreview('0','100',20,20,'5','10'));
assert.throws(()=>stressPreview('100','0',20,20,'5','10'));
const quality=(nbfcId,reportingDate,npa,total='1000')=>({bankId:'SYN-BANK-001',nbfcId,reportingDate,totalAmount:total,buckets:{CURRENT:String(Number(total)-Number(npa)),DPD_1_30:'0',DPD_31_60:'0',DPD_61_90:'0',NPA_90_PLUS:npa,RESTRUCTURED:'0',WRITE_OFF_RECOVERY:'0'}});
const sameInstant = latestNpaPeriods([{...quality('SYN-NBFC-001','2026-08-31','10'),id:'a',createdAt:'2026-10-01T00:00:00Z'},{...quality('SYN-NBFC-001','2026-08-31','20'),id:'b',createdAt:'2026-10-01T00:00:00.000000001Z'}],'SYN-BANK-001','SYN-NBFC-001');
assert.equal(sameInstant[0].id,'b');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.UI_CHROME_PATH,headless:true});
 try{
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  await context.addInitScript(()=>{if(!sessionStorage.getItem('workbench-demo-role'))sessionStorage.setItem('workbench-demo-role','ANALYST');});
  const page=await context.newPage();page.setDefaultTimeout(15000);const errors=[],writes=[];page.on('pageerror',e=>errors.push(e.message));
  let failHistory=false, mode='normal';
  const details={
   'sep-current':quality('SYN-NBFC-001','2026-09-30','70'),
   'aug-old':quality('SYN-NBFC-001','2026-08-31','900'),
   'aug-new':quality('SYN-NBFC-001','2026-08-31','100'),
   'jul-zero':quality('SYN-NBFC-001','2026-07-31','0','0'),
   'other-nbfc':quality('SYN-NBFC-002','2026-08-31','990'),
  };
  const entries=()=>[
   {id:'portfolio',kind:'PORTFOLIO',status:mode==='unapproved'?'VALIDATED':'APPROVED',reportingDate:'2026-09-30',createdAt:'2026-10-03T00:00:00Z'},
   {id:'capital',kind:'CAPITAL',status:'APPROVED',reportingDate:'2026-09-30',createdAt:'2026-10-03T00:00:00Z'},
   {id:'stale-capital',kind:'CAPITAL',status:'APPROVED',reportingDate:'2026-08-31',createdAt:'2026-10-03T00:00:00Z'},
   {id:'threshold',kind:'THRESHOLDS',status:'APPROVED',reportingDate:null,createdAt:'2026-10-03T00:00:00Z'},
   ...Object.entries(details).filter(([id])=>mode!=='single'||id==='sep-current').map(([id,q])=>({id,kind:'QUALITY',status:'APPROVED',reportingDate:q.reportingDate,createdAt:id==='aug-new'?'2026-10-02T00:00:00Z':'2026-10-01T00:00:00Z'}))
  ];
  await page.route('**/api/**',async route=>{
   const request=route.request(),url=new URL(request.url());if(request.method()!=='GET')writes.push(url.pathname);
   let body=[];
   if(url.pathname==='/api/portfolios')body=entries();
   else if(url.pathname==='/api/portfolios/portfolio')body={id:'portfolio',bankId:'SYN-BANK-001',reportingDate:'2026-09-30',status:mode==='unapproved'?'VALIDATED':'APPROVED',warnings:[],rows:[{loanId:'SYN-LOAN-001',nbfcId:'SYN-NBFC-001',amountLent:'12000000000',outstandingAmount:'12000000000',eadAmount:'12000000000',dpdDays:0,loanStatus:'CURRENT'}]};
   else if(url.pathname==='/api/exposures/portfolios/portfolio')body={snapshotId:'portfolio',totalLent:'12000000000',totalOutstanding:'12000000000',totalEad:'12000000000',concentrationDenominator:'Synthetic explicit EAD',nbfcs:[{nbfcId:'SYN-NBFC-001',eadAmount:'12000000000',outstandingAmount:'12000000000',loans:1}]};
   else if(url.pathname.startsWith('/api/portfolios/capital/'))body={bankId:'SYN-BANK-001',reportingDate:url.pathname.endsWith('stale-capital')?'2026-08-31':'2026-09-30',eligibleCapital:'50000000000',label:'Synthetic capital'};
   else if(url.pathname==='/api/portfolios/thresholds/threshold')body={bankId:'SYN-BANK-001',name:'Demo thresholds',mediumPct:'5',highPct:'10'};
   else if(url.pathname.startsWith('/api/portfolios/quality/')){
    if(failHistory&&url.pathname.endsWith('aug-old'))return route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'Synthetic unavailable'})});
    body=details[url.pathname.split('/').pop()];
   }
   await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
  });
  await page.goto(process.env.UI_TEST_URL||'http://localhost:5173/',{waitUntil:'networkidle'});
  const heatmap=page.getByRole('region',{name:'Severity–recovery heatmap',exact:true});
  const history=page.getByRole('region',{name:'NPA percentage history',exact:true});
  await page.getByLabel('Heatmap capital version').selectOption('capital');await page.getByLabel('Heatmap threshold version').selectOption('threshold');
  await heatmap.getByRole('button',{name:'Generate heatmap preview'}).click();
  await heatmap.getByRole('button',{name:'Severity 100%, recovery 0%, impact 24.000000%, HIGH',exact:true}).waitFor();
  assert.equal(await heatmap.locator('tbody button').count(),36);
  await heatmap.getByRole('button',{name:'Severity 60%, recovery 40%, impact 8.640000%, MEDIUM',exact:true}).focus();
  await heatmap.getByRole('status').getByText(/Direct loss ₹4,32,00,00,000.00/).waitFor();
  assert.equal(await heatmap.getByRole('button',{name:'Severity 100%, recovery 100%, impact 0.000000%, LOW',exact:true}).count(),1);
  if(process.env.UI_SCREENSHOT_DIR){fs.mkdirSync(process.env.UI_SCREENSHOT_DIR,{recursive:true});await heatmap.screenshot({path:path.join(process.env.UI_SCREENSHOT_DIR,'severity-recovery-heatmap.png')});}
  await page.getByLabel('Heatmap capital version').selectOption('stale-capital');
  await heatmap.getByRole('button',{name:'Generate heatmap preview'}).click();await heatmap.getByRole('alert').getByText('Select capital matching the portfolio reporting date.').waitFor();assert.equal(await heatmap.locator('tbody button').count(),0);
  await page.getByLabel('Current snapshot').selectOption('sep-current');await page.getByRole('button',{name:'View asset-quality trend',exact:true}).click();
  await history.getByRole('button',{name:'Load NPA history'}).click();await history.getByRole('table').waitFor();
  assert.equal(await history.locator('tbody tr').count(),3);
  const august=history.locator('tbody tr').filter({hasText:'2026-08-31'});assert.match(await august.innerText(),/10.000000%/);assert.match(await august.innerText(),/aug-new/);
  assert.match(await history.locator('tbody tr').first().innerText(),/Not available \(zero total\)/);
  assert.equal(await history.locator('.recharts-line-curve').count(),1);
  if(process.env.UI_SCREENSHOT_DIR)await history.screenshot({path:path.join(process.env.UI_SCREENSHOT_DIR,'npa-percentage-line.png')});
  await page.setViewportSize({width:390,height:844});assert.equal(await history.evaluate(el=>el.getBoundingClientRect().right<=innerWidth),true);assert.equal(await heatmap.evaluate(el=>el.getBoundingClientRect().right<=innerWidth),true);
  failHistory=true;await history.getByRole('button',{name:'Load NPA history'}).click();await history.getByRole('alert').waitFor();assert.equal(await history.locator('table').count(),0);failHistory=false;
  mode='single';await page.reload({waitUntil:'networkidle'});await page.getByLabel('Current snapshot').selectOption('sep-current');await page.getByRole('button',{name:'View asset-quality trend',exact:true}).click();await history.getByRole('button',{name:'Load NPA history'}).click();await history.getByText(/At least two distinct reporting dates/).waitFor();assert.equal(await history.locator('.recharts-line-curve').count(),0);
  mode='unapproved';await page.reload({waitUntil:'networkidle'});await page.getByLabel('Heatmap capital version').selectOption('capital');await page.getByLabel('Heatmap threshold version').selectOption('threshold');await heatmap.getByRole('button',{name:'Generate heatmap preview'}).click();await heatmap.getByRole('alert').getByText(/Select an approved portfolio/).waitFor();
  assert.deepEqual(writes,[]);assert.deepEqual(errors,[]);
  console.log('PASS: heatmap formula/rounding/boundaries, 36 previews, stale/unapproved input blocking, no writes, correct NBFC/latest versions, zero-total gaps, single-period and failed-history handling, mobile widths (synthetic mocked APIs).');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
