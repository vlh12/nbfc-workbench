import App from "../App";
import PasswordChangeForm from "../components/PasswordChangeForm";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
  type FormEvent,
  type ReactNode,
} from "react";

import {
  sessionAuth,
  type LoginCode,
  type WorkbenchRole,
} from "../sessionAuth";

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

const roleDescriptions: Record<WorkbenchRole, string> = {
  CONTRIBUTOR: "Upload and validate portfolio inputs.",
  ANALYST: "Analyse exposure and run scenarios.",
  SUPERVISOR: "Review assessments, follow-up work and manage users.",
};

const inputClass =
  "mt-2 block w-full rounded-lg border border-slate-300 bg-white px-3 py-3";

const primaryButtonClass =
  "rounded-lg bg-teal-800 px-5 py-3 font-semibold text-white hover:bg-teal-900 disabled:opacity-50";

function errorMessage(error: unknown): string {
  if (error instanceof TypeError) {
    return "The workbench service could not be reached. Please try again.";
  }

  return error instanceof Error
    ? error.message
    : "The request could not be completed. Please try again.";
}

function Page({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <section className="w-full max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
        <p className="mb-6 text-sm font-bold uppercase tracking-wider text-teal-800">
          NBFC Exposure Stress & Alert Workbench
        </p>

        {children}
      </section>
    </main>
  );
}

function LoginPage({
  role,
  onBack,
}: {
  role: WorkbenchRole;
  onBack: () => void;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [challenge, setChallenge] = useState<LoginCode | null>(
    null,
  );

  const [enteredCode, setEnteredCode] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const inFlight = useRef(false);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;

    return () => {
      mounted.current = false;
    };
  }, []);

  function clearLoginAttempt(): void {
    setPassword("");
    setChallenge(null);
    setEnteredCode("");
  }

  async function submit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();

    if (inFlight.current) {
      return;
    }

    inFlight.current = true;
    setBusy(true);
    setError("");
    setNotice("");

    try {
      if (challenge === null) {
        const issued = await sessionAuth.beginLogin(
          role,
          username.trim(),
          password,
        );

        if (mounted.current) {
          setPassword("");
          setEnteredCode("");
          setChallenge(issued);
        }
      } else {
        await sessionAuth.verifyLogin(
          enteredCode.trim().toUpperCase(),
        );

        if (mounted.current) {
          clearLoginAttempt();
        }
      }
    } catch (failure) {
      if (!mounted.current) {
        return;
      }

      setError(errorMessage(failure));

      // Recover if login succeeded but its response was interrupted.
      try {
        await sessionAuth.init();
      } catch {
        // Keep the original request error visible.
      }
    } finally {
      inFlight.current = false;

      if (mounted.current) {
        setBusy(false);
      }
    }
  }

  async function back(): Promise<void> {
    if (inFlight.current) {
      return;
    }

    inFlight.current = true;
    setBusy(true);
    setError("");
    setNotice("");

    try {
      if (challenge !== null) {
        await sessionAuth.logout();
      }

      if (mounted.current) {
        clearLoginAttempt();
        onBack();
      }
    } catch (failure) {
      if (mounted.current) {
        setError(errorMessage(failure));
      }
    } finally {
      inFlight.current = false;

      if (mounted.current) {
        setBusy(false);
      }
    }
  }

  function openPasswordChange(): void {
    if (inFlight.current) {
      return;
    }

    clearLoginAttempt();
    setError("");
    setNotice("");
    setChangingPassword(true);
  }

  if (changingPassword) {
    return (
      <PasswordChangeForm
        role={role}
        onBack={() => {
          if (!mounted.current) {
            return;
          }

          clearLoginAttempt();
          setError("");
          setNotice("");
          setChangingPassword(false);
        }}
        onChanged={(verifiedUsername) => {
          if (!mounted.current) {
            return;
          }

          clearLoginAttempt();
          setUsername(verifiedUsername);
          setError("");

          setNotice(
            "Password changed. Sign in using your new password.",
          );

          setChangingPassword(false);
        }}
      />
    );
  }

  return (
    <>
      <button
        type="button"
        onClick={() => void back()}
        disabled={busy}
        className="mb-6 font-medium text-teal-800 underline disabled:opacity-50"
      >
        ← Back to Welcome
      </button>

      <h1 className="text-3xl font-bold text-slate-900">
        {roleNames[role]} Login
      </h1>

      <p className="mt-3 text-slate-600">
        Enter the credentials for your assigned account.
      </p>

      {notice && (
        <p
          role="status"
          className="mt-5 rounded-lg bg-green-50 p-4 text-green-800"
        >
          {notice}
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="mt-5 rounded-lg bg-red-50 p-4 text-red-800"
        >
          {error}
        </p>
      )}

      <form onSubmit={submit} className="mt-6 space-y-5">
        {challenge === null ? (
          <>
            <label className="block font-medium text-slate-700">
              Username

              <input
                name="username"
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
                value={username}
                onChange={(event) =>
                  setUsername(event.target.value)
                }
                required
                maxLength={64}
                disabled={busy}
                className={inputClass}
              />
            </label>

            <label className="block font-medium text-slate-700">
              Password

              <input
                name="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                required
                disabled={busy}
                className={inputClass}
              />
            </label>
          </>
        ) : (
          <>
            <p className="text-sm text-slate-600">
              Signing in as{" "}
              <strong className="text-slate-900">
                {username.trim()}
              </strong>
            </p>

            <div className="rounded-xl border border-teal-200 bg-teal-50 p-5">
              <p className="font-medium text-teal-900">
                Login verification code
              </p>

              <output
                aria-live="polite"
                className="mt-3 block font-mono text-3xl font-bold tracking-widest text-teal-950"
              >
                {challenge.code}
              </output>

              <p className="mt-3 text-sm text-teal-900">
                Type the code below. It is valid for{" "}
                {Math.ceil(challenge.expiresInSeconds / 60)}{" "}
                minutes from creation.
              </p>
            </div>

            <label className="block font-medium text-slate-700">
              Enter verification code

              <input
                name="verificationCode"
                autoComplete="off"
                autoFocus
                spellCheck={false}
                value={enteredCode}
                onChange={(event) =>
                  setEnteredCode(event.target.value.toUpperCase())
                }
                required
                minLength={6}
                maxLength={6}
                disabled={busy}
                className={inputClass}
              />
            </label>
          </>
        )}

        <div className="flex flex-wrap items-center gap-4">
          <button
            type="submit"
            disabled={busy}
            className={primaryButtonClass}
          >
            {busy
              ? "Please wait…"
              : challenge === null
                ? "Continue"
                : "Sign in"}
          </button>

          {challenge === null && (
            <button
              type="button"
              onClick={openPasswordChange}
              disabled={busy}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-3 font-medium text-teal-800 hover:bg-slate-50 disabled:opacity-50"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.8}
                aria-hidden="true"
                className="h-5 w-5"
              >
                <rect
                  x="5"
                  y="10"
                  width="14"
                  height="11"
                  rx="2"
                />

                <path
                  d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"
                  strokeLinecap="round"
                />
              </svg>

              <span>Change password</span>
            </button>
          )}
        </div>
      </form>
    </>
  );
}

