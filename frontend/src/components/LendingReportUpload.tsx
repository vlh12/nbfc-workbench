import { useEffect, useRef, useState, type FormEvent } from "react";
import { api } from "../auth";
import LendingReportHistory from "./LendingReportHistory";

type Props = {
  referenceId: string;
  formatMoney: (amount: string) => string;
};

type ValidationResult = {
  status: string;
  message: string;
  referenceId: string;
  bankId: string;
  nbfcId: string;
  nbfcName: string;
  reportingDate: string;
  rowCount: number;
  revenuePeriodStart: string;
  revenuePeriodEnd: string;
  revenueAmount: string;
  currency: string;
  unit: string;
};

type SaveResponse = {
  report: {
    id: string;
    status: string;
  };
};

type Issue = {
  line: number;
  message: string;
};

type Problem = {
  status?: string;
  message: string;
  issues: Issue[];
};

type Action = "validate" | "save";

function isIssue(value: unknown): value is Issue {
  if (typeof value !== "object" || value === null) return false;

  const issue = value as Record<string, unknown>;

  return (
    typeof issue.line === "number" &&
    typeof issue.message === "string"
  );
}

function readProblem(error: unknown, action: Action): Problem {
  const body =
    typeof error === "object" && error !== null
      ? (error as Record<string, unknown>)
      : {};

  return {
    status: typeof body.status === "string" ? body.status : undefined,
    message:
      typeof body.message === "string"
        ? body.message
        : action === "save"
          ? "Could not confirm saving. Check saved reports before trying again."
          : "Could not validate the report. Please try again.",
    issues: Array.isArray(body.issues)
      ? body.issues.filter(isIssue)
      : [],
  };
}

export default function LendingReportUpload({
  referenceId,
  formatMoney,
}: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [synthetic, setSynthetic] = useState(false);
  const [action, setAction] = useState<Action | null>(null);
  const [result, setResult] = useState<ValidationResult | null>(null);
  const [savedId, setSavedId] = useState<string | null>(null);
  const [historyRefresh, setHistoryRefresh] = useState(0);
  const [problem, setProblem] = useState<Problem | null>(null);

  const pending = useRef<AbortController | null>(null);
  const busy = action !== null;

  useEffect(() => {
    return () => pending.current?.abort();
  }, []);

  function clearResults() {
    setResult(null);
    setSavedId(null);
    setProblem(null);
  }

  async function run(nextAction: Action) {
    if (
      pending.current ||
      !file ||
      !synthetic ||
      savedId !== null
    ) {
      return;
    }

    if (
      nextAction === "save" &&
      result?.status !== "VALIDATED"
    ) {
      return;
    }

    const controller = new AbortController();
    pending.current = controller;

    setAction(nextAction);
    setProblem(null);

    if (nextAction === "validate") {
      setResult(null);
      setSavedId(null);
    }

    const body = new FormData();
    body.append("referenceId", referenceId);
    body.append("file", file);
    body.append("synthetic", "true");

    try {
      if (nextAction === "validate") {
        const response = await api<ValidationResult>(
          "/api/lending-reports/validate",
          {
            method: "POST",
            body,
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        if (response.status !== "VALIDATED") {
          throw new Error(
            "The server did not confirm successful validation.",
          );
        }

        setResult(response);
      } else {
        const response = await api<SaveResponse>(
          "/api/lending-reports",
          {
            method: "POST",
            body,
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        if (
          response.report?.status !== "VALIDATED" ||
          !response.report.id
        ) {
          throw new Error(
            "The server did not confirm that the report was saved.",
          );
        }

        setSavedId(response.report.id);
        setHistoryRefresh((value) => value + 1);
      }
    } catch (error: unknown) {
      if (controller.signal.aborted) return;

      const failure = readProblem(error, nextAction);
      setProblem(failure);

      if (
        nextAction === "save" &&
        failure.status === "INVALID"
      ) {
        setResult(null);
      }
    } finally {
      if (pending.current === controller) {
        pending.current = null;
      }

      if (!controller.signal.aborted) {
        setAction(null);
      }
    }
  }

  function handleValidate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void run("validate");
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h3 className="text-lg font-semibold">
        Validate and save lending report
      </h3>

      <p className="mt-1 text-sm text-slate-600">
        Upload one NBFC's complete loan report for the selected bank
        reference.
      </p>

      <a
        href="/lending-report-demo.csv"
        download
        className="mt-3 inline-block text-sm font-medium text-blue-700 underline"
      >
        Download sample: NBFC A, 2026-09-30
      </a>

      <form onSubmit={handleValidate} className="mt-4 space-y-4">
        <label className="block text-sm font-medium">
          Lending report CSV
          <input
            type="file"
            accept=".csv,text/csv"
            disabled={busy}
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setSynthetic(false);
              clearResults();
            }}
            className="mt-2 block w-full text-sm"
          />
        </label>

        <label className="flex items-start gap-2 text-sm">
          <input
            type="checkbox"
            checked={synthetic}
            disabled={busy}
            onChange={(event) => {
              setSynthetic(event.target.checked);
              clearResults();
            }}
            className="mt-1"
          />
          <span>
            I confirm that this file contains synthetic data only.
          </span>
        </label>

        <div className="flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={
              busy || !file || !synthetic || savedId !== null
            }
            className="rounded-lg bg-blue-700 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {action === "validate"
              ? "Validating..."
              : "Validate report"}
          </button>

          {result && (
            <button
              type="button"
              disabled={
                busy || !file || !synthetic || savedId !== null
              }
              onClick={() => void run("save")}
              className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              {savedId !== null
                ? "Saved"
                : action === "save"
                  ? "Saving..."
                  : "Save report"}
            </button>
          )}
        </div>
      </form>

      {problem && (
        <div
          role="alert"
          className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-900"
        >
          <p className="font-semibold">
            {problem.status === "INVALID"
              ? "Report needs correction"
              : "Request could not be completed"}
          </p>

          <p className="mt-1">{problem.message}</p>

          {problem.issues.length > 0 && (
            <ul className="mt-2 list-disc space-y-1 pl-5">
              {problem.issues.map((issue, index) => (
                <li key={index}>
                  {issue.line > 0
                    ? "Line " + issue.line
                    : "Report"}
                  : {issue.message}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {result && (
        <div
          role="status"
          className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-950"
        >
          <p className="font-semibold">Validated</p>
          <p className="mt-1">{result.message}</p>

          <div className="mt-3 space-y-1">
            <p>
              <strong>NBFC:</strong> {result.nbfcName} (
              {result.nbfcId})
            </p>
            <p>
              <strong>Reporting date:</strong>{" "}
              {result.reportingDate}
            </p>
            <p>
              <strong>Loan records:</strong> {result.rowCount}
            </p>
            <p>
              <strong>Revenue period:</strong>{" "}
              {result.revenuePeriodStart} to{" "}
              {result.revenuePeriodEnd}
            </p>
            <p>
              <strong>Company revenue:</strong>{" "}
              {formatMoney(result.revenueAmount)}
            </p>
          </div>
        </div>
      )}

      {savedId !== null && (
        <div
          role="status"
          className="mt-4 rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-950"
        >
          <p className="font-semibold">Saved successfully</p>
          <p className="mt-1 break-all">Report ID: {savedId}</p>
        </div>
      )}

            <LendingReportHistory
        refreshKey={historyRefresh}
        formatMoney={formatMoney}
      />
    
    </section>
  );
}