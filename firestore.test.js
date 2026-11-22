const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const STUDENT_A_UID = "student_a_123";
const STUDENT_B_UID = "student_b_456";
const ADMIN_UID = "admin_yasiru_001";
const ADMIN_EMAIL = "thathsarayasiru97@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, email = "student@royal.lk") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    firstName: "Kavindu",
    lastName: "Perera",
    nickname: "Kavi",
    email,
    schoolName: "Royal College, Colombo",
    grade: "Grade 13",
    alStream: "Physical Science",
    className: "13M2",
    alExamYear: 2026,
    subjects: ["Combined Maths", "Physics", "Chemistry"],
    reminderEnabled: true,
    reminderHour: 14,
    reminderMinute: 0,
    countExcusedAsPresent: true,
    priorPresentCount: 10,
    priorAbsentCount: 1,
    setupCompleted: true,
    presentCount: 0,
    absentCount: 0,
    medicalCount: 0,
    schoolEventCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validAttendancePayload(uid, dateKey = "2026-11-20", markedBy = "student") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    dateKey,
    termId: "term_3_2026",
    status: "present",
    subjectTags: ["Combined Maths", "Physics"],
    note: "Attended all periods",
    markedBy,
    createdAt: now,
    updatedAt: now,
  };
}

function validNoticePayload(overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    title: "Physics Teacher Absent Tomorrow",
    message: "Period 3 & 4 Physics will be self-study in the library.",
    category: "teacher_absent",
    pinned: true,
    targetType: "class",
    targetValue: "13M2",
    eventDate: "2026-11-21",
    priority: "urgent",
    published: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(unauthDb.collection("notices").where("published", "==", true).get());
  await assertFails(unauthDb.collection("academic_terms").where("published", "==", true).get());
  await assertFails(unauthDb.collection("calendar_events").where("published", "==", true).get());
  await assertFails(unauthDb.collection("app_settings").doc("global").get());
});

test("Student profile: owner CRUD succeeds, shadow/extra keys & cross-user access denied", async () => {
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, { email: "a@royal.lk", email_verified: true }).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID, { email: "b@royal.lk", email_verified: true }).firestore();

  const payload = validProfilePayload(STUDENT_A_UID, "a@royal.lk");
  await assertSucceeds(studentA.collection("users").doc(STUDENT_A_UID).set(payload));
  await assertSucceeds(studentA.collection("users").doc(STUDENT_A_UID).get());

  // Partial update preserves createdAt and allows bounded avatar + percentage baseline lock fields
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      nickname: "KP",
      presentCount: 1,
      lastCheckInDate: "2026-11-20",
      photoUrl: "https://lh3.googleusercontent.com/a/example-avatar",
      customAvatarBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
      priorPresentCount: 35,
      priorAbsentCount: 7,
      baselineStartDate: "2026-09-07",
      baselineEndDate: "2026-11-13",
      baselinePercent: 83.3,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject out-of-range baselinePercent (> 100) or malformed baselineEndDate
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      baselinePercent: 105,
      updatedAt: new Date(Date.now() - 450),
    })
  );
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      baselineEndDate: "11/13/2026",
      updatedAt: new Date(Date.now() - 440),
    })
  );

  // Reject oversized customAvatarBase64 payload (> 120000 chars)
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      customAvatarBase64: "A".repeat(120001),
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject future timestamp
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      nickname: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation & student list prohibition
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(studentB.collection("users").get());
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).update({ nickname: "Hijack", updatedAt: new Date(Date.now() - 500) }));
});

test("Attendance subcollection: student manages own records; cross-user denied; cannot spoof markedBy=admin", async () => {
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, { email: "a@royal.lk", email_verified: true }).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID, { email: "b@royal.lk", email_verified: true }).firestore();

  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).collection("attendance").doc("2026-11-20")
      .set(validAttendancePayload(STUDENT_A_UID, "2026-11-20", "student"))
  );

  // List own subcollection
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).collection("attendance").get()
  );

  // Update own attendance
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).collection("attendance").doc("2026-11-20").update({
      status: "school_event",
      note: "Big Match committee",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Student cannot spoof markedBy = admin
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).collection("attendance").doc("2026-11-21")
      .set(validAttendancePayload(STUDENT_A_UID, "2026-11-21", "admin"))
  );

  // Cross-user access blocked
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).collection("attendance").get());
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).collection("attendance").doc("2026-11-20").get());
});

test("Email spoofing test: unverified admin email is rejected from admin actions", async () => {
  const spoofedAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofedAdmin.collection("users").get());
  await assertFails(spoofedAdmin.collection("notices").doc("notice_1").set(validNoticePayload()));
});

