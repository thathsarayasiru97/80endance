package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.system.SyncScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

object FirestoreProvider {
    @Volatile
    private var configured = false

    fun getInstance(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext
        val databaseId = appContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        if (!configured) {
            synchronized(this) {
                if (!configured) {
                    try {
                        val settings = firestoreSettings {
                            setLocalCacheSettings(
                                persistentCacheSettings {
                                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                }
                            )
                        }
                        db.firestoreSettings = settings
                    } catch (e: IllegalStateException) {
                        // Settings already initialized; persistent disk cache is enabled by default on Android
                        Log.d("FirestoreProvider", "Firestore settings already configured")
                    }
                    configured = true
                }
            }
        }
        return db
    }
}

class AttendanceRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirestoreProvider.getInstance(context.applicationContext),
        appContext = context.applicationContext
    )

    private val _hasPendingWrites = MutableStateFlow(false)
    val hasPendingWrites: StateFlow<Boolean> = _hasPendingWrites.asStateFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- STUDENT OBSERVABLE FLOWS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    _hasPendingWrites.value = snapshot.metadata.hasPendingWrites()
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/attendance"
        emitAll(
            db.collection("users").document(userId).collection("attendance")
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    if (snapshot.metadata.hasPendingWrites()) {
                        _hasPendingWrites.value = true
                    }
                    snapshot.toObjects(
                        AttendanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePublishedNotices(): Flow<List<Notice>> = flow {
        val uid = requireUserId()
        val path = "notices"
        emitAll(
            db.collection("notices")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Notice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePublishedTerms(): Flow<List<AcademicTerm>> = flow {
        val uid = requireUserId()
        val path = "academic_terms"
        emitAll(
            db.collection("academic_terms")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AcademicTerm::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.startDate }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePublishedCalendarEvents(): Flow<List<CalendarEvent>> = flow {
        val uid = requireUserId()
        val path = "calendar_events"
        emitAll(
            db.collection("calendar_events")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CalendarEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.dateKey }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        val uid = requireUserId()
        val path = "app_settings/global"
        emitAll(
            db.collection("app_settings").document("global")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        AppSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: AppSettings()
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    // --- ADMIN OBSERVABLE FLOWS (CALLED ONLY WHEN VERIFIED ADMIN) ---

    fun observeAllStudentsAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.fullName.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllNoticesAdmin(): Flow<List<Notice>> = flow {
        val uid = requireUserId()
        val path = "notices"
        emitAll(
            db.collection("notices")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Notice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllFeedbackAdmin(): Flow<List<FeedbackReport>> = flow {
        val uid = requireUserId()
        val path = "feedback"
        emitAll(
            db.collection("feedback")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FeedbackReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    // --- STUDENT FEEDBACK SUBMISSIONS ---

    suspend fun submitFeedback(
        userId: String,
        userName: String,
        userEmail: String,
        className: String?,
        type: FeedbackType,
        title: String,
        message: String,
        appVersion: String
    ): Result<Unit> {
        val docRef = db.collection("feedback").document()
        val path = docRef.path
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanMessage = message.trim().take(1500)
            require(cleanTitle.isNotEmpty()) { "Please enter a short summary" }
            require(cleanMessage.isNotEmpty()) { "Please describe the bug or suggestion" }
            val cleanName = userName.trim().ifEmpty { "Student" }.take(100)
            val cleanEmail = userEmail.trim().ifEmpty { "student@80endance.app" }.take(120)
            val cleanClass = className?.trim()?.take(20)?.ifEmpty { null }
            val cleanVersion = appVersion.trim().ifEmpty { "1.0" }.take(40)

            val payload = mapOf(
                "userId" to userId,
                "userName" to cleanName,
                "userEmail" to cleanEmail,
                "className" to cleanClass,
                "type" to type.raw,
                "title" to cleanTitle,
                "message" to cleanMessage,
                "appVersion" to cleanVersion,
                "status" to FeedbackStatus.OPEN.raw,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFeedbackStatus(feedbackId: String, status: FeedbackStatus): Result<Unit> {
        val path = "feedback/$feedbackId"
        return try {
            requireUserId()
            db.collection("feedback").document(feedbackId).update(
                mapOf(
                    "status" to status.raw,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
        val path = "feedback/$feedbackId"
        return try {
            requireUserId()
            db.collection("feedback").document(feedbackId).delete().awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PROFILE MUTATIONS ---

    suspend fun saveUserProfile(
        targetUserId: String,
        firstName: String,
        lastName: String,
        nickname: String?,
        email: String,
        schoolName: String,
        grade: String,
        alStream: String,
        className: String,
        alExamYear: Int,
        subjects: List<String>,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        countExcusedAsPresent: Boolean,
        priorPresentCount: Int,
        priorAbsentCount: Int,
        setupCompleted: Boolean,
        isExisting: Boolean,
        photoUrl: String? = null,
        baselineStartDate: String? = null,
        baselineEndDate: String? = null,
        baselinePercent: Double? = null,
        updateBaselineFields: Boolean = false
    ): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            val cleanFirst = firstName.trim().take(50)
            val cleanLast = lastName.trim().take(50)
            val cleanNick = nickname?.trim()?.take(40)?.ifEmpty { null }
            val cleanSchool = schoolName.trim().ifEmpty { AppConstants.DEFAULT_SCHOOL_NAME }.take(100)
            val cleanGrade = if (grade in AppConstants.GRADES) grade else "Grade 13"
            val cleanStream = if (alStream in AppConstants.AL_STREAMS) alStream else "Physical Science"
            val cleanClass = className.trim().take(20)
            val cleanYear = alExamYear.coerceIn(2025, 2035)
            val cleanSubjects = subjects.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(6)

            require(cleanFirst.isNotEmpty()) { "First name is required" }
            require(cleanLast.isNotEmpty()) { "Last name is required" }
            require(cleanClass.isNotEmpty()) { "Class is required" }

            val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
            val cleanBaseStart = baselineStartDate?.trim()?.takeIf { it.matches(dateRegex) }
            val cleanBaseEnd = baselineEndDate?.trim()?.takeIf { it.matches(dateRegex) }
            val cleanBasePct = baselinePercent?.coerceIn(0.0, 100.0)
            val hasValidBaseline = cleanBaseStart != null && cleanBaseEnd != null && cleanBaseStart <= cleanBaseEnd

            val docRef = db.collection("users").document(targetUserId)
            val shouldUpdate = isExisting || runCatching { docRef.get().await().exists() }.getOrDefault(false)

            val cleanPhotoUrl = photoUrl?.trim()?.take(500)?.ifEmpty { null }

            if (shouldUpdate) {
                val updates = buildMap<String, Any> {
                    put("firstName", cleanFirst)
                    put("lastName", cleanLast)
                    if (cleanNick != null) put("nickname", cleanNick) else put("nickname", "")
                    put("email", email.trim().take(120))
                    put("schoolName", cleanSchool)
                    put("grade", cleanGrade)
                    put("alStream", cleanStream)
                    put("className", cleanClass)
                    put("alExamYear", cleanYear)
                    put("subjects", cleanSubjects)
                    put("reminderEnabled", reminderEnabled)
                    put("reminderHour", reminderHour.coerceIn(0, 23))
                    put("reminderMinute", reminderMinute.coerceIn(0, 59))
                    put("countExcusedAsPresent", countExcusedAsPresent)
                    put("priorPresentCount", priorPresentCount.coerceIn(0, 600))
                    put("priorAbsentCount", priorAbsentCount.coerceIn(0, 600))
                    if (updateBaselineFields) {
                        if (hasValidBaseline) {
                            put("baselineStartDate", cleanBaseStart!!)
                            put("baselineEndDate", cleanBaseEnd!!)
                            if (cleanBasePct != null) {
                                put("baselinePercent", cleanBasePct)
                            } else {
                                put("baselinePercent", FieldValue.delete())
                            }
                        } else {
                            put("baselineStartDate", FieldValue.delete())
                            put("baselineEndDate", FieldValue.delete())
                            put("baselinePercent", FieldValue.delete())
                        }
                    }
                    put("setupCompleted", setupCompleted)
                    if (cleanPhotoUrl != null) put("photoUrl", cleanPhotoUrl)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).awaitOfflineAware()
            } else {
                val createMap = mapOf(
                    "userId" to targetUserId,
                    "firstName" to cleanFirst,
                    "lastName" to cleanLast,
                    "nickname" to cleanNick,
                    "email" to email.trim().take(120),
                    "schoolName" to cleanSchool,
                    "grade" to cleanGrade,
                    "alStream" to cleanStream,
                    "className" to cleanClass,
                    "alExamYear" to cleanYear,
                    "subjects" to cleanSubjects,
                    "reminderEnabled" to reminderEnabled,
                    "reminderHour" to reminderHour.coerceIn(0, 23),
                    "reminderMinute" to reminderMinute.coerceIn(0, 59),
                    "countExcusedAsPresent" to countExcusedAsPresent,
                    "priorPresentCount" to priorPresentCount.coerceIn(0, 600),
                    "priorAbsentCount" to priorAbsentCount.coerceIn(0, 600),
                    "baselineStartDate" to if (hasValidBaseline) cleanBaseStart else null,
                    "baselineEndDate" to if (hasValidBaseline) cleanBaseEnd else null,
                    "baselinePercent" to if (hasValidBaseline) cleanBasePct else null,
                    "setupCompleted" to setupCompleted,
                    "presentCount" to 0,
                    "absentCount" to 0,
                    "medicalCount" to 0,
                    "schoolEventCount" to 0,
                    "photoUrl" to cleanPhotoUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).awaitOfflineAware()
            }
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAttendancePercentageBaseline(
        userId: String,
        startDateKey: String,
        endDateKey: String,
        percentage: Double,
        presentDays: Int,
        absentDays: Int,
        existingRecords: List<AttendanceRecord>
    ): Result<Int> {
        val path = "users/$userId"
        return try {
            requireUserId()
            val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
            require(startDateKey.matches(dateRegex) && endDateKey.matches(dateRegex)) {
                "Choose valid start and end dates (YYYY-MM-DD)"
            }
            require(startDateKey <= endDateKey) { "End date must be on or after start date" }
            require(endDateKey <= DateUtils.todayKey()) { "End date cannot be in the future" }

            val cleanPresent = presentDays.coerceIn(0, 600)
            val cleanAbsent = absentDays.coerceIn(0, 600)
            require(cleanPresent + cleanAbsent > 0) { "Selected period must include at least 1 school day" }

            val cleanPct = percentage.coerceIn(0.0, 100.0)
            val overlappingRecords = existingRecords.filter {
                it.dateKey >= startDateKey && it.dateKey <= endDateKey
            }
            val remainingRecords = existingRecords.filterNot {
                it.dateKey >= startDateKey && it.dateKey <= endDateKey
            }

            val batch = db.batch()
            val userDocRef = db.collection("users").document(userId)
            for (rec in overlappingRecords) {
                batch.delete(userDocRef.collection("attendance").document(rec.dateKey))
            }

            val present = remainingRecords.count { it.statusEnum == AttendanceStatus.PRESENT }
            val absent = remainingRecords.count { it.statusEnum == AttendanceStatus.ABSENT }
            val medical = remainingRecords.count { it.statusEnum == AttendanceStatus.MEDICAL }
            val schoolEvent = remainingRecords.count { it.statusEnum == AttendanceStatus.SCHOOL_EVENT }
            val lastDate = remainingRecords.maxOfOrNull { it.dateKey } ?: ""

            batch.update(
                userDocRef,
                mapOf(
                    "priorPresentCount" to cleanPresent,
                    "priorAbsentCount" to cleanAbsent,
                    "baselineStartDate" to startDateKey,
                    "baselineEndDate" to endDateKey,
                    "baselinePercent" to cleanPct,
                    "presentCount" to present,
                    "absentCount" to absent,
                    "medicalCount" to medical,
                    "schoolEventCount" to schoolEvent,
                    "lastCheckInDate" to lastDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(overlappingRecords.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAttendancePercentageBaseline(
        userId: String,
        resetPriorCounts: Boolean = true
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            val updates = buildMap<String, Any> {
                put("baselineStartDate", FieldValue.delete())
                put("baselineEndDate", FieldValue.delete())
                put("baselinePercent", FieldValue.delete())
                if (resetPriorCounts) {
                    put("priorPresentCount", 0)
                    put("priorAbsentCount", 0)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(userId).update(updates).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        countExcusedAsPresent: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "reminderEnabled" to reminderEnabled,
                    "reminderHour" to reminderHour.coerceIn(0, 23),
                    "reminderMinute" to reminderMinute.coerceIn(0, 59),
                    "countExcusedAsPresent" to countExcusedAsPresent,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(
        userId: String,
        customAvatarBase64: String?,
        googlePhotoUrl: String? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            val cleanBase64 = customAvatarBase64?.trim().orEmpty()
            require(cleanBase64.length <= 120_000) { "Selected image is too large. Please try another photo." }
            val updates = buildMap<String, Any> {
                put("customAvatarBase64", cleanBase64)
                val cleanGoogleUrl = googlePhotoUrl?.trim()?.take(500)?.ifEmpty { null }
                if (cleanGoogleUrl != null) {
                    put("photoUrl", cleanGoogleUrl)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(userId).update(updates).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun isPublishedNonSchoolDay(dateKey: String): Boolean {
        if (DateUtils.isWeekend(dateKey)) return true
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return !AttendanceCalculator.isSchoolDay(dateKey, SriLankaCalendarDefaults.defaultEvents)
        }
        val profile = runCatching {
            withTimeoutOrNull(2_000L) {
                db.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
            }
        }.getOrNull()
        val fetched = runCatching {
            withTimeoutOrNull(3_000L) {
                db.collection("calendar_events")
                    .whereEqualTo("published", true)
                    .get()
                    .await()
                    .toObjects(CalendarEvent::class.java)
            }
        }.getOrNull()
        val rawEvents = fetched?.takeIf { it.isNotEmpty() } ?: SriLankaCalendarDefaults.defaultEvents
        val applicableEvents = AttendanceCalculator.filterCalendarEventsForStudent(rawEvents, profile)
        return !AttendanceCalculator.isSchoolDay(dateKey, applicableEvents)
    }

    suspend fun hasLoggedAttendance(userId: String, dateKey: String): Boolean {
        if (userId.isBlank()) return false
        return runCatching {
            withTimeoutOrNull(2_500L) {
                db.collection("users").document(userId)
                    .collection("attendance").document(dateKey)
                    .get().await().exists()
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun getQuickWidgetMetrics(userId: String, todayKey: String): Triple<UserProfile?, List<AttendanceRecord>, List<CalendarEvent>> {
        val profile = runCatching {
            withTimeoutOrNull(2_500L) {
                db.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
            }
        }.getOrNull()
        val records = runCatching {
            withTimeoutOrNull(2_500L) {
                db.collection("users").document(userId).collection("attendance")
                    .get().await().toObjects(AttendanceRecord::class.java)
            }
        }.getOrNull().orEmpty()
        val events = runCatching {
            withTimeoutOrNull(2_500L) {
                db.collection("calendar_events").whereEqualTo("published", true)
                    .get().await().toObjects(CalendarEvent::class.java)
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: SriLankaCalendarDefaults.defaultEvents
        return Triple(profile, records, AttendanceCalculator.filterCalendarEventsForStudent(events, profile))
    }

    // --- ATTENDANCE MUTATIONS ---

    suspend fun upsertAttendance(
        targetUserId: String,
        dateKey: String,
        termId: String,
        status: AttendanceStatus,
        subjectTags: List<String>,
        note: String?,
        markedBy: String,
        existingRecords: List<AttendanceRecord>? = null
    ): Result<Unit> {
        val path = "users/$targetUserId/attendance/$dateKey"
        return try {
            requireUserId()
            require(dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid date key" }
            val cleanTermId = termId.trim().ifEmpty { "term_3_2026" }.take(60)
            val cleanSubjects = subjectTags.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(6)
            val cleanNote = note?.trim()?.take(200)?.ifEmpty { null }
            val cleanMarkedBy = if (markedBy == "admin") "admin" else "student"

            val docRef = db.collection("users").document(targetUserId)
                .collection("attendance").document(dateKey)

            val alreadyExists = existingRecords?.any { it.dateKey == dateKey }
                ?: runCatching { docRef.get().await().exists() }.getOrDefault(false)

            if (alreadyExists) {
                val updates = mutableMapOf<String, Any>(
                    "termId" to cleanTermId,
                    "status" to status.raw,
                    "subjectTags" to cleanSubjects,
                    "markedBy" to cleanMarkedBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updates["note"] = cleanNote ?: ""
                docRef.update(updates).awaitOfflineAware()
            } else {
                val payload = mapOf(
                    "userId" to targetUserId,
                    "dateKey" to dateKey,
                    "termId" to cleanTermId,
                    "status" to status.raw,
                    "subjectTags" to cleanSubjects,
                    "note" to cleanNote,
                    "markedBy" to cleanMarkedBy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitOfflineAware()
            }

            syncProfileCounters(targetUserId, dateKey, status, existingRecords)
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(
        targetUserId: String,
        dateKey: String,
        existingRecords: List<AttendanceRecord>? = null
    ): Result<Unit> {
        val path = "users/$targetUserId/attendance/$dateKey"
        return try {
            requireUserId()
            db.collection("users").document(targetUserId)
                .collection("attendance").document(dateKey)
                .delete()
                .awaitOfflineAware()

            val remaining = existingRecords?.filterNot { it.dateKey == dateKey }
            if (remaining != null) {
                updateProfileCounterFields(targetUserId, remaining)
            }
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun syncProfileCounters(
        targetUserId: String,
        dateKey: String,
        status: AttendanceStatus,
        existingRecords: List<AttendanceRecord>?
    ) {
        val updatedList = if (existingRecords != null) {
            existingRecords.filterNot { it.dateKey == dateKey } +
                AttendanceRecord(userId = targetUserId, dateKey = dateKey, status = status.raw)
        } else {
            runCatching {
                db.collection("users").document(targetUserId).collection("attendance")
                    .get().await().toObjects(AttendanceRecord::class.java)
            }.getOrNull() ?: return
        }
        updateProfileCounterFields(targetUserId, updatedList)
    }

    private suspend fun updateProfileCounterFields(
        targetUserId: String,
        records: List<AttendanceRecord>
    ) {
        val present = records.count { it.statusEnum == AttendanceStatus.PRESENT }
        val absent = records.count { it.statusEnum == AttendanceStatus.ABSENT }
        val medical = records.count { it.statusEnum == AttendanceStatus.MEDICAL }
        val schoolEvent = records.count { it.statusEnum == AttendanceStatus.SCHOOL_EVENT }
        val lastDate = records.maxOfOrNull { it.dateKey } ?: ""
        runCatching {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "presentCount" to present,
                    "absentCount" to absent,
                    "medicalCount" to medical,
                    "schoolEventCount" to schoolEvent,
                    "lastCheckInDate" to lastDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitOfflineAware()
        }
    }

    // --- ADMIN MUTATIONS (NOTICES, TERMS, CALENDAR EVENTS, GLOBAL SETTINGS) ---

    suspend fun saveNotice(
        noticeId: String?,
        title: String,
        message: String,
        category: NoticeCategory,
        pinned: Boolean,
        targetType: NoticeTargetType,
        targetValue: String?,
        eventDate: String?,
        urgent: Boolean,
        published: Boolean
    ): Result<Unit> {
        val docRef = if (noticeId.isNullOrBlank()) {
            db.collection("notices").document()
        } else {
            db.collection("notices").document(noticeId)
        }
        val path = docRef.path
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanMsg = message.trim().take(1000)
            require(cleanTitle.isNotEmpty()) { "Notice title is required" }
            require(cleanMsg.isNotEmpty()) { "Notice message is required" }

            val cleanTargetVal = if (targetType == NoticeTargetType.ALL) null else targetValue?.trim()?.take(60)?.ifEmpty { null }
            val cleanEventDate = eventDate?.trim()?.takeIf { it.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) }
            val priority = if (urgent) "urgent" else "normal"

            if (noticeId.isNullOrBlank()) {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "message" to cleanMsg,
                    "category" to category.raw,
                    "pinned" to pinned,
                    "targetType" to targetType.raw,
                    "targetValue" to cleanTargetVal,
                    "eventDate" to cleanEventDate,
                    "priority" to priority,
                    "published" to published,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitOfflineAware()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "message" to cleanMsg,
                    "category" to category.raw,
                    "pinned" to pinned,
                    "targetType" to targetType.raw,
                    "targetValue" to (cleanTargetVal ?: ""),
                    "eventDate" to (cleanEventDate ?: ""),
                    "priority" to priority,
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).awaitOfflineAware()
            }
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNoticePin(noticeId: String, pinned: Boolean): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            db.collection("notices").document(noticeId).update(
                mapOf(
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            db.collection("notices").document(noticeId).delete().awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCalendarEvent(
        eventId: String?,
        title: String,
        dateKey: String,
        eventType: CalendarEventType,
        academicYear: Int,
        description: String?,
        targetType: CalendarEventTargetType = CalendarEventTargetType.ALL,
        targetValue: String? = null
    ): Result<Unit> {
        val docRef = if (eventId.isNullOrBlank()) {
            db.collection("calendar_events").document()
        } else {
            db.collection("calendar_events").document(eventId)
        }
        val path = docRef.path
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Title is required" }
            require(dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Date must be YYYY-MM-DD" }
            val cleanDesc = description?.trim()?.take(200)?.ifEmpty { null }
            val cleanTargetVal = if (targetType == CalendarEventTargetType.ALL) {
                null
            } else {
                targetValue?.trim()?.take(60)?.ifEmpty { null }
            }
            val exists = !eventId.isNullOrBlank() && runCatching { docRef.get().await().exists() }.getOrDefault(false)
            if (exists) {
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "dateKey" to dateKey,
                    "eventType" to eventType.raw,
                    "academicYear" to academicYear.coerceIn(2024, 2035),
                    "description" to (cleanDesc ?: ""),
                    "targetType" to targetType.raw,
                    "targetValue" to (cleanTargetVal ?: ""),
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).awaitOfflineAware()
            } else {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "dateKey" to dateKey,
                    "eventType" to eventType.raw,
                    "academicYear" to academicYear.coerceIn(2024, 2035),
                    "description" to cleanDesc,
                    "targetType" to targetType.raw,
                    "targetValue" to cleanTargetVal,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitOfflineAware()
            }
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBulkNoSchoolDays(
        dateKeys: List<String>,
        reason: String,
        targetType: CalendarEventTargetType,
        targetValue: String?,
        alsoPostNotice: Boolean
    ): Result<Int> {
        return try {
            requireUserId()
            val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
            val uniqueDates = dateKeys
                .map { it.trim() }
                .filter { it.matches(dateRegex) }
                .distinct()
                .sorted()
                .take(60)
            require(uniqueDates.isNotEmpty()) { "Select at least one valid date (YYYY-MM-DD)" }

            val cleanReason = reason.trim().ifEmpty { "No School Day" }.take(100)
            val cleanTargetVal = if (targetType == CalendarEventTargetType.ALL) {
                null
            } else {
                targetValue?.trim()?.take(60)?.ifEmpty { null }
            }
            if (targetType != CalendarEventTargetType.ALL) {
                require(!cleanTargetVal.isNullOrBlank()) { "Enter a ${targetType.label.lowercase()} to target" }
            }

            val audienceLabel = cleanTargetVal ?: "all students"
            val targetSlug = if (targetType == CalendarEventTargetType.ALL) {
                "all"
            } else {
                val sanitized = cleanTargetVal.orEmpty()
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .take(24)
                    .ifEmpty { "group" }
                "${targetType.raw}_$sanitized"
            }

            val existingIds = runCatching {
                db.collection("calendar_events").get().await().documents.map { it.id }.toSet()
            }.getOrDefault(emptySet())

            val batch = db.batch()
            for (dk in uniqueDates) {
                val docId = "no_school_${dk.replace("-", "_")}_$targetSlug".take(120)
                val ref = db.collection("calendar_events").document(docId)
                val year = dk.take(4).toIntOrNull()?.coerceIn(2024, 2035) ?: 2026
                val desc = "Marked as No School for $audienceLabel".take(200)
                if (docId in existingIds) {
                    batch.update(
                        ref,
                        mapOf(
                            "title" to cleanReason,
                            "dateKey" to dk,
                            "eventType" to CalendarEventType.SCHOOL_HOLIDAY.raw,
                            "academicYear" to year,
                            "description" to desc,
                            "targetType" to targetType.raw,
                            "targetValue" to (cleanTargetVal ?: ""),
                            "published" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val payload = mapOf(
                        "title" to cleanReason,
                        "dateKey" to dk,
                        "eventType" to CalendarEventType.SCHOOL_HOLIDAY.raw,
                        "academicYear" to year,
                        "description" to desc,
                        "targetType" to targetType.raw,
                        "targetValue" to cleanTargetVal,
                        "published" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    batch.set(ref, payload)
                }
            }
            batch.commit().awaitOfflineAware()

            if (alsoPostNotice) {
                val noticeTargetType = when (targetType) {
                    CalendarEventTargetType.ALL -> NoticeTargetType.ALL
                    CalendarEventTargetType.GRADE -> NoticeTargetType.GRADE
                    CalendarEventTargetType.STREAM -> NoticeTargetType.STREAM
                    CalendarEventTargetType.CLASS -> NoticeTargetType.CLASS
                }
                val dateSummary = if (uniqueDates.size == 1) {
                    DateUtils.formatReadableShort(uniqueDates.first())
                } else {
                    "${DateUtils.formatReadableShort(uniqueDates.first())} – ${DateUtils.formatReadableShort(uniqueDates.last())} (${uniqueDates.size} days)"
                }
                val scopeSummary = if (cleanTargetVal != null) "for $cleanTargetVal" else "for everyone"
                saveNotice(
                    noticeId = null,
                    title = "No School: $dateSummary".take(120),
                    message = "$cleanReason — marked as No School $scopeSummary on the calendar.".take(1000),
                    category = NoticeCategory.SCHEDULE_CHANGE,
                    pinned = true,
                    targetType = noticeTargetType,
                    targetValue = cleanTargetVal,
                    eventDate = uniqueDates.first(),
                    urgent = true,
                    published = true
                )
            }
            scheduleSyncIfOffline()
            Result.success(uniqueDates.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "calendar_events")
            Result.failure(e)
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> {
        val path = "calendar_events/$eventId"
        return try {
            requireUserId()
            db.collection("calendar_events").document(eventId).delete().awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAcademicTerm(
        termId: String?,
        name: String,
        startDate: String,
        endDate: String,
        academicYear: Int,
        minEligibilityPercent: Int,
        active: Boolean
    ): Result<Unit> {
        val docRef = if (termId.isNullOrBlank()) {
            db.collection("academic_terms").document()
        } else {
            db.collection("academic_terms").document(termId)
        }
        val path = docRef.path
        return try {
            requireUserId()
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Term name is required" }
            require(startDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid start date" }
            require(endDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid end date" }
            val exists = !termId.isNullOrBlank() && runCatching { docRef.get().await().exists() }.getOrDefault(false)
            if (exists) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "startDate" to startDate,
                        "endDate" to endDate,
                        "academicYear" to academicYear.coerceIn(2024, 2035),
                        "minEligibilityPercent" to minEligibilityPercent.coerceIn(50, 100),
                        "active" to active,
                        "published" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitOfflineAware()
            } else {
                val payload = mapOf(
                    "name" to cleanName,
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "academicYear" to academicYear.coerceIn(2024, 2035),
                    "minEligibilityPercent" to minEligibilityPercent.coerceIn(50, 100),
                    "active" to active,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitOfflineAware()
            }
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAcademicTerm(termId: String): Result<Unit> {
        val path = "academic_terms/$termId"
        return try {
            requireUserId()
            db.collection("academic_terms").document(termId).delete().awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically transitions the school to a new active term using a WriteBatch:
     * deactivates all previously active terms in Firestore, activates (or creates) the target term,
     * preserves all historical student attendance for Overall (G12 & G13), and optionally posts a pinned notice.
     */
    suspend fun rolloverActiveTerm(
        targetTerm: AcademicTerm,
        alsoPostNotice: Boolean
    ): Result<String> {
        return try {
            requireUserId()
            val cleanName = targetTerm.name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Term name is required" }
            require(targetTerm.startDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid start date" }
            require(targetTerm.endDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid end date" }
            require(targetTerm.startDate <= targetTerm.endDate) { "End date must be on or after start date" }

            val existingDocs = runCatching {
                db.collection("academic_terms").get().await().documents
            }.getOrDefault(emptyList())
            val existingIds = existingDocs.map { it.id }.toSet()

            val targetDocRef = if (targetTerm.id.isNotBlank()) {
                db.collection("academic_terms").document(targetTerm.id)
            } else {
                db.collection("academic_terms").document()
            }
            val targetId = targetDocRef.id
            val year = targetTerm.startDate.take(4).toIntOrNull()?.coerceIn(2024, 2035)
                ?: targetTerm.academicYear.coerceIn(2024, 2035)
            val eligibility = targetTerm.minEligibilityPercent.coerceIn(50, 100)

            val batch = db.batch()
            // Deactivate all other stored terms
            for (doc in existingDocs) {
                if (doc.id != targetId && doc.getBoolean("active") == true) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "active" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (targetId in existingIds) {
                batch.update(
                    targetDocRef,
                    mapOf(
                        "name" to cleanName,
                        "startDate" to targetTerm.startDate,
                        "endDate" to targetTerm.endDate,
                        "academicYear" to year,
                        "minEligibilityPercent" to eligibility,
                        "active" to true,
                        "published" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    targetDocRef,
                    mapOf(
                        "name" to cleanName,
                        "startDate" to targetTerm.startDate,
                        "endDate" to targetTerm.endDate,
                        "academicYear" to year,
                        "minEligibilityPercent" to eligibility,
                        "active" to true,
                        "published" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().awaitOfflineAware()

            if (alsoPostNotice) {
                saveNotice(
                    noticeId = null,
                    title = "Welcome to $cleanName!".take(120),
                    message = "$cleanName (${DateUtils.formatReadableShort(targetTerm.startDate)} – ${DateUtils.formatReadableShort(targetTerm.endDate)}) is now active. Your Overall (G12 & 13) attendance history is preserved while Term tracking starts fresh!".take(1000),
                    category = NoticeCategory.NORMAL_NOTE,
                    pinned = true,
                    targetType = NoticeTargetType.ALL,
                    targetValue = null,
                    eventDate = targetTerm.startDate,
                    urgent = false,
                    published = true
                )
            }

            scheduleSyncIfOffline()
            Result.success(cleanName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "academic_terms")
            Result.failure(e)
        }
    }

    suspend fun saveAppSettings(settings: AppSettings): Result<Unit> {
        val path = "app_settings/global"
        return try {
            requireUserId()
            val payload = mapOf(
                "defaultEligibilityPercent" to settings.defaultEligibilityPercent.coerceIn(50, 100),
                "defaultReminderHour" to settings.defaultReminderHour.coerceIn(0, 23),
                "defaultReminderMinute" to settings.defaultReminderMinute.coerceIn(0, 59),
                "currentAcademicYear" to settings.currentAcademicYear.coerceIn(2024, 2035),
                "al2026ExamDate" to settings.al2026ExamDate,
                "al2027ExamDate" to settings.al2027ExamDate,
                "al2028ExamDate" to settings.al2028ExamDate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_settings").document("global").set(payload).awaitOfflineAware()
            scheduleSyncIfOffline()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedSriLankaCalendar2026And2027(): Result<Unit> {
        return try {
            requireUserId()
            val existingTermIds = runCatching {
                db.collection("academic_terms").get().await().documents.map { it.id }.toSet()
            }.getOrDefault(emptySet())
            val existingEventIds = runCatching {
                db.collection("calendar_events").get().await().documents.map { it.id }.toSet()
            }.getOrDefault(emptySet())

            val batch = db.batch()
            for (term in SriLankaCalendarDefaults.defaultTerms) {
                val ref = db.collection("academic_terms").document(term.id)
                if (term.id in existingTermIds) {
                    batch.update(
                        ref,
                        mapOf(
                            "name" to term.name,
                            "startDate" to term.startDate,
                            "endDate" to term.endDate,
                            "academicYear" to term.academicYear,
                            "minEligibilityPercent" to term.minEligibilityPercent,
                            "active" to term.active,
                            "published" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        ref,
                        mapOf(
                            "name" to term.name,
                            "startDate" to term.startDate,
                            "endDate" to term.endDate,
                            "academicYear" to term.academicYear,
                            "minEligibilityPercent" to term.minEligibilityPercent,
                            "active" to term.active,
                            "published" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            for (ev in SriLankaCalendarDefaults.defaultEvents) {
                val ref = db.collection("calendar_events").document(ev.id)
                if (ev.id in existingEventIds) {
                    batch.update(
                        ref,
                        mapOf(
                            "title" to ev.title,
                            "dateKey" to ev.dateKey,
                            "eventType" to ev.eventType,
                            "academicYear" to ev.academicYear,
                            "description" to (ev.description ?: ""),
                            "published" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        ref,
                        mapOf(
                            "title" to ev.title,
                            "dateKey" to ev.dateKey,
                            "eventType" to ev.eventType,
                            "academicYear" to ev.academicYear,
                            "description" to (ev.description ?: ""),
                            "published" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().awaitOfflineAware()
            saveAppSettings(AppSettings())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "calendar_events")
            Result.failure(e)
        }
    }

    suspend fun flushPendingWrites(): Result<Unit> {
        return try {
            if (!SyncScheduler.isNetworkAvailable(appContext)) {
                SyncScheduler.scheduleBackgroundSync(appContext)
                return Result.success(Unit)
            }
            withTimeoutOrNull(8_000L) {
                db.waitForPendingWrites().await()
            }
            _hasPendingWrites.value = false
            LocalPreferencesStore(appContext).markSyncedNow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncScheduler.scheduleBackgroundSync(appContext)
            Result.failure(e)
        }
    }

    /**
     * When offline, Firestore persists writes to local disk cache immediately, while the
     * returned Task resolves only when acknowledged by the server. Wait briefly when online
     * so offline writes return seamlessly without freezing the UI!
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOfflineAware() {
        if (SyncScheduler.isNetworkAvailable(appContext)) {
            val completed = withTimeoutOrNull(4_000L) {
                this@awaitOfflineAware.await()
                true
            }
            if (completed == null) {
                _hasPendingWrites.value = true
                SyncScheduler.scheduleBackgroundSync(appContext)
            }
        } else {
            _hasPendingWrites.value = true
            SyncScheduler.scheduleBackgroundSync(appContext)
        }
    }

    private fun scheduleSyncIfOffline() {
        if (!SyncScheduler.isNetworkAvailable(appContext)) {
            SyncScheduler.scheduleBackgroundSync(appContext)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
