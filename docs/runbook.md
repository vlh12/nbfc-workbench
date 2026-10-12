# Local development runbook

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

