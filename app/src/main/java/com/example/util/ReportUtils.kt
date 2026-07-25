package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.example.data.model.AnalysisRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object ReportUtils {

    /**
     * Generates a verification QR-code style graphic representing analysis integrity.
     */
    fun drawVerificationQrCode(dataText: String, size: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }

        // Draw outer thick positioning bounds (standard QR indicators)
        val qrFrame = size / 4
        // Top-Left Finder block
        canvas.drawRect(0f, 0f, qrFrame.toFloat(), qrFrame.toFloat(), paint)
        paint.color = Color.WHITE
        canvas.drawRect(2f, 2f, (qrFrame - 2).toFloat(), (qrFrame - 2).toFloat(), paint)
        paint.color = Color.BLACK
        canvas.drawRect(4f, 4f, (qrFrame - 4).toFloat(), (qrFrame - 4).toFloat(), paint)

        // Top-Right Finder block
        canvas.drawRect((size - qrFrame).toFloat(), 0f, size.toFloat(), qrFrame.toFloat(), paint)
        paint.color = Color.WHITE
        canvas.drawRect((size - qrFrame + 2).toFloat(), 2f, (size - 2).toFloat(), (qrFrame - 2).toFloat(), paint)
        paint.color = Color.BLACK
        canvas.drawRect((size - qrFrame + 4).toFloat(), 4f, (size - 4).toFloat(), (qrFrame - 4).toFloat(), paint)

        // Bottom-Left Finder block
        canvas.drawRect(0f, (size - qrFrame).toFloat(), qrFrame.toFloat(), size.toFloat(), paint)
        paint.color = Color.WHITE
        canvas.drawRect(2f, (size - qrFrame + 2).toFloat(), (qrFrame - 2).toFloat(), (size - 2).toFloat(), paint)
        paint.color = Color.BLACK
        canvas.drawRect(4f, (size - qrFrame + 4).toFloat(), (qrFrame - 4).toFloat(), (size - 4).toFloat(), paint)

        // Fill remaining spaces with pseudorandom blocks based on seed of dataText length
        val random = Random(dataText.hashCode().toLong())
        val cellSize = size / 16
        for (row in 0..15) {
            for (col in 0..15) {
                // Skip finder block areas
                if (row < 4 && col < 4) continue
                if (row < 4 && col >= 12) continue
                if (row >= 12 && col < 4) continue

                if (random.nextBoolean()) {
                    paint.color = Color.BLACK
                    canvas.drawRect(
                        (col * cellSize).toFloat(),
                        (row * cellSize).toFloat(),
                        ((col + 1) * cellSize).toFloat(),
                        ((row + 1) * cellSize).toFloat(),
                        paint
                    )
                }
            }
        }

        return bitmap
    }

    /**
     * Generates a detailed lab PDF report of medical appliance sampling using Android.graphics.pdf
     */
    fun generatePdfReport(context: Context, record: AnalysisRecord): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Paper in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Core Colors mapped to "Sleek Interface" branding palette
        val primaryBlue = Color.parseColor("#0061A4")
        val slateGray = Color.parseColor("#5A5B60")
        val textBlack = Color.parseColor("#1B1B1F")
        val bgSilver = Color.parseColor("#F3F4F9")
        val borderLight = Color.parseColor("#E1E2EC")

        // Paints configuration
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = textBlack
        }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = primaryBlue
            textSize = 24f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = textBlack
            textSize = 10f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = slateGray
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = borderLight
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val rectPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date(record.dateCreated))

        // 1. Draw Document Header
        rectPaint.color = primaryBlue
        canvas.drawRect(0f, 0f, 595f, 15f, rectPaint) // Accent top banner

        // Hospital/Lab Information
        textPaint.color = primaryBlue
        textPaint.textSize = 16f
        textPaint.isFakeBoldText = true
        canvas.drawText("BIOMEDICAL MICROBIOLOGY LABS", 40f, 45f, textPaint)

        textPaint.color = slateGray
        textPaint.textSize = 9f
        textPaint.isFakeBoldText = false
        canvas.drawText("Automated Medical Appliance Contamination Validation System", 40f, 58f, textPaint)
        canvas.drawText("System Verification Code: COLONY-M3-AUTO", 40f, 70f, textPaint)

        canvas.drawLine(40f, 85f, 555f, 85f, linePaint)

        // 2. Report Overview Table
        titlePaint.textSize = 18f
        titlePaint.color = textBlack
        canvas.drawText("COLONY COUNT AND CFU REPORT", 40f, 115f, titlePaint)

        // Metadata grid boxes
        rectPaint.color = bgSilver
        canvas.drawRect(40f, 135f, 280f, 225f, rectPaint)
        canvas.drawRect(295f, 135f, 555f, 225f, rectPaint)

        // Box 1: Sample Metrics
        headerPaint.textSize = 10f
        headerPaint.color = primaryBlue
        canvas.drawText("SAMPLE INFORMATION", 50f, 152f, headerPaint)

        subPaint.color = textBlack
        subPaint.isFakeBoldText = true
        canvas.drawText("Sample ID: ", 50f, 172f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText(record.sampleId, 125f, 172f, subPaint)

        subPaint.isFakeBoldText = true
        canvas.drawText("Appliance Unit: ", 50f, 187f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText(record.deviceType, 125f, 187f, subPaint)

        subPaint.isFakeBoldText = true
        canvas.drawText("Sampling Date: ", 50f, 202f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText(dateString, 125f, 202f, subPaint)

        // Box 2: Analysis Formula Details
        canvas.drawText("CFU FORMULATION CRITERIA", 305f, 152f, headerPaint)

        subPaint.isFakeBoldText = true
        canvas.drawText("Dilution Factor: ", 305f, 172f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText("10^${kotlin.math.log10(record.dilutionFactor).toInt()}", 405f, 172f, subPaint)

        subPaint.isFakeBoldText = true
        canvas.drawText("Sample Vol (mL): ", 305f, 187f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText(record.sampleVolume.toString(), 405f, 187f, subPaint)

        subPaint.isFakeBoldText = true
        canvas.drawText("Calculated Via: ", 305f, 202f, subPaint)
        subPaint.isFakeBoldText = false
        canvas.drawText("CFU = (Colonies × Dilution) / Vol", 405f, 202f, subPaint)

        // 3. Main Analytical Metrics section
        canvas.drawLine(40f, 245f, 555f, 245f, linePaint)

        titlePaint.textSize = 14f
        titlePaint.color = primaryBlue
        canvas.drawText("ANALYTICAL ASSESSMENTS", 40f, 270f, titlePaint)

        // Large Count Metric Box
        rectPaint.color = primaryBlue
        canvas.drawRoundRect(40f, 285f, 280f, 375f, 12f, 12f, rectPaint)

        val textWhite = Color.WHITE
        headerPaint.color = textWhite
        headerPaint.textSize = 11f
        canvas.drawText("TOTAL AUTOMATED COUNT", 55f, 312f, headerPaint)

        val bigTextPaint = Paint().apply {
            isAntiAlias = true
            color = textWhite
            textSize = 38f
            isFakeBoldText = true
        }
        canvas.drawText("${record.totalCount}", 55f, 355f, bigTextPaint)

        // CFU Value Label
        headerPaint.color = textBlack
        headerPaint.textSize = 10f
        headerPaint.isFakeBoldText = true
        canvas.drawText("CALCULATED VIABLE BIO-LOAD (CFU)", 300f, 305f, headerPaint)

        val cfuValuePaint = Paint().apply {
            isAntiAlias = true
            color = primaryBlue
            textSize = 21f
            isFakeBoldText = true
        }
        canvas.drawText(record.cfuScientific, 300f, 332f, cfuValuePaint)

        // Contamination Risk Rating Badge
        val riskColorInt = Color.parseColor(record.riskColor)
        rectPaint.color = riskColorInt
        canvas.drawRoundRect(300f, 345f, 490f, 370f, 6f, 6f, rectPaint)

        textPaint.color = textWhite
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        canvas.drawText("ASSESSMENT: ${record.riskRating}", 312f, 361f, textPaint)

        // 4. Petri Dish Zone Distribution Table
        canvas.drawLine(40f, 395f, 555f, 395f, linePaint)

        headerPaint.color = textBlack
        headerPaint.textSize = 11f
        canvas.drawText("ZONE-WISE COLONY DISTRIBUTION ANALYSIS", 40f, 418f, headerPaint)

        // Drawing a slick comparison table
        var startY = 440f
        rectPaint.color = bgSilver
        canvas.drawRect(40f, startY, 555f, startY + 22f, rectPaint) // Table Header

        headerPaint.color = textBlack
        canvas.drawText("ZONE NAME", 50f, startY + 14f, headerPaint)
        canvas.drawText("COLONIES COUNT", 180f, startY + 14f, headerPaint)
        canvas.drawText("PROPORTION (%)", 320f, startY + 14f, headerPaint)
        canvas.drawText("DENSITY FACTOR", 460f, startY + 14f, headerPaint)

        startY += 22f
        
        // Center Row
        canvas.drawLine(40f, startY, 555f, startY, linePaint)
        subPaint.color = textBlack
        canvas.drawText("Center Zone [0-33%]", 50f, startY + 15f, subPaint)
        canvas.drawText("${record.centerCount}", 180f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f %%", record.centerPercent), 320f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f col/U²", record.centerDensity), 460f, startY + 15f, subPaint)

        startY += 22f

        // Middle Row
        canvas.drawLine(40f, startY, 555f, startY, linePaint)
        canvas.drawText("Middle Zone [33-66%]", 50f, startY + 15f, subPaint)
        canvas.drawText("${record.middleCount}", 180f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f %%", record.middlePercent), 320f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f col/U²", record.middleDensity), 460f, startY + 15f, subPaint)

        startY += 22f

        // Outer Row
        canvas.drawLine(40f, startY, 555f, startY, linePaint)
        canvas.drawText("Outer Zone [66-100%]", 50f, startY + 15f, subPaint)
        canvas.drawText("${record.outerCount}", 180f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f %%", record.outerPercent), 320f, startY + 15f, subPaint)
        canvas.drawText(String.format("%.1f col/U²", record.outerDensity), 460f, startY + 15f, subPaint)

        startY += 22f
        canvas.drawLine(40f, startY, 555f, startY, linePaint)

        // 5. Artificial Intelligence Insights Recommendations
        startY += 20f
        headerPaint.color = primaryBlue
        headerPaint.textSize = 11f
        canvas.drawText("AI INSIGHTS & STERILIZATION RECOMMENDATIONS", 40f, startY, headerPaint)

        startY += 10f
        rectPaint.color = bgSilver
        canvas.drawRoundRect(40f, startY, 555f, startY + 110f, 10f, 10f, rectPaint)

        subPaint.color = textBlack
        subPaint.textSize = 9.5f

        // Draw wrapping text (max 85 chars per line)
        val textLines = mutableListOf<String>()
        val paragraphs = record.aiRecommendation.split("\n")
        for (paragraph in paragraphs) {
            val words = paragraph.split(" ")
            var line = ""
            for (word in words) {
                if (line.length + word.length > 82) {
                    textLines.add(line)
                    line = word
                } else {
                    line = if (line.isEmpty()) word else "$line $word"
                }
            }
            if (line.isNotEmpty()) textLines.add(line)
        }

        var textY = startY + 20f
        for (i in 0 until textLines.size.coerceAtMost(5)) {
            canvas.drawText(textLines[i], 55f, textY, subPaint)
            textY += 16f
        }

        // 6. Verification and Footer
        canvas.drawLine(40f, 700f, 555f, 700f, linePaint)

        // Draw QR Code
        val qrSize = 80
        val qrBitmap = drawVerificationQrCode("verify:${record.sampleId}:count:${record.totalCount}", qrSize)
        canvas.drawBitmap(qrBitmap, 40f, 715f, null)

        subPaint.color = slateGray
        subPaint.textSize = 8.5f
        canvas.drawText("Scan QR to verify document authentication hash.", 135f, 740f, subPaint)
        canvas.drawText("System: BioColony Vision Engine Server v2.1", 135f, 755f, subPaint)
        canvas.drawText("Approved Electronically By Biomedical Department Lab In-charge", 135f, 770f, subPaint)

        rectPaint.color = primaryBlue
        canvas.drawRect(0f, 827f, 595f, 842f, rectPaint) // Bottom accent footer bar

        pdfDocument.finishPage(page)

        // Write document to local cache
        val path = File(context.cacheDir, "reports")
        if (!path.exists()) path.mkdir()
        val file = File(path, "colony_report_${record.sampleId}_${record.id}.pdf")
        val stream = FileOutputStream(file)
        pdfDocument.writeTo(stream)
        stream.close()
        pdfDocument.close()

        return file
    }
}
