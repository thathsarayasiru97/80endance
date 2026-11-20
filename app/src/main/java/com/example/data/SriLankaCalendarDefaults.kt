package com.example.data

object SriLankaCalendarDefaults {

    val defaultTerms: List<AcademicTerm> = listOf(
        AcademicTerm(
            id = "term_1_2026",
            name = "Term 1 — 2026",
            startDate = "2026-01-05",
            endDate = "2026-04-10",
            academicYear = 2026,
            minEligibilityPercent = 80,
            active = false,
            published = true
        ),
        AcademicTerm(
            id = "term_2_2026",
            name = "Term 2 — 2026",
            startDate = "2026-04-27",
            endDate = "2026-08-14",
            academicYear = 2026,
            minEligibilityPercent = 80,
            active = false,
            published = true
        ),
        AcademicTerm(
            id = "term_3_2026",
            name = "Term 3 — 2026",
            startDate = "2026-09-01",
            endDate = "2026-12-04",
            academicYear = 2026,
            minEligibilityPercent = 80,
            active = true,
            published = true
        ),
        AcademicTerm(
            id = "term_1_2027",
            name = "Term 1 — 2027",
            startDate = "2027-01-04",
            endDate = "2027-04-09",
            academicYear = 2027,
            minEligibilityPercent = 80,
            active = false,
            published = true
        ),
        AcademicTerm(
            id = "term_2_2027",
            name = "Term 2 — 2027",
            startDate = "2027-04-26",
            endDate = "2027-08-06",
            academicYear = 2027,
            minEligibilityPercent = 80,
            active = false,
            published = true
        ),
        AcademicTerm(
            id = "term_3_2027",
            name = "Term 3 — 2027",
            startDate = "2027-08-30",
            endDate = "2027-12-03",
            academicYear = 2027,
            minEligibilityPercent = 80,
            active = false,
            published = true
        )
    )

    val defaultEvents: List<CalendarEvent> = listOf(
        // 2026 Sri Lankan Full Moon Poya Days & Major Public Holidays
        CalendarEvent("poya_2026_01", "Duruthu Full Moon Poya Day", "2026-01-03", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2026_pongal", "Tamil Thai Pongal Day", "2026-01-15", "public_holiday", 2026, "Public holiday", true),
        CalendarEvent("poya_2026_02", "Navam Full Moon Poya Day", "2026-02-01", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2026_indep", "National Independence Day", "2026-02-04", "public_holiday", 2026, "Sri Lanka Independence Day", true),
        CalendarEvent("poya_2026_03", "Medin Full Moon Poya Day", "2026-03-02", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2026_04", "Bak Full Moon Poya Day", "2026-04-01", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2026_avurudu1", "Day Prior to Sinhala & Tamil New Year", "2026-04-13", "public_holiday", 2026, "Avurudu holiday", true),
        CalendarEvent("hol_2026_avurudu2", "Sinhala & Tamil New Year Day", "2026-04-14", "public_holiday", 2026, "Avurudu holiday", true),
        CalendarEvent("poya_2026_05", "Vesak Full Moon Poya Day", "2026-05-01", "poya", 2026, "Vesak Festival", true),
        CalendarEvent("hol_2026_vesak2", "Day Following Vesak Full Moon Poya", "2026-05-02", "public_holiday", 2026, "Vesak holiday", true),
        CalendarEvent("poya_2026_05b", "Adhi Poson Full Moon Poya Day", "2026-05-30", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2026_06", "Poson Full Moon Poya Day", "2026-06-29", "poya", 2026, "Poson Festival", true),
        CalendarEvent("poya_2026_07", "Esala Full Moon Poya Day", "2026-07-29", "poya", 2026, "Esala Festival", true),
        CalendarEvent("poya_2026_08", "Nikini Full Moon Poya Day", "2026-08-27", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2026_09", "Binara Full Moon Poya Day", "2026-09-26", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2026_10", "Vap Full Moon Poya Day", "2026-10-25", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2026_deepavali", "Deepavali Festival Day", "2026-11-08", "public_holiday", 2026, "Festival of Lights", true),
        CalendarEvent("poya_2026_11", "Il Full Moon Poya Day", "2026-11-24", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("exam_2026_al", "GCE A/L 2026 Examination Start", "2026-11-25", "exam", 2026, "Department of Examinations Sri Lanka", true),
        CalendarEvent("poya_2026_12", "Unduvap Full Moon Poya Day", "2026-12-23", "poya", 2026, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2026_xmas", "Christmas Day", "2026-12-25", "public_holiday", 2026, "Public holiday", true),

        // 2027 Sri Lankan Full Moon Poya Days & Major Public Holidays
        CalendarEvent("hol_2027_pongal", "Tamil Thai Pongal Day", "2027-01-15", "public_holiday", 2027, "Public holiday", true),
        CalendarEvent("poya_2027_01", "Duruthu Full Moon Poya Day", "2027-01-22", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2027_indep", "National Independence Day", "2027-02-04", "public_holiday", 2027, "Sri Lanka Independence Day", true),
        CalendarEvent("poya_2027_02", "Navam Full Moon Poya Day", "2027-02-20", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2027_03", "Medin Full Moon Poya Day", "2027-03-22", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2027_avurudu1", "Day Prior to Sinhala & Tamil New Year", "2027-04-13", "public_holiday", 2027, "Avurudu holiday", true),
        CalendarEvent("hol_2027_avurudu2", "Sinhala & Tamil New Year Day", "2027-04-14", "public_holiday", 2027, "Avurudu holiday", true),
        CalendarEvent("poya_2027_04", "Bak Full Moon Poya Day", "2027-04-20", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("hol_2027_mayday", "May Day", "2027-05-01", "public_holiday", 2027, "International Workers' Day", true),
        CalendarEvent("poya_2027_05", "Vesak Full Moon Poya Day", "2027-05-20", "poya", 2027, "Vesak Festival", true),
        CalendarEvent("hol_2027_vesak2", "Day Following Vesak Full Moon Poya", "2027-05-21", "public_holiday", 2027, "Vesak holiday", true),
        CalendarEvent("poya_2027_06", "Poson Full Moon Poya Day", "2027-06-18", "poya", 2027, "Poson Festival", true),
        CalendarEvent("poya_2027_07", "Esala Full Moon Poya Day", "2027-07-18", "poya", 2027, "Esala Festival", true),
        CalendarEvent("exam_2027_al", "GCE A/L 2027 Examination Target", "2027-08-09", "exam", 2027, "Department of Examinations Sri Lanka", true),
        CalendarEvent("poya_2027_08", "Nikini Full Moon Poya Day", "2027-08-17", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2027_09", "Binara Full Moon Poya Day", "2027-09-15", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2027_10", "Vap Full Moon Poya Day", "2027-10-15", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2027_11", "Il Full Moon Poya Day", "2027-11-13", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true),
        CalendarEvent("poya_2027_12", "Unduvap Full Moon Poya Day", "2027-12-13", "poya", 2027, "Sri Lankan Full Moon Poya holiday", true)
    )
}
