import LendingLoanChart from "./LendingLoanChart";
import { useEffect, useState } from "react";
import {
    loadReviewerReport,
    type LendingReportDetail,
} from "../lendingReports";

const columns = [
    ["amountLent", "Amount lent"],
    ["interestAmount", "Recorded interest"],
    ["totalPayable", "Total payable"],
    ["amountRepaid", "Amount repaid"],
    ["outstandingAmount", "Outstanding"],
] as const;

type Props = {
    reportId: string;
    formatMoney: (amount: string) => string;
};

export default function LendingReportDetails({
    reportId,
    formatMoney,
}: Props) {
    const [detail, setDetail] = useState<LendingReportDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setDetail(null);

        void loadReviewerReport(reportId, controller.signal)
            .then(result => {
                if (controller.signal.aborted) return;

                if (
                    !result.report ||
                    result.report.id !== reportId ||
                    !Array.isArray(result.loans)
                ) {
                    throw new Error("The saved report details could not be read.");
                }

                setDetail(result);
            })
            .catch((failure: unknown) => {
                if (controller.signal.aborted) return;

                const message =
                    failure !== null &&
                        typeof failure === "object" &&
                        "message" in failure &&
                        typeof failure.message === "string"
                        ? failure.message
                        : "Report details could not be loaded. Please retry.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [reportId, retry]);

    if (loading) {
        return (
            <p role="status" className="mt-4 text-sm">
                Loading saved report details…
            </p>
        );
    }

    if (error) {
        return (
            <div className="mt-4">
                <p role="alert" className="text-sm text-red-700">
                    {error}
                </p>
                <button
                    type="button"
                    onClick={() => setRetry(value => value + 1)}
                    className="mt-3 rounded-lg border border-slate-300 px-4 py-2 text-sm"
                >
                    Retry
                </button>
            </div>
        );
    }

    if (!detail) {
        return <p className="mt-4 text-sm">No report details were returned.</p>;
    }

    const { report, loans } = detail;

    return (
        <section className="mt-4 rounded-lg border border-slate-200 p-4">
            <h4 className="text-lg font-semibold">
                Selected report: {report.nbfcName}
            </h4>

            <div className="mt-3 space-y-1 text-sm">
                <p className="break-all">
                    <strong>Report ID:</strong> {report.id}
                </p>
                <p>
                    <strong>Bank:</strong> {report.bankId}
                    {" · "}
                    <strong>NBFC:</strong> {report.nbfcId}
                </p>
                <p>
                    <strong>Loan figures as of:</strong> {report.reportingDate}
                </p>
                <p>
                    <strong>Status:</strong> {report.status}
                </p>
                <p className="break-all">
                    <strong>Saved file:</strong> {report.originalFilename}
                </p>
                <p>
                    Amounts in {report.currency} ({report.unit}).
                </p>
            </div>

            <LendingLoanChart loans={loans} formatMoney={formatMoney} />

            <h5 className="mt-5 font-semibold">Saved loan figures</h5>

            {loans.length === 0 ? (
                <p className="mt-3 text-sm">This report has no saved loan rows.</p>
            ) : (
                <div className="mt-3 overflow-x-auto">
                    <table className="w-full whitespace-nowrap text-sm">
                        <caption className="sr-only">
                            Exact saved loan amounts in INR rupees
                        </caption>
                        <thead>
                            <tr className="border-b bg-slate-50">
                                <th scope="col" className="p-3 text-left">
                                    Loan ID
                                </th>
                                {columns.map(([field, label]) => (
                                    <th
                                        key={field}
                                        scope="col"
                                        className="p-3 text-right"
                                    >
                                        {label}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {loans.map(loan => (
                                <tr key={loan.loanId} className="border-b">
                                    <td className="p-3 text-left">{loan.loanId}</td>
                                    {columns.map(([field]) => (
                                        <td
                                            key={field}
                                            className="p-3 text-right tabular-nums"
                                        >
                                            {formatMoney(loan[field])}
                                        </td>
                                    ))}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <div className="mt-5 border-t border-slate-200 pt-4">
                <h5 className="font-semibold">Company revenue</h5>

                <p className="mt-2 text-sm">
                    <strong>Revenue period:</strong>{" "}
                    {report.revenuePeriodStart} to {report.revenuePeriodEnd}
                </p>

                <p className="mt-2 text-xl font-semibold tabular-nums">
                    {formatMoney(report.revenueAmount)}
                </p>

                <p className="mt-1 text-sm text-slate-600">
                    Income before expenses for this period.
                </p>
            </div>

        </section >
    );
}