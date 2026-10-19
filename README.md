# NBFC Exposure Stress & Alert Workbench

Synthetic-data prototype for collecting bank-to-NBFC lending reports and reviewing saved loan figures.

## Current handover

Branch: `feature/saved-report-review`

Working flow:

Contributor selects bank reference → uploads CSV → validates → explicitly saves report → Analyst or Supervisor selects the saved report → views saved figures and loan graphs.

Company revenue appears as an exact amount with its start/end period. The current implementation has no revenue chart or growth calculation.

## Implemented features

### Contributor

- View independently stored bank reference loans and company revenue records.
- Upload one NBFC's complete loan set for a selected reporting date.
- Validate loan figures, reporting date, revenue period and revenue amount.
- Reject incorrect values, missing loans and duplicate loans with correction messages.
- Explicitly save a successfully validated report.
- Revalidate the uploaded file on the server before saving.
- Save the report and its loan rows in one transaction.
- Show the new Report ID and prevent another save of the current upload.
- Refresh saved report history automatically.
- Open saved reports and inspect their saved figures.

### Analyst and Supervisor

- Browse saved validated reports for the current demo bank.
- Navigate a paginated report list.
- Select one saved Report ID and view its details.
- Display a grouped loan bar chart with checkbox controls for amount lent, recorded interest, total payable, amount repaid and outstanding amount.
- Show exact rupee amounts in tooltips and the supporting table.
- Display company revenue once for the selected report.
- Handle loading, empty and error states.

## Technology versions

Preserve the versions declared in the existing Maven and frontend package files.

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.7 |
| Spring Cloud | 2025.0.0 |
| React | 19.1.1 |
| TypeScript | 5.9.2 |
| Vite | 7.1.7 |
| Tailwind CSS | 4.1.13 |
| Recharts | 3.2.1 |
| Database | Oracle Database 26ai Free |
| Backend build | Maven |
| Frontend package manager | npm |

Use a Node.js version compatible with Vite 7: Node 20.19+ within the 20.x line, or Node 22.12+.

The local development environment uses Windows 11, Spring Tool Suite and SQL Developer.

## Service boundaries

| Service | Port | Responsibility |
|---|---:|---|
| Access & Data | 8081 | Bank references, report validation, saved reports and reviewer reads |
| Exposure Analytics | 8082 | Separate older batch/Kafka exposure projection |
| Stress Simulation | 8083 | Existing boundary for future stress functionality |
| Risk Governance | 8084 | Separate governance prototype |
| Gateway | 8090 | Routes API requests to the owning service |
| Frontend | 5173 | React application |

The saved-report feature requires Oracle, Access & Data, Gateway and the frontend.

Kafka can remain stopped for this feature. The older `/api/batches` and exposure projection flow remains separate.

Existing exposure/governance panels may show unavailable messages when their services are stopped. Use the **Saved lending reports** section to review the current feature.

## Get the code

After the handover branch has been pushed:

```cmd
git clone --branch feature/saved-report-review https://github.com/vlh12/nbfc-workbench.git
cd nbfc-workbench
```

For an existing clone without a local copy of this branch, first finish or preserve any local work, then run:

```cmd
git fetch origin
git switch -c feature/saved-report-review --track origin/feature/saved-report-review
```

## Local setup

### 1. Prepare prerequisites

Install or configure:

- Java 21 and Maven.
- Compatible Node.js and npm.
- Spring Tool Suite.
- A running Oracle Database instance and listener.
- An application schema with the privileges and tablespace quota needed by Flyway.
- SQL Developer for inspecting the database.

SQL Developer is a database client. Opening it does not start Oracle.

### 2. Import the backend into STS

1. Choose **File → Import → Maven → Existing Maven Projects**.
2. Select the repository root containing the parent `pom.xml`.
3. Import the Maven modules.
4. Configure Java 21 for the projects and run configurations.

Run existing backend checks from the repository root:

```cmd
mvn.cmd test
```

### 3. Configure Access & Data

Open the Access & Data run configuration in STS.

Under **Environment**, add these variables using your own Oracle connection details:

```text
WORKBENCH_DEMO_ENABLED=true
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
SPRING_DATASOURCE_USERNAME=YOUR_ORACLE_USERNAME
SPRING_DATASOURCE_PASSWORD=YOUR_ORACLE_PASSWORD
```

Each line represents a separate environment variable. Replace the URL if your Oracle address or service name differs.

Keep personal credentials in local run settings.

### 4. Configure Gateway

In the Gateway run configuration, add:

```text
WORKBENCH_DEMO_ENABLED=true
```

Demo mode must be enabled in both Gateway and Access & Data.

The `X-Demo-Role` mechanism is for local demonstration. Full authentication and bank identity management are outside this handover.

### 5. Start the backend

Start these main classes in STS:

