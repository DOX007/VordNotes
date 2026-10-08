package com.mhss.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mhss.app.alarm.model.Alarm
import com.mhss.app.alarm.model.AlarmType

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val time: Long,
    val title: String,
    val description: String,
    val showOnLockScreen: Boolean,
    val type: AlarmType
)

fun AlarmEntity.toAlarm() = Alarm(
    id = id,
    time = time,
    title = title,
    description = description,
    showOnLockScreen = showOnLockScreen,
    type = type
)

fun Alarm.toAlarmEntity() = AlarmEntity(
    id = id,
    time = time,
    title = title,
    description = description,
    showOnLockScreen = showOnLockScreen,
    type = type
)
