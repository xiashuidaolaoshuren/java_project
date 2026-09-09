# Snapshot task data in plan work entries

A saved work entry stores immutable `sourceTaskId`, title, priority, status, and estimate-at-generation, plus a nullable `taskReferenceId` foreign key with `ON DELETE SET NULL`. Session grouping and warning identity use the immutable source id; the live reference only enables navigation while the Task exists. Keeping only a live Task would make edits rewrite history and deletion fail, while cascading deletion would punch holes in schedules and destroy 1.3.0 actuals.
