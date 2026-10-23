# Decisions

- Preserve the existing single-module architecture, application ID, Room schema, offline boundary, and brand system unless a confirmed defect requires a targeted change.
- Treat `README.md`/docs and screenshots as inventory leads, not proof of current runtime behavior.
- Keep deterministic analytics as the source of financial facts; do not add cloud AI/network dependencies or fabricate data.
- Split implementation by non-overlapping ownership and route routine discovery/testing to MiMo-Flash, coherent cross-module fixes to LongCat, and reserve independent reviewer scrutiny for money/privacy/destructive/data-integrity risks.
- Treat PIN-attempt throttling/backoff as a separate security slice: do not mix it into copy/layout edits, and require independent security review before calling lockout behavior fixed.
- Treat CSV formula-prefix neutralization as export safety, not cosmetic copy; preserve ordinary CSV interoperability and add executable regression coverage.
