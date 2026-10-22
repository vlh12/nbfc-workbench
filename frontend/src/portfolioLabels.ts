type PortfolioIdentity = { id: string; name?: string | null; reportingDate?: string | null; status?: string };
export function portfolioName(portfolio: Pick<PortfolioIdentity, "name" | "reportingDate">): string {
  return portfolio.name?.trim() || (portfolio.reportingDate ? "Portfolio — " + portfolio.reportingDate : "Portfolio");
}
export function portfolioLabel(portfolio: PortfolioIdentity): string {
  return [portfolioName(portfolio), portfolio.reportingDate, portfolio.status].filter(Boolean).join(" · ");
}
export function portfolioDetails(portfolio: PortfolioIdentity): string {
  return portfolioName(portfolio);
}
