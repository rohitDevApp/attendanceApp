package com.geofencedattendance

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader

class MainApplication : Application(), ReactApplication {
    // This will hold the app state (foreground, background, killed)
    private var appState: String = "UNKNOWN"
    //Context
    companion object {
        private lateinit var instance: MainApplication
        fun getContext(): Context = instance.applicationContext
    }


  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {

              // Packages that cannot be autolinked yet can be added manually here, for example:
               add(ReactNativeBridgePackage())
            }

        override fun getJSMainModuleName(): String = "index"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
      instance = this
    SoLoader.init(this, OpenSourceMergedSoMapping)
      // Register the activity lifecycle callbacks to track app state
      registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
          override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

          override fun onActivityStarted(activity: Activity) {
              appState = "FOREGROUND"
              Log.d("RunningAppState",appState)
          }

          override fun onActivityResumed(activity: Activity) {}

          override fun onActivityPaused(activity: Activity) {}

          override fun onActivityStopped(activity: Activity) {
              // Get the ActivityManager system service
              val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

              // Check if the app is in the foreground
              val runningTasks = activityManager.getRunningAppProcesses()

              // If the app has no running processes in the foreground, set the app state to background
              val isAppInBackground = runningTasks.none { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }

              if (isAppInBackground) {
                  appState = "BACKGROUND"
              }
              Log.d("RunningAppState",appState)
          }

          override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

          override fun onActivityDestroyed(activity: Activity) {
              appState = "KILL_MODE"
              Log.d("RunningAppState",appState)
          }

      })
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // If you opted-in for the New Architecture, we load the native entry point for this app.
      load()
    }
  }
    // Method to get the current app state
    fun getAppState(): String = appState
}
