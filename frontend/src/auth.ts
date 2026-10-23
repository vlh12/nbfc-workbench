import { sessionAuth, type UserProfile } from "./sessionAuth";

export const auth = sessionAuth;

type ApiProblem = Record<string, unknown> & {
  message: string;
  httpStatus: number;
};

function isObject(value: unknown): value is Record<string, unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value)
  );
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 401:
      return "Your session has expired. Please sign in again.";
    case 403:
      return "This action is not permitted. Check your permissions or refresh the page.";
    case 503:
      return "The service is temporarily unavailable. Please try again.";
    default:
      return "The service could not complete your request.";
  }
}

function apiProblem(status: number, body: unknown): ApiProblem {
  const details = isObject(body) ? body : {};

  const message =
    typeof details.message === "string" && details.message.trim()
      ? details.message
      : typeof details.detail === "string" && details.detail.trim()
        ? details.detail
        : fallbackMessage(status);

  return {
    ...details,
    message,
    httpStatus: status,
  };
}

function checkCurrentProfile(expectedProfile: UserProfile): void {
  if (sessionAuth.getSnapshot() !== expectedProfile) {
    throw new Error(
      "Your sign-in state changed. Please repeat the action."
    );
  }
}

async function readResponse(
  response: Response,
  expectedProfile: UserProfile,
  method: string
): Promise<unknown> {
  if (response.status === 401) {
    sessionAuth.expireSession(expectedProfile);
  }

  if (
    response.ok &&
    (response.status === 204 ||
      response.status === 205 ||
      method === "HEAD")
  ) {
    return undefined;
  }

  const text = await response.text();

  let body: unknown;
  let parsed = false;

  if (text.trim()) {
    try {
      body = JSON.parse(text);
      parsed = true;
    } catch {
      if (response.ok) {
        throw new Error(
          "The service returned an unreadable response."
        );
      }
    }
  }

  if (!response.ok) {
    throw apiProblem(response.status, body);
  }

  if (!parsed) {
    throw new Error("The service returned an empty response.");
  }

  return body;
}

export async function api<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const expectedProfile = sessionAuth.getSnapshot();

  if (!expectedProfile) {
    throw apiProblem(401, {
      message: "Please sign in before using the workbench.",
    });
  }

  const target = new URL(url, window.location.href);

  if (target.origin !== window.location.origin) {
    throw new Error(
      "Workbench requests must use the current website."
    );
  }

  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);

  headers.delete("X-Demo-Role");

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  const needsCsrf = !["GET", "HEAD", "OPTIONS"].includes(method);

  if (needsCsrf) {
    const csrfResponse = await fetch("/api/auth/csrf", {
      method: "GET",
      credentials: "same-origin",
      cache: "no-store",
      headers: {
        Accept: "application/json",
      },
      signal: options.signal,
    });

    const csrf = await readResponse(
      csrfResponse,
      expectedProfile,
      "GET"
    );

    if (
      !isObject(csrf) ||
      typeof csrf.headerName !== "string" ||
      !csrf.headerName.trim() ||
      typeof csrf.token !== "string" ||
      !csrf.token.trim()
    ) {
      throw new Error(
        "Could not prepare the request. Please refresh the page."
      );
    }

    headers.set(csrf.headerName, csrf.token);
  }

  checkCurrentProfile(expectedProfile);

  const response = await fetch(target.href, {
    ...options,
    method,
    headers,
    credentials: "same-origin",
    cache: "no-store",
  });

  const body = await readResponse(
    response,
    expectedProfile,
    method
  );

  checkCurrentProfile(expectedProfile);

  return body as T;
}