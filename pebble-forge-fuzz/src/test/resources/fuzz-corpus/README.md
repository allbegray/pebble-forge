# Fuzz corpus

Reduced parser inputs kept as regressions. Files under `valid/` must parse and
evaluate with the fixture context in `FuzzCorpusTest`. Files under `invalid/`
must raise `PebbleException`.

No property run has found a real failure yet. When one does, shrink it with
`FailureMinimizer`, add the result here, and name the file after the property
that found it. During a run, the minimizer writes reproducers to
`build/fuzz-failures/<name>.txt`; move a reproducer here once its cause is
understood.
