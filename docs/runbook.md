# Local development runbook

> Current portfolio workflow (18 October 2026): see [Phase 0 implementation](phase0-implementation.md) for the 19/20-column upload, all-service startup, analytics, stress/review APIs, and outstanding verification limits. The older descriptions below cover the earlier upload/report slice and are superseded where the new guide differs.

## Services and ports

| Component | Port | Start method |
|---|---:|---|
| Frontend | 5173 | `npm.cmd run dev` in `frontend` |
| Access & Data | 8081 | VS Code compound or Spring Boot |
| Exposure Analytics | 8082 | VS Code compound or Spring Boot |
| Stress Simulation | 8083 | Optional scaffold process |
| Risk Governance | 8084 | Separate Spring Boot process |
| API Gateway | 8090 | VS Code compound or Spring Boot |
| Oracle | 1521 | Local Oracle or Compose |
| Kafka | 9092 | Local Kafka or Compose |

## Native startup

~~~powershell
mvn clean install
~~~

Open Run and Debug in VS Code, select **Upload-to-dashboard backend** and press `F5`. Start the frontend separately:

~~~powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
~~~

The original compound starts Access & Data, Exposure Analytics and Gateway. Start Risk Governance separately when testing assessment review. The frontend proxy points `/api` to `http://localhost:8090`, so governance calls require a deliberate gateway route or separate development proxy configuration.

## Compose startup and shutdown

~~~powershell
if (!(Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail 100 oracle kafka access-data exposure-analytics gateway frontend
~~~

Stop without deleting data:

~~~powershell
docker compose down
~~~

Use `docker compose down -v` only when intentionally deleting Oracle and Kafka volumes.

## Functional smoke test

1. Open http://localhost:5173 and enter the local demo.
2. Select Contributor and download the bundled synthetic CSV.
3. Confirm synthetic data and upload the file.
4. Select Analyst and wait for Kafka processing.
5. Confirm INR 2,500,000.00 across two NBFCs and three records.
6. If Risk Governance is running, submit the validated batch, switch to Supervisor, approve or return it, create a follow-up and inspect audit history.

## Useful checks

~~~powershell
mvn test
Set-Location frontend
npm.cmd run typecheck
npm.cmd run build
~~~

Health endpoints are available at `/actuator/health` on each Spring Boot service. Manual authorization checks should confirm contributors cannot read summaries, analysts cannot upload and unknown roles receive 401.

Kafka delivery is at least once. The processed-event marker prevents duplicate totals, but failed or malformed events require operator investigation. Demo credentials are local development values; replace them with secrets and real identity controls before deployment.


## Swagger UI and OpenAPI (20 October 2026)

Restart **All workbench services** from VS Code after Maven imports the updated dependencies. Swagger uses Springdoc 2.8.17, pinned for the existing Spring Boot 3.5 / Spring MVC stack. Documentation is enabled by WORKBENCH_DEMO_ENABLED=true (already present in the VS Code launch configurations); it is disabled when demo mode is off. Business APIs still enforce their existing role checks.

| Service | Swagger UI | OpenAPI JSON |
|---|---|---|
| Access & Data | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |
| Exposure Analytics | http://localhost:8082/swagger-ui/index.html | http://localhost:8082/v3/api-docs |
| Stress Simulation | http://localhost:8083/swagger-ui/index.html | http://localhost:8083/v3/api-docs |
| Risk Governance | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |
| API Gateway selector | http://localhost:8090/swagger-ui/index.html | Four proxied specifications, listed below |

The gateway UI has an API-definition selector for the four business services. It fetches /openapi/access-data, /openapi/exposure-analytics, /openapi/stress-simulation and /openapi/risk-governance from the gateway origin. Each proxy reads that service's /v3/api-docs; the service must be running. Relative server URL / keeps Try it out on the current origin, so gateway requests use the existing /api routes and direct-service requests stay on that service. No browser CORS workaround is needed. Gateway /v3/api-docs describes the gateway itself and is not a combined controller catalogue; use the selector for business APIs.

Open a page, click **Authorize**, enter CONTRIBUTOR, ANALYST or SUPERVISOR, then click **Authorize** and close the dialog. This sends the existing X-Demo-Role header when executing API requests. Each operation describes its allowed demo roles. Use Contributor for uploads/capital/asset-quality inputs, Analyst for stress execution/submission, and Supervisor for thresholds/reviews/actions. API documentation is readable without a role in demo mode; this does not make business requests anonymous. Authorization is not persisted across browser refreshes.

Swagger's **Try it out → Execute** calls the real application. POST/PATCH may create or change synthetic records. Use the generated request schemas and existing synthetic CSVs; multipart portfolio saves accept file, synthetic=true and the contributor's name. Generated specs include legacy backend endpoints that remain implemented even where their frontend sections were removed. Standard /swagger-ui.html redirects to the UI; /v3/api-docs.yaml is also available.

Verification: real controller OpenAPI schemas and UI assets are tested for all four business services using mocked business dependencies; demo-off documentation access is denied. The gateway integration test starts a real HTTP gateway and four local specification stubs, verifies all four same-origin proxy routes and checks anonymous business API denial. These tests do not write to Oracle or require Kafka. Springdoc compatibility reference: https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot


## Redesigned frontend workspace (20 October 2026)

Run the existing frontend and backends as before, then refresh http://localhost:5173/. Analyst and supervisor roles open the dashboard; contributors open Data Management. Select the reporting period and named portfolio above the content. Sidebar links open each workflow; All tools displays the preserved full workbench. The mobile menu button opens navigation on narrow screens.

Use Export Report for a local HTML report of the selected saved portfolio and loaded scenario evidence; open it in a browser and print to PDF if needed. Downloads & Reports also offers JSON evidence and CSV analysis exports. The CSV exports are not upload templates. Bank scope remains SYN-BANK-001. Saved alert badges use recorded scenario thresholds; no concentration threshold from the reference image was introduced.

The synthetic browser regression scripts are check-contributor-ui.cjs, check-dashboard-ui.cjs, check-analysis-ui.cjs, check-risk-exploration-ui.cjs and check-workspace-ui.cjs under scripts/. They require the frontend on UI_TEST_URL (default localhost:5173), Playwright via PLAYWRIGHT_MODULE_PATH and a Chromium executable via UI_CHROME_PATH. They mock API traffic. The workspace suite saves desktop/mobile previews to UI_SCREENSHOT_DIR or the parent outputs/ui-redesign folder.
