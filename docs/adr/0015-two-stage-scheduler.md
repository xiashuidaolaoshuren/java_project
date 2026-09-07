# The scheduler is two pure stages: free intervals, then placement

Stage 1 subtracts fixed breaks, commitments, and the trailing buffer from the work window. Stage 2 walks the ranking and fills those intervals, inserting cadence breaks and splitting sessions. Both stages are immutable records in `com.focusflow.schedule` with no Spring or JPA types, so a failing test names which job broke. A single-pass clock walk couples availability bugs to placement bugs. A slot grid would quantize every boundary to the slot size. 1.3.0's mid-day re-plan reuses stage 1 with a shrunk window.