| Module | Main class |
|---|---|
| Access & Data | `com.nbfc.accessdata.Application` |
| Gateway | `com.nbfc.gateway.Application` |

Confirm Access & Data starts on port 8081 and Gateway on port 8090.

Access & Data uses Flyway migrations V1 through V4 for its tables and reference seed data. Preserve already-applied migrations. Future schema changes must use a new migration.

### 6. Start the frontend

From the repository root:

```cmd
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open `http://127.0.0.1:5173`.

Use `npm.cmd` on Windows when PowerShell blocks `npm.ps1`.

## Populate a fresh developer database

Git transfers source files; it does not transfer another developer's Oracle records.

Migrations seed the reference records. A fresh database starts without the original developer's saved report history.

1. Enter the demo as **Contributor**.
2. Select the synthetic bank reference dated `2026-09-30`.
3. Download the lending-report sample, or use `frontend/public/lending-report-demo.csv`.
4. Select the CSV and confirm it contains synthetic data.
5. Click **Validate report**.
6. After successful validation, click **Save report**.
7. Note the returned Report ID.
8. Switch to **Analyst** or **Supervisor**.
9. Open that report under **Saved lending reports**.

The NBFC A sample contains three loans and quarter revenue of INR 180,000,000.

Other supplied samples include six-month and annual revenue periods, plus invalid files for correction testing.

## Saved-report APIs

Send frontend API requests through Gateway at `http://127.0.0.1:8090`.

