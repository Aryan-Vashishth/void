# Audits

Audit files are organized into three states:

- `backlog/` - audit requests not started yet
- `ongoing/` - audits in progress, open findings, or partially addressed items
- `fulfilled/` - audits fully satisfied by current architecture and implementation

## Promotion Rule

Move an audit into `fulfilled/` only when all audit criteria are verified as implemented and no blocking findings remain.

