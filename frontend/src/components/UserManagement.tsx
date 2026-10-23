import { useRef, useState, type FormEvent } from "react";

import { api } from "../auth";
import {
  sessionAuth,
  type WorkbenchRole,
} from "../sessionAuth";

type CreatedUser = {
  id: string;
  username: string;
  role: WorkbenchRole;
  status: string;
  createdBy: string;
  createdAt: string;
};

const roles: WorkbenchRole[] = [
  "CONTRIBUTOR",
  "ANALYST",
  "SUPERVISOR",
];

const roleNames: Record<WorkbenchRole, string> = {
  CONTRIBUTOR: "Contributor",
  ANALYST: "Analyst",
  SUPERVISOR: "Supervisor",
};

const inputClass =
  "mt-2 block w-full rounded-lg border border-slate-300 bg-white px-3 py-3";

function creationError(error: unknown): string {
  if (
    error &&
    typeof error === "object" &&
    "httpStatus" in error
  ) {
    const status = error.httpStatus;

    if (
      (status === 400 || status === 409) &&
      "message" in error &&
      typeof error.message === "string" &&
      error.message.trim()
    ) {
      return error.message;
    }

    if (status === 401) {
      return "Your session has expired. Please sign in again.";
    }

    if (status === 403) {
      return "The request was not permitted. Refresh the page before trying again.";
    }
  }

  return (
    "Could not confirm account creation. " +
    "Keep the same username if you retry."
  );
}

export function UserManagement() {
  const [username, setUsername] = useState("");
  const [initialPassword, setInitialPassword] = useState("");
  const [role, setRole] = useState<WorkbenchRole>("CONTRIBUTOR");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [createdUser, setCreatedUser] =
    useState<CreatedUser | null>(null);

  const inFlight = useRef(false);

  async function createUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (inFlight.current) return;

    setError("");
    setCreatedUser(null);

    const normalizedUsername = username.trim().toLowerCase();

    if (!/^[a-z][a-z0-9._-]{2,63}$/.test(normalizedUsername)) {
      setError(
        "Username must contain 3 to 64 characters, start with " +
          "a letter, and use letters, digits, dots, underscores " +
          "or hyphens."
      );
      return;
    }

    if (
      !initialPassword.trim() ||
      [...initialPassword].length < 12
    ) {
      setError(
        "Initial password must contain at least 12 characters."
      );
      return;
    }

    if (
      new TextEncoder().encode(initialPassword).length > 72
    ) {
      setError(
        "The initial password exceeds the length limit. " +
          "Please use a shorter password."
      );
      return;
    }

    inFlight.current = true;
    setBusy(true);
    setUsername(normalizedUsername);

    try {
      const saved = await api<CreatedUser>("/api/auth/users", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: normalizedUsername,
          password: initialPassword,
          role,
        }),
      });

      setCreatedUser(saved);
      setUsername("");
      setInitialPassword("");
      setRole("CONTRIBUTOR");
    } catch (failure) {
      setError(creationError(failure));
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  if (!sessionAuth.hasRealmRole("SUPERVISOR")) {
    return (
      <p role="alert">
        User management is available to Supervisors.
      </p>
    );
  }

  return (
    <section
      aria-labelledby="create-user-heading"
      className="rounded-xl border border-slate-200 bg-white p-6"
    >
      <h2
        id="create-user-heading"
        className="text-xl font-bold text-slate-900"
      >
        Create user
      </h2>

      <p className="mt-2 text-sm text-slate-600">
        Provide a username, initial password and assigned role.
        The new user can sign in with these credentials.
      </p>

      {error && (
        <p
          role="alert"
          className="mt-5 rounded-lg bg-red-50 p-4 text-red-800"
        >
          {error}
        </p>
      )}

      {createdUser && (
        <div
          role="status"
          className="mt-5 rounded-lg bg-teal-50 p-4 text-teal-900"
        >
          <p className="font-semibold">
            User created successfully.
          </p>

          <p className="mt-2">
            <strong>{createdUser.username}</strong> can sign in
            through{" "}
            <strong>
              {roleNames[createdUser.role]} Login
            </strong>.
          </p>
        </div>
      )}

      <form
        onSubmit={createUser}
        autoComplete="off"
        aria-busy={busy}
        className="mt-6 max-w-xl"
      >
        <fieldset disabled={busy} className="space-y-5">
          <legend className="sr-only">
            New account details
          </legend>

          <div>
            <label
              htmlFor="new-account-username"
              className="block font-medium text-slate-700"
            >
              Username
            </label>

            <input
              id="new-account-username"
              name="new-account-username"
              autoComplete="off"
              autoCapitalize="none"
              spellCheck={false}
              value={username}
              onChange={(event) =>
                setUsername(event.target.value)
              }
              required
              maxLength={64}
              aria-describedby="new-account-username-help"
              className={inputClass}
            />

            <p
              id="new-account-username-help"
              className="mt-2 text-sm text-slate-500"
            >
              3–64 characters. Start with a letter.
              Usernames are saved in lowercase.
            </p>
          </div>

          <div>
            <label
              htmlFor="new-account-password"
              className="block font-medium text-slate-700"
            >
              Initial password
            </label>

            <input
              id="new-account-password"
              name="new-account-password"
              type="password"
              autoComplete="new-password"
              value={initialPassword}
              onChange={(event) =>
                setInitialPassword(event.target.value)
              }
              required
              minLength={12}
              aria-describedby="new-account-password-help"
              className={inputClass}
            />

            <p
              id="new-account-password-help"
              className="mt-2 text-sm text-slate-500"
            >
              Use at least 12 characters.
            </p>
          </div>

          <div>
            <label
              htmlFor="new-account-role"
              className="block font-medium text-slate-700"
            >
              Role
            </label>

            <select
              id="new-account-role"
              name="new-account-role"
              value={role}
              onChange={(event) =>
                setRole(event.target.value as WorkbenchRole)
              }
              required
              className={inputClass}
            >
              {roles.map((value) => (
                <option key={value} value={value}>
                  {roleNames[value]}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={busy}
            className="rounded-lg bg-teal-800 px-5 py-3 font-semibold text-white hover:bg-teal-900 disabled:opacity-50"
          >
            {busy ? "Creating user…" : "Create user"}
          </button>
        </fieldset>
      </form>
    </section>
  );
}