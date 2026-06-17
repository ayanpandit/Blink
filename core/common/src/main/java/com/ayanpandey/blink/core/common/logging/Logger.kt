package com.ayanpandey.blink.core.common.logging

import android.util.Log

interface BlinkLogger {
    fun d(
        tag: String,
        message: String,
    )

    fun i(
        tag: String,
        message: String,
    )

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

class DebugLogger : BlinkLogger {
    override fun d(
        tag: String,
        message: String,
    ) {
        Log.d(tag, message)
    }

    override fun i(
        tag: String,
        message: String,
    ) {
        Log.i(tag, message)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.w(tag, message, throwable)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(tag, message, throwable)
    }
}

class ReleaseLogger : BlinkLogger {
    override fun d(
        tag: String,
        message: String,
    ) {
        // No-op in release
    }

    override fun i(
        tag: String,
        message: String,
    ) {
        // No-op or minimal tracking
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // In release, we could route to a crash reporter, but since we are offline-only,
        // we write to system log only critical warnings
        Log.w(tag, "Warning: [ObfuscatedTag] $message", throwable)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // Safe logging of errors
        Log.e(tag, "Error: [ObfuscatedTag] $message", throwable)
    }
}
