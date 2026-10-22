import { requireReportApproval, scopedReview, type ReportReview } from "./reportReview";
import { scenarioName } from "./workflowLabels";
import type { AnalysisPortfolio, AnalysisScenario } from "./analysisData";
import { exposureMetrics, moneyCents, shareLabel } from "./dashboardMetrics";
import { stressPreview } from "./riskExploration";
import { PDFDocument, StandardFonts, rgb, type PDFFont, type PDFPage } from "./vendor/pdf-lib.js";

const WIDTH = 595.28, HEIGHT = 841.89, MARGIN = 44, BOTTOM = 60;
const CONTENT = WIDTH - 2 * MARGIN;
const ink = rgb(0.09, 0.18, 0.3), muted = rgb(0.35, 0.42, 0.5);
const blue = rgb(0.07, 0.26, 0.53), pale = rgb(0.93, 0.96, 0.99);

// Stored amounts and totals use exact integer arithmetic; only chart geometry is approximate.
export async function createPortfolioPdf(snapshot: AnalysisPortfolio, runs: AnalysisScenario[], review: ReportReview = { state: "not_loaded" }): Promise<Uint8Array> {
  const scoped = [requireReportApproval(snapshot, runs, review)];
  const pdf = await PDFDocument.create();
  pdf.setTitle("NBFC portfolio evidence report - " + snapshot.reportingDate);
  pdf.setSubject("Saved synthetic portfolio and recorded scenario evidence");
  pdf.setCreator("NBFC Workbench");
  const regular = await pdf.embedFont(StandardFonts.Helvetica);
  const bold = await pdf.embedFont(StandardFonts.HelveticaBold);
  let page: PDFPage, y = 0;
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");

  function normalized(value: string) { return value.replace(/\t/g, "    ").replace(/\r/g, ""); }
  function supported(text: string, font: PDFFont) {
    try { font.encodeText(text); return true; } catch { return false; }
  }
  function measure(text: string, size: number, font: PDFFont): number {
    if (supported(text, font)) return font.widthOfTextAtSize(text, size);
    if (!context) throw new Error("Your browser cannot render report text. Please use a current browser.");
    context.font = `${font === bold ? "bold " : ""}${size}px Arial, sans-serif`;
    return context.measureText(text).width;
  }
  function wrap(value: string, width: number, size: number, font = regular): string[] {
    return normalized(value).split("\n").flatMap(paragraph => {
      const output: string[] = [];
      let line = "";
      // Character wrapping also handles long IDs, unbroken JSON and Unicode safely.
      for (const char of paragraph) {
        if (line && measure(line + char, size, font) > width) {
          const space = line.lastIndexOf(" ");
          if (space > line.length / 2) { output.push(line.slice(0, space)); line = line.slice(space + 1); }
          else { output.push(line); line = ""; }
        }
        line += char;
      }
      output.push(line);
      return output;
    });
  }
  async function draw(text: string, x: number, baseline: number, size: number, font = regular, color = ink) {
    if (!text) return;
    if (supported(text, font)) { page.drawText(text, { x, y: baseline, size, font, color }); return; }
    // Preserve Unicode names and notes locally; ordinary report text remains selectable.
    if (!context) throw new Error("Your browser cannot render report text.");
    const width = measure(text, size, font), scale = 3;
    canvas.width = Math.ceil((width + 4) * scale);
    canvas.height = Math.ceil(size * 1.7 * scale);
    context.scale(scale, scale);
    context.font = `${font === bold ? "bold " : ""}${size}px Arial, sans-serif`;
    context.fillStyle = `rgb(${Math.round(color.red * 255)},${Math.round(color.green * 255)},${Math.round(color.blue * 255)})`;
    context.textBaseline = "alphabetic";
    context.fillText(text, 2, size * 1.2);
    const image = await pdf.embedPng(canvas.toDataURL("image/png"));
    page.drawImage(image, { x: x - 2, y: baseline - size * 0.5, width: canvas.width / scale, height: canvas.height / scale });
  }
  async function newPage() {
    page = pdf.addPage([WIDTH, HEIGHT]);
    y = HEIGHT - MARGIN;
    await draw("NBFC WORKBENCH  /  SYNTHETIC DATA", MARGIN, y, 9, bold, blue);
    y -= 18;
    page.drawRectangle({ x: MARGIN, y, width: CONTENT, height: 2, color: blue });
    y -= 24;
  }
  async function ensure(height: number) { if (y - height < BOTTOM) await newPage(); }
  async function paragraph(text: string, size = 10, font = regular, color = ink, width = CONTENT) {
    for (const line of wrap(text, width, size, font)) {
      await ensure(size * 1.45);
      await draw(line, MARGIN, y, size, font, color);
      y -= size * 1.45;
    }
    y -= 6;
  }
  async function heading(text: string) {
    await ensure(58);
    y -= 10;
    await paragraph(text, 14, bold, blue);
  }
  async function field(label: string, value: unknown) {
    await paragraph(label + ": " + (value == null ? "Not supplied" : String(value)));
  }
  const chartColors = [rgb(0.059,0.463,0.431), rgb(0.388,0.4,0.945), rgb(0.851,0.467,0.024), rgb(0.745,0.094,0.365), rgb(0.008,0.518,0.78), rgb(0.486,0.227,0.929)];
  function crore(value: bigint) {
    const n=(value+5000000n)/10000000n;
    return (n/100n).toLocaleString("en-IN")+"."+(n%100n).toString().padStart(2,"0")+" Cr";
  }
  function moneyString(value: bigint) { return (value/100n)+"."+(value%100n).toString().padStart(2,"0"); }
  function geometry(value: bigint, total: bigint) { return total>0n ? Number(value*100000000n/total)/100000000 : 0; }
  function percentUnits(value: string) {
    if (!/^\d+(\.\d{1,6})?$/.test(value)) throw new Error("Invalid recorded percentage in report.");
    const [whole,fraction=""]=value.split(".");
    return BigInt(whole)*1000000n+BigInt(fraction.padEnd(6,"0"));
  }
  async function barRows(rows: {label:string; amount:bigint; display:string; color:ReturnType<typeof rgb>}[], max:bigint) {
    for (const row of rows) {
      const labelLines=wrap(row.label,110,8);
      const rowHeight=Math.max(34,labelLines.length*10+10);
      await ensure(rowHeight+3);
      for (let i=0;i<labelLines.length;i++) await draw(labelLines[i],MARGIN,y-i*10,8);
      page.drawRectangle({x:MARGIN+120,y:y-10,width:245,height:15,color:pale});
      const length=245*geometry(row.amount,max);
      if(length>0)page.drawRectangle({x:MARGIN+120,y:y-10,width:length,height:15,color:row.color});
      await draw(row.display,MARGIN+375,y-5,Math.min(8,128/Math.max(1,measure(row.display,1,bold))),bold);
      y-=rowHeight;
    }
  }
  function readablePercent(value: string) { return value.includes(".") ? value.replace(/0+$/, "").replace(/\.$/, "") : value; }
  async function scenarioInsight(run: AnalysisScenario) {
    const result=run.result;
    if(!result || run.status!=="COMPLETED")return;
    await newPage();
    await heading("Understanding the result");
    await paragraph(run.nbfcId+" | Saved completed scenario | "+snapshot.reportingDate,9,regular,muted);
    const cards=[
      {label:"Direct stressed loss (INR)",value:crore(moneyCents(result.stressedLoss))},
      {label:"Impact on eligible capital",value:readablePercent(result.capitalImpactPct)+"%"},
      {label:"Saved prototype alert",value:result.alertSeverity}
    ];
    await ensure(64);
    for(let i=0;i<cards.length;i++) {
      const x=MARGIN+i*(CONTENT/3+3);
      page.drawRectangle({x,y:y-45,width:CONTENT/3-6,height:55,color:pale});
      await draw(cards[i].label,x+9,y-7,8,regular,muted);
      const size=Math.min(16,(CONTENT/3-24)/Math.max(1,measure(cards[i].value,1,bold)));
      await draw(cards[i].value,x+9,y-30,size,bold,blue);
    }
    y-=68;
    await paragraph("Where does this result sit?",12,bold,blue);
    await paragraph("The marker shows your result against the selected alert settings. It is below MEDIUM when it sits in the green band.",8,regular,muted);
    const medium=percentUnits(result.mediumPct), high=percentUnits(result.highPct), impact=percentUnits(result.capitalImpactPct);
    const maximum=impact>high?impact:high;
    const range=maximum+maximum/5n+1n;
    const lowWidth=CONTENT*geometry(medium,range), midWidth=CONTENT*geometry(high-medium,range);
    const barY=y-16;
    page.drawRectangle({x:MARGIN,y:barY,width:lowWidth,height:18,color:rgb(0.82,0.94,0.89)});
    page.drawRectangle({x:MARGIN+lowWidth,y:barY,width:midWidth,height:18,color:rgb(1,0.91,0.65)});
    page.drawRectangle({x:MARGIN+lowWidth+midWidth,y:barY,width:CONTENT-lowWidth-midWidth,height:18,color:rgb(1,0.82,0.84)});
    const marker=MARGIN+CONTENT*geometry(impact,range);
    page.drawRectangle({x:marker-1,y:barY-5,width:2,height:28,color:blue});
    page.drawCircle({x:marker,y:barY+23,size:3,color:blue});
    await draw("0%",MARGIN,barY-17,8);
    await draw(readablePercent(result.mediumPct)+"%",MARGIN+lowWidth-8,barY-17,8);
    await draw(readablePercent(result.highPct)+"%",MARGIN+lowWidth+midWidth-8,barY-17,8);
    y=barY-34;
    await paragraph("Marker: saved impact. Green LOW < "+readablePercent(result.mediumPct)+"%; amber MEDIUM < "+readablePercent(result.highPct)+"%; red HIGH. Prototype bands, not regulatory limits.",8,regular,muted);

    const recoveryText=run.result?.recoveryPct ?? run.inputs?.recoveryPct;
    const severityText=run.result?.severityPct ?? run.inputs?.severityPct;
    const recovery=recoveryText==null?NaN:Number(recoveryText), savedSeverity=severityText==null?NaN:Number(severityText);
    const canPreview=run.modelVersion==="phase0-direct-loss-v1" && moneyCents(result.eadAmount)>0n && moneyCents(result.eligibleCapital)>0n && Number.isInteger(recovery) && recovery>=0 && recovery<=100 && Number.isFinite(savedSeverity) && savedSeverity>=0 && savedSeverity<=100;
    await paragraph("Scatter plot: severity vs capital impact",12,bold,blue);
    if(canPreview) {
      await paragraph("Recovery held at "+readablePercent(recoveryText!)+"%. Grey dots are model previews; the blue dot is the saved result. These are not observed losses or additional saved runs.",8,regular,muted);
      const points=Array.from({length:11},(_,i)=>stressPreview(result.eadAmount,result.eligibleCapital,i*10,recovery,result.mediumPct,result.highPct));
      const largest=points.reduce((value,p)=>{const n=percentUnits(p.impact);return n>value?n:value;},maximum);
      const step=5000000n, top=((largest+step-1n)/step+1n)*step;
      const plotX=MARGIN+41, plotWidth=CONTENT-55, plotTop=y-18, plotHeight=174, plotBottom=plotTop-plotHeight;
      await ensure(236);
      for(let i=0;i<=5;i++) {
        const tick=top*BigInt(i)/5n, py=plotBottom+plotHeight*i/5;
        page.drawRectangle({x:plotX,y:py,width:plotWidth,height:0.5,color:rgb(0.88,0.91,0.94)});
        const label=readablePercent((tick/1000000n)+"."+(tick%1000000n).toString().padStart(6,"0"))+"%";
        await draw(label,MARGIN,py-3,7);
      }
      page.drawRectangle({x:plotX,y:plotBottom,width:1,height:plotHeight,color:muted});
      page.drawRectangle({x:plotX,y:plotBottom,width:plotWidth,height:1,color:muted});
      for(let severity=0;severity<=100;severity+=20)await draw(String(severity),plotX+plotWidth*severity/100-4,plotBottom-14,8);
      await draw("Capital impact (%)",plotX,plotTop+7,8,bold);
      await draw("Severity (%)",plotX+plotWidth/2-28,plotBottom-30,8,bold);
      for(const threshold of [{value:medium,label:"MEDIUM "+readablePercent(result.mediumPct)+"%",color:rgb(0.72,0.4,0.03)}, {value:high,label:"HIGH "+readablePercent(result.highPct)+"%",color:rgb(0.75,0.16,0.22)}]) {
        const py=plotBottom+plotHeight*geometry(threshold.value,top);
        for(let x=plotX;x<plotX+plotWidth;x+=9)page.drawRectangle({x,y:py,width:Math.min(5,plotX+plotWidth-x),height:0.7,color:threshold.color});
        await draw(threshold.label,plotX+plotWidth-measure(threshold.label,7,bold)-3,py+4,7,bold,threshold.color);
      }
      for(const point of points)page.drawCircle({x:plotX+plotWidth*point.severity/100,y:plotBottom+plotHeight*geometry(percentUnits(point.impact),top),size:3,color:rgb(0.44,0.49,0.55)});
      const px=plotX+plotWidth*savedSeverity/100, py=plotBottom+plotHeight*geometry(impact,top);
      page.drawCircle({x:px,y:py,size:6.5,color:rgb(1,1,1)});
      page.drawCircle({x:px,y:py,size:4.5,color:blue});
      const label="Saved: "+readablePercent(severityText!)+"% severity / "+readablePercent(result.capitalImpactPct)+"% impact";
      const lx=Math.max(plotX,Math.min(px+8,plotX+plotWidth-measure(label,7,bold)));
      await draw(label,lx,Math.min(py+13,plotTop-12),7,bold,blue);
      y=plotBottom-46;
    } else await paragraph("Sensitivity preview is unavailable for this run's model or fractional recovery setting. The saved result above remains unchanged.",9,regular,muted);
    await ensure(92);
    await paragraph("What the scatter plot tells you",12,bold,blue);
    await paragraph("Moving right means testing stronger stress. With the same exposure, capital and recovery, stronger severity raises the modelled capital impact.",9);
    await paragraph("The blue dot is the saved result. Grey dots only show what-if previews; they are not extra losses or completed assessments.",9);
  }

  async function visualSummary() {
    const grouped=new Map<string,bigint>();
    const complete=snapshot.rows.every(row=>row.eadAmount!=null);
    let total=0n, outstanding=0n;
    for (const loan of snapshot.rows) {
      outstanding+=moneyCents(loan.outstandingAmount);
      if(loan.eadAmount!=null) {
        const value=moneyCents(loan.eadAmount);total+=value;
        grouped.set(loan.nbfcId,(grouped.get(loan.nbfcId)||0n)+value);
      }
    }
    const metrics=complete?exposureMetrics(moneyString(total),[...grouped].map(([nbfcId,value])=>({nbfcId,eadAmount:moneyString(value)}))):null;
    const completed=scoped.filter(run=>run.status==="COMPLETED"&&run.result).sort((a,b)=>b.createdAt.localeCompare(a.createdAt));
    const nbfcs=new Set(snapshot.rows.map(loan=>loan.nbfcId));
    await heading("Visual portfolio overview");
    const cards=[
      {label:"Total explicit EAD",value:complete?crore(total):"Incomplete EAD"},
      {label:"Reported outstanding",value:crore(outstanding)},
      {label:"NBFCs / loans",value:nbfcs.size+" / "+snapshot.rows.length}
    ];
    await ensure(78);
    for(let i=0;i<cards.length;i++) {
      const x=MARGIN+i*(CONTENT/3+3);
      page.drawRectangle({x,y:y-55,width:CONTENT/3-6,height:62,color:pale});
      await draw(cards[i].label,x+9,y-10,8,regular,muted);
      const size=Math.min(13, (CONTENT/3-24)/Math.max(1,measure(cards[i].value,1,bold)));
      await draw(cards[i].value,x+9,y-32,size,bold,blue);
    }
    y-=78;
    await paragraph("The bars and doughnut show how the saved portfolio is spread across NBFCs. Larger bars and slices mean more exposure. Labels use INR crore; exact rupee values are in Loan details.",9,regular,muted);
    await heading("Exposure by NBFC (Top 10)");
    if(!metrics || total<=0n) {
      await paragraph(complete?"No positive EAD to chart.":"Exposure charts are unavailable because explicit EAD is missing. Missing EAD is never treated as zero.");
    } else {
      const ranked=[...grouped].sort(([a,x],[b,z])=>x===z?a.localeCompare(b):x>z?-1:1);
      await paragraph("Bar length: explicit EAD, scaled to the largest shown exposure. Units: INR crore. Each NBFC appears once.",9,regular,muted);
      await barRows(ranked.slice(0,10).map(([label,value])=>({label,amount:value,display:crore(value),color:rgb(0.204,0.525,0.961)})),ranked[0][1]);
      if(ranked.length>10)await paragraph("Showing the 10 largest of "+ranked.length+" NBFCs. All loans remain in the detailed tables.",8,regular,muted);
      await newPage();
      await heading("Exposure composition");
      await paragraph("Five largest NBFCs plus Others, matching the website doughnut. Shares use the full portfolio EAD denominator.",9,regular,muted);
      const top=y, cx=MARGIN+99, cy=top-100, radius=86;
      let start=-Math.PI/2;
      const positive=metrics.slices.filter(slice=>slice.amount>0n);
      for(let i=0;i<metrics.slices.length;i++) {
        const slice=metrics.slices[i];if(slice.amount<=0n)continue;
        const angle=geometry(slice.amount,total)*2*Math.PI;
        if(positive.length===1) page.drawCircle({x:cx,y:cy,size:radius,color:chartColors[i]});
        else {
          const finish=slice===positive[positive.length-1]?3*Math.PI/2:start+angle;
          const sx=radius*Math.cos(start),sy=radius*Math.sin(start),ex=radius*Math.cos(finish),ey=radius*Math.sin(finish);
          const path=`M 0 0 L ${sx} ${sy} A ${radius} ${radius} 0 ${finish-start>Math.PI?1:0} 1 ${ex} ${ey} Z`;
          page.drawSvgPath(path,{x:cx,y:cy,color:chartColors[i]});
          start=finish;
        }
      }
      page.drawCircle({x:cx,y:cy,size:54,color:rgb(1,1,1)});
      const totalLabel=crore(total);
      const size=Math.min(11,94/Math.max(1,measure(totalLabel,1,bold)));
      await draw("Total EAD",cx-23,cy+9,8,regular,muted);
      await draw(totalLabel,cx-measure(totalLabel,size,bold)/2,cy-9,size,bold,blue);
      let legendY=top-16;
      for(let i=0;i<metrics.slices.length;i++) {
        const slice=metrics.slices[i];
        page.drawRectangle({x:MARGIN+220,y:legendY-2,width:8,height:8,color:chartColors[i]});
        const label=wrap(slice.name,245,9,bold);
        for(const line of label){await draw(line,MARGIN+235,legendY,9,bold);legendY-=12;}
        await draw(crore(slice.amount)+" | "+shareLabel(slice.amount,total),MARGIN+235,legendY,8);
        legendY-=22;
      }
      y=Math.min(top-210,legendY-12);
      const others=metrics.slices.find(slice=>slice.members?.length);
      if(others?.members) {
        await paragraph("Others includes "+others.members.length+" NBFCs. Each share below uses the full portfolio total:",8,regular,muted);
        for(const member of others.members)await paragraph(member.name+": "+crore(member.amount)+" | "+shareLabel(member.amount,total),8);
      }
    }
    await ensure(100);
    await heading("Saved scenario capital impact");
    await paragraph("The approved scenario included in this report. Other saved scenarios are not included; alternative losses must not be added together.",9,regular,muted);
    const latest=completed.slice(0,6);
    if(!latest.length) await paragraph("No completed saved scenario results are available to chart.");
    else {
      const largest=latest.reduce((max,run)=>{const value=percentUnits(run.result!.capitalImpactPct);return value>max?value:max;},0n);
      const scaleLabel=(largest/1000000n)+"."+(largest%1000000n).toString().padStart(6,"0");
      await paragraph("Bar scale: 0% to "+scaleLabel+"% (largest impact shown). Bar colour uses each saved run's prototype alert.",8,regular,muted);
      await barRows(latest.map(run=>({label:scenarioName(run),amount:percentUnits(run.result!.capitalImpactPct),display:run.result!.capitalImpactPct+"% / "+run.result!.alertSeverity,
        color:run.result!.alertSeverity==="HIGH"?rgb(0.76,0.15,0.22):run.result!.alertSeverity==="MEDIUM"?rgb(0.8,0.43,0.06):rgb(0.059,0.463,0.431)})),largest);
      await paragraph("Alert colours: green LOW, amber MEDIUM, red HIGH. The exact prototype threshold version is recorded with each scenario below.",8,regular,muted);
    }
    const basis=completed.find(run=>run.modelVersion==="phase0-direct-loss-v1" && run.result && moneyCents(run.result.eadAmount)>0n && moneyCents(run.result.eligibleCapital)>0n);
    if(basis && complete && snapshot.status==="APPROVED") {
      await newPage();
      await heading("Severity-recovery heatmap");
      await paragraph("Preview calculated with the website's existing model using the latest eligible saved scenario's EAD, capital and thresholds. These cells are not additional saved scenarios or published alerts.",9,regular,muted);
      await field("Saved scenario",scenarioName(basis));
      const result=basis.result!;
      await field("EAD / eligible capital (INR)",result.eadAmount+" / "+result.eligibleCapital);
      await field("Capital definition",result.capitalLabel);
      await field("Saved alert policy","MEDIUM from "+result.mediumPct+"%; HIGH from "+result.highPct+"%");
      await paragraph("MEDIUM begins at "+result.mediumPct+"%; HIGH begins at "+result.highPct+"%. Prototype settings.",9);
      const steps=[0,20,40,60,80,100], cellWidth=(CONTENT-64)/6, rowHeight=43;
      await ensure(350);
      await draw("Recovery ->",MARGIN+65,y,9,bold);
      y-=22;
      await draw("Severity",MARGIN,y,8,bold);
      for(let col=0;col<6;col++)await draw(steps[col]+"%",MARGIN+64+col*cellWidth+25,y,8,bold);
      y-=15;
      for(const severity of steps) {
        await draw(severity+"%",MARGIN+6,y-22,9,bold);
        for(let col=0;col<6;col++) {
          const cell=stressPreview(result.eadAmount,result.eligibleCapital,severity,steps[col],result.mediumPct,result.highPct);
          const x=MARGIN+64+col*cellWidth;
          const color=cell.alert==="HIGH"?rgb(1,0.87,0.89):cell.alert==="MEDIUM"?rgb(1,0.94,0.76):rgb(0.87,0.96,0.93);
          page.drawRectangle({x,y:y-rowHeight+2,width:cellWidth-2,height:rowHeight-2,color});
          const label=cell.impact+"%";
          await draw(label,x+(cellWidth-2-measure(label,7,bold))/2,y-16,7,bold);
          await draw(cell.alert,x+(cellWidth-2-measure(cell.alert,7,regular))/2,y-30,7);
        }
        y-=rowHeight;
      }
      y-=12;
      await paragraph("How to read this grid: moving down applies stronger stress; moving right assumes more recovery. Each cell shows the resulting modelled capital impact. These are previews, not extra saved losses.",9,regular,muted);
    }
  }

  async function loanTable() {
    const widths = [70, 70, 88, 88, 88, 30, CONTENT - 434];
    const labels = ["NBFC", "Loan", "Amount lent INR", "Outstanding INR", "Explicit EAD INR", "DPD", "Status"];
    const size = 7.5, leading = 11;
    async function cells(values: string[], header = false) {
      const lines = values.map((value, i) => wrap(value, widths[i] - 10, size, header ? bold : regular));
      let start = 0;
      const count = Math.max(...lines.map(value => value.length));
      while (start < count) {
        if (y - (leading + 10) < BOTTOM) { await newPage(); if (!header) await cells(labels, true); }
        const visible = Math.min(count - start, Math.max(1, Math.floor((y - BOTTOM - 10) / leading)));
        const height = visible * leading + 10;
        page.drawRectangle({ x: MARGIN, y: y - height, width: CONTENT, height, color: header ? pale : rgb(0.98, 0.985, 0.99) });
        let x = MARGIN;
        for (let i = 0; i < values.length; i++) {
          for (let j = 0; j < visible; j++) await draw(lines[i][start + j] || "", x + 5, y - 10 - j * leading, size, header ? bold : regular);
          x += widths[i];
        }
        y -= height + 2;
        start += visible;
      }
    }
    await ensure(80);
    await cells(labels, true);
    for (const loan of snapshot.rows) {
      if (y - 36 < BOTTOM) { await newPage(); await cells(labels, true); }
      await cells([loan.nbfcId, loan.loanId, loan.amountLent, loan.outstandingAmount, loan.eadAmount ?? "Not supplied", String(loan.dpdDays), loan.loanStatus]);
    }
    if (!snapshot.rows.length) await paragraph("No loans recorded.");
  }

  const reviewFor=(run:AnalysisScenario)=>scopedReview(review,run.id,snapshot.id);
  const latestAssessment=(run:AnalysisScenario)=>reviewFor(run)?.assessments.slice().sort((a,b)=>Date.parse(b.submittedAt)-Date.parse(a.submittedAt)||b.id.localeCompare(a.id))[0];
  function reviewDate(value:string|null|undefined) {
    if(!value)return "Date not recorded";
    return new Intl.DateTimeFormat("en-IN",{timeZone:"Asia/Kolkata",dateStyle:"medium",timeStyle:"short"}).format(new Date(value))+" IST";
  }
  function decisionLabel(status:string|undefined) {
    return status==="APPROVED"?"Approved":status==="RETURNED"?"Returned for correction":status==="SUBMITTED"?"Awaiting review":"Not reviewed";
  }
  async function reviewHistory(run:AnalysisScenario) {
    await newPage();
    await paragraph("Analyst submission & Supervisor review",19,bold,blue);
    await paragraph("Saved review information for "+run.nbfcId+" and this calculation. The status cards mirror the workflow shown on the website.",9,regular,muted);
    const history=reviewFor(run), latest=latestAssessment(run);
    const stages=[
      {label:"1. Scenario calculation",value:run.status==="COMPLETED"?"Completed":run.status,done:run.status==="COMPLETED"},
      {label:"2. Analyst submission",value:history?(history.assessments.length?"Submitted":"Not submitted"):"Not included",done:!!history?.assessments.length},
      {label:"3. Supervisor decision",value:history?decisionLabel(latest?.status):"Not included",done:latest?.status==="APPROVED"}
    ];
    await ensure(80);
    for(let i=0;i<stages.length;i++) {
      const x=MARGIN+i*(CONTENT/3+3), width=CONTENT/3-6;
      page.drawRectangle({x,y:y-53,width,height:61,color:stages[i].done?rgb(0.87,0.96,0.93):pale});
      await draw(stages[i].label,x+8,y-8,8,regular,muted);
      const size=Math.min(13,(width-16)/Math.max(1,measure(stages[i].value,1,bold)));
      await draw(stages[i].value,x+8,y-32,size,bold,blue);
    }
    y-=78;
    await paragraph("The analyst saved the calculation on "+reviewDate(run.createdAt)+".",9);
    if(!history) {
      await heading("Review details are not included");
      await paragraph(review.state==="restricted"
        ? "These records are restricted to the Supervisor role. Download the report while signed in as Supervisor to include the submission, decision and follow-up history."
        : "The review records could not be loaded for this download. Check the Supervisor workspace and download again. This does not mean the assessment is unsubmitted or approved.",10);
      return;
    }
    if(review.state==="loaded")await paragraph("Review status checked on "+reviewDate(review.fetchedAt)+". A later decision requires a new download.",8,regular,muted);
    if(!history.assessments.length) {
      await heading("The result has not been submitted yet");
      await paragraph("No assessment submission is recorded for this scenario. The analyst can add an assessment note and submit the saved calculation to the Supervisor.");
      return;
    }
    if(review.state==="loaded" && review.inputReviews?.length){
      await heading("Supervisor checks before analysis");
      await paragraph("The contributor saved the data for review. The following decisions were recorded before the analyst used these inputs.",9);
      for(const event of review.inputReviews){
        await paragraph(event.name+" - "+event.decision+" on "+reviewDate(event.reviewedAt),9,bold);
        await paragraph("Review note: "+event.reviewNote,9);
      }
    }
    for(const item of history.assessments) {
      await heading(scenarioName(run)+" - Assessment: "+decisionLabel(item.status));
      await paragraph("The analyst submitted this scenario for review on "+reviewDate(item.submittedAt)+".");
      await field("Analyst's submission note",item.submissionNote || "No note recorded");
      if(item.status==="APPROVED")await paragraph("The Supervisor approved this assessment on "+reviewDate(item.decidedAt)+".",10,bold);
      else if(item.status==="RETURNED")await paragraph("The Supervisor returned this assessment for correction on "+reviewDate(item.decidedAt)+".",10,bold);
      else await paragraph("The assessment is waiting for a Supervisor decision.",10,bold);
      if(item.decisionNote)await field("Supervisor's reason",item.decisionNote);
    }
    await heading("Follow-up work shown on the website");
    if(!history.followUps.length)await paragraph("No follow-up actions are recorded for these assessments. Approval and follow-up closure are separate steps.",9);
    for(const action of history.followUps) {
      await paragraph(action.title+" - "+action.status.replaceAll("_"," "),11,bold);
      await paragraph("Owner: "+action.owner+" | Due date: "+action.dueDate,9);
      if(action.evidence)await field("Recorded evidence",action.evidence);
      if(action.status==="CLOSED")await paragraph("This follow-up is recorded as closed.",9);
      else if(action.status==="VERIFIED")await paragraph("Evidence has been verified; the action is not yet recorded as closed.",9);
      else if(action.status==="EVIDENCE_SUBMITTED")await paragraph("Evidence has been submitted and is waiting for verification.",9);
      else await paragraph("The action is recorded as "+action.status.toLowerCase().replaceAll("_"," ")+". Check its next step in the Supervisor workspace.",9);
    }
    if(history.events.length) {
      await heading("Recorded sequence of events");
      const labels:Record<string,string>={SCENARIO_SUBMITTED:"Analyst submitted the scenario",SUBMITTED:"Assessment submitted",APPROVED:"Supervisor approved the assessment",RETURNED:"Supervisor returned the assessment",CREATED:"Follow-up action created",IN_PROGRESS:"Follow-up work started",EVIDENCE_SUBMITTED:"Follow-up evidence submitted",VERIFIED:"Follow-up evidence verified",REOPENED:"Follow-up reopened",CLOSED:"Follow-up closed",COMPLETED:"Follow-up marked complete",OPEN:"Follow-up opened"};
      for(const event of history.events.slice().sort((a,b)=>Date.parse(a.createdAt)-Date.parse(b.createdAt)||a.id.localeCompare(b.id))) {
        await paragraph(reviewDate(event.createdAt)+" - "+(labels[event.action] || event.action.replaceAll("_"," ")),9);
        if((event.action==="APPROVED"||event.action==="RETURNED")&&event.details)await paragraph("Reason: "+event.details,9,regular,muted);
      }
    }
    await paragraph("Supervisor approval records a review decision. It is separate from the model's LOW, MEDIUM or HIGH alert band.",8,regular,muted);
  }

  async function explainRun(run: AnalysisScenario) {
    const result=run.status==="COMPLETED"?run.result:null;
    await heading(scenarioName(run) + (result ? ": calculation completed" : ": no completed result"));
    if(!result) {
      await paragraph(run.failureReason || "This run has no completed calculation result.");
      await paragraph("Check the reported issue, correct the inputs and run the calculation again. No final loss or alert is available for this run.");
      return;
    }
    const severity=run.result?.severityPct ?? run.inputs?.severityPct;
    const recovery=run.result?.recoveryPct ?? run.inputs?.recoveryPct;
    await paragraph("What happened?",12,bold,blue);
    await paragraph("This what-if test uses INR "+crore(moneyCents(result.eadAmount))+" of exposure to "+run.nbfcId+" and INR "+crore(moneyCents(result.eligibleCapital))+" of eligible bank capital.");
    if(severity!=null && recovery!=null)await paragraph("With "+readablePercent(severity)+"% stress severity and "+readablePercent(recovery)+"% recovery of the stressed portion, the model calculates a loss of INR "+crore(moneyCents(result.stressedLoss))+". That is "+readablePercent(result.capitalImpactPct)+"% of the selected bank capital.");
    else await paragraph("The saved calculated loss is INR "+crore(moneyCents(result.stressedLoss))+", or "+readablePercent(result.capitalImpactPct)+"% of the selected bank capital. Some assumption details were not recorded.");
    await paragraph("What does the alert mean?",12,bold,blue);
    const impact=percentUnits(result.capitalImpactPct), medium=percentUnits(result.mediumPct), high=percentUnits(result.highPct);
    const reason=impact<medium
      ? "The impact is below the saved MEDIUM threshold of "+readablePercent(result.mediumPct)+"%."
      : impact<high ? "The impact has reached the saved MEDIUM threshold of "+readablePercent(result.mediumPct)+"%, but is below HIGH at "+readablePercent(result.highPct)+"%."
      : "The impact has reached the saved HIGH threshold of "+readablePercent(result.highPct)+"%.";
    await paragraph("The saved alert is "+result.alertSeverity+". "+reason+" These are the prototype's selected settings, not RBI limits. LOW does not mean there is no risk.");
    await paragraph("COMPLETED means the calculation finished. It does not mean an actual loss occurred or that a Supervisor has approved the assessment.",9,regular,muted);
    await paragraph("What should the team do next?",12,bold,blue);
    const current=latestAssessment(run), history=reviewFor(run);
    if(current?.status==="APPROVED") {
      await paragraph("1. Supervisor approval is already recorded. Keep the approved assessment and its reason with this report.",9);
      await paragraph("2. Complete and verify any assigned follow-up work; approval alone does not close those actions.",9);
      await paragraph("3. Continue monitoring. If inputs or assumptions change, create a new scenario and review its new result.",9);
    } else if(current?.status==="RETURNED") {
      await paragraph("1. Read the Supervisor's return reason and correct the requested issues.",9);
      await paragraph("2. Explain the revised assumptions and resubmit for review. Create a new scenario if calculation inputs change.",9);
      await paragraph("3. The assessment is returned, not approved. Track any follow-up work and supporting evidence.",9);
    } else if(current?.status==="SUBMITTED") {
      await paragraph("1. The analyst has already submitted the assessment. It is waiting for Supervisor review.",9);
      await paragraph("2. The Supervisor checks the saved inputs and submission note, then approves or returns it with a reason.",9);
      await paragraph("3. Track any resulting follow-up actions and rerun stress when inputs change.",9);
    } else {
      await paragraph("1. Analyst: check that the exposure, capital and asset-quality inputs use the same reporting date. Explain why the severity and recovery assumptions were chosen.",9);
      await paragraph(history ? "2. Submit the assessment for Supervisor review. The Supervisor checks the saved result and approves or returns the assessment with a reason." : "2. Confirm the current assessment status in the Supervisor workspace; review records are not included in this download.",9);
      await paragraph("3. If follow-up is needed, record the action, owner and due date. Attach evidence, verify the work before closure, and rerun stress when inputs change.",9);
    }
  }
  async function dataChecks() {
    await paragraph("Data checks",12,bold,blue);
    if(!snapshot.warnings.length) {
      await paragraph("No data warnings were recorded for this saved portfolio.",9);
    } else {
      await paragraph("The saved portfolio has "+snapshot.warnings.length+" data warning(s). These can flag missing exposure inputs or differences between reported loan amounts. Review them before relying on the results.",9);
      for(const warning of snapshot.warnings)await paragraph("Row "+warning.line+": "+warning.message,9);
    }
  }

  const completedRuns=scoped.filter(run=>run.status==="COMPLETED" && run.result).sort((a,b)=>b.createdAt.localeCompare(a.createdAt));
  const mainRun=completedRuns[0] || [...scoped].sort((a,b)=>b.createdAt.localeCompare(a.createdAt))[0];
  await newPage();
  await paragraph("Stress result and next steps",23,bold,blue);
  if(snapshot.name)await paragraph(snapshot.name,12,bold);
  await paragraph(snapshot.bankId+" | Reporting date: "+snapshot.reportingDate+" | Synthetic data",9,regular,muted);
  if(mainRun)await explainRun(mainRun);
  else {
    await heading("No completed scenario to explain");
    await paragraph("No saved scenarios loaded for this portfolio. Save a completed stress scenario to include its result and recommended review steps.");
  }
  await dataChecks();
  if(mainRun) await reviewHistory(mainRun);
  if(mainRun?.status==="COMPLETED" && mainRun.result)await scenarioInsight(mainRun);
  await newPage();
  await paragraph("Supporting visualizations",20,bold,blue);
  await paragraph("Start with the result and actions above. The charts below help explain where exposure sits and how the model behaves under different assumptions.",9,regular,muted);
  await visualSummary();
  await newPage();
  await heading("Loan details - supporting information");
  await paragraph("Exact INR rupee amounts from the selected saved portfolio. Missing EAD is shown as Not supplied and is not treated as zero.",9,regular,muted);
  await loanTable();
  await newPage();
  await heading("Report references - for review");
  await paragraph("These references let a reviewer find the exact saved inputs used for the calculation. The full technical record is available separately through Download evidence JSON; it is not repeated in this report.",9,regular,muted);
  await field("Portfolio reference",snapshot.id);
  await field("Report generated (UTC)",new Date().toISOString());
  for(const run of scoped) {
    await heading(run.nbfcId+" / "+run.status);
    if(run.id!==mainRun?.id) {
      if(run.status==="COMPLETED" && run.result)await paragraph("Separate saved scenario: loss INR "+crore(moneyCents(run.result.stressedLoss))+"; capital impact "+readablePercent(run.result.capitalImpactPct)+"%; alert "+run.result.alertSeverity+". Do not add this loss to another scenario.",9);
      else await paragraph(run.failureReason || "No completed result recorded.",9);
    }
    await field("Calculation reference",run.id);
    await field("Saved (UTC)",run.createdAt);
    await field("Model version",run.modelVersion);
    await field("Capital reference",run.inputs?.capitalId);
    await field("Asset-quality reference",run.inputs?.qualityId);
    await field("Alert-settings reference",run.result?.thresholdVersion ?? run.inputs?.thresholdId);
    await field("Saved analyst note",run.inputs?.rationale);
    if(run.result) {
      await paragraph("Exact saved INR values - EAD: "+run.result.eadAmount+"; eligible capital: "+run.result.eligibleCapital+"; stressed loss: "+run.result.stressedLoss+".",8,regular,muted);
      await paragraph("Exact capital impact: "+run.result.capitalImpactPct+"%. EAD / eligible capital: "+run.result.concentrationCapitalPct+"%. The EAD / capital ratio is not portfolio share.",8,regular,muted);
    }
  }
  if(mainRun && reviewFor(mainRun)) {
    const history=reviewFor(mainRun)!;
    await heading("Assessment references");
    for(const item of history.assessments) {
      await field("Assessment reference",item.id);
      await field("Submitted by account",item.submittedBy);
      if(item.decidedBy)await field("Decision recorded by account",item.decidedBy);
    }
  }
  if(!scoped.length)await paragraph("No calculation references are available for this portfolio.",9);
  const pages=pdf.getPages();
  for(let i=0;i<pages.length;i++) {
    page=pages[i];
    await draw("Synthetic what-if report | "+snapshot.reportingDate,MARGIN,32,8,regular,muted);
    await draw(`Page ${i+1} of ${pages.length}`,WIDTH-MARGIN-82,32,8,regular,muted);
  }
  return pdf.save();
}
