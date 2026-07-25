package com.example.analyzer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

data class ValidationResult(
    val isValid: Boolean,
    val confidence: Int, // 0 to 100
    val ringDetected: Boolean,
    val structuresDetected: Boolean,
    val cultureResembles: Boolean,
    val reasons: List<String>
)

data class ColonyLoc(
    val x: Float, // Fractional coordinate (0.0 to 1.0)
    val y: Float, // Fractional coordinate (0.0 to 1.0)
    val radius: Float, // Size estimate (fraction of width)
    val confidence: Float,
    val zone: String // "Center", "Middle", "Outer"
)

data class PreprocessResult(
    val grayscale: Bitmap,
    val thresholded: Bitmap,
    val enhanced: Bitmap
)

data class ZoneMetrics(
    val zoneId: String,
    val name: String,
    val count: Int,
    val percent: Double,
    val density: Double // colonies per arbitrary area unit
)

data class ZoneAnalysisResult(
    val centerZone: ZoneMetrics,
    val middleZone: ZoneMetrics,
    val outerZone: ZoneMetrics
)

object ColonyAnalyzer {

    // Scales bitmap to a highly responsive analytical size to ensure real-time speeds in VM
    private const val ANALYTICAL_SIZE = 256

    fun preprocessImage(source: Bitmap): PreprocessResult {
        // 1. Create scalable buffers
        val width = source.width
        val height = source.height
        
        // Scaled Analytical Bitmap
        val scaled = Bitmap.createScaledBitmap(source, ANALYTICAL_SIZE, ANALYTICAL_SIZE, true)
        
        val grayscale = Bitmap.createBitmap(ANALYTICAL_SIZE, ANALYTICAL_SIZE, Bitmap.Config.ARGB_8888)
        val enhanced = Bitmap.createBitmap(ANALYTICAL_SIZE, ANALYTICAL_SIZE, Bitmap.Config.ARGB_8888)
        val thresholded = Bitmap.createBitmap(ANALYTICAL_SIZE, ANALYTICAL_SIZE, Bitmap.Config.ARGB_8888)

        // 2. Grayscale & Contrast Enhancement Loop
        val grayPixels = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        val enhancedPixels = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        
        var minLum = 255
        var maxLum = 0
        
        // Read scaled values
        val pixels = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        scaled.getPixels(pixels, 0, ANALYTICAL_SIZE, 0, 0, ANALYTICAL_SIZE, ANALYTICAL_SIZE)

        // Pass 1: Grayscale + find min/max for scaling contrast
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            
            // Luma calculation
            val grayVal = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            grayPixels[i] = grayVal
            
            if (grayVal < minLum) minLum = grayVal
            if (grayVal > maxLum) maxLum = grayVal
        }

        // Pass 2: Contrast stretching (enhanced) & Adaptive Thresholding
        grayscale.setPixels(grayPixels.map { Color.rgb(it, it, it) }.toIntArray(), 0, ANALYTICAL_SIZE, 0, 0, ANALYTICAL_SIZE, ANALYTICAL_SIZE)
        
        val range = (maxLum - minLum).coerceAtLeast(1)
        for (i in grayPixels.indices) {
            // Contrast Stretching
            val norm = (((grayPixels[i] - minLum).toFloat() / range) * 255).toInt().coerceIn(0, 255)
            enhancedPixels[i] = norm
        }
        enhanced.setPixels(enhancedPixels.map { Color.rgb(it, it, it) }.toIntArray(), 0, ANALYTICAL_SIZE, 0, 0, ANALYTICAL_SIZE, ANALYTICAL_SIZE)

        // Pass 3: Adaptive/Local Thresholding (using local box average)
        val thresholdPixels = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        val kernelSize = 15
        val halfKernel = kernelSize / 2
        val C = 7 // offset parameter

