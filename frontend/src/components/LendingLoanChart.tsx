import { useMemo, useState } from "react";
import {
    Bar,
    BarChart,
    CartesianGrid,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import type { LendingReportLoan } from "../lendingReports";

const SERIES = [
    { key: "amountLent", label: "Amount lent", color: "#0f766e" },
    { key: "interestAmount", label: "Recorded interest", color: "#b45309" },
    { key: "totalPayable", label: "Total payable", color: "#7c3aed" },
    { key: "amountRepaid", label: "Amount repaid", color: "#2563eb" },
    { key: "outstandingAmount", label: "Outstanding", color: "#be123c" },
] as const;

type MoneyField = (typeof SERIES)[number]["key"];

type Props = {
    loans: LendingReportLoan[];
    formatMoney: (amount: string) => string;
};

function chartCrore(amount: string): number {
    const [whole, fraction = ""] = amount.split(".");
    const paise = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"));
    const paisePerCrore = 1_000_000_000n;

    return (
        Number(paise / paisePerCrore) +
        Number(paise % paisePerCrore) / Number(paisePerCrore)
    );
}

export default function LendingLoanChart({ loans, formatMoney }: Props) {
    const [enabled, setEnabled] = useState<Record<MoneyField, boolean>>({
        amountLent: true,
        interestAmount: true,
        totalPayable: true,
        amountRepaid: true,
        outstandingAmount: true,
    });

    const chartData = useMemo(
        () =>
            loans.map(loan => ({
                loanId: loan.loanId,
                amountLent: chartCrore(loan.amountLent),
                interestAmount: chartCrore(loan.interestAmount),
                totalPayable: chartCrore(loan.totalPayable),
                amountRepaid: chartCrore(loan.amountRepaid),
                outstandingAmount: chartCrore(loan.outstandingAmount),
            })),
        [loans],
    );

    const visibleSeries = SERIES.filter(series => enabled[series.key]);

    return (
        <section className="mt-5 border-t border-slate-200 pt-4">
            <h5 className="font-semibold">Loan comparison</h5>
            <p className="mt-1 text-sm text-slate-600">
                Vertical axis: INR crore. 1 crore = ₹1,00,00,000.00.
                Tooltips and the table show exact INR rupees.
            </p>

            <fieldset className="mt-4">
                <legend className="text-sm font-medium">
                    Choose amounts to display
                </legend>
                <div className="mt-2 flex flex-wrap gap-4">
                    {SERIES.map(series => (
                        <label key={series.key} className="flex items-center gap-2 text-sm">
                            <input
                                type="checkbox"
                                checked={enabled[series.key]}
                                onChange={event => {
                                    const checked = event.target.checked;
                                    setEnabled(current => ({
                                        ...current,
                                        [series.key]: checked,
                                    }));
                                }}
                            />
                            <span
                                aria-hidden="true"
                                className="h-3 w-3 rounded-sm"
                                style={{ backgroundColor: series.color }}
                            />
                            {series.label}
                        </label>
                    ))}
                </div>
            </fieldset>

            {loans.length === 0 ? (
                <p className="mt-4 text-sm">No saved loans are available to chart.</p>
            ) : visibleSeries.length === 0 ? (
                <p role="status" className="mt-4 text-sm">
                    Select at least one amount to display the chart.
                </p>
            ) : (
                <div
                    className="mt-4 overflow-x-auto"
                    role="group"
                    aria-label="Loan amounts by loan ID, in INR crore"
                >
                    <div
                        style={{
                            height: 360,
                            minWidth: Math.max(640, loans.length * 180),
                        }}
                    >
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart
                                data={chartData}
                                margin={{ top: 16, right: 24, bottom: 20, left: 0 }}
                                barGap={4}
                            >
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis
                                    dataKey="loanId"
                                    interval={0}
                                    height={55}
                                    angle={-20}
                                    textAnchor="end"
                                    tick={{ fontSize: 12 }}
                                />
                                <YAxis
                                    width={85}
                                    domain={[0, "auto"]}
                                    tickFormatter={value =>
                                        Number(value).toLocaleString("en-IN", {
                                            maximumFractionDigits: 2,
                                        })
                                    }
                                />
                                <Tooltip
                                    content={({ active, label }) => {
                                        if (!active) return null;

                                        const loan = loans.find(
                                            saved => saved.loanId === String(label),
                                        );
                                        if (!loan) return null;

                                        return (
                                            <div className="rounded-lg border border-slate-200 bg-white p-3 text-sm shadow-lg">
                                                <p className="break-all font-semibold">{loan.loanId}</p>
                                                <p className="mb-2 text-xs text-slate-600">
                                                    Exact amounts in INR rupees
                                                </p>
                                                {visibleSeries.map(series => (
                                                    <p key={series.key} className="mt-1">
                                                        <span style={{ color: series.color }}>
                                                            {series.label}:
                                                        </span>{" "}
                                                        <span className="font-medium tabular-nums">
                                                            {formatMoney(loan[series.key])}
                                                        </span>
                                                    </p>
                                                ))}
                                            </div>
                                        );
                                    }}
                                />
                                {visibleSeries.map(series => (
                                    <Bar
                                        key={series.key}
                                        dataKey={series.key}
                                        name={series.label}
                                        fill={series.color}
                                        maxBarSize={36}
                                        isAnimationActive={false}
                                    />
                                ))}
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            )}
        </section>
    );
}