test("Verified admin: full control over notices, terms, calendar events, settings, and student overrides", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "a@royal.lk",
    email_verified: true,
  }).firestore();

  // Seed student A
  await assertSucceeds(studentA.collection("users").doc(STUDENT_A_UID).set(validProfilePayload(STUDENT_A_UID, "a@royal.lk")));

  // Admin can list all students and override a student's attendance
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(STUDENT_A_UID).collection("attendance").doc("2026-11-19")
      .set(validAttendancePayload(STUDENT_A_UID, "2026-11-19", "admin"))
  );

  // Admin creates notice, term, calendar event, and global settings
  await assertSucceeds(adminDb.collection("notices").doc("notice_1").set(validNoticePayload()));
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    adminDb.collection("academic_terms").doc("term_3_2026").set({
      name: "Term 3 — 2026",
      startDate: "2026-09-01",
      endDate: "2026-12-04",
      academicYear: 2026,
      minEligibilityPercent: 80,
      active: true,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("calendar_events").doc("poya_2026_11").set({
      title: "Il Full Moon Poya Day",
      dateKey: "2026-11-24",
      eventType: "poya",
      academicYear: 2026,
      description: "Sri Lankan Full Moon Poya Day",
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  // Class-specific & grade-specific No School day + grade-targeted notice
  await assertSucceeds(
    adminDb.collection("calendar_events").doc("no_school_2026_11_26_class_13m2").set({
      title: "13M2 Practical Lab Maintenance",
      dateKey: "2026-11-26",
      eventType: "school_holiday",
      academicYear: 2026,
      description: "Marked as No School for 13M2",
      targetType: "class",
      targetValue: "13M2",
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("notices").doc("notice_grade_13").set(
      validNoticePayload({ targetType: "grade", targetValue: "Grade 13" })
    )
  );
  // Reject invalid calendar event targetType
  await assertFails(
    adminDb.collection("calendar_events").doc("bad_target").set({
      title: "Bad Target",
      dateKey: "2026-11-27",
      eventType: "school_holiday",
      academicYear: 2026,
      targetType: "invalid_scope",
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_settings").doc("global").set({
      defaultEligibilityPercent: 80,
      defaultReminderHour: 14,
      defaultReminderMinute: 0,
      currentAcademicYear: 2026,
      al2026ExamDate: "2026-11-25",
      al2027ExamDate: "2027-08-09",
      al2028ExamDate: "2028-08-07",
      updatedAt: now,
    })
  );

  // Student reads published shared collections with exact Kotlin queries
  await assertSucceeds(studentA.collection("notices").where("published", "==", true).get());
  await assertSucceeds(studentA.collection("academic_terms").where("published", "==", true).get());
  await assertSucceeds(studentA.collection("calendar_events").where("published", "==", true).get());
  await assertSucceeds(studentA.collection("app_settings").doc("global").get());

  // Student CANNOT run unfiltered list on notices or write to shared collections
  await assertFails(studentA.collection("notices").get());
  await assertFails(studentA.collection("notices").doc("hack_notice").set(validNoticePayload()));
  await assertFails(studentA.collection("app_settings").get());

  // Admin can publish normal_note notice
  await assertSucceeds(
    adminDb.collection("notices").doc("notice_normal_note").set(
      validNoticePayload({ category: "normal_note", title: "Lab Coat Reminder", pinned: false })
    )
  );
});

test("Feedback collection: students submit open bugs/suggestions, isolated from others; admin reviews & deletes", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "a@royal.lk",
    email_verified: true,
  }).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID, {
    email: "b@royal.lk",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);
  const validFeedback = {
    userId: STUDENT_A_UID,
    userName: "Kavindu Perera",
    userEmail: "a@royal.lk",
    className: "13M2",
    type: "suggestion",
    title: "Add OLED dark theme",
    message: "Would love a true black theme for battery savings.",
    appVersion: "1.4.0",
    status: "open",
    createdAt: now,
    updatedAt: now,
  };

  // Student A can create own open feedback
  await assertSucceeds(studentA.collection("feedback").doc("fb_1").set(validFeedback));

  // Student A cannot spoof another userId or pre-resolve status
  await assertFails(
    studentA.collection("feedback").doc("fb_spoof_uid").set({
      ...validFeedback,
      userId: STUDENT_B_UID,
    })
  );
  await assertFails(
    studentA.collection("feedback").doc("fb_spoof_status").set({
      ...validFeedback,
      status: "resolved",
    })
  );

  // Student A can read own feedback; Student B cannot read or list all feedback
  await assertSucceeds(studentA.collection("feedback").doc("fb_1").get());
  await assertFails(studentB.collection("feedback").doc("fb_1").get());
  await assertFails(studentB.collection("feedback").get());

  // Student A cannot update or delete feedback after submitting
  await assertFails(
    studentA.collection("feedback").doc("fb_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(studentA.collection("feedback").doc("fb_1").delete());

  // Admin can list all feedback, mark resolved, and delete
  await assertSucceeds(adminDb.collection("feedback").get());
  await assertSucceeds(
    adminDb.collection("feedback").doc("fb_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(adminDb.collection("feedback").doc("fb_1").delete());
});

test("User presence, community account stats, and admin broadcast notifications obey zero-trust rules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "a@royal.lk",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);

  // Unauthenticated denied on new collections
  await assertFails(unauthDb.collection("app_stats").doc("community").get());
  await assertFails(unauthDb.collection("broadcast_notifications").get());

  // Student A creates profile & updates presence heartbeat (onlineNow + lastSeenAt)
  await assertSucceeds(studentA.collection("users").doc(STUDENT_A_UID).set(validProfilePayload(STUDENT_A_UID, "a@royal.lk")));
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      onlineNow: true,
      lastSeenAt: now,
      updatedAt: now,
    })
  );
  // Reject future lastSeenAt timestamp
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      onlineNow: true,
      lastSeenAt: new Date(Date.now() + 60000),
      updatedAt: now,
    })
  );

  // Community stats: student can initialize at 1 and increment strictly by +1; cannot forge arbitrary jump or decrement
  await assertFails(
    studentA.collection("app_stats").doc("community").set({ totalAccounts: 999, updatedAt: now })
  );
  await assertSucceeds(
    studentA.collection("app_stats").doc("community").set({ totalAccounts: 1, updatedAt: now })
  );
  await assertSucceeds(studentA.collection("app_stats").doc("community").get());
  await assertFails(studentA.collection("app_stats").get()); // list denied
  await assertSucceeds(
    studentA.collection("app_stats").doc("community").update({ totalAccounts: 2, updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    studentA.collection("app_stats").doc("community").update({ totalAccounts: 50, updatedAt: new Date(Date.now() - 400) })
  );
  await assertFails(
    studentA.collection("app_stats").doc("community").update({ totalAccounts: 1, updatedAt: new Date(Date.now() - 300) })
  );
  // Admin can reconcile exact student count
  await assertSucceeds(
    adminDb.collection("app_stats").doc("community").update({ totalAccounts: 42, updatedAt: new Date(Date.now() - 200) })
  );

  // Broadcast notifications: admin creates/deletes; student reads; student cannot create/update/delete
  const validBroadcast = {
    title: "Assembly Postponed",
    message: "Grade 13 Physical Science & 13M2 report to the main hall at 8:00 AM.",
    targetAll: false,
    targetGrades: ["Grade 13"],
    targetStreams: ["Physical Science"],
    targetClasses: ["13M2"],
    targetUserIds: [STUDENT_A_UID],
    audienceSummary: "Grade 13 • Physical Science • 13M2 • +1 student",
    sentByEmail: ADMIN_EMAIL,
    createdAt: now,
  };
  await assertFails(studentA.collection("broadcast_notifications").doc("bc_1").set(validBroadcast));
  await assertSucceeds(adminDb.collection("broadcast_notifications").doc("bc_1").set(validBroadcast));
  await assertSucceeds(studentA.collection("broadcast_notifications").get());
  await assertFails(adminDb.collection("broadcast_notifications").doc("bc_1").update({ title: "Mutated" }));
  await assertFails(studentA.collection("broadcast_notifications").doc("bc_1").delete());
  await assertSucceeds(adminDb.collection("broadcast_notifications").doc("bc_1").delete());
});