        for (y in 0 until ANALYTICAL_SIZE) {
            for (x in 0 until ANALYTICAL_SIZE) {
                // Compute local average
                var sum = 0
                var count = 0
                for (ky in -halfKernel..halfKernel) {
                    val py = (y + ky).coerceIn(0, ANALYTICAL_SIZE - 1)
                    for (kx in -halfKernel..halfKernel) {
                        val px = (x + kx).coerceIn(0, ANALYTICAL_SIZE - 1)
                        sum += grayPixels[py * ANALYTICAL_SIZE + px]
                        count++
                    }
                }
                val localAverage = sum / count
                val currentPixel = grayPixels[y * ANALYTICAL_SIZE + x]
                
                // Segment based on local context (dark colonies vs bright medium or vice versa)
                // In generic agar, petri dish background is light/translucent, colonies are darker.
                val binary = if (currentPixel < (localAverage - C)) {
                    Color.WHITE // Colony candidate foreground
                } else {
                    Color.BLACK // Background
                }
                thresholdPixels[y * ANALYTICAL_SIZE + x] = binary
            }
        }
        thresholded.setPixels(thresholdPixels, 0, ANALYTICAL_SIZE, 0, 0, ANALYTICAL_SIZE, ANALYTICAL_SIZE)

        return PreprocessResult(
            grayscale = Bitmap.createScaledBitmap(grayscale, width, height, true),
            enhanced = Bitmap.createScaledBitmap(enhanced, width, height, true),
            thresholded = Bitmap.createScaledBitmap(thresholded, width, height, true)
        )
    }

    /**
     * Colony Detection based on Local Minima Peaks & Watershed boundary clustering.
     */
    fun detectColonies(source: Bitmap, userMarkers: List<ColonyLoc> = emptyList()): List<ColonyLoc> {
        val scaled = Bitmap.createScaledBitmap(source, ANALYTICAL_SIZE, ANALYTICAL_SIZE, true)
        val pixels = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        scaled.getPixels(pixels, 0, ANALYTICAL_SIZE, 0, 0, ANALYTICAL_SIZE, ANALYTICAL_SIZE)
        
        val luma = IntArray(ANALYTICAL_SIZE * ANALYTICAL_SIZE)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luma[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        // Circular physical boundary definition of petri dish in analytical coordinates
        val centerX = ANALYTICAL_SIZE / 2f
        val centerY = ANALYTICAL_SIZE / 2f
        val dishRadius = ANALYTICAL_SIZE * 0.44f // 88% diameter dish

        val rawPeaks = mutableListOf<Triple<Int, Int, Float>>() // x, y, size estimate

        // Find local luma minima (peaks representing colony centers) inside dish boundary
        val window = 5
        val step = 2
        for (y in window until ANALYTICAL_SIZE - window step step) {
            for (x in window until ANALYTICAL_SIZE - window step step) {
                // Check if inside Petri dish boundary
                val dx = x - centerX
                val dy = y - centerY
                if (sqrt(dx*dx + dy*dy) > dishRadius) continue

                val index = y * ANALYTICAL_SIZE + x
                val centerLuma = luma[index]

                // For a typical dark colony, luma of center should be less than luma of surrounding boundary
                var isMinima = true
                var outerAvg = 0f
                var outerCount = 0
                for (ky in -2..2) {
                    for (kx in -2..2) {
                        if (kx == 0 && ky == 0) continue
                        val neighborLuma = luma[(y + ky) * ANALYTICAL_SIZE + (x + kx)]
                        if (neighborLuma < centerLuma) {
                            isMinima = false
                            break
                        }
                        outerAvg += neighborLuma
                        outerCount++
                    }
                    if (!isMinima) break
                }

                if (isMinima && outerCount > 0) {
                    outerAvg /= outerCount
                    val contrast = outerAvg - centerLuma
                    if (contrast > 12) { // Minimal local contrast threshold
                        // Estimate colony size by finding boundary where luma jumps back to background luma
                        var radiusEstimate = 2f
                        for (r in 2..15) {
                            var rEdgeAvg = 0f
                            var edgeCount = 0
                            // Sample circular edges
                            for (angle in 0 until 360 step 60) {
                                val rad = Math.toRadians(angle.toDouble())
                                val ex = (x + r * Math.cos(rad)).toInt().coerceIn(0, ANALYTICAL_SIZE - 1)
                                val ey = (y + r * Math.sin(rad)).toInt().coerceIn(0, ANALYTICAL_SIZE - 1)
                                rEdgeAvg += luma[ey * ANALYTICAL_SIZE + ex]
                                edgeCount++
                            }
                            rEdgeAvg /= edgeCount
                            if (rEdgeAvg >= outerAvg - 3f) {
                                radiusEstimate = r.toFloat()
                                break
                            }
                        }
                        rawPeaks.add(Triple(x, y, radiusEstimate))
                    }
                }
            }
        }

        // Watershed overlapping resolution: Merge peaks that are closer than their combined radii
        val processedPeaks = mutableListOf<ColonyLoc>()
        val sortedPeaks = rawPeaks.sortedByDescending { it.third } // largest first

        for (peak in sortedPeaks) {
            val px = peak.first.toFloat()
            val py = peak.second.toFloat()
            val pr = peak.third

            var isDuplicate = false
            for (pColony in processedPeaks) {
                val cx = pColony.x * ANALYTICAL_SIZE
                val cy = pColony.y * ANALYTICAL_SIZE
                val cr = pColony.radius * ANALYTICAL_SIZE
                
                val dist = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
                // If centers are too close (nearly fully overlapping), count as same
                if (dist < (pr + cr) * 0.45f || dist < 3.5f) {
                    isDuplicate = true
                    break
                }
            }

            if (!isDuplicate) {
                // Convert coordinates to fractional scale
                val fx = px / ANALYTICAL_SIZE
                val fy = py / ANALYTICAL_SIZE
                val fr = pr / ANALYTICAL_SIZE
                
                // Calculate zone
                val dx = fx - 0.5f
                val dy = fy - 0.5f
                val unitDist = sqrt(dx * dx + dy * dy) / 0.44f // relative to dish radius
                val zoneName = when {
                    unitDist < 0.33f -> "Center"
                    unitDist < 0.66f -> "Middle"
                    else -> "Outer"
                }

                processedPeaks.add(
                    ColonyLoc(
                        x = fx,
                        y = fy,
                        radius = fr.coerceIn(0.015f, 0.08f),
                        confidence = 0.75f + (pr * 0.02f).coerceAtMost(0.25f),
                        zone = zoneName
                    )
                )
            }
        }

        // Apply manual markers if there are any
        val finalColonies = processedPeaks.toMutableList()
        // If userMarkers exist, we merge or replace
        for (m in userMarkers) {
            // Ensure no auto colony is right where user placed a manual marker to avoid duplicate counts
            finalColonies.removeIf { auto ->
                val dist = sqrt((auto.x - m.x) * (auto.x - m.x) + (auto.y - m.y) * (auto.y - m.y))
                dist < 0.04f
            }
            finalColonies.add(m)
        }

        return finalColonies
    }

    /**
     * Zone-wise detailed CFU, counts and density distribution analysis.
     */
    fun analyzeZones(colonies: List<ColonyLoc>): ZoneAnalysisResult {
        var centerCount = 0
        var middleCount = 0
        var outerCount = 0

        for (c in colonies) {
            when (c.zone) {
                "Center" -> centerCount++
                "Middle" -> middleCount++
                else -> outerCount++
            }
        }

        val total = colonies.size.coerceAtLeast(1).toDouble()

        // Zone relative areas (Center r=0.33 -> area ~ 11%, Mid r=0.66 -> area ~ 33%, Outer r=1.0 -> area ~ 56%)
        val centerArea = 0.1089
        val middleArea = 0.3267
        val outerArea = 0.5644

        return ZoneAnalysisResult(
            centerZone = ZoneMetrics(
                zoneId = "center",
                name = "Center Zone",
                count = centerCount,
                percent = (centerCount / total) * 100.0,
                density = centerCount.toDouble() / centerArea
            ),
            middleZone = ZoneMetrics(
                zoneId = "middle",
                name = "Middle Zone",
                count = middleCount,
                percent = (middleCount / total) * 100.0,
                density = middleCount.toDouble() / middleArea
            ),
            outerZone = ZoneMetrics(
                zoneId = "outer",
                name = "Outer Zone",
                count = outerCount,
                percent = (outerCount / total) * 100.0,
                density = outerCount.toDouble() / outerArea
            )
        )
    }

    /**
     * Compute CFU based on mathematical equation:
     * CFU = (Colonies * Dilution Factor) / Sample Volume
     */
    fun calculateCfu(colonies: Int, dilutionFactor: Double, sampleVolume: Double): Double {
        if (sampleVolume <= 0.0) return 0.0
        // dilutionFactor is standard negative exponent power (e.g. 10^3 is 1000)
        return (colonies.toDouble() * dilutionFactor) / sampleVolume
    }

    fun formatCfuScientific(cfu: Double): String {
        if (cfu == 0.0) return "0.0 CFU"
        val exp = kotlin.math.log10(cfu).toInt()
        val base = cfu / Math.pow(10.0, exp.toDouble())
        return String.format("%.2f × 10^%d CFU/mL", base, exp)
    }

    /**
     * Simple circular layout heatmap generator overlay context.
     */
    fun drawDensityHeatmap(width: Int, height: Int, colonies: List<ColonyLoc>): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw overlapping radial gradients for colony locations
        for (colony in colonies) {
            val cx = colony.x * width
            val cy = colony.y * height
            val maxRad = (colony.radius * width * 3.5f).coerceAtLeast(30f)

            // Multi-tier gradient rings to smooth density overlay
            for (rFactor in 10 downTo 1) {
                val currentRad = maxRad * (rFactor / 10f)
                val alpha = (12 * (11 - rFactor)).coerceAtMost(70)
                paint.color = Color.argb(alpha, 255, 60, 60) // Density glow color
                canvas.drawCircle(cx, cy, currentRad, paint)
            }
        }

        return bitmap
    }

    fun validateImage(source: Bitmap): ValidationResult {
        val size = 256
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        
        val luma = IntArray(size * size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luma[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        // 1. Search for best centered circular boundary
        var bestNThreshold = 0
        var bestRadius = 110f
        var bestCenterX = 128f
        var bestCenterY = 128f
        
        // Scan concentric circles to find highest-scoring Petri dish plastic border
        for (cx in 100..156 step 4) {
            for (cy in 100..156 step 4) {
                for (r in 85..125 step 4) {
                    var matchCount = 0
                    val numPoints = 36
                    for (i in 0 until numPoints) {
                        val angle = 2 * Math.PI * i / numPoints
                        val xOut = (cx + (r + 3) * Math.cos(angle)).toInt().coerceIn(0, size - 1)
                        val yOut = (cy + (r + 3) * Math.sin(angle)).toInt().coerceIn(0, size - 1)
                        val xIn = (cx + (r - 3) * Math.cos(angle)).toInt().coerceIn(0, size - 1)
                        val yIn = (cy + (r - 3) * Math.sin(angle)).toInt().coerceIn(0, size - 1)
                        
                        val lOut = luma[yOut * size + xOut]
                        val lIn = luma[yIn * size + xIn]
                        if (Math.abs(lOut - lIn) > 13) {
                            matchCount++
                        }
                    }
                    if (matchCount > bestNThreshold) {
                        bestNThreshold = matchCount
                        bestRadius = r.toFloat()
                        bestCenterX = cx.toFloat()
                        bestCenterY = cy.toFloat()
                    }
                }
            }
        }
        
        val ringDetected = bestNThreshold >= 11
        val petriDishScore = if (ringDetected) 40 else 0
        
        // 2. Measure agar uniformity inside
        var sumLuma = 0.0
        var countIn = 0
        val rInner = bestRadius * 0.90f
        
        var gradientSum = 0.0
        var highGradientsCount = 0
        
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - bestCenterX
                val dy = y - bestCenterY
                val dist = sqrt(dx*dx + dy*dy)
                if (dist < rInner) {
                    val index = y * size + x
                    sumLuma += luma[index]
                    countIn++
                    
                    if (x > 0 && x < size - 1 && y > 0 && y < size - 1) {
                        val gx = luma[y * size + (x + 1)] - luma[y * size + (x - 1)]
                        val gy = luma[(y + 1) * size + x] - luma[(y - 1) * size + x]
                        val grad = sqrt((gx*gx + gy*gy).toDouble())
                        gradientSum += grad
                        if (grad > 15.0) {
                            highGradientsCount++
                        }
                    }
                }
            }
        }
        
        val countInCoerced = countIn.coerceAtLeast(1)
        val meanLuma = sumLuma / countInCoerced
        
        var sumVar = 0.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - bestCenterX
                val dy = y - bestCenterY
                if (sqrt(dx*dx + dy*dy) < rInner) {
                    val diff = luma[y * size + x] - meanLuma
                    sumVar += diff * diff
                }
            }
        }
        val variance = sumVar / countInCoerced
        val stdDev = sqrt(variance)
        
        val edgePercent = (highGradientsCount.toFloat() / countInCoerced) * 100f
        
        // Agar must be uniform; complex backgrounds, keyboards, or screenshots will have huge edge percent or high luma deviations.
        val agarUniformityScore = (100f - (edgePercent * 2.8f) - (stdDev.toFloat() * 0.45f)).coerceIn(0f, 100f)
        val cultureResembles = agarUniformityScore >= 45.0f
        val cultureScore = if (cultureResembles) 20 else 0
        
        // 3. Colony-like structural check
        val rawPeaksCount = detectRawPeaksCount(luma, bestCenterX, bestCenterY, bestRadius)
        val structuresDetected = rawPeaksCount >= 1
        val colonyScore = if (structuresDetected) 40 else 0
        
        // Formulate final weighted validation confidence score
        val finalConfidence = petriDishScore + colonyScore + cultureScore
        val isValid = finalConfidence >= 70
        
        val reasons = mutableListOf<String>()
        reasons.add("Petri Dish Detected: ${if (ringDetected) "Yes" else "No"}")
        reasons.add("Colonies Detected: ${if (structuresDetected) "Yes" else "No"}")
        reasons.add("Culture Plate Match: ${if (cultureResembles) "Yes" else "No"}")
        reasons.add("Confidence: $finalConfidence%")
        reasons.add("Status: ${if (isValid) "Valid Sample" else "Invalid Sample"}")
        
        return ValidationResult(
            isValid = isValid,
            confidence = finalConfidence,
            ringDetected = ringDetected,
            structuresDetected = structuresDetected,
            cultureResembles = cultureResembles,
            reasons = reasons
        )
    }

    private fun detectRawPeaksCount(luma: IntArray, centerX: Float, centerY: Float, radius: Float): Int {
        var count = 0
        val size = 256
        for (y in 5 until size - 5 step 3) {
            for (x in 5 until size - 5 step 3) {
                val dx = x - centerX
                val dy = y - centerY
                if (sqrt(dx*dx + dy*dy) > radius * 0.95f) continue
                
                val centerLuma = luma[y * size + x]
                var isMinima = true
                var outerSum = 0
                var outerCount = 0
                for (ky in -2..2 step 2) {
                    for (kx in -2..2 step 2) {
                        if (kx == 0 && ky == 0) continue
                        val neighbor = luma[(y + ky) * size + (x + kx)]
                        if (neighbor < centerLuma) {
                            isMinima = false
                            break
                        }
                        outerSum += neighbor
                        outerCount++
                    }
                    if (!isMinima) break
                }
                if (isMinima && outerCount > 0) {
                    val averageOuter = outerSum.toFloat() / outerCount
                    if (averageOuter - centerLuma > 10.0f) {
                        count++
                    }
                }
            }
        }
        return count
    }
}
