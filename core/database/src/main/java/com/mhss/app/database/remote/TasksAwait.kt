package com.mhss.app.database.remote

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { res -> cont.resume(res) {} }
        addOnFailureListener { e -> cont.resumeWithException(e) }
    }

