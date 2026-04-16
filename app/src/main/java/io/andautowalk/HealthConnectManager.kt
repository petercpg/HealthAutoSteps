package io.andautowalk

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min
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
            endZoneOffset = ZonedDateTime.now().offset,
            metadata = HealthMetadata.manualEntry()
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun writeDistributedSteps(totalSteps: Long, startTime: Instant, endTime: Instant) {
        val stepChunks = LogicUtils.calculateStepChunks(totalSteps, startTime, endTime)
        if (stepChunks.isEmpty()) return

        val records = mutableListOf<StepsRecord>()
        var currentStart = startTime

        stepChunks.forEach { chunkSteps ->
            val chunkMinutes = min(30L, Duration.between(currentStart, endTime).toMinutes())
            val currentEnd = currentStart.plus(chunkMinutes, ChronoUnit.MINUTES)

            records.add(
                StepsRecord(
                    count = chunkSteps,
                    startTime = currentStart,
                    endTime = currentEnd,
                    startZoneOffset = ZonedDateTime.now().offset,
                    endZoneOffset = ZonedDateTime.now().offset,
                    metadata = HealthMetadata.manualEntry()
                )
            )
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
            dataOriginFilter = setOf(DataOrigin(context.packageName)),
            ascendingOrder = false,
            pageSize = limit
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }
}
