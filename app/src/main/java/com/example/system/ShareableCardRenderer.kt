package com.example.system

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.AttendanceMilestone
import com.example.data.AttendancePatternInsights
import com.example.data.AttendanceStats
import com.example.data.DateUtils
import com.example.data.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Generates high-resolution PNG graphic cards on Android Canvas and shares them via FileProvider
 * (zero storage permissions needed — fully Play-policy compliant).
 */
object ShareableCardRenderer {

    fun renderMonthlyWrappedBitmap(
        profile: UserProfile?,
        monthLabel: String,
        stats: AttendanceStats,
        insights: AttendancePatternInsights,
        streak: Int,
        milestones: List<AttendanceMilestone>
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Deep Royal Navy gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#081229"),
                    Color.parseColor("#0F244D"),
                    Color.parseColor("#0A1836")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Ambient glow circles
        val goldGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#24E2B93B")
        }
        canvas.drawCircle(940f, 180f, 260f, goldGlow)
        val blueGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#283B82F6")
        }
        canvas.drawCircle(120f, 1180f, 300f, blueGlow)

        val goldColor = Color.parseColor("#E2B93B")
        val emeraldColor = Color.parseColor("#34D399")
        val coralColor = Color.parseColor("#FB7185")
        val accentColor = if (stats.eligible) emeraldColor else coralColor

        // Header Pill
        val pillRect = RectF(72f, 76f, 490f, 140f)
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26E2B93B") }
        val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#88E2B93B")
        }
        canvas.drawRoundRect(pillRect, 40f, 40f, pillBg)
        canvas.drawRoundRect(pillRect, 40f, 40f, pillBorder)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("80ENDANCE WRAPPED", 108f, 118f, pillTextPaint)

        // Month Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(monthLabel, 72f, 238f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A8C0E8")
            textSize = 30f
        }
        val studentMeta = listOfNotNull(
            profile?.preferredDisplayName?.takeIf { it.isNotBlank() },
            profile?.className?.takeIf { it.isNotBlank() },
            profile?.alStream?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ").ifBlank { "GCE A/L Attendance Summary" }
        canvas.drawText(studentMeta, 72f, 288f, subPaint)

        // Hero Attendance Card
        val heroCard = RectF(72f, 340f, width - 72f, 670f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18FFFFFF") }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#35FFFFFF")
        }
        canvas.drawRoundRect(heroCard, 44f, 44f, cardFill)
        canvas.drawRoundRect(heroCard, 44f, 44f, cardStroke)

        val pctBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 148f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pctLabel = if (stats.totalCountedDays == 0) "--%" else String.format(Locale.US, "%.1f%%", stats.percentage)
        canvas.drawText(pctLabel, 116f, 512f, pctBigPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val standingLine = if (stats.eligible) {
            "✓ Eligible (${stats.thresholdPercent}%+)  •  ${stats.safeSkipsRemaining} safe skip${if (stats.safeSkipsRemaining == 1) "" else "s"} left"
        } else {
            "Recovery Mode  •  Attend ${stats.daysNeededToRecover} day${if (stats.daysNeededToRecover == 1) "" else "s"} to reach ${stats.thresholdPercent}%"
        }
        canvas.drawText(standingLine, 116f, 582f, badgePaint)

        val countedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 28f
        }
        canvas.drawText(
            "${stats.effectiveAttended} attended out of ${stats.totalCountedDays} counted school days",
            116f, 628f, countedPaint
        )

        // 2x2 Highlights Grid
        val bestDay = insights.bestWeekday?.let { "${it.fullName} (${it.percentage.toInt()}%)" } ?: "Building..."
        val bestMonth = insights.bestMonth?.let { "${it.shortLabel} (${it.percentage.toInt()}%)" } ?: monthLabel
        val gridTiles = listOf(
            Triple("CURRENT STREAK", "$streak school day${if (streak == 1) "" else "s"}", goldColor),
            Triple("SAFEST WEEKDAY", bestDay, emeraldColor),
            Triple("TOP MONTH", bestMonth, Color.parseColor("#93C5FD")),
            Triple("ABSENT DAYS", "${stats.absentDays} day${if (stats.absentDays == 1) "" else "s"}", coralColor)
        )
        val tileW = (width - 144f - 28f) / 2f
        val tileH = 165f
        gridTiles.forEachIndexed { idx, (label, value, tint) ->
            val col = idx % 2
            val row = idx / 2
            val left = 72f + col * (tileW + 28f)
            val top = 710f + row * (tileH + 24f)
            val rect = RectF(left, top, left + tileW, top + tileH)
            canvas.drawRoundRect(rect, 32f, 32f, cardFill)
            canvas.drawRoundRect(rect, 32f, 32f, cardStroke)

            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.06f
            }
            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, left + 30f, top + 58f, lblPaint)
            canvas.drawText(value.take(22), left + 30f, top + 122f, valPaint)
        }

        // Unlocked Milestones Section
        val unlocked = milestones.filter { it.unlocked }
        val msHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("MILESTONE BADGES (${unlocked.size}/${milestones.size})", 72f, 1138f, msHeaderPaint)

        val chipText = if (unlocked.isEmpty()) {
            "Keep logging daily to unlock your first consistency badge!"
        } else {
            unlocked.take(4).joinToString("   •   ") { "★ ${it.title}" }
        }
        val msBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(chipText.take(64), 72f, 1188f, msBodyPaint)

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80CBD5E1")
            textSize = 24f
        }
        canvas.drawText(
            "80endance  •  Royal College GCE A/L Attendance Companion  •  ${DateUtils.formatReadableShort(DateUtils.todayKey())}",
            72f, height - 62f, footerPaint
        )
        return bmp
    }

    /**
     * Clean, formal light-theme summary card specifically formatted to show parents or guardians.
     * Deliberately excludes all private daily notes or skip-simulator figures.
     */
    fun renderParentSummaryBitmap(
        profile: UserProfile?,
        termName: String,
        stats: AttendanceStats
    ): Bitmap {
        val width = 1080
        val height = 1140
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Crisp paper-white background
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // Royal Navy Header Banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 260f,
                Color.parseColor("#0E2A5C"),
                Color.parseColor("#1B4080"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 270f, headerPaint)

        // Gold accent stripe
        val goldStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D4AF37") }
        canvas.drawRect(0f, 270f, width.toFloat(), 282f, goldStripe)

        val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2B93B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(
            (profile?.schoolName?.uppercase(Locale.US) ?: "ROYAL COLLEGE, COLOMBO"),
            72f, 92f, schoolPaint
        )

        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Student Attendance Summary", 72f, 166f, headerTitle)

        val studentLine = listOfNotNull(
            profile?.fullName?.takeIf { it.isNotBlank() },
            profile?.grade?.takeIf { it.isNotBlank() },
            profile?.className?.takeIf { it.isNotBlank() },
            profile?.alStream?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ").ifBlank { "GCE A/L Student" }
        val headerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6E4FF")
            textSize = 28f
        }
        canvas.drawText(studentLine, 72f, 224f, headerSub)

        // Main Summary Card
        val mainCard = RectF(64f, 330f, width - 64f, 650f)
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#E2E8F0")
        }
        canvas.drawRoundRect(mainCard, 36f, 36f, whiteFill)
        canvas.drawRoundRect(mainCard, 36f, 36f, borderPaint)

        val termLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(termName, 112f, 404f, termLbl)

        val bigPct = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 132f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pctText = if (stats.totalCountedDays == 0) "--%" else String.format(Locale.US, "%.1f%%", stats.percentage)
        canvas.drawText(pctText, 112f, 542f, bigPct)

        // Eligibility Badge
        val badgeColor = if (stats.eligible) Color.parseColor("#059669") else Color.parseColor("#DC2626")
        val badgeBgColor = if (stats.eligible) Color.parseColor("#D1FAE5") else Color.parseColor("#FEE2E2")
        val badgeRect = RectF(560f, 438f, width - 108f, 520f)
        canvas.drawRoundRect(badgeRect, 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBgColor })
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val eligibilityLabel = if (stats.eligible) "✓ ${stats.thresholdPercent}% ELIGIBLE" else "! BELOW ${stats.thresholdPercent}%"
        canvas.drawText(eligibilityLabel, badgeRect.centerX(), 490f, badgeTextPaint)

        val summaryDesc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 28f
        }
        canvas.drawText(
            "Attended ${stats.effectiveAttended} of ${stats.totalCountedDays} counted school days (Minimum required: ${stats.thresholdPercent}%)",
            112f, 608f, summaryDesc
        )

        // Term Breakdown Rows (4 clean metric boxes)
        val breakdown = listOf(
            Triple("Total Attended", "${stats.effectiveAttended} days", Color.parseColor("#059669")),
            Triple("Absent Days", "${stats.absentDays + stats.priorAbsentDays + stats.periodAbsentDays} days", Color.parseColor("#DC2626")),
            Triple("Medical Leaves", "${stats.medicalDays} days", Color.parseColor("#D97706")),
            Triple("School Events / Excused", "${stats.schoolEventDays} days", Color.parseColor("#2563EB"))
        )
        val boxW = (width - 128f - 28f) / 2f
        val boxH = 148f
        breakdown.forEachIndexed { idx, (label, value, tint) ->
            val col = idx % 2
            val row = idx / 2
            val left = 64f + col * (boxW + 28f)
            val top = 690f + row * (boxH + 24f)
            val rect = RectF(left, top, left + boxW, top + boxH)
            canvas.drawRoundRect(rect, 28f, 28f, whiteFill)
            canvas.drawRoundRect(rect, 28f, 28f, borderPaint)

            val lPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, left + 32f, top + 54f, lPaint)
            canvas.drawText(value, left + 32f, top + 114f, vPaint)
        }

        // Footer Note
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
        }
        canvas.drawText(
            "Generated on ${DateUtils.formatReadableFull(DateUtils.todayKey())} via 80endance (Personal notes omitted)",
            64f, height - 54f, footerPaint
        )
        return bmp
    }

    fun shareBitmapViaChooser(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        chooserTitle: String,
        captionText: String
    ): Boolean {
        return runCatching {
            val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
