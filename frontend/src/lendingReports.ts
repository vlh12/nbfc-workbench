import { api } from "./auth";

export type LendingReportSummary = {
    id: string;
    referenceId: string;
    bankId: string;
    nbfcId: string;
    nbfcName: string;
    reportingDate: string;
    rowCount: number;
    status: string;
    revenuePeriodStart: string;
    revenuePeriodEnd: string;
    revenueAmount: string;
    currency: string;
    unit: string;
    originalFilename: string;
    createdAt: string;
};

export type LendingReportLoan = {
    loanId: string;
    amountLent: string;
    interestAmount: string;
    totalPayable: string;
    amountRepaid: string;
    outstandingAmount: string;
};

export type LendingReportDetail = {
    report: LendingReportSummary;
    loans: LendingReportLoan[];
};

export type LendingReportPage = {
    content: LendingReportSummary[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
};

export function loadReviewerReports(
    page: number,
    size: number,
    signal?: AbortSignal,
): Promise<LendingReportPage> {
    const parameters = new URLSearchParams({
        page: String(page),
        size: String(size),
    });

    return api<LendingReportPage>(
        "/api/lending-reports/review?" + parameters.toString(),
        { signal },
    );
}

export function loadReviewerReport(
    reportId: string,
    signal?: AbortSignal,
): Promise<LendingReportDetail> {
    return api<LendingReportDetail>(
        "/api/lending-reports/review/" + encodeURIComponent(reportId),
        { signal },
    );
}