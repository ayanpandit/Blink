package com.ayanpandey.blink.core.common.context

import android.app.Activity
import java.lang.ref.WeakReference

object ActiveContextHolder {
    private var activeActivityRef: WeakReference<Activity>? = null

    var activeActivity: Activity?
        get() = activeActivityRef?.get()
        set(value) {
            activeActivityRef = value?.let { WeakReference(it) }
        }
}
