import { useState, type ReactNode } from "react";
import { auth } from "../auth";
import { type WorkspaceView, viewTitles } from "../workspace";
export function Icon({ name, size = 20 }: { name: string; size?: number }) {
 const paths: Record<string, ReactNode> = {
 bank: <><path d="m3 8 9-5 9 5H3ZM4 21h16M5 18V10m5 8V10m4 8V10m5 8V10M3 18h18" /></>,
 dashboard: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
 chart: <><path d="M4 20V4m0 16h17M8 16v-5m5 5V7m5 9V3"/></>,
 search: <><circle cx="10" cy="10" r="6"/><path d="m15 15 6 6M8 10h4m-2-2v4"/></>,
 play: <><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4Z"/></>,
 alert: <><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v.1"/></>,
 file: <><path d="M14 3H5v18h14V8l-5-5Zm0 0v5h5M8 12h8m-8 4h6"/></>,
 clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
 download: <><path d="M12 3v12m-4-4 4 4 4-4M4 16v5h16v-5"/></>,
 sliders: <><path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="3"/><circle cx="15" cy="17" r="3"/></>,
 book: <><path d="M12 5C8 2 4 3 2 4v16c3-2 7-1 10 1 3-2 7-3 10-1V4c-3-1-7-2-10 1Zm0 0v16"/></>,
 menu: <path d="M4 6h16M4 12h16M4 18h16"/>,
 chevron: <path d="m14 5-7 7 7 7"/>,
 bell: <><path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5l-2 3Zm5 3h4"/></>,
 refresh: <><path d="M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5"/></>,
 };
 return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] || paths.file}</svg>;
}
export function WorkbenchShell({ view, navigate, children }: { view: WorkspaceView; navigate: (view: WorkspaceView) => void; children: ReactNode }) {
 const [collapsed, setCollapsed] = useState(false), [mobileOpen, setMobileOpen] = useState(false);
 const contributor = auth.hasRealmRole("CONTRIBUTOR"), supervisor = auth.hasRealmRole("SUPERVISOR");
 const role = contributor ? "Data Contributor" : supervisor ? "Risk Supervisor" : "Risk Analyst";
 const items: { id: WorkspaceView; icon: string; label: string; group: string }[] = contributor ? [
  { id:"data",icon:"file",label:"Data Management",group:"WORKSPACE" },{id:"reports",icon:"download",label:"Downloads",group:"DATA & REPORTS"},
 ] : [
  {id:"dashboard",icon:"dashboard",label:"Dashboard",group:"WORKSPACE"},
  {id:"exposure",icon:"chart",label:"Exposure Explorer",group:"ANALYSIS"},{id:"analysis",icon:"search",label:"NBFC Details",group:"ANALYSIS"},
  {id:"stress",icon:"play",label:"Stress Simulation",group:"ANALYSIS"},{id:"alerts",icon:"alert",label:"Risk Alerts",group:"ANALYSIS"},
  {id:"assessments",icon:"file",label:supervisor ? "Review & Follow-up" : "Assessments",group:"ANALYSIS"},{id:"history",icon:"clock",label:"Scenario History",group:"ANALYSIS"},
  {id:"reports",icon:"download",label:"Downloads & Reports",group:"DATA & REPORTS"},
  ...(supervisor ? [{id:"thresholds" as const,icon:"sliders",label:"Alert Thresholds",group:"GOVERNANCE"}] : []),
 ];
 items.push({id:"guide",icon:"book",label:"User Guide",group:"WORKSPACE TOOLS"},{id:"all",icon:"sliders",label:"All tools",group:"WORKSPACE TOOLS"});
 const go = (next: WorkspaceView) => { navigate(next); setMobileOpen(false); window.scrollTo({ top: 0, behavior: "instant" }); };
 return <div className={"workbench-app " + (collapsed ? "sidebar-collapsed " : "") + (mobileOpen ? "mobile-menu-open" : "")}>
  <a className="skip-link" href="#workspace-main">Skip to content</a>
  {mobileOpen && <button className="sidebar-scrim" aria-label="Close navigation" onClick={()=>setMobileOpen(false)} />}
  <aside className="app-sidebar" aria-label="Workspace navigation">
   <div className="sidebar-brand"><span className="brand-icon"><Icon name="bank" size={32}/></span><div className="sidebar-label"><strong>NBFC Workbench</strong><span>Exposure Stress & Alert</span></div></div>
   <nav aria-label="Main navigation">{items.map((item, i)=><div key={item.id}>{(i===0 || items[i-1].group!==item.group) && <p className="nav-group sidebar-label">{item.group}</p>}<button type="button" title={item.label} aria-label={item.label} aria-current={view===item.id ? "page" : undefined} onClick={()=>go(item.id)} className={"nav-item " + (view===item.id ? "active" : "")}><Icon name={item.icon}/><span className="sidebar-label">{item.label}</span></button></div>)}</nav>
   <button className="nav-item collapse-menu" aria-label={collapsed ? "Expand menu" : "Collapse menu"} onClick={()=>setCollapsed(!collapsed)}><Icon name={collapsed ? "menu" : "chevron"}/><span className="sidebar-label">Collapse Menu</span></button>
  </aside>
  <div className="workspace-frame">
   <header className="app-topbar">
    <button className="mobile-menu-button icon-button" aria-label="Open navigation" aria-expanded={mobileOpen} onClick={()=>setMobileOpen(!mobileOpen)}><Icon name="menu"/></button>
    <div className="topbar-location"><span>WORKSPACE</span><strong>{viewTitles[view]}</strong></div>
    <div className="topbar-actions">{!contributor && auth.authenticated && <button className="icon-button" aria-label="Open risk alerts" title="Saved scenario alerts" onClick={()=>go("alerts")}><Icon name="bell"/></button>}
     <span className="profile-avatar">{contributor ? "DC" : supervisor ? "RS" : "RA"}</span>
     {auth.authenticated && <label className="profile-role"><span>{role}</span><select aria-label="Demo role" value={auth.role ?? "CONTRIBUTOR"} onChange={e=>auth.setRole(e.target.value as "CONTRIBUTOR"|"ANALYST"|"SUPERVISOR")}><option value="CONTRIBUTOR">Contributor</option><option value="ANALYST">Analyst</option><option value="SUPERVISOR">Supervisor</option></select></label>}
     <button className="exit-button" onClick={()=>auth.authenticated ? void auth.logout() : void auth.login()}>{auth.authenticated ? "Exit demo" : "Enter demo"}</button>
    </div>
   </header>
   <main id="workspace-main" className="workspace-content">
    <div className="page-heading"><div><p className="eyebrow">EXPOSURE & RISK INTELLIGENCE</p><h1>{view==="dashboard" ? (supervisor ? "Risk Supervisor Dashboard" : "Risk Analyst Dashboard") : viewTitles[view]}</h1><p>{contributor ? "Prepare synthetic portfolio data and share validated inputs with the risk team." : "Review exposures, compare evidence and assess stress risk for the selected portfolio."}</p></div><span className="demo-badge">Synthetic data only</span></div>
    <p role="note" className="demo-notice">Local demo · Roles are selectable; production authentication and bank isolation are not enabled.</p>
    {children}
   </main>
  </div>
 </div>;
}
