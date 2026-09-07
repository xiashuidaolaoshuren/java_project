# Buffer is a trailing reserved slice of the work window

Buffer time is the last N minutes of the work window, subtracted in stage 1 and rendered as a distinct `BUFFER` block so contingency is visible rather than an invisible shorter day. Per-block padding would multiply blocks and fragment the rail. Cutting buffer until 1.3.0 was tempting, but a persisted preference and a block kind now mean 1.3.0's overrun handling consumes something the user can already see.
