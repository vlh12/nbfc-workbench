# Version-3 local login startup

The browser and all business services must use this same project folder. A version-2 frontend on 5173 cannot display AuthenticationEntry; version-2 backends do not provide version-3 login endpoints.

Use scripts/Start-LocalWorkbench.ps1 after packaging the backend. It refuses ports occupied by another project, starts Access & Data before dependent services, and stores logs in ignored .local-run/. Oracle and Kafka must already be running. Do not start duplicate VS Code service instances while these processes are running.

Gateway obtains its database password from the ignored .vscode/local.env file. All Java launch configurations now load this file and select their Maven project explicitly. Vite uses strictPort so a conflict cannot silently move the frontend to another port.

If the local account table is empty, run scripts/Initialize-Supervisor.ps1 in an interactive PowerShell terminal. It prompts privately for the first Supervisor username and password and restarts only this project's packaged Access & Data instance. It uses the existing bootstrap runner, which refuses to create a new setup identity after accounts exist and preserves existing passwords. The helper has been syntax-checked; account creation is intentionally left to the user to choose credentials locally. No account or password was created or changed during the repair.

Open http://localhost:5173/, choose Supervisor Login, enter the chosen credentials, then enter the verification code shown by the local prototype. Use User Management after signing in to create Contributor and Analyst accounts.

Validation on 2026-10-22: all three role login forms opened in a separate browser context with no runtime errors; frontend /api/auth/csrf returned 200 with a token; the frontend was verified as version 3; all six application ports listened. Frontend typecheck/build and the complete Maven test run passed. Password-based sign-in was not exercised against the empty local account database.
