# Fixed breaks and commitments are two aggregates, one scheduler concept

A fixed break is a recurring preference (lunch every planning day). A commitment is a one-off fact about a date (dentist on Thursday). They are persisted separately and flattened in stage 1 into one list of unavailable intervals. A single nullable-date entity would mix lifecycles on one CRUD surface. Transient commitments on the generate request would vanish on 1.3.0's re-plan.
