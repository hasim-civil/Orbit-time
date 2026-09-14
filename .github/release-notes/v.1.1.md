## Timesheet Daily History

**Every date appears in history.** The list was built from the attendance records, so any date without a check-in silently vanished — weekends, holidays, leave and absences all disappeared, leaving gaps. It is now built from the period's own calendar dates, with none skipped.

**Holiday, Leave and Absent statuses.** A past working day with no attendance, no leave and no holiday reads *Absent*. Leave days name their leave type; holidays name the holiday. Weekends are shown as such and are never reported as absent.

**Past Holiday/Leave changes appear immediately.** Adding or editing a holiday or leave — including for a date months back — re-labels that date in the Timesheet with no attendance record involved. Holiday and Leave override both the punch record and Absent; the hours worked on such a day are still shown alongside.

Two defects were behind this. Every data listener was ended permanently by its first error, and the repositories close their flow on *any* Firestore snapshot error — a moment offline was enough to stop the screen ever updating again. Separately, the month being rendered was whatever the attendance listener last reported, so a leave or holiday arriving mid-month-change rebuilt the previous month under the new month's heading. Attendance, leaves, holidays and the user's shift are now one pipeline, each source retrying instead of dying.

**Late and Overtime are visually distinct.** They previously shared the same amber. Late keeps amber; Overtime moves to the palette's purple accent. They are also decided from different things: Late from the check-in against the scheduled shift start, Overtime from the worked duration.

**Overtime is actual worked duration beyond 8 hours** — `max(0, worked − 8h)` — independent of the scheduled shift, and never from "checked out after the shift end", which would flag a late-starting full day and miss an early-starting long one.

| Worked | Overtime | Status |
|---|---|---|
| 10:00 → 18:00 = 8h | — | On time |
| 10:00 → 18:10 = 8h 10m | 10m | Overtime |
| 11:00 → 19:00 = 8h | — | On time |
| 11:00 → 19:10 = 8h 10m | 10m | Overtime |
| 11:00 → 19:30 = 8h 30m | 30m | Overtime |

**Duplicate Overtime text removed.** The time line repeated "· overtime" next to the Overtime status; it now reads `10:46 am → 7:00 pm` with the status shown once.

**Daily History reads newest first.** Today sits at the top, with older dates running below it.

**Progress bar shares the 8-hour baseline.** 4h fills half the bar, 8h fills it, and an overtime day fills it exactly rather than overflowing the card.

Also: a past holiday no longer triggers a "Missed attendance" notification, and a weekend punch is no longer reported as late against a weekday shift start.

The card layout, typography, spacing, navigation, background and colour palette are unchanged throughout.

### Verification

159 logic checks passing against the shipped functions — date continuity including leap February and month boundaries, status precedence, a full add-then-remove round trip for a past holiday and a past leave, every overtime example above at minute precision, progress bounds from zero to a 24-hour day, and the newest-first ordering being strictly descending with no dropped or duplicated dates. All CI builds on the branch green.
