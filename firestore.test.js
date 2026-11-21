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
});
