import { useEffect, useState } from "react";
import { api } from "../auth";
import LendingReportUpload from "./LendingReportUpload";

type Reference = {
  id: string;
  bankName: string;
  reportingDate: string;
  version: number;
  currency: string;
  unit: string;
};

type Loan = {
  loanId: string;
  nbfcId: string;
  nbfcName: string;
  amountLent: string;
  interestAmount: string;
  totalPayable: string;
  amountRepaid: string;
  outstandingAmount: string;
};

type Revenue = {
  nbfcId: string;
  periodStart: string;
  periodEnd: string;
  revenueAmount: string;
};

type Detail = {
  reference: Reference;
  loans: Loan[];
  revenues: Revenue[];
};

type Props = {
  formatMoney: (amount: string) => string;
};

function revenuePeriods(rows: Revenue[]) {
  const periods = new Map<
    string,
    { key: string; start: string; end: string }
  >();

  for (const row of rows) {
    const key = row.periodStart + "|" + row.periodEnd;
    periods.set(key, {
      key,
      start: row.periodStart,
      end: row.periodEnd,
    });
  }

  return [...periods.values()].sort(
    (a, b) =>
      b.end.localeCompare(a.end) ||
      b.start.localeCompare(a.start)
  );
}

export function LendingReferencePanel({ formatMoney }: Props) {
  const [references, setReferences] = useState<Reference[]>([]);
  const [referenceId, setReferenceId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    setLoading(true);
    setError("");
    setReferences([]);
    setReferenceId("");

    api<Reference[]>("/api/lending-references", {
      signal: controller.signal,
    })
      .then(rows => {
        if (!active) return;
        setReferences(rows);
        setReferenceId(rows[0]?.id ?? "");
      })
      .catch(() => {
        if (active) {
          setError(
            "Reference records could not be loaded. Select Refresh to try again."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [refresh]);

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-lg font-bold">Bank reference records</h3>
          <p className="mt-1 text-sm text-slate-600">
            Loan balances and company revenue · Synthetic data
          </p>
        </div>

        <button
          type="button"
          disabled={loading}
          onClick={() => setRefresh(value => value + 1)}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold disabled:opacity-50"
        >
          Refresh
        </button>
      </div>

      {loading && (
        <p role="status" className="mt-4">
          Loading reference records…
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="mt-4 rounded-lg bg-amber-50 p-3 text-amber-900"
        >
          {error}
        </p>
      )}

      {!loading && !error && references.length === 0 && (
        <p className="mt-4">No bank reference records are available.</p>
      )}

      {!loading && !error && references.length > 0 && (
        <>
          <label className="mt-4 block text-sm font-medium">
            Reference snapshot
            <select
              value={referenceId}
              onChange={event => setReferenceId(event.target.value)}
              className="mt-2 block w-full rounded-lg border border-slate-300 p-2"
            >
              {references.map(reference => (
                <option key={reference.id} value={reference.id}>
                  {reference.bankName} · {reference.reportingDate} · Version{" "}
                  {reference.version}
                </option>
              ))}
            </select>
          </label>

          {referenceId && (
            <ReferenceDetail
              key={referenceId}
              referenceId={referenceId}
              formatMoney={formatMoney}
            />
          )}
        </>
      )}
    </section>
  );
}

function ReferenceDetail({
  referenceId,
  formatMoney,
}: Props & { referenceId: string }) {
  const [detail, setDetail] = useState<Detail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [nbfcId, setNbfcId] = useState("");
  const [periodKey, setPeriodKey] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    setLoading(true);
    setError("");
    setDetail(null);

    api<Detail>(
      "/api/lending-references/" + encodeURIComponent(referenceId),
      { signal: controller.signal }
    )
      .then(value => {
        if (!active) return;
        setDetail(value);
        setNbfcId("");
        setPeriodKey(revenuePeriods(value.revenues)[0]?.key ?? "");
      })
      .catch(() => {
        if (active) {
          setError(
            "This reference could not be loaded. Select Refresh to try again."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [referenceId]);

  if (loading) {
    return (
      <p role="status" className="mt-4">
        Loading loan and revenue details…
      </p>
    );
  }

  if (error) {
    return (
      <p
        role="alert"
        className="mt-4 rounded-lg bg-amber-50 p-3 text-amber-900"
      >
        {error}
      </p>
    );
  }

  if (!detail) {
    return <p className="mt-4">No reference details were returned.</p>;
  }

  const companyNames = new Map<string, string>();

  for (const loan of detail.loans) {
    companyNames.set(loan.nbfcId, loan.nbfcName);
  }

  for (const revenue of detail.revenues) {
    if (!companyNames.has(revenue.nbfcId)) {
      companyNames.set(revenue.nbfcId, revenue.nbfcId);
    }
  }

  const companies = [...companyNames.entries()].sort(
    ([a], [b]) => a.localeCompare(b)
  );

  const periods = revenuePeriods(detail.revenues);

  const loans = detail.loans.filter(
    row => !nbfcId || row.nbfcId === nbfcId
  );

  const revenues = detail.revenues.filter(
    row =>
      (!nbfcId || row.nbfcId === nbfcId) &&
      row.periodStart + "|" + row.periodEnd === periodKey
  );

  return (
    <div className="mt-5 space-y-5">
    <LendingReportUpload
  key={referenceId}
  referenceId={referenceId}
  formatMoney={formatMoney}
/>
      <p className="text-sm text-slate-600">
        Loan figures as of{" "}
        <strong>{detail.reference.reportingDate}</strong>.
        {" "}Amounts in {detail.reference.currency} ({detail.reference.unit}).
      </p>

      <label className="block text-sm font-medium">
        NBFC
        <select
          value={nbfcId}
          onChange={event => setNbfcId(event.target.value)}
          className="mt-2 block w-full rounded-lg border border-slate-300 p-2"
        >
          <option value="">All NBFCs</option>
          {companies.map(([id, name]) => (
            <option key={id} value={id}>
              {name} ({id})
            </option>
          ))}
        </select>
      </label>

      <div>
        <h4 className="font-semibold">Loan and repayment details</h4>
        <p className="mt-1 text-sm text-slate-500">
          Interest and repayments are cumulative through the snapshot date.
        </p>

        {loans.length === 0 ? (
          <p className="mt-3">No loans for this selection.</p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full whitespace-nowrap text-sm">
              <caption className="sr-only">
                Reference loan and repayment amounts in INR rupees
              </caption>
              <thead>
                <tr className="border-b text-left">
                  <th scope="col" className="px-3 py-3">Loan ID</th>
                  <th scope="col" className="px-3 py-3">NBFC</th>
                  {[
                    "Amount lent",
                    "Recorded interest",
                    "Total before repayments",
                    "Repaid",
                    "Outstanding",
                  ].map(label => (
                    <th
                      key={label}
                      scope="col"
                      className="px-3 py-3 text-right"
                    >
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loans.map(loan => (
                  <tr
                    key={loan.loanId}
                    className="border-b border-slate-100"
                  >
                    <td className="px-3 py-3">{loan.loanId}</td>
                    <td className="px-3 py-3">{loan.nbfcName}</td>
                    {[
                      loan.amountLent,
                      loan.interestAmount,
                      loan.totalPayable,
                      loan.amountRepaid,
                      loan.outstandingAmount,
                    ].map((amount, index) => (
                      <td
                        key={index}
                        className="px-3 py-3 text-right tabular-nums"
                      >
                        {formatMoney(amount)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div>
        <h4 className="font-semibold">Company revenue</h4>
        <p className="mt-1 text-sm text-slate-500">
          Income before expenses. Select one reporting period at a time.
        </p>

        {periods.length > 0 && (
          <label className="mt-3 block text-sm font-medium">
            Revenue period
            <select
              value={periodKey}
              onChange={event => setPeriodKey(event.target.value)}
              className="mt-2 block w-full rounded-lg border border-slate-300 p-2"
            >
              {periods.map(period => (
                <option key={period.key} value={period.key}>
                  {period.start} to {period.end}
                </option>
              ))}
            </select>
          </label>
        )}

        {revenues.length === 0 ? (
          <p className="mt-3">
            No revenue reference for this selection.
          </p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full whitespace-nowrap text-sm">
              <caption className="sr-only">
                Company revenue for the selected reporting period in INR rupees
              </caption>
              <thead>
                <tr className="border-b text-left">
                  <th scope="col" className="px-3 py-3">NBFC</th>
                  <th scope="col" className="px-3 py-3">Period start</th>
                  <th scope="col" className="px-3 py-3">Period end</th>
                  <th scope="col" className="px-3 py-3 text-right">
                    Revenue
                  </th>
                </tr>
              </thead>
              <tbody>
                {revenues.map(row => (
                  <tr
                    key={
                      row.nbfcId + "|" + row.periodStart + "|" + row.periodEnd
                    }
                    className="border-b border-slate-100"
                  >
                    <td className="px-3 py-3">
                      {companyNames.get(row.nbfcId)}
                    </td>
                    <td className="px-3 py-3">{row.periodStart}</td>
                    <td className="px-3 py-3">{row.periodEnd}</td>
                    <td className="px-3 py-3 text-right tabular-nums">
                      {formatMoney(row.revenueAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}