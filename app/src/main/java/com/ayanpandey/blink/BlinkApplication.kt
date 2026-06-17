package com.ayanpandey.blink

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ayanpandey.blink.di.AppContainerImpl
import com.ayanpandey.blink.domain.di.DomainContainer
import java.lang.ref.WeakReference

class BlinkApplication : Application() {
    lateinit var container: DomainContainer

    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        container.logger.i(TAG, "BlinkApplication initialized successfully.")

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    container.logger.d(TAG, "onActivityCreated: ${activity.localClassName}")
                    currentActivityRef = WeakReference(activity)
                    com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity = activity
                }

                override fun onActivityStarted(activity: Activity) {
                    container.logger.d(TAG, "onActivityStarted: ${activity.localClassName}")
                    currentActivityRef = WeakReference(activity)
                    com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity = activity
                }

                override fun onActivityResumed(activity: Activity) {
                    container.logger.d(TAG, "onActivityResumed: ${activity.localClassName}")
                    currentActivityRef = WeakReference(activity)
                    com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity = activity
                }

                @Suppress("EmptyFunctionBlock")
                override fun onActivityPaused(activity: Activity) {}

                @Suppress("EmptyFunctionBlock")
                override fun onActivityStopped(activity: Activity) {}

                @Suppress("EmptyFunctionBlock")
                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {}

                override fun onActivityDestroyed(activity: Activity) {
                    container.logger.d(TAG, "onActivityDestroyed: ${activity.localClassName}")
                    if (currentActivityRef?.get() == activity) {
                        currentActivityRef = null
                    }
                    if (com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity == activity) {
                        com.ayanpandey.blink.core.common.context.ActiveContextHolder.activeActivity = null
                    }
                }
            },
        )
    }

    companion object {
        private const val TAG = "BlinkApplication"
    }
}
