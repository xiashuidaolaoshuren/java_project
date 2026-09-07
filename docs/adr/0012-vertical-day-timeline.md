# The day is a vertical time rail on both surfaces

`DailyPlanView` renders a proportional vertical rail on the dashboard aside and on plan detail. Duration is space, so a 20-minute tail next to a 50-minute session is visible at a glance, and the layout does not depend on horizontal pixels the aside does not have. A horizontal Gantt is the better fit for 1.4.0's multi-day view but collapses to an unlabelled bar at ~250px. A hybrid list-plus-sparkline would keep the old component and never read as a schedule.
