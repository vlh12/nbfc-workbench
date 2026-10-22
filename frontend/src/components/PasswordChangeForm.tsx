import {
  useRef,
  useState,
  type FormEvent,
} from "react";

import {
  AuthenticationError,
  sessionAuth,
  type PasswordChangeVerification,
  type WorkbenchRole,
} from "../sessionAuth";

type Props = {
  role: WorkbenchRole;
  onBack: () => void;
  onChanged: (username: string) => void;
};

type PendingChange = PasswordChangeVerification & {
  expiresAt: number;
};

const roleNames: Record<WorkbenchRole, string> = {
  CONTRIBUTOR: "Contributor",
  ANALYST: "Analyst",
  SUPERVISOR: "Supervisor",
};

const inputClass =
  "mt-2 block w-full rounded-lg border border-slate-300 bg-white px-3 py-3 disabled:opacity-60";

const buttonClass =
  "rounded-lg bg-teal-800 px-5 py-3 font-semibold text-white hover:bg-teal-900 disabled:opacity-50";

function errorMessage(error: unknown): string {
  if (error instanceof TypeError) {
    return "The workbench service could not be reached. Please try again.";
  }

  return error instanceof Error
    ? error.message
    : "The request could not be completed. Please try again.";
}

function passwordProblem(
  password: string,
  confirmation: string,
): string | null {
  if (
    !password.trim() ||
    Array.from(password).length < 12
  ) {
    return "New password must contain at least 12 characters.";
  }

  if (new TextEncoder().encode(password).length > 72) {
    return "New password is too long. Use no more than 72 UTF-8 bytes.";
  }

  if (password !== confirmation) {
    return "The new passwords do not match.";
  }

  return null;
}

export default function PasswordChangeForm({
  role,
  onBack,
  onChanged,
}: Props) {
  const [username, setUsername] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");

  const [verification, setVerification] =
    useState<PendingChange | null>(null);

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const inFlight = useRef(false);

  function clearVerification(): void {
    setVerification(null);
    setCurrentPassword("");
    setNewPassword("");
    setConfirmation("");
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

    const wasChanging = verification !== null;

    try {
      if (verification === null) {
        const requestedAt = Date.now();

        const issued = await sessionAuth.beginPasswordChange(
          role,
          username.trim(),
          currentPassword,
        );

        if (issued.role !== role) {
          throw new Error(
            "The account role did not match. Return to login and try again.",
          );
        }

        setUsername(issued.username);
        setCurrentPassword("");
        setNewPassword("");
        setConfirmation("");

        setVerification({
          ...issued,
          expiresAt:
            requestedAt + issued.expiresInSeconds * 1_000,
        });

        return;
      }

      if (Date.now() >= verification.expiresAt) {
        clearVerification();

        setError(
          "Verification expired. Enter your username and current password again.",
        );

        return;
      }

      const problem = passwordProblem(
        newPassword,
        confirmation,
      );

      if (problem !== null) {
        setError(problem);
        return;
      }

      await sessionAuth.completePasswordChange(
        verification.flowId,
        newPassword,
        confirmation,
      );

      const verifiedUsername = verification.username;

      clearVerification();
      onChanged(verifiedUsername);
    } catch (failure) {
      const validationFailure =
        failure instanceof AuthenticationError &&
        failure.status === 400 &&
        !failure.restartRequired;

      if (wasChanging && !validationFailure) {
        clearVerification();
      }

      const uncertainResult =
        wasChanging &&
        !(
          failure instanceof AuthenticationError &&
          [400, 401, 403, 409].includes(failure.status)
        );

      if (uncertainResult) {
        setError(
          "Could not confirm whether your password changed. Use Back to login and try signing in before starting again.",
        );
      } else {
        setError(errorMessage(failure));
      }
    } finally {
      setCurrentPassword("");
      inFlight.current = false;
      setBusy(false);
    }
  }

  async function back(): Promise<void> {
    if (inFlight.current) {
      return;
    }

    inFlight.current = true;
    setBusy(true);
    setError("");

    try {
      await sessionAuth.logout();
      clearVerification();
      onBack();
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => void back()}
        disabled={busy}
        className="mb-6 font-medium text-teal-800 underline disabled:opacity-50"
      >
        ← Back to login
      </button>

      <h1 className="text-3xl font-bold text-slate-900">
        Change password
      </h1>

      <p className="mt-2 font-medium text-teal-800">
        {roleNames[role]} account
      </p>

      <p className="mt-3 text-slate-600">
        {verification === null
          ? "Enter your username and current password to continue."
          : "Your current credentials were verified. Choose and confirm your new password."}
      </p>

      {error && (
        <p
          role="alert"
          className="mt-5 rounded-lg bg-red-50 p-4 text-red-800"
        >
          {error}
        </p>
      )}

      <form
        key={verification?.flowId ?? "credentials"}
        onSubmit={submit}
        className="mt-6 space-y-5"
      >
        {verification === null ? (
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
              Current password

              <input
                name="currentPassword"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(event) =>
                  setCurrentPassword(event.target.value)
                }
                required
                disabled={busy}
                className={inputClass}
              />
            </label>
          </>
        ) : (
          <>
            <label className="block font-medium text-slate-700">
              Verified username

              <input
                name="username"
                autoComplete="username"
                value={verification.username}
                readOnly
                className={`${inputClass} bg-slate-50`}
              />
            </label>

            <p
              id="password-rules"
              className="text-sm text-slate-600"
            >
              Use at least 12 characters, up to 72 UTF-8 bytes.
              The new password must differ from your current password.
            </p>

            <label className="block font-medium text-slate-700">
              New password

              <input
                name="newPassword"
                type="password"
                autoComplete="new-password"
                aria-describedby="password-rules"
                value={newPassword}
                onChange={(event) =>
                  setNewPassword(event.target.value)
                }
                required
                autoFocus
                disabled={busy}
                className={inputClass}
              />
            </label>

            <label className="block font-medium text-slate-700">
              Confirm new password

              <input
                name="confirmNewPassword"
                type="password"
                autoComplete="new-password"
                value={confirmation}
                onChange={(event) =>
                  setConfirmation(event.target.value)
                }
                required
                disabled={busy}
                className={inputClass}
              />
            </label>

            <p className="text-sm text-slate-600">
              After saving, sign in using your new password.
            </p>
          </>
        )}

        <button
          type="submit"
          disabled={busy}
          className={buttonClass}
        >
          {busy
            ? "Please wait…"
            : verification === null
              ? "Verify current password"
              : "Save new password"}
        </button>
      </form>
    </>
  );
}