export default function AuthenticationEntry() {
  const profile = useSyncExternalStore(
    sessionAuth.subscribe,
    sessionAuth.getSnapshot,
  );

  const [loading, setLoading] = useState(true);

  const [selectedRole, setSelectedRole] =
    useState<WorkbenchRole | null>(null);

  const [error, setError] = useState("");

  useEffect(() => {
    if (profile) {
      setSelectedRole(null);
    }
  }, [profile]);

  const loadSession = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      await sessionAuth.init();
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadSession();
  }, [loadSession]);

  if (loading) {
    return (
      <Page>
        <p role="status" className="text-slate-600">
          Checking your session…
        </p>
      </Page>
    );
  }

  if (profile) {
    return <App key={`${profile.id}:${profile.role}`} />;
  }

  if (error) {
    return (
      <Page>
        <h1 className="text-2xl font-bold">
          The workbench could not connect
        </h1>

        <p role="alert" className="mt-4 text-red-800">
          {error}
        </p>

        <button
          type="button"
          onClick={() => void loadSession()}
          className={`mt-6 ${primaryButtonClass}`}
        >
          Retry
        </button>
      </Page>
    );
  }

  if (selectedRole !== null) {
    return (
      <Page>
        <LoginPage
          key={selectedRole}
          role={selectedRole}
          onBack={() => setSelectedRole(null)}
        />
      </Page>
    );
  }

  return (
    <Page>
      <h1 className="text-3xl font-bold text-slate-900">
        Welcome to your workbench
      </h1>

      <p className="mt-4 text-slate-600">
        Submit portfolio inputs, analyse exposure and review
        assessments. Choose your assigned role to sign in.
      </p>

      <div className="mt-8 grid gap-4 md:grid-cols-3">
        {roles.map((role) => (
          <button
            key={role}
            type="button"
            onClick={() => setSelectedRole(role)}
            className="rounded-xl border border-slate-200 p-5 text-left transition hover:border-teal-700 hover:bg-teal-50"
          >
            <span className="block text-lg font-bold text-slate-900">
              {roleNames[role]} Login
            </span>

            <span className="mt-2 block text-sm text-slate-600">
              {roleDescriptions[role]}
            </span>
          </button>
        ))}
      </div>
    </Page>
  );
}