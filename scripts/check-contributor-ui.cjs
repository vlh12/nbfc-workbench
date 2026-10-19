const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE_PATH || "playwright");

(async () => {
  const csv = await fs.readFile(path.join(__dirname, "../frontend/public/portfolio-loans-19.csv"), "utf8");
  const lines = csv.trim().split(/\r?\n/);
  const loans = lines.slice(1).map(line => {
    const v = line.split(",");
    return { loanId: v[2], nbfcId: v[1], amountLent: v[5], outstandingAmount: v[9], eadAmount: null, dpdDays: Number(v[10]), loanStatus: v[11], instrumentType: v[4], collateralType: v[13], revenueAmount: v[16], revenuePeriodStart: v[14], revenuePeriodEnd: v[15] };
  });
  const warnings = [{ line: 1, field: "ead_amount", message: "No explicit EAD supplied. Review is available; stress calculations are blocked." }];
  const snapshot = { id: "synthetic-portfolio-1", bankId: "SYN-BANK-001", reportingDate: "2026-09-30", status: "VALIDATED", rows: loans, warnings };
  let saved = false;
  const legacyRequests = [];
  const browser = await chromium.launch({ executablePath: process.env.UI_CHROME_PATH, headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    await context.addInitScript(() => sessionStorage.setItem("workbench-demo-role", "CONTRIBUTOR"));
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", e => errors.push(e.message));
    await page.route("**/api/**", async route => {
      const request = route.request();
      const url = new URL(request.url());
      let body = [];
      if (url.pathname === "/api/portfolios/validate") body = { rows: loans, warnings, errors: [] };
      else if (url.pathname === "/api/portfolios" && request.method() === "POST") { saved = true; body = snapshot; }
      else if (url.pathname === "/api/portfolios") body = saved ? [{ id: snapshot.id, kind: "PORTFOLIO", reportingDate: snapshot.reportingDate, status: snapshot.status, createdAt: "2026-10-18T12:00:00Z", rowCount: 16 }] : [];
      else if (url.pathname === "/api/portfolios/" + snapshot.id) body = snapshot;
      else legacyRequests.push(url.pathname);
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
    });
    await page.goto(process.env.UI_TEST_URL || "http://localhost:5173", { waitUntil: "networkidle" });
    assert.equal(await page.getByRole("heading", { name: "Upload your loan portfolio", exact: true }).isVisible(), true);
    assert.equal(await page.getByRole("heading", { name: "Bank capital", exact: true }).isVisible(), false);
    assert.equal(await page.getByRole("heading", { name: "Saved portfolios", exact: true }).isVisible(), false);
    assert.equal(await page.getByRole("heading", { name: "Upload synthetic exposures", exact: true }).isVisible(), false);
    assert.deepEqual(legacyRequests, [], "Collapsed legacy uploads must not fetch data on initial load");
    const output = process.env.UI_SCREENSHOT_DIR;
    if (output) { await fs.mkdir(output, { recursive: true }); await page.screenshot({ path: path.join(output, "contributor-upload-desktop.png"), fullPage: true, animations: "disabled" }); await page.setViewportSize({ width: 390, height: 844 }); await page.screenshot({ path: path.join(output, "contributor-upload-mobile.png"), fullPage: true, animations: "disabled" }); await page.setViewportSize({ width: 1440, height: 1000 }); }

    await page.getByRole("button", { name: "Supporting data", exact: true }).click();
    assert.equal(await page.getByRole("heading", { name: "Bank capital", exact: true }).isVisible(), true);
    assert.equal(await page.getByRole("heading", { name: "NBFC asset quality", exact: true }).isVisible(), false);
    await page.getByRole("button", { name: "NBFC asset quality", exact: true }).click();
    assert.equal(await page.getByRole("heading", { name: "NBFC asset quality", exact: true }).isVisible(), true);
    assert.equal(await page.getByRole("heading", { name: "Bank capital", exact: true }).isVisible(), false);

    await page.getByRole("button", { name: "Upload portfolio", exact: true }).click();
    const picker = page.locator('input[type="file"]').first();
    await picker.setInputFiles(path.join(__dirname, "../frontend/public/portfolio-loans-19.csv"));
    await page.getByRole("button", { name: "Saved portfolios", exact: true }).click();
    await page.getByRole("button", { name: "Upload portfolio", exact: true }).click();
    assert.equal(await picker.evaluate(el => el.files.length), 1, "Navigation must retain the chosen file");
    await page.getByRole("checkbox", { name: "I confirm this file contains synthetic data only." }).check();
    await page.getByRole("button", { name: "Validate file", exact: true }).click();
    await page.getByRole("heading", { name: "Ready to save with warnings", exact: true }).waitFor();
    await page.getByRole("button", { name: "Save portfolio", exact: true }).click();
    await page.getByRole("table", { name: "Reported bank loan values from this immutable portfolio" }).waitFor();
    assert.equal(await page.getByRole("button", { name: /Saved portfolios/ }).getAttribute("aria-pressed"), "true");
    assert.equal(await page.locator("tbody tr:visible").count(), 16);

    await page.getByRole("button", { name: "Upload portfolio", exact: true }).click();
    await page.setViewportSize({ width: 390, height: 844 });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true, "Mobile layout must not overflow");

    assert.deepEqual(errors, [], "No browser runtime errors");
    console.log("PASS: default simplicity, supporting-data navigation, preserved file selection, validate/save journey, 16 saved rows, mobile width, and browser error checks (synthetic mocked API).");
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
