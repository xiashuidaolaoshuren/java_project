# Separate scheduled blocks from unplaced work in responses

`DailyPlanResponse` exposes `blocks` and `unplacedWork` rather than one flat nullable `items` record. A Scheduled block always has a positive clock interval; Unplaced work never has clock times and always has a reason. Scheduled work and non-work blocks are themselves discriminated variants. Persistence may remain one table with check constraints, but the domain and TypeScript API should not call something “timed” when it is off the clock or permit invalid nullable-field combinations.
