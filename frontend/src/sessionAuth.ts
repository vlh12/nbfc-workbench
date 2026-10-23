export type WorkbenchRole =
  | "CONTRIBUTOR"
  | "ANALYST"
  | "SUPERVISOR";

export type UserProfile = Readonly<{
  id: string;
  username: string;
  role: WorkbenchRole;
}>;

export type LoginCode = {
  code: string;
  expiresInSeconds: number;
};

export type PasswordChangeVerification = Readonly<{
  flowId: string;
  username: string;
  role: WorkbenchRole;
  expiresInSeconds: number;
}>;

type CsrfResponse = {
  headerName: string;
  token: string;
};

export class AuthenticationError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly restartRequired: boolean = false,
  ) {
    super(message);
    this.name = "AuthenticationError";
  }
}

let currentProfile: UserProfile | null = null;

const listeners = new Set<() => void>();

function publishProfile(profile: UserProfile | null): void {
  if (currentProfile === profile) {
    return;
  }

  currentProfile = profile;

  for (const listener of listeners) {
    listener();
  }
}

function checkedProfile(value: unknown): UserProfile {
  if (!value || typeof value !== "object") {
    throw new Error("The server returned an invalid account profile.");
  }

  const profile = value as Record<string, unknown>;

  const id = profile.id;
  const username = profile.username;
  const role = profile.role;

  if (
    typeof id !== "string" ||
    !id ||
    typeof username !== "string" ||
    !username ||
    (
      role !== "CONTRIBUTOR" &&
      role !== "ANALYST" &&
      role !== "SUPERVISOR"
    )
  ) {
    throw new Error("The server returned an invalid account profile.");
  }

  return { id, username, role };
}

function checkedPasswordChangeVerification(
  value: unknown,
): PasswordChangeVerification {
  if (!value || typeof value !== "object") {
    throw new Error(
      "The server returned an invalid password-change verification.",
    );
  }

  const verification = value as Record<string, unknown>;

  const flowId = verification.flowId;
  const username = verification.username;
  const role = verification.role;
  const expiresInSeconds = verification.expiresInSeconds;

  if (
    typeof flowId !== "string" ||
    flowId.length !== 36 ||
    typeof username !== "string" ||
    !username.trim() ||
    (
      role !== "CONTRIBUTOR" &&
      role !== "ANALYST" &&
      role !== "SUPERVISOR"
    ) ||
    typeof expiresInSeconds !== "number" ||
    !Number.isInteger(expiresInSeconds) ||
    expiresInSeconds <= 0
  ) {
    throw new Error(
      "The server returned an invalid password-change verification.",
    );
  }

  return {
    flowId,
    username,
    role,
    expiresInSeconds,
  };
}

async function readResponse<T>(response: Response): Promise<T> {
  const emptyResponse =
    response.status === 204 || response.status === 205;

  const body: unknown = emptyResponse
    ? undefined
    : await response.json().catch(() => undefined);

  if (!response.ok) {
    let message =
      `Request failed (HTTP ${response.status}). Please try again.`;

    let restartRequired = false;

    if (body && typeof body === "object") {
      const problem = body as Record<string, unknown>;
      const suppliedMessage = problem.message ?? problem.detail;

      if (
        typeof suppliedMessage === "string" &&
        suppliedMessage.trim()
      ) {
        message = suppliedMessage;
      }

      restartRequired = problem.restartRequired === true;
    }

    throw new AuthenticationError(
      response.status,
      message,
      restartRequired,
    );
  }

  if (!emptyResponse && body === undefined) {
    throw new Error(
      "The authentication service returned an unreadable response.",
    );
  }

  return body as T;
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`/api/auth${path}`, {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: {
      Accept: "application/json",
    },
  });

  return readResponse<T>(response);
}

async function post<T>(
  path: string,
  payload: unknown = {},
): Promise<T> {
  const expectedProfile = currentProfile;

  const csrf = await get<CsrfResponse>("/csrf");

  if (currentProfile !== expectedProfile) {
    throw new Error(
      "Your sign-in state changed. Please try again.",
    );
  }

  if (
    !csrf ||
    typeof csrf.headerName !== "string" ||
    typeof csrf.token !== "string"
  ) {
    throw new Error(
      "The authentication service did not provide a CSRF token.",
    );
  }

  const headers = new Headers({
    Accept: "application/json",
    "Content-Type": "application/json",
  });

  headers.set(csrf.headerName, csrf.token);

  const response = await fetch(`/api/auth${path}`, {
    method: "POST",
    credentials: "same-origin",
    cache: "no-store",
    headers,
    body: JSON.stringify(payload),
  });

  return readResponse<T>(response);
}

export const sessionAuth = {
  subscribe(listener: () => void): () => void {
    listeners.add(listener);

    return () => {
      listeners.delete(listener);
    };
  },

  getSnapshot(): UserProfile | null {
    return currentProfile;
  },

  get authenticated(): boolean {
    return currentProfile !== null;
  },

  get role(): WorkbenchRole | null {
    return currentProfile?.role ?? null;
  },

  hasRealmRole(role: string): boolean {
    return currentProfile?.role === role;
  },

  expireSession(expectedProfile: UserProfile | null): void {
    if (currentProfile === expectedProfile) {
      publishProfile(null);
    }
  },

  async init(): Promise<boolean> {
    const expectedProfile = currentProfile;

    try {
      const profile = checkedProfile(
        await get<unknown>("/me"),
      );

      if (currentProfile === expectedProfile) {
        publishProfile(profile);
      }

      return currentProfile !== null;
    } catch (error) {
      if (
        error instanceof AuthenticationError &&
        error.status === 401
      ) {
        if (currentProfile === expectedProfile) {
          publishProfile(null);
        }

        return currentProfile !== null;
      }

      throw error;
    }
  },

  beginLogin(
    role: WorkbenchRole,
    username: string,
    password: string,
  ): Promise<LoginCode> {
    return post<LoginCode>("/login", {
      username,
      password,
      role,
    });
  },

  async verifyLogin(code: string): Promise<UserProfile> {
    const profile = checkedProfile(
      await post<unknown>("/verify", { code }),
    );

    publishProfile(profile);

    return profile;
  },

  async beginPasswordChange(
    role: WorkbenchRole,
    username: string,
    currentPassword: string,
  ): Promise<PasswordChangeVerification> {
    const response = await post<unknown>(
      "/password/check",
      {
        username,
        currentPassword,
        role,
      },
    );

    return checkedPasswordChangeVerification(response);
  },

  async completePasswordChange(
    flowId: string,
    newPassword: string,
    confirmNewPassword: string,
  ): Promise<void> {
    const expectedProfile = currentProfile;

    await post<void>("/password/change", {
      flowId,
      newPassword,
      confirmNewPassword,
    });

    if (currentProfile === expectedProfile) {
      publishProfile(null);
    }
  },

  async logout(): Promise<void> {
    const expectedProfile = currentProfile;

    try {
      await post<void>("/logout");
    } catch (error) {
      if (
        !(error instanceof AuthenticationError) ||
        error.status !== 401
      ) {
        throw error;
      }
    }

    if (currentProfile === expectedProfile) {
      publishProfile(null);
    }
  },
};