package com.mhss.app.alarm.model


data class Alarm(
    val id: Int,
    val time: Long,
    val title: String,
    val description: String,
    val showOnLockScreen: Boolean,
    val type: AlarmType
)