Local demo requests use the `X-Demo-Role` header with `CONTRIBUTOR`, `ANALYST` or `SUPERVISOR`.

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/lending-reports/validate` | Contributor |
| POST | `/api/lending-reports` | Contributor |
| GET | `/api/lending-reports` | Contributor's own latest 50 reports |
| GET | `/api/lending-reports/{reportId}` | Contributor's own report |
| GET | `/api/lending-reports/review?page=0&size=20` | Analyst and Supervisor |
| GET | `/api/lending-reports/review/{reportId}` | Analyst and Supervisor |

Validation/save requests use multipart fields `referenceId`, `file` and `synthetic`.

Reviewer page numbers start at zero. Page size must be between 1 and 50.

Contributor reads retain bank and submitting-identity filters. Reviewer reads use bank scope across contributor submissions.

The current demo bank is fixed as `SYN-BANK-001`.

## Data and graph rules

- Use synthetic data only.
- One CSV contains one NBFC's complete loan set for the selected reporting date.
- Reporting date is an as-of date, not a loan disbursement date.
- Recorded interest is an existing monetary amount.
- Total payable = amount lent + recorded interest.
- Outstanding = total payable minus cumulative amount repaid.
- Currency is INR and monetary unit is RUPEE.
- Company revenue is income before expenses for an exact start/end period.
- Revenue repeats in CSV loan rows but is saved once on the report.
- Loan graphs read one saved Report ID and its saved loan rows.
- Repeated submissions remain separate selectable reports.
- Never sum report history as additional lending or replace saved figures with current reference values.
- Monetary API fields are decimal strings.
- Chart coordinates use INR crore; displayed amounts and tooltips retain the original exact values.
- Use grouped bars for the five loan figures because some figures already include others.
- The quarter, six-month and annual revenue samples have unequal durations and overlapping periods. They do not establish a valid growth trend.
- Future revenue comparisons must select one submission per NBFC and period and use equivalent periods. Contributor latest-50 history is not an exhaustive comparison dataset.

## Important source locations

| Location | Purpose |
|---|---|
| `backend/access-data/src/main/java/com/nbfc/accessdata/access/LendingReportController.java` | Contributor and reviewer endpoints |
| `backend/access-data/src/main/java/com/nbfc/accessdata/data/report/` | Saved report services, entities and repositories |
| `backend/access-data/src/main/java/com/nbfc/accessdata/data/reference/` | Bank loan and revenue references |
| `backend/access-data/src/main/resources/db/migration/` | Access & Data migrations |
| `frontend/src/lendingReports.ts` | Reviewer API functions and types |
| `frontend/src/components/LendingReportReview.tsx` | Paginated report list and selection |
| `frontend/src/components/LendingReportDetails.tsx` | Selected report details |
| `frontend/src/components/LendingLoanChart.tsx` | Loan chart and series controls |

## Verification at handover

The developer reported these checks successful on 16 October 2026:

- Frontend TypeScript checking and production build.
- Existing backend Maven tests.
- Analyst and Supervisor report list/detail access.
- Contributor denial on reviewer endpoints.
- Unauthenticated request rejection.
- Reviewer denial on contributor validation/save endpoints.
- Pagination and invalid page parameter handling.
- Invalid contributor report rejection.
- Valid report saving and history refresh.
- Opening the newly saved report under both reviewer roles.
- Loan chart display and checkbox controls.

The frontend production build reports a non-blocking bundle-size warning.

Additional automated reviewer bank-scoping tests, live Oracle cross-bank integration verification and a complete fresh-clone setup verification remain outstanding.

## Continue development

After checking out the handover branch and ensuring your working tree is clean, create your own development branch:

```cmd
git switch feature/saved-report-review
git switch -c feature/your-phase
```

Before changing business behavior, read `AGENTS.md` and the approved project requirements.

Preserve existing service boundaries, the working contributor flow, contributor-only validation/save actions, bank scoping, exact monetary handling, saved history, dependency versions and applied migrations.

Future stress calculations, alerts, approval workflows and governance integration require reviewed requirements. Existing legacy governance code is separate from the saved-report graph flow.

AI features and dependencies are outside the current scope.

## Related documentation

- [Project engineering rules](AGENTS.md)
- [Requirements status](docs/requirements-status.md)
- [Architecture](docs/architecture.md)

Some older documentation describes the separate batch/Kafka flow. This README describes the saved-report handover.

## AI agent workspace

Agent guidance was added on 16 October 2026 while preserving application code and the handover instructions above. Start at [AGENTS.md](AGENTS.md), [project evidence](context/PROJECT.md) and [decision status](context/DECISIONS.md). The Phase 0 proposal and client Q&A are unavailable in the current local sources folder; their analysis remains pending. Referenced conversation assistant replies are content-reference placeholders. Do not treat them as verified schemas or approvals.

Explicit ead_amount and INR rupees are approved targets. The candidate EAD CSV is proposed, not accepted by either current upload flow. Snapshot replacement, automatic dataset approval, bank-scoped RBAC, shared supervisor review and lead override are target requirements with recorded gaps. Current explicit saved-report Save and separate governance assessment decisions remain distinct.

| Workspace area | Purpose |
|---|---|
| [.agent/AGENT.md](.agent/AGENT.md) | Standing agent rules; commands/ contains six task recipes, not installed slash commands |
| [.github/copilot-instructions.md](.github/copilot-instructions.md) | Copilot entry point to the same canonical guidance |
| [Master implementation prompt](prompts/master-implementation.md) | Detailed reusable prompt for a tested incremental implementation |
| [Bootstrap](prompts/bootstrap.md), [Contributor](prompts/contributor.md), [Analyst](prompts/analyst.md), [Supervisor](prompts/supervisor.md), [Testing](prompts/testing.md) | Focused task prompts |
| [Requirements](context/REQUIREMENTS.md), [architecture](context/ARCHITECTURE.md), [flows](context/USER-FLOWS.md), [data contracts](context/DATA-CONTRACTS.md) | Approved goals, observed code, recommendations and unresolved choices |
| [SOUL](context/SOUL.md), [USER](context/USER.md), [OPS](context/OPS.md) | Agent role, user preferences and operating constraints |
| [Repository memory](memory/MEMORY.md), [dated log](memory/2026-10-16.md) | Local project notes, separate from global assistant memory |
| [Data workspace](data/README.md) | Current-format synthetic inputs, proposed EAD fixture and independent expected outputs |
| [Acceptance](evals/acceptance-tests.md), [financial cases](evals/financial-test-cases.md), [failure cases](evals/failure-scenarios.md) | Reusable specifications with explicit current/target/recommended status |
| [Verification](.agent/verification.md) | Checks executed for this workspace addition and limits |

Modular local skills: [reconcile requirements](skills/reconcile-requirements/SKILL.md), [ingestion](skills/implement-ingestion/SKILL.md), [analytics](skills/implement-analytics/SKILL.md), [governance](skills/implement-governance/SKILL.md), [workspace verification](skills/verify-workspace/SKILL.md). These are repository resources, not automatically installed global skills.

To use the master prompt, provide the next requested outcome, requirement IDs, authorized scope and observable acceptance result. Settle only decisions blocking that increment, preserve working flows, implement the vertical slice and run discovered checks. See OPS for manifest-backed command candidates; do not inherit earlier test-pass claims.

No hosted workflow or external source processing is introduced. Restricted originals stay local/read-only; no source excerpts, embeddings, secrets or real financial records may be sent externally. Candidate financial/architecture mechanics remain recommendations until human approval is recorded.

## Phase 0 portfolio workflow update

The contributor/analyst/supervisor portfolio flow now accepts the supplied 19-column multi-NBFC CSV and a compatible explicit-EAD template. See [Phase 0 implementation](docs/phase0-implementation.md) for controls, APIs, migration notes, acceptance results and remaining gaps. Choose **All workbench services** in VS Code to launch Access & Data, Exposure Analytics, Stress Simulation, Risk Governance and Gateway. The original upload compound is preserved. Live Oracle/browser validation remains pending; do not treat automated unit checks as deployment verification.
