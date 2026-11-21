package com.example.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Structured Sri Lankan GCE A/L Subject Buckets (Stream -> Bucket 1/2/3 -> Subjects)
 * and Class Lists (Grade + Stream -> Classes), editable by the admin and synced via
 * Firestore /app_config/academic_catalog.
 */
data class AcademicCatalog(
    val streamSubjectBuckets: Map<String, Map<String, List<String>>> = defaultStreamSubjectBuckets(),
    val gradeStreamClasses: Map<String, List<String>> = defaultGradeStreamClasses()
) {
    fun bucketsForStream(stream: String): Map<String, List<String>> {
        val raw = streamSubjectBuckets[stream] ?: defaultStreamSubjectBuckets()[stream].orEmpty()
        return BUCKET_KEYS.associateWith { bucket ->
            raw[bucket]?.filter { it.isNotBlank() }?.distinct().orEmpty()
        }
    }

    fun subjectsFor(stream: String, bucketKey: String): List<String> {
        return bucketsForStream(stream)[bucketKey].orEmpty()
    }

    fun defaultSelectionForStream(stream: String): List<String> {
        val buckets = bucketsForStream(stream)
        val fallback = AppConstants.defaultSubjectsForStream(stream)
        return BUCKET_KEYS.mapIndexed { idx, key ->
            buckets[key]?.firstOrNull() ?: fallback.getOrElse(idx) { "" }
        }
    }

    fun classesFor(grade: String, stream: String): List<String> {
        val key = classGroupKey(grade, stream)
        return (gradeStreamClasses[key] ?: defaultGradeStreamClasses()[key].orEmpty())
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun withAddedSubject(stream: String, bucketKey: String, subject: String): AcademicCatalog {
        val clean = subject.trim().take(60)
        if (clean.isEmpty()) return this
        val updatedStreams = streamSubjectBuckets.toMutableMap()
        val existingBuckets = bucketsForStream(stream).toMutableMap()
        val currentList = existingBuckets[bucketKey].orEmpty()
        if (currentList.none { it.equals(clean, ignoreCase = true) }) {
            existingBuckets[bucketKey] = currentList + clean
        }
        updatedStreams[stream] = existingBuckets
        return copy(streamSubjectBuckets = updatedStreams)
    }

    fun withRemovedSubject(stream: String, bucketKey: String, subject: String): AcademicCatalog {
        val updatedStreams = streamSubjectBuckets.toMutableMap()
        val existingBuckets = bucketsForStream(stream).toMutableMap()
        existingBuckets[bucketKey] = existingBuckets[bucketKey].orEmpty()
            .filterNot { it.equals(subject, ignoreCase = true) }
        updatedStreams[stream] = existingBuckets
        return copy(streamSubjectBuckets = updatedStreams)
    }

    fun withAddedClass(grade: String, stream: String, classCode: String): AcademicCatalog {
        val clean = classCode.trim().uppercase().take(20)
        if (clean.isEmpty()) return this
        val key = classGroupKey(grade, stream)
        val updatedMap = gradeStreamClasses.toMutableMap()
        val current = classesFor(grade, stream)
        if (current.none { it.equals(clean, ignoreCase = true) }) {
            updatedMap[key] = current + clean
        }
        return copy(gradeStreamClasses = updatedMap)
    }

    fun withRemovedClass(grade: String, stream: String, classCode: String): AcademicCatalog {
        val key = classGroupKey(grade, stream)
        val updatedMap = gradeStreamClasses.toMutableMap()
        updatedMap[key] = classesFor(grade, stream)
            .filterNot { it.equals(classCode, ignoreCase = true) }
        return copy(gradeStreamClasses = updatedMap)
    }

    fun toFirestoreJsonPair(): Pair<String, String> {
        val streamsObj = JSONObject()
        AppConstants.AL_STREAMS.forEach { stream ->
            val bucketsObj = JSONObject()
            val buckets = bucketsForStream(stream)
            BUCKET_KEYS.forEach { bucketKey ->
                val arr = JSONArray()
                buckets[bucketKey].orEmpty().forEach { arr.put(it) }
                bucketsObj.put(bucketKey, arr)
            }
            streamsObj.put(stream, bucketsObj)
        }

        val classesObj = JSONObject()
        AppConstants.GRADES.forEach { grade ->
            AppConstants.AL_STREAMS.forEach { stream ->
                val key = classGroupKey(grade, stream)
                val arr = JSONArray()
                classesFor(grade, stream).forEach { arr.put(it) }
                classesObj.put(key, arr)
            }
        }
        return streamsObj.toString() to classesObj.toString()
    }

    companion object {
        const val BUCKET_1 = "Bucket 1"
        const val BUCKET_2 = "Bucket 2"
        const val BUCKET_3 = "Bucket 3"
        val BUCKET_KEYS = listOf(BUCKET_1, BUCKET_2, BUCKET_3)

        fun classGroupKey(grade: String, stream: String): String = "$grade|$stream"

        fun defaultCatalog(): AcademicCatalog = AcademicCatalog(
            streamSubjectBuckets = defaultStreamSubjectBuckets(),
            gradeStreamClasses = defaultGradeStreamClasses()
        )

        fun defaultStreamSubjectBuckets(): Map<String, Map<String, List<String>>> = mapOf(
            "Physical Science" to mapOf(
                BUCKET_1 to listOf("Combined Maths"),
                BUCKET_2 to listOf("Physics"),
                BUCKET_3 to listOf("Chemistry", "ICT")
            ),
            "Bio Science" to mapOf(
                BUCKET_1 to listOf("Biology"),
                BUCKET_2 to listOf("Chemistry"),
                BUCKET_3 to listOf("Physics", "Agricultural Science")
            ),
            "Commerce" to mapOf(
                BUCKET_1 to listOf("Accounting"),
                BUCKET_2 to listOf("Business Studies", "Business Statistics"),
                BUCKET_3 to listOf("Economics", "ICT", "German", "French", "Geography")
            ),
            "Arts" to mapOf(
                BUCKET_1 to listOf("Economics", "Political Science", "Geography", "History", "Sinhala", "Tamil"),
                BUCKET_2 to listOf("Logic & Scientific Method", "Media & Communication", "Home Economics", "Buddhism", "Hinduism", "Islam", "Christianity"),
                BUCKET_3 to listOf("English Literature", "ICT", "French", "German", "Japanese", "Chinese", "Hindi", "Drama & Theatre", "Art", "Music")
            ),
            "Technology" to mapOf(
                BUCKET_1 to listOf("Engineering Tech", "Bio Systems Tech"),
                BUCKET_2 to listOf("Science for Tech"),
                BUCKET_3 to listOf("ICT", "Economics", "Geography", "Agricultural Science", "Business Studies", "Art")
            )
        )

        fun defaultGradeStreamClasses(): Map<String, List<String>> = mapOf(
            classGroupKey("Grade 12", "Bio Science") to listOf("12S1", "12S2", "12S3", "12S4", "12S5", "12S6"),
            classGroupKey("Grade 12", "Physical Science") to listOf("12M1", "12M2", "12M3", "12M4", "12M5", "12M6"),
            classGroupKey("Grade 12", "Commerce") to listOf("12C1", "12C2", "12C3", "12C4"),
            classGroupKey("Grade 12", "Arts") to listOf("12A1", "12A2"),
            classGroupKey("Grade 12", "Technology") to listOf("12T1", "12T2"),
            classGroupKey("Grade 13", "Bio Science") to listOf("13S1", "13S2", "13S3", "13S4", "13S5", "13S6"),
            classGroupKey("Grade 13", "Physical Science") to listOf("13M1", "13M2", "13M3", "13M4", "13M5", "13M6"),
            classGroupKey("Grade 13", "Commerce") to listOf("13C1", "13C2", "13C3", "13C4"),
            classGroupKey("Grade 13", "Arts") to listOf("13A1", "13A2"),
            classGroupKey("Grade 13", "Technology") to listOf("13T1", "13T2")
        )

        fun fromFirestoreJson(
            streamSubjectBucketsJson: String?,
            gradeStreamClassesJson: String?
        ): AcademicCatalog {
            val defaultStreams = defaultStreamSubjectBuckets().toMutableMap()
            if (!streamSubjectBucketsJson.isNullOrBlank()) {
                runCatching {
                    val root = JSONObject(streamSubjectBucketsJson)
                    val streamIter = root.keys()
                    while (streamIter.hasNext()) {
                        val streamKey = streamIter.next()
                        val bucketsJson = root.optJSONObject(streamKey) ?: continue
                        val bucketMap = mutableMapOf<String, List<String>>()
                        BUCKET_KEYS.forEach { bKey ->
                            val arr = bucketsJson.optJSONArray(bKey)
                            if (arr != null) {
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) {
                                    val item = arr.optString(i).trim()
                                    if (item.isNotEmpty()) list.add(item)
                                }
                                bucketMap[bKey] = list.distinct()
                            } else {
                                bucketMap[bKey] = defaultStreams[streamKey]?.get(bKey).orEmpty()
                            }
                        }
                        defaultStreams[streamKey] = bucketMap
                    }
                }
            }

            val defaultClasses = defaultGradeStreamClasses().toMutableMap()
            if (!gradeStreamClassesJson.isNullOrBlank()) {
                runCatching {
                    val root = JSONObject(gradeStreamClassesJson)
                    val iter = root.keys()
                    while (iter.hasNext()) {
                        val key = iter.next()
                        val arr = root.optJSONArray(key) ?: continue
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val item = arr.optString(i).trim().uppercase()
                            if (item.isNotEmpty()) list.add(item)
                        }
                        defaultClasses[key] = list.distinct()
                    }
                }
            }

            return AcademicCatalog(
                streamSubjectBuckets = defaultStreams,
                gradeStreamClasses = defaultClasses
            )
        }
    }
}
