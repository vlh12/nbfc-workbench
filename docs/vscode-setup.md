# Run and debug in VS Code

> Current portfolio workflow (18 October 2026): see [Phase 0 implementation](phase0-implementation.md) for the 19/20-column upload, all-service startup, analytics, stress/review APIs, and outstanding verification limits. The older descriptions below cover the earlier upload/report slice and are superseded where the new guide differs.

Open the `nbfc-exposure-stress-alert-workbench` folder itself, not its parent ChatGPT mirror. The folder must contain `pom.xml`, `compose.yml`, `backend` and `frontend`.

## Native upload-to-dashboard run

Install the VS Code Extension Pack for Java, Java 21, Maven 3.9 or newer, Node.js 22.12 or newer and npm. Allow workspace trust if you trust the generated source.

1. Start Oracle `FREEPDB1` and Kafka.
2. Open the project root in VS Code.
3. Wait for Java to import all Maven modules.
4. Press `Ctrl+Shift+D`.
5. Select **Upload-to-dashboard backend**.
6. Press `F5`.

This original compound starts Access & Data on 8081, Exposure Analytics on 8082 and API Gateway on 8090. The environment in `.vscode/launch.json` enables `WORKBENCH_DEMO_ENABLED=true`, points at Oracle `FREEPDB1` and Kafka `localhost:9092`, and sets local database values. Do not run native processes alongside Compose equivalents.

Start the frontend separately:

~~~powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
~~~

Open `http://localhost:5173`, enter the demo, select Contributor and upload the sample. Switch to Analyst after processing.

## Debugging

Set breakpoints in `UploadController`, `UploadService`, `OutboxPublisher`, `ExposureConsumer` or `ExposureController`. Use integrated logs to distinguish validation, Oracle, Kafka and authorization failures. Restart the affected debug session after Java changes that cannot be hot-reloaded.

Risk Governance is not part of the original upload compound. Start `com.nbfc.riskgovernance.Application` separately on port 8084 for assessment review, follow-ups and audit history.

## Compose alternative

~~~powershell
if (!(Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
docker compose config --quiet
docker compose up --build -d
~~~

Use `docker compose ps` and `docker compose logs --tail 100 <service>` to inspect startup. Oracle initialization can take several minutes. Stop with `docker compose down`; keep named volumes unless intentionally resetting local data.

## Troubleshooting

- **Configuration missing:** reload the VS Code window after opening the project root.
- **Java class not found:** wait for Maven import and confirm Java 21 in `Java: Configure Java Runtime`.
- **401 response:** confirm `WORKBENCH_DEMO_ENABLED=true` and choose a role.
- **Port already allocated:** stop existing Java, Node or Docker processes.
- **Empty summary:** inspect Access & Data, Kafka and Exposure Analytics logs.
- **Old Keycloak page:** open `http://localhost:5173`; Keycloak is no longer part of this prototype.

Maven reactor tests and the frontend production build pass in the current workspace. Oracle migrations and end-to-end browser execution still need local verification.

