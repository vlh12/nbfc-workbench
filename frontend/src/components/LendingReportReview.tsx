import { useEffect, useState } from "react";
import LendingReportDetails from "./LendingReportDetails";
import {
    loadReviewerReports,
    type LendingReportPage,
} from "../lendingReports";

const PAGE_SIZE = 2;

export default function LendingReportReview({
    formatMoney,
}: {
    formatMoney: (amount: string) => string;
}) {
    const [page, setPage] = useState(0);
    const [refresh, setRefresh] = useState(0);
    const [data, setData] = useState<LendingReportPage | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [selectedId, setSelectedId] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setData(null);
        setSelectedId(null);

        void loadReviewerReports(page, PAGE_SIZE, controller.signal)
            .then(result => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(result.content)) {
                    throw new Error("The saved report list could not be read.");
                }

                setData(result);
            })
            .catch((failure: unknown) => {
                if (controller.signal.aborted) return;

                const message =
                    failure !== null &&
                        typeof failure === "object" &&
                        "message" in failure &&
                        typeof failure.message === "string"
                        ? failure.message
                        : "Saved reports could not be loaded. Select Refresh to retry.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [page, refresh]);

    return (
        <section
            aria-busy={loading}
            className="rounded-xl border border-slate-200 bg-white p-6"
        >
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h3 className="text-lg font-bold">Saved lending reports</h3>
                    <p className="mt-1 text-sm text-slate-600">
                        Validated contributor reports for our bank.
                        Each submission is listed separately.
                    </p>
                </div>

                <button
                    type="button"
                    disabled={loading}
                    onClick={() => setRefresh(value => value + 1)}
                    className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:opacity-50"
                >
                    Refresh
                </button>
            </div>

            {loading && (
                <p role="status" className="mt-4 text-sm">
                    Loading saved reports…
                </p>
            )}

            {error && (
                <p role="alert" className="mt-4 text-sm text-red-700">
                    {error}
                </p>
            )}

            {data && (
                <>
                    <p className="mt-4 text-sm text-slate-600">
                        Saved submissions: {data.totalElements}
                    </p>

                    {data.content.length === 0 ? (
                        <p className="mt-4 text-sm">
                            No saved reports were found on this page.
                        </p>
                    ) : (
                        <div className="mt-4 overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <caption className="sr-only">
                                    Saved lending reports for reviewer access
                                </caption>
                                <thead>
                                    <tr className="border-b bg-slate-50">
                                        <th scope="col" className="p-3">NBFC / report</th>
                                        <th scope="col" className="p-3">Reporting date</th>
                                        <th scope="col" className="p-3">Revenue period</th>
                                        <th scope="col" className="p-3">Saved on</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.content.map(report => (
                                        <tr key={report.id} className="border-b">
                                            <td className="p-3">
                                                <p className="font-semibold">{report.nbfcName}</p>
                                                <p className="text-xs text-slate-600">
                                                    {report.bankId} · {report.nbfcId}
                                                </p>
                                                <p className="mt-1 break-all font-mono text-xs">
                                                    {report.id}
                                                </p>
                                                <p className="mt-1 text-xs text-slate-600">
                                                    {report.rowCount} loans · {report.status}
                                                </p>
                                                <button
                                                    type="button"
                                                    aria-pressed={selectedId === report.id}
                                                    onClick={() => setSelectedId(report.id)}
                                                    className="mt-2 font-medium text-teal-800 underline"
                                                >
                                                    {selectedId === report.id
                                                        ? "Viewing report"
                                                        : "View report"}
                                                </button>
                                            </td>
                                            <td className="whitespace-nowrap p-3">
                                                {report.reportingDate}
                                            </td>
                                            <td className="whitespace-nowrap p-3">
                                                {report.revenuePeriodStart}
                                                <br />
                                                to {report.revenuePeriodEnd}
                                            </td>
                                            <td className="whitespace-nowrap p-3">
                                                {new Date(report.createdAt).toLocaleString("en-IN")}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
                <button
                    type="button"
                    disabled={loading || page === 0}
                    onClick={() => setPage(value => value - 1)}
                    className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:opacity-50"
                >
                    Previous
                </button>

                {data && (
                    <p className="text-sm">
                        Page {data.totalPages === 0 ? 0 : data.page + 1}
                        {" "}of {data.totalPages}
                    </p>
                )}

                <button
                    type="button"
                    disabled={loading || !data || page + 1 >= data.totalPages}
                    onClick={() => setPage(value => value + 1)}
                    className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:opacity-50"
                >
                    Next
                </button>
            </div>

            {selectedId && (
                <div className="mt-6 border-t border-slate-200 pt-4">
                    <button
                        type="button"
                        onClick={() => setSelectedId(null)}
                        className="text-sm text-slate-600 underline"
                    >
                        Close report
                    </button>

                    <LendingReportDetails
                        key={selectedId}
                        reportId={selectedId}
                        formatMoney={formatMoney}
                    />
                </div>
            )}
        </section>
    );
}