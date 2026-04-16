package io.andautowalk

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class LogicUtilsTest {

    @Test
    fun `calculateNextSyncDelay - same day in future`() {
        val now = LocalDateTime.of(2026, 4, 16, 10, 0)
        val syncTime = LocalTime.of(11, 0)
        val delay = LogicUtils.calculateNextSyncDelay(now, syncTime)
        assertEquals(60L, delay)
    }

    @Test
    fun `calculateNextSyncDelay - already passed today should be tomorrow`() {
        val now = LocalDateTime.of(2026, 4, 16, 12, 0)
        val syncTime = LocalTime.of(11, 0)
        val delay = LogicUtils.calculateNextSyncDelay(now, syncTime)
        // From 12:00 today to 11:00 tomorrow is 23 hours = 1380 minutes
        assertEquals(1380L, delay)
    }

    @Test
    fun `calculateNextSyncDelay - exact same time should be tomorrow`() {
        val now = LocalDateTime.of(2026, 4, 16, 11, 0)
        val syncTime = LocalTime.of(11, 0)
        val delay = LogicUtils.calculateNextSyncDelay(now, syncTime)
        // 1440 minutes = 24 hours
        assertEquals(1440L, delay)
    }

    @Test
    fun `calculateNextSyncDelay - forceNextDay should be tomorrow`() {
        val now = LocalDateTime.of(2026, 4, 16, 10, 0)
        val syncTime = LocalTime.of(11, 0)
        val delay = LogicUtils.calculateNextSyncDelay(now, syncTime, forceNextDay = true)
        assertEquals(1440L + 60L, delay) // 25 hours = 1500 mins
    }

    @Test
    fun `calculateStepChunks - distributes correctly over multiple chunks`() {
        val startTime = Instant.parse("2026-04-16T08:00:00Z")
        val endTime = Instant.parse("2026-04-16T10:00:00Z") // 120 minutes = 4 chunks of 30 mins
        val totalSteps = 10000L
        
        val chunks = LogicUtils.calculateStepChunks(totalSteps, startTime, endTime)
        
        assertEquals(4, chunks.size)
        assertEquals(totalSteps, chunks.sum())
        
        // Check that each chunk is roughly 2500 but with variation
        chunks.forEach { chunk ->
            assertTrue(chunk > 0, "Chunk should be positive")
        }
    }

    @Test
    fun `calculateStepChunks - handles short duration less than 30 mins`() {
        val startTime = Instant.parse("2026-04-16T08:00:00Z")
        val endTime = Instant.parse("2026-04-16T08:15:00Z") // 15 minutes
        val totalSteps = 1000L
        
        val chunks = LogicUtils.calculateStepChunks(totalSteps, startTime, endTime)
        
        assertEquals(1, chunks.size)
        assertEquals(totalSteps, chunks[0])
    }

    @Test
    fun `calculateStepChunks - handles zero or negative duration`() {
        val startTime = Instant.parse("2026-04-16T08:00:00Z")
        val endTime = Instant.parse("2026-04-16T07:00:00Z")
        val totalSteps = 1000L
        
        val chunks = LogicUtils.calculateStepChunks(totalSteps, startTime, endTime)
        
        assertEquals(1, chunks.size)
        assertEquals(totalSteps, chunks[0])
    }

    @Test
    fun `calculateStepChunks - handles large step counts precisely`() {
        val startTime = Instant.parse("2026-04-16T08:00:00Z")
        val endTime = Instant.parse("2026-04-16T11:00:00Z") // 180 mins = 6 chunks
        val totalSteps = 1234567L
        
        val chunks = LogicUtils.calculateStepChunks(totalSteps, startTime, endTime)
        
        assertEquals(6, chunks.size)
        assertEquals(totalSteps, chunks.sum())
    }
}
