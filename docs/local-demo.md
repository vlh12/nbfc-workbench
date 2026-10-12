# Run the workbench locally without Keycloak

Keycloak was removed at the user's request. The local UI uses selectable Contributor, Analyst or Supervisor demo roles. No login server or password is required, but the selector is caller-controlled and must never be used as production authentication.

## Prerequisites

- VS Code with the Extension Pack for Java.
- Java 21 and Maven 3.9 or newer for native backend startup.
- Node.js 22.12 or newer and npm for the frontend.
- Oracle `FREEPDB1` and Kafka locally, or Docker Desktop for Compose mode.
- Synthetic data only.

## Original native startup

1. Open the `nbfc-exposure-stress-alert-workbench` folder containing `pom.xml`.
2. Run `mvn clean install` from the project root.
3. In VS Code, open Run and Debug with `Ctrl+Shift+D`.
4. Select **Upload-to-dashboard backend** and press `F5`. This starts Access & Data on 8081, Exposure Analytics on 8082 and Gateway on 8090.
5. In a second terminal:

~~~powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
~~~

6. Open http://localhost:5173, choose **Enter demo**, select Contributor and upload the sample CSV.
7. Switch to Analyst after Kafka processing.

Risk Governance runs separately on port 8084 when its Spring Boot application is started. The original upload gateway does not route `/api/governance/**`; use the direct service during governance development or add that route as a deliberate deployment change.

## Compose startup

~~~powershell
if (!(Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
docker compose config --quiet
docker compose up --build -d
docker compose ps
~~~

Oracle may take several minutes to become healthy. Stop with `docker compose down`; do not add `-v` unless you intend to delete local Oracle and Kafka volumes.

## Expected sample result

The bundled synthetic sample contains three exposure records. After processing, the dashboard should show INR 2,500,000.00, two NBFCs and three records. Re-uploading is additive because each upload receives a new batch ID.

## Troubleshooting

- `401`: confirm `WORKBENCH_DEMO_ENABLED=true` and select a demo role.
- Empty analyst totals: inspect Access & Data, Kafka and Exposure Analytics logs; processing is asynchronous.
- Oracle errors: check that `FREEPDB1` is open and the account can create tables and has quota.
- Port conflict: stop the old Java, Node or Docker process before restarting.
- Old Keycloak URL: open http://localhost:5173 directly; there is no Keycloak login flow.

