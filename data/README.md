# Local synthetic data workspace

All fixtures here are fictional. No source proposal/Q&A, real bank records, personal data, secrets or copies of restricted originals belong here.

- raw/ contains unchanged copies of existing synthetic repository samples for current-format regression/reference. Raw means input fixture, not a place for real uploads.
- synthetic/ contains proposed exposure EAD-v1. It is documentation/test-design input and is not accepted by current APIs. Header/limits remain recommendations pending DEC-02/03.
- processed/ contains independently calculated expected outputs. These are oracle fixtures, not output from a running application.

The candidate EAD fixture uses bank/date scope with multiple NBFCs as an illustration of one option. If bank/NBFC/date scope is chosen, split it by NBFC. No scope decision is implied. Schema version is kept in fixture metadata/filename rather than adding an unapproved CSV column. Run the local checker described in skills/verify-workspace/SKILL.md after changing fixtures.
