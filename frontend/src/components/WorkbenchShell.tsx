import {
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";

import { sessionAuth } from "../sessionAuth";
import { type WorkspaceView, viewTitles } from "../workspace";

export function Icon({
  name,
  size = 20,
}: {
  name: string;
  size?: number;
}) {
  const paths: Record<string, ReactNode> = {
    bank: <path d="m3 8 9-5 9 5H3ZM4 21h16M5 18V10m5 8V10m4 8V10m5 8V10M3 18h18" />,
    dashboard: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    chart: <path d="M4 20V4m0 16h17M8 16v-5m5 5V7m5 9V3" />,
    search: <><circle cx="10" cy="10" r="6" /><path d="m15 15 6 6M8 10h4m-2-2v4" /></>,
    play: <><circle cx="12" cy="12" r="9" /><path d="m10 8 6 4-6 4Z" /></>,
    alert: <path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v.1" />,
    file: <path d="M14 3H5v18h14V8l-5-5Zm0 0v5h5M8 12h8m-8 4h6" />,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    download: <path d="M12 3v12m-4-4 4 4 4-4M4 16v5h16v-5" />,
    sliders: <><path d="M4 7h16M4 17h16" /><circle cx="9" cy="7" r="3" /><circle cx="15" cy="17" r="3" /></>,
    book: <path d="M12 5C8 2 4 3 2 4v16c3-2 7-1 10 1 3-2 7-3 10-1V4c-3-1-7-2-10 1Zm0 0v16" />,
    menu: <path d="M4 6h16M4 12h16M4 18h16" />,
    chevron: <path d="m14 5-7 7 7 7" />,
    bell: <path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5l-2 3Zm5 3h4" />,
    refresh: <path d="M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5" />,
    users: (
      <>
        <circle cx="9" cy="8" r="3" />
        <path d="M3 21v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M21 21v-2a6 6 0 0 0-4-5.65" />
      </>
    ),
  };

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] || paths.file}
    </svg>
  );
}

type NavigationItem = {
  id: WorkspaceView;
  icon: string;
  label: string;
  group: string;
};

export function WorkbenchShell({
  view,
  navigate,
  children,
}: {
  view: WorkspaceView;
  navigate: (view: WorkspaceView) => void;
  children: ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [logoutBusy, setLogoutBusy] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  const profile = useSyncExternalStore(
    sessionAuth.subscribe,
    sessionAuth.getSnapshot,
  );

  if (!profile) {
    return null;
  }

  const contributor = profile.role === "CONTRIBUTOR";
  const supervisor = profile.role === "SUPERVISOR";

  const role = contributor
    ? "Data Contributor"
    : supervisor
      ? "Risk Supervisor"
      : "Risk Analyst";

  const description =
    view === "users"
      ? "Create accounts and assign Contributor, Analyst or Supervisor access."
      : contributor
        ? "Prepare synthetic portfolio data and share validated inputs with the risk team."
        : "Review exposures, compare evidence and assess stress risk for the selected portfolio.";

  const items: NavigationItem[] = contributor
    ? [
        {
          id: "data",
          icon: "file",
          label: "Data Management",
          group: "WORKSPACE",
        },
        {
          id: "reports",
          icon: "download",
          label: "Downloads",
          group: "DATA & REPORTS",
        },
      ]
    : [
        {
          id: "dashboard",
          icon: "dashboard",
          label: "Dashboard",
          group: "WORKSPACE",
        },
        {
          id: "exposure",
          icon: "chart",
          label: "Exposure Explorer",
          group: "ANALYSIS",
        },
        {
          id: "analysis",
          icon: "search",
          label: "NBFC Details",
          group: "ANALYSIS",
        },
        {
          id: "stress",
          icon: "play",
          label: "Stress Simulation",
          group: "ANALYSIS",
        },
        {
          id: "alerts",
          icon: "alert",
          label: "Risk Alerts",
          group: "ANALYSIS",
        },
        {
          id: "assessments",
          icon: "file",
          label: supervisor ? "Review & Follow-up" : "Assessments",
          group: "ANALYSIS",
        },
        {
          id: "history",
          icon: "clock",
          label: "Scenario History",
          group: "ANALYSIS",
        },
        {
          id: "reports",
          icon: "download",
          label: "Downloads & Reports",
          group: "DATA & REPORTS",
        },
      ];

  if (supervisor) {
    items.push(
      {
        id: "thresholds",
        icon: "sliders",
        label: "Alert Thresholds",
        group: "GOVERNANCE",
      },
      {
        id: "users",
        icon: "users",
        label: "User Management",
        group: "ADMINISTRATION",
      },
    );
  }

  items.push(
    {
      id: "guide",
      icon: "book",
      label: "User Guide",
      group: "WORKSPACE TOOLS",
    },
    {
      id: "all",
      icon: "sliders",
      label: "All tools",
      group: "WORKSPACE TOOLS",
    },
  );

  function go(next: WorkspaceView): void {
    navigate(next);
    setMobileOpen(false);
    window.scrollTo({ top: 0, behavior: "instant" });
  }

  async function handleLogout(): Promise<void> {
    if (logoutBusy) {
      return;
    }

    setLogoutBusy(true);
    setLogoutError("");

    try {
      await sessionAuth.logout();
    } catch (error) {
      setLogoutError(
        error instanceof Error
          ? error.message
          : "Logout could not be completed. Please try again.",
      );
    } finally {
      setLogoutBusy(false);
    }
  }

  return (
    <div
      className={
        "workbench-app " +
        (collapsed ? "sidebar-collapsed " : "") +
        (mobileOpen ? "mobile-menu-open" : "")
      }
    >
      <a className="skip-link" href="#workspace-main">
        Skip to content
      </a>

      {mobileOpen && (
        <button
          type="button"
          className="sidebar-scrim"
          aria-label="Close navigation"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside
        className="app-sidebar"
        aria-label="Workspace navigation"
      >
        <div className="sidebar-brand">
          <span className="brand-icon">
            <Icon name="bank" size={32} />
          </span>

          <div className="sidebar-label">
            <strong>NBFC Workbench</strong>
            <span>Exposure Stress & Alert</span>
          </div>
        </div>

        <nav aria-label="Main navigation">
          {items.map((item, index) => (
            <div key={item.id}>
              {(index === 0 ||
                items[index - 1].group !== item.group) && (
                <p className="nav-group sidebar-label">
                  {item.group}
                </p>
              )}

              <button
                type="button"
                title={item.label}
                aria-label={item.label}
                aria-current={
                  view === item.id ? "page" : undefined
                }
                onClick={() => go(item.id)}
                className={
                  "nav-item " +
                  (view === item.id ? "active" : "")
                }
              >
                <Icon name={item.icon} />

                <span className="sidebar-label">
                  {item.label}
                </span>
              </button>
            </div>
          ))}
        </nav>

        <button
          type="button"
          className="nav-item collapse-menu"
          aria-label={
            collapsed ? "Expand menu" : "Collapse menu"
          }
          onClick={() => setCollapsed(value => !value)}
        >
          <Icon name={collapsed ? "menu" : "chevron"} />
          <span className="sidebar-label">
            Collapse Menu
          </span>
        </button>
      </aside>

      <div className="workspace-frame">
        <header className="app-topbar">
          <button
            type="button"
            className="mobile-menu-button icon-button"
            aria-label="Open navigation"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen(value => !value)}
          >
            <Icon name="menu" />
          </button>

          <div className="topbar-location">
            <span>WORKSPACE</span>
            <strong>{viewTitles[view]}</strong>
          </div>

          <div className="topbar-actions">
            {!contributor && (
              <button
                type="button"
                className="icon-button"
                aria-label="Open risk alerts"
                title="Saved scenario alerts"
                onClick={() => go("alerts")}
              >
                <Icon name="bell" />
              </button>
            )}

            <span className="profile-avatar" aria-hidden="true">
              {contributor ? "DC" : supervisor ? "RS" : "RA"}
            </span>

            <div className="profile-role">
              <span
                title={profile.username}
                style={{
                  maxWidth: 160,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {profile.username}
              </span>

              <span
                style={{
                  color: "#617491",
                  fontWeight: 400,
                }}
              >
                {role}
              </span>
            </div>

            <button
              type="button"
              className="exit-button"
              disabled={logoutBusy}
              aria-busy={logoutBusy}
              onClick={() => void handleLogout()}
            >
              {logoutBusy ? "Logging out…" : "Logout"}
            </button>
          </div>
        </header>

        <main
          id="workspace-main"
          className="workspace-content"
        >
          <div className="page-heading">
            <div>
              <p className="eyebrow">
                {view === "users"
                  ? "ACCOUNT ADMINISTRATION"
                  : "EXPOSURE & RISK INTELLIGENCE"}
              </p>

              <h1>
                {view === "dashboard"
                  ? supervisor
                    ? "Risk Supervisor Dashboard"
                    : "Risk Analyst Dashboard"
                  : viewTitles[view]}
              </h1>

              <p>{description}</p>
            </div>

            <span className="demo-badge">
              Synthetic data only
            </span>
          </div>

          <p role="note" className="demo-notice">
            Use synthetic data only. Your account role controls
            available actions.
          </p>

          {logoutError && (
            <p
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900"
            >
              {logoutError}
            </p>
          )}

          {children}
        </main>
      </div>
    </div>
  );
}