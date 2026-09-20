# Failure playbook
- API unavailable/offline: keep last cache when implemented; mark stale and show timestamp.
- Missing quote: SEM DADOS, never zero.
- Old quote: confidence penalty; stale quote excluded from Next Best Action.
- Extreme ROI: require freshness/liquidity validation before recommendation.
- Partial/disappeared order: warn that market orders can change before execution.
- Black Market missing data: no fabricated opportunity.
- Android background restrictions: local refresh is best-effort; future push backend for time-critical alerts.
- Tax/fee changes: keep configurable/versioned; never scatter constants across UI.
