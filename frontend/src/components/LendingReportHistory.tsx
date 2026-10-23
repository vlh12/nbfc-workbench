import { useEffect, useState } from "react";
import { api } from "../auth";

type Props = {
  refreshKey: number;
  formatMoney: (amount: string) => string;
};

type SavedReport = {
  id: string;
  referenceId: string;
  nbfcId: string;
  nbfcName: string;
  reportingDate: string;
  rowCount: number;
  status: string;
  revenuePeriodStart: string;
  revenuePeriodEnd: string;
  revenueAmount: string;
  originalFilename: string;
  createdAt: string;
};

type SavedLoan = {
  loanId: string;
  amountLent: string;
  interestAmount: string;
  totalPayable: string;
  amountRepaid: string;
  outstandingAmount: string;
};

type ReportDetail = {
  report: SavedReport;
  loans: SavedLoan[];
};

function errorMessage(error: unknown): string {
  if (typeof error === "object" && error !== null) {
    const value = error as { message?: unknown };

    if (typeof value.message === "string") {
      return value.message;
    }
  }

  return "Could not load the report information. Please try again.";
}

export default function LendingReportHistory({
  refreshKey,
  formatMoney,
}: Props) {
  const [reports, setReports] = useState<SavedReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError(null);

    void api<SavedReport[]>("/api/lending-reports", {
      signal: controller.signal,
    })
      .then((data) => {
        if (controller.signal.aborted) return;

        if (!Array.isArray(data)) {
          throw new Error("The saved report list could not be read.");
        }

        setReports(data);

        setSelectedId((current) =>
          data.some((report) => report.id === current)
            ? current
            : null,
        );
      })
      .catch((failure: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorMessage(failure));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [refreshKey, reload]);

  return (
    <section className="mt-6 border-t border-slate-200 pt-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold">
            Saved report history
          </h3>
          <p className="mt-1 text-sm text-slate-600">
            Your latest 50 saved reports, including earlier reporting dates.
          </p>
        </div>

        <button
          type="button"
          disabled={loading}
          onClick={() => setReload((value) => value + 1)}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:opacity-50"
        >
          {loading ? "Loading..." : "Refresh"}
        </button>
      </div>

      {loading ? (
        <p className="mt-4 text-sm text-slate-600">
          Loading saved reports...
        </p>
      ) : error ? (
        <p role="alert" className="mt-4 text-sm text-red-700">
          {error}
        </p>
      ) : reports.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">
          You have no saved reports yet.
        </p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b bg-slate-50">
              <tr>
                <th className="p-3">NBFC / reference</th>
                <th className="p-3">Reporting date</th>
                <th className="p-3">Loans</th>
                <th className="p-3">Status</th>
                <th className="p-3">Saved on</th>
                <th className="p-3">Details</th>
              </tr>
            </thead>

            <tbody>
              {reports.map((report) => (
                <tr
                  key={report.id}
                  className={
                    selectedId === report.id
                      ? "border-b bg-blue-50"
                      : "border-b"
                  }
                >
                  <td className="p-3">
                    <div className="font-medium">
                      {report.nbfcName}
                    </div>
                    <div className="text-xs text-slate-500">
                      {report.referenceId}
                    </div>
                  </td>
                  <td className="whitespace-nowrap p-3">
                    {report.reportingDate}
                  </td>
                  <td className="p-3">{report.rowCount}</td>
                  <td className="p-3">{report.status}</td>
                  <td className="whitespace-nowrap p-3">
                    {new Date(report.createdAt).toLocaleString("en-IN")}
                  </td>
                  <td className="p-3">
                    <button
                      type="button"
                      aria-pressed={selectedId === report.id}
                      onClick={() => setSelectedId(report.id)}
                      className="font-medium text-blue-700 underline"
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedId && (
        <div className="mt-5">
          <button
            type="button"
            onClick={() => setSelectedId(null)}
            className="mb-3 text-sm text-slate-600 underline"
          >
            Close details
          </button>

          <SavedReportDetails
            key={selectedId}
            reportId={selectedId}
            formatMoney={formatMoney}
          />
        </div>
      )}
    </section>
  );
}

function SavedReportDetails({
  reportId,
  formatMoney,
}: {
  reportId: string;
  formatMoney: (amount: string) => string;
}) {
  const [detail, setDetail] = useState<ReportDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError(null);
    setDetail(null);

    void api<ReportDetail>(
      "/api/lending-reports/" + encodeURIComponent(reportId),
      { signal: controller.signal },
    )
      .then((data) => {
        if (controller.signal.aborted) return;

        if (!data.report || !Array.isArray(data.loans)) {
          throw new Error("The saved report details could not be read.");
        }

        setDetail(data);
      })
      .catch((failure: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorMessage(failure));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [reportId]);

  if (loading) {
    return <p className="text-sm">Loading report details...</p>;
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-red-700">
        {error}
      </p>
    );
  }

  if (!detail) return null;

  const report = detail.report;

  return (
    <div className="rounded-lg border border-slate-200 p-4">
      <h4 className="font-semibold">{report.nbfcName}</h4>

      <div className="mt-3 space-y-1 text-sm">
        <p className="break-all">
          <strong>Report ID:</strong> {report.id}
        </p>
        <p>
          <strong>NBFC ID:</strong> {report.nbfcId}
        </p>
        <p>
          <strong>Reporting date:</strong> {report.reportingDate}
        </p>
        <p className="break-all">
          <strong>File:</strong> {report.originalFilename}
        </p>
        <p>
          <strong>Revenue period:</strong>{" "}
          {report.revenuePeriodStart} to {report.revenuePeriodEnd}
        </p>
        <p>
          <strong>Company revenue:</strong>{" "}
          {formatMoney(report.revenueAmount)}
        </p>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="min-w-full whitespace-nowrap text-left text-sm">
          <thead className="border-b bg-slate-50">
            <tr>
              <th className="p-3">Loan ID</th>
              <th className="p-3">Amount lent</th>
              <th className="p-3">Interest</th>
              <th className="p-3">Total payable</th>
              <th className="p-3">Repaid</th>
              <th className="p-3">Outstanding</th>
            </tr>
          </thead>
          <tbody>
            {detail.loans.map((loan) => (
              <tr key={loan.loanId} className="border-b">
                <td className="p-3">{loan.loanId}</td>
                <td className="p-3">
                  {formatMoney(loan.amountLent)}
                </td>
                <td className="p-3">
                  {formatMoney(loan.interestAmount)}
                </td>
                <td className="p-3">
                  {formatMoney(loan.totalPayable)}
                </td>
                <td className="p-3">
                  {formatMoney(loan.amountRepaid)}
                </td>
                <td className="p-3">
                  {formatMoney(loan.outstandingAmount)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}