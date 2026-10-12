export type DemoRole = "CONTRIBUTOR" | "ANALYST" | "SUPERVISOR";
const key = "workbench-demo-role";
const roles: DemoRole[] = ["CONTRIBUTOR", "ANALYST", "SUPERVISOR"];
function currentRole(): DemoRole | null {
  const value = sessionStorage.getItem(key);
  return roles.includes(value as DemoRole) ? value as DemoRole : null;
}
export const auth = {
  get authenticated() { return currentRole() !== null; },
  get role() { return currentRole(); },
  hasRealmRole(role: string) { return currentRole() === role; },
  init(_options?: unknown) { return Promise.resolve(this.authenticated); },
  login() { sessionStorage.setItem(key, "CONTRIBUTOR"); location.reload(); },
  logout(_options?: unknown) { sessionStorage.removeItem(key); location.reload(); },
  setRole(role: DemoRole) { sessionStorage.setItem(key, role); location.reload(); },
};
export async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const role = currentRole();
  if (!role) throw { message: "Choose a demo role first." };
  const headers = new Headers(options?.headers);
  headers.set("X-Demo-Role", role);
  const response = await fetch(url, { ...options, headers });
  const body = await response.json().catch(() => ({ message: "The service could not complete your request." }));
  if (!response.ok) throw body;
  return body as T;
}
