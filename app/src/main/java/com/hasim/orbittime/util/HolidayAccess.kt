package com.hasim.orbittime.util

/**
 * Who may add, edit and delete the organization's holidays.
 *
 * This only decides what the app *shows* — the Add/Edit/Delete controls. The actual boundary is
 * `isHolidayManager()` in firestore.rules, which checks the same address on the verified ID
 * token, so a write from any other account is refused by Firestore whatever the app shows.
 * Keep the two addresses identical.
 */
object HolidayAccess {

    const val HOLIDAY_MANAGER_EMAIL = "hasim.radiant@gmail.com"

    /** Mirrors the rule: the manager's address *and* a verified email. Firebase stores emails
     * lower-cased, so the comparison ignores case rather than hiding the controls over it. */
    fun canManageHolidays(email: String?, isEmailVerified: Boolean): Boolean =
        isEmailVerified && email != null && email.trim().equals(HOLIDAY_MANAGER_EMAIL, ignoreCase = true)
}
