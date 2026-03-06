package com.example.healthautosteps

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun writeSteps(count: Long, startTime: Instant, endTime: Instant) {
        val record = StepsRecord(
            count = count,
            startTime = startTime,
            endTime = endTime,
            startZoneOffset = ZonedDateTime.now().offset,
            endZoneOffset = ZonedDateTime.now().offset
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun writeDistributedSteps(totalSteps: Long, startTime: Instant, endTime: Instant) {
        val duration = java.time.Duration.between(startTime, endTime)
        val minutes = duration.toMinutes()
        if (minutes <= 0) {
            writeSteps(totalSteps, startTime, endTime)
            return
        }

        val records = mutableListOf<StepsRecord>()
        var remainingSteps = totalSteps
        var currentStart = startTime

        while (currentStart.isBefore(endTime) && remainingSteps > 0) {
            val chunkMinutes = kotlin.math.min(30L, java.time.Duration.between(currentStart, endTime).toMinutes())
            if (chunkMinutes <= 0) break

            val currentEnd = currentStart.plus(chunkMinutes, ChronoUnit.MINUTES)

            val fraction = chunkMinutes.toDouble() / minutes.toDouble()
            var chunkSteps = (totalSteps * fraction).toLong()

            val variation = (chunkSteps * 0.2).toLong()
            if (variation > 0) {
                chunkSteps += Random.nextLong(-variation, variation + 1)
            }

            if (currentStart.plus(chunkMinutes, ChronoUnit.MINUTES).isAfter(endTime.minus(1, ChronoUnit.MINUTES)) || chunkSteps > remainingSteps) {
                chunkSteps = remainingSteps
            }
            if (chunkSteps <= 0 && remainingSteps > 0) chunkSteps = 1

            records.add(
                StepsRecord(
                    count = chunkSteps,
                    startTime = currentStart,
                    endTime = currentEnd,
                    startZoneOffset = ZonedDateTime.now().offset,
                    endZoneOffset = ZonedDateTime.now().offset
                )
            )
            remainingSteps -= chunkSteps
            currentStart = currentEnd
        }

        if (records.isNotEmpty()) {
            healthConnectClient.insertRecords(records)
        }
    }

    suspend fun readRecentRecords(limit: Int = 10): List<StepsRecord> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
            dataOriginFilter = setOf(androidx.health.connect.client.records.metadata.DataOrigin(context.packageName)),
            ascendingOrder = false,
            pageSize = limit
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }
}
