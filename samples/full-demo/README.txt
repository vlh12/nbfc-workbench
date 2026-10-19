NBFC FULL SYNTHETIC DEMO PACK

Open data-entry-guide.html first.

PowerShell 7, all five backend services and Oracle must be running.
Default validation: pwsh -File .\Import-Demo.ps1
Populate synthetic demo: pwsh -File .\Import-Demo.ps1 -Import

Only portfolios/*.csv are portfolio uploads. Supporting CSVs are form-entry references.
All EAD values are explicit fictional assumptions, not derived from outstanding.
The importer uses local demo roles and appends records without deleting existing data.
Use import-state.json to identify the created versions. Existing dashboard counts may differ.
No client data or database credentials are included.