test("Academic catalog, targeted multi-audience calendar events, and catalog_request feedback obey zero-trust rules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "a@royal.lk",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);

  // Academic catalog: unauth denied; student read-only; admin create/update; list & delete denied
  await assertFails(unauthDb.collection("app_config").doc("academic_catalog").get());
  const catalogPayload = {
    streamSubjectBucketsJson: JSON.stringify({ "Physical Science": { "Bucket 1": ["Combined Maths"] } }),
    gradeStreamClassesJson: JSON.stringify({ "Grade 12|Bio Science": ["12S1", "12S2"] }),
    updatedAt: now,
  };
  await assertFails(studentA.collection("app_config").doc("academic_catalog").set(catalogPayload));
  await assertSucceeds(adminDb.collection("app_config").doc("academic_catalog").set(catalogPayload));
  await assertSucceeds(studentA.collection("app_config").doc("academic_catalog").get());
  await assertFails(studentA.collection("app_config").get());
  await assertSucceeds(
    adminDb.collection("app_config").doc("academic_catalog").update({
      gradeStreamClassesJson: JSON.stringify({ "Grade 12|Bio Science": ["12S1", "12S2", "12S3"] }),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(adminDb.collection("app_config").doc("academic_catalog").delete());

  // Targeted multi-audience school_event calendar entry + edit
  const targetedEvent = {
    title: "Science Day Practice",
    dateKey: "2026-11-28",
    eventType: "school_event",
    academicYear: 2026,
    description: "There might be no teaching for Grade 12 Bio Science classes 12S1 & 12S2.",
    targetAll: false,
    targetGrades: ["Grade 12"],
    targetStreams: ["Bio Science"],
    targetClasses: ["12S1", "12S2"],
    targetUserIds: [],
    audienceSummary: "Grade 12 • Bio Science • 12S1, 12S2",
    published: true,
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(studentA.collection("calendar_events").doc("ev_multi_1").set(targetedEvent));
  await assertSucceeds(adminDb.collection("calendar_events").doc("ev_multi_1").set(targetedEvent));
  await assertSucceeds(
    adminDb.collection("calendar_events").doc("ev_multi_1").update({
      description: "Updated note for 12S1 only.",
      targetClasses: ["12S1"],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Student submits catalog_request feedback & queries own feedback list
  await assertSucceeds(
    studentA.collection("feedback").doc("fb_catalog_1").set({
      userId: STUDENT_A_UID,
      userName: "Kavindu Perera",
      userEmail: "a@royal.lk",
      className: "12S1",
      type: "catalog_request",
      title: "Missing Subject: French",
      message: "Student entered custom subject 'French' under Arts → Bucket 3.",
      appVersion: "1.5",
      status: "open",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(studentA.collection("feedback").where("userId", "==", STUDENT_A_UID).get());
});

test("Next-day school poll votes and AppSettings.nextDayPollEnabled follow zero-trust rules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "a@royal.lk",
    email_verified: true,
  }).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID, {
    email: "b@royal.lk",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);
  const later = new Date(Date.now() - 500);
  const targetDate = "2026-11-23";
  const voteIdA = `${targetDate}_${STUDENT_A_UID}`;
  const voteIdB = `${targetDate}_${STUDENT_B_UID}`;

  // Global app_settings.nextDayPollEnabled: admin only
  const baseSettings = {
    defaultEligibilityPercent: 80,
    defaultReminderHour: 14,
    defaultReminderMinute: 0,
    currentAcademicYear: 2026,
    al2026ExamDate: "2026-11-25",
    al2027ExamDate: "2027-08-09",
    al2028ExamDate: "2028-08-07",
    nextDayPollEnabled: false,
    updatedAt: now,
  };
  await assertSucceeds(adminDb.collection("app_settings").doc("global").set(baseSettings));
  await assertFails(
    studentA.collection("app_settings").doc("global").update({ nextDayPollEnabled: true, updatedAt: later })
  );
  await assertSucceeds(
    adminDb.collection("app_settings").doc("global").update({ nextDayPollEnabled: true, updatedAt: later })
  );

  // Unauthenticated denied
  await assertFails(unauthDb.collection("next_day_votes").doc(voteIdA).get());
  await assertFails(unauthDb.collection("next_day_votes").where("targetDateKey", "==", targetDate).get());

  const validVoteA = {
    userId: STUDENT_A_UID,
    targetDateKey: targetDate,
    going: true,
    grade: "Grade 13",
    alStream: "Physical Science",
    className: "13M2",
    createdAt: now,
    updatedAt: now,
  };

  // Spoofed userId, mismatched docId, or extra PII/ghost field denied
  await assertFails(
    studentA.collection("next_day_votes").doc(voteIdA).set({ ...validVoteA, userId: STUDENT_B_UID })
  );
  await assertFails(
    studentA.collection("next_day_votes").doc("arbitrary_id").set(validVoteA)
  );
  await assertFails(
    studentA.collection("next_day_votes").doc(voteIdA).set({ ...validVoteA, firstName: "Kavindu" })
  );

  // Valid create by studentA and studentB
  await assertSucceeds(studentA.collection("next_day_votes").doc(voteIdA).set(validVoteA));
  await assertSucceeds(
    studentB.collection("next_day_votes").doc(voteIdB).set({
      userId: STUDENT_B_UID,
      targetDateKey: targetDate,
      going: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Signed-in date-scoped query succeeds
  await assertSucceeds(
    studentA.collection("next_day_votes").where("targetDateKey", "==", targetDate).get()
  );

  // StudentA can flip their own vote (partial update) while preserving createdAt/userId/targetDateKey
  await assertSucceeds(
    studentA.collection("next_day_votes").doc(voteIdA).update({
      going: false,
      updatedAt: later,
    })
  );

  // StudentA cannot mutate immutable fields or flip StudentB's vote
  await assertFails(
    studentA.collection("next_day_votes").doc(voteIdA).update({
      targetDateKey: "2026-11-24",
      updatedAt: later,
    })
  );
  await assertFails(
    studentA.collection("next_day_votes").doc(voteIdB).update({
      going: true,
      updatedAt: later,
    })
  );
});


