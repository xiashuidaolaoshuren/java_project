# Times are wall-clock; there is no timezone preference

Work windows, breaks, commitments, and block boundaries are `LocalTime`; plan dates and commitment dates are `LocalDate`. "09:00" means nine in the morning for that planning date, wherever the user is. The client already supplies `planDate` (1.1.0); 1.3.0 can supply "now" the same way. An IANA timezone on the user would only help the server infer "today." UTC instants would make "09:00" ambiguous across DST. Overnight spans are out of scope.
