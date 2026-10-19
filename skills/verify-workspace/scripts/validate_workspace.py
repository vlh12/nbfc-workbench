"""Local agent-documentation checks. No network; not product-test evidence."""
from pathlib import Path
from decimal import Decimal
from datetime import date
import csv
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
ERRORS = []

def check(ok, message):
    if not ok:
        ERRORS.append(message)

def load_csv(relative):
    with (ROOT / relative).open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))

def main():
    required = ["AGENTS.md", "README.md", ".github/copilot-instructions.md", ".agent/AGENT.md",
        ".agent/verification.md", "data/README.md", "data/raw/README.md", "data/processed/README.md",
        "memory/MEMORY.md", "memory/2026-10-16.md"]
    required += [f"context/{name}.md" for name in ["SOUL", "USER", "OPS", "PROJECT",
        "REQUIREMENTS", "ARCHITECTURE", "USER-FLOWS", "DATA-CONTRACTS", "DECISIONS"]]
    required += [f"prompts/{name}.md" for name in ["master-implementation", "bootstrap",
        "contributor", "analyst", "supervisor", "testing"]]
    required += [f".agent/commands/{name}.md" for name in ["bootstrap", "contributor", "analyst",
        "supervisor", "testing", "implement"]]
    required += [f"evals/{name}.md" for name in ["acceptance-tests", "financial-test-cases", "failure-scenarios"]]
    required += [f"skills/{name}/SKILL.md" for name in ["reconcile-requirements", "implement-ingestion",
        "implement-analytics", "implement-governance", "verify-workspace"]]
    required += ["data/raw/legacy-exposures.csv", "data/raw/lending-report-demo.csv",
        "data/synthetic/exposures-ead-v1.proposed.csv", "data/processed/exposures-ead-v1.expected.json",
        "data/processed/lending-report-demo.expected.json"]
    for relative in required:
        check((ROOT / relative).is_file(), f"Missing file: {relative}")
    if ERRORS:
        return
    # Scan only maintained workspace docs and their root entry links; no restricted sources.
    docs = [ROOT / name for name in required if name.endswith(".md")]
    for document in docs:
        text = document.read_text(encoding="utf-8")
        for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", text):
            if re.match(r"[a-zA-Z][a-zA-Z0-9+.-]*:", target) or target.startswith("#"):
                continue
            local = target.split("#", 1)[0].strip("<>")
            check((document.parent / local).exists(), f"Broken link: {document.relative_to(ROOT)} -> {target}")
    for skill in (ROOT / "skills").glob("*/SKILL.md"):
        text = skill.read_text(encoding="utf-8")
        check(text.startswith("---\n"), f"Missing frontmatter: {skill}")
        front = text.split("---", 2)[1]
        name = re.search(r"^name: (.+)$", front, re.M)
        description = re.search(r"^description: (.+)$", front, re.M)
        check(bool(name) and name.group(1) == skill.parent.name, f"Skill name mismatch: {skill}")
        check(bool(description) and bool(description.group(1).strip()), f"Missing skill description: {skill}")
    raw_legacy = ROOT / "data/raw/legacy-exposures.csv"
    raw_report = ROOT / "data/raw/lending-report-demo.csv"
    check(raw_legacy.read_bytes() == (ROOT / "samples/synthetic-exposures.csv").read_bytes(), "Legacy fixture copy changed")
    check(raw_report.read_bytes() == (ROOT / "frontend/public/lending-report-demo.csv").read_bytes(), "Report fixture copy changed")
    rows = load_csv("data/synthetic/exposures-ead-v1.proposed.csv")
    header = ["bank_id", "nbfc_id", "exposure_id", "reporting_date", "ead_amount", "currency", "unit"]
    check(bool(rows) and list(rows[0]) == header, "Wrong candidate header")
    totals = {}
    keys = set()
    for row in rows:
        for field in ["bank_id", "nbfc_id", "exposure_id"]:
            limit = 64 if field == "exposure_id" else 44
            value = row[field]
            check(bool(re.fullmatch(r"SYN-[A-Z0-9-]+", value)) and len(value) <= limit, f"Invalid synthetic {field}")
        check(row["currency"] == "INR" and row["unit"] == "RUPEE", "Candidate units changed")
        check(bool(re.fullmatch(r"[0-9]{1,16}(\.[0-9]{1,2})?", row["ead_amount"])), "Invalid candidate decimal")
        check(date.fromisoformat(row["reporting_date"]).isoformat() == row["reporting_date"], "Invalid candidate date")
        key = (row["bank_id"], row["reporting_date"], row["exposure_id"])
        check(key not in keys, "Duplicate candidate exposure key")
        keys.add(key)
        totals[row["nbfc_id"]] = totals.get(row["nbfc_id"], Decimal("0")) + Decimal(row["ead_amount"])
    expected = json.loads((ROOT / "data/processed/exposures-ead-v1.expected.json").read_text())
    check(len(rows) == expected["row_count"], "Candidate row count mismatch")
    check({row["bank_id"] for row in rows} == {expected["bank_id"]}, "Candidate bank mismatch")
    check({row["reporting_date"] for row in rows} == {expected["reporting_date"]}, "Candidate date mismatch")
    check({key: f"{value:.2f}" for key, value in totals.items()} == expected["ead_by_nbfc"], "NBFC EAD totals mismatch")
    check(f"{sum(totals.values(), Decimal('0')):.2f}" == expected["total_ead_amount"], "Bank EAD total mismatch")
    check(Decimal("0.10") + Decimal("0.20") == Decimal("0.30"), "Decimal arithmetic mismatch")
    report = load_csv("data/raw/lending-report-demo.csv")
    report_expected = json.loads((ROOT / "data/processed/lending-report-demo.expected.json").read_text())
    check(len(report) == report_expected["row_count"], "Report row count mismatch")
    for row in report:
        check(Decimal(row["amount_lent"]) + Decimal(row["interest_amount"]) == Decimal(row["total_payable"]), "Report payable mismatch")
        check(Decimal(row["total_payable"]) - Decimal(row["amount_repaid"]) == Decimal(row["outstanding_amount"]), "Report outstanding mismatch")
        check(row["currency"] == "INR" and row["unit"] == "RUPEE", "Report unit mismatch")
        check(Decimal(row["revenue_amount"]) == Decimal(report_expected["revenue_amount_once"]), "Report revenue mismatch")
    for field in ["amount_lent", "interest_amount", "total_payable", "amount_repaid", "outstanding_amount"]:
        check(sum((Decimal(row[field]) for row in report), Decimal("0")) == Decimal(report_expected[field]), f"Report total mismatch: {field}")
    if not ERRORS:
        print(f"PASS: {len(required)} required files, relative links, 5 skill entrypoints, fixture copies and exact arithmetic.")
        print("Scope: workspace documentation only; candidate EAD schema remains proposed; application not tested.")

if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, ArithmeticError, IndexError) as error:
        ERRORS.append(f"Cannot validate workspace: {error}")
    if ERRORS:
        for error in ERRORS:
            print("FAIL:", error, file=sys.stderr)
        sys.exit(1)
