# Split long tasks at cadence; absorb short tails

A focus cadence only matters if a task longer than one stretch becomes several sessions with breaks between them. The scheduler splits at `maxFocusMinutes` and will not emit a non-final session shorter than `minSessionMinutes`; that tail is absorbed into the previous session of the same task in the same free interval, which may slightly exceed the cadence maximum. Never splitting would let a three-hour task run unbroken. Splitting with no minimum would scatter three-minute fragments on the rail.
