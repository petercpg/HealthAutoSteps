package io.andautowalk

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.random.Random

object LogicUtils {

    /**
     * Calculates the delay in minutes until the next sync time.
     */
    fun calculateNextSyncDelay(now: LocalDateTime, syncTime: LocalTime, forceNextDay: Boolean = false): Long {
        var nextSync = now.withHour(syncTime.hour).withMinute(syncTime.minute).withSecond(0).withNano(0)
        
        if (forceNextDay || now.isAfter(nextSync) || now.isEqual(nextSync)) {
            nextSync = nextSync.plusDays(1)
        }
        
        return Duration.between(now, nextSync).toMinutes()
    }

    /**
     * Calculates step chunks for distribution.
     * Returns a list of counts to be written in sequential 30-minute blocks.
     */
    fun calculateStepChunks(totalSteps: Long, startTime: Instant, endTime: Instant): List<Long> {
        val duration = Duration.between(startTime, endTime)
        val minutes = duration.toMinutes()
        if (minutes <= 0) {
            return if (totalSteps > 0) listOf(totalSteps) else emptyList()
        }

        val chunks = mutableListOf<Long>()
        var remainingSteps = totalSteps
        var currentStart = startTime

        while (currentStart.isBefore(endTime) && remainingSteps > 0) {
            val chunkMinutes = min(30L, Duration.between(currentStart, endTime).toMinutes())
            if (chunkMinutes <= 0) break

            val currentEnd = currentStart.plus(chunkMinutes, ChronoUnit.MINUTES)

            val fraction = chunkMinutes.toDouble() / minutes.toDouble()
            var chunkSteps = (totalSteps * fraction).toLong()

            // Apply ±20% variation except for the last chunk or if steps are very small
            val variation = (chunkSteps * 0.2).toLong()
            if (variation > 0) {
                chunkSteps += Random.nextLong(-variation, variation + 1)
            }

            // Ensure we don't exceed remaining steps and handle the tail end
            if (currentEnd.isAfter(endTime.minus(1, ChronoUnit.MINUTES)) || chunkSteps > remainingSteps) {
                chunkSteps = remainingSteps
            }
            if (chunkSteps <= 0 && remainingSteps > 0) chunkSteps = 1

            chunks.add(chunkSteps)
            remainingSteps -= chunkSteps
            currentStart = currentEnd
        }
        
        // If there's still a tiny bit of remainingSteps due to rounding/logic, add to last chunk
        if (remainingSteps > 0 && chunks.isNotEmpty()) {
            chunks[chunks.size - 1] += remainingSteps
        }

        return chunks
    }
}
