package com.geofencedattendance

//import com.facebook.react.bridge.Callback
import GeofencedModule
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
//import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


class ReactNativeBridgeModule internal constructor(context: ReactApplicationContext?) :
    ReactContextBaseJavaModule(context){
    private val reactContext: ReactApplicationContext = context as ReactApplicationContext
    private  val _context = context as Context;
    val geoFence =  GeofencedModule(context!!);

    override fun getName(): String {
        return "ReactNativeBridge"
    }

    @ReactMethod
    fun initializeGeoFenceApplication(lat: Double, long: Double, radius: Int,promise: Promise){
        try {
//            runEveryFiveSeconds();
            geoFence.initialize(lat,long,radius);
            promise.resolve("INITIALIZED")
        }
        catch (e:Exception){
            promise.reject(e)
        }
    }

    @ReactMethod
    fun getSavedGeofenceEventFromBridge(promise: Promise) {
        Log.d("RNBridgeCallSave","Yes")
       geoFence.getSavedGeofenceEvent(promise)
    }

    @ReactMethod
    fun clearSavedGeofenceEvents(promise: Promise) {
        Log.d("RNBridgeCallSaveForClear","Yes")
        geoFence.clearSavedGeofenceEvents(promise)
    }

    @ReactMethod
    fun stopGeofencing(promise: Promise) {
        Log.d("RNBridgeCallSaveForClear","Yes")
        geoFence.stopGeofencing(promise)
    }

    @ReactMethod
    fun showNotification (message:String){
        val notificationManager1  = RNNotificationManager(_context);
        notificationManager1.createChannel()
        // Create an Intent that will open the app when the notification is clicked
        val intent = Intent(_context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            _context,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(_context, "GEOFENCE_CHANNEL")
            .setContentTitle("Geofence Alert")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            _context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        notificationManager.notify(1, builder.build())
    }


    fun runEveryFiveSeconds() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(10000)
//                val eventType ="Enter"
//                val latitude = "28.22222"
//                val longitude = "77.222434"
//                val currentDate = "23/22/2002"
//                val currentTime = "9:10"
//                val fullAddress = "Karnal , Haryana"

//                val data = "${eventType}|${latitude}|${longitude}|${currentDate}|${currentTime}|${fullAddress}"
//                if(isAppOnForeground(_context) || isAppOnBackground(_context)){
//                    Log.d("AppRunInMode","Foreground or Background is running")
//                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
//                        .emit("GeofenceEvent", data)
//                }else{
//                    Log.d("AppRunInMode","Kill Mode is running")
//                }
//                val notificationManager1  = RNNotificationManager(_context);
//                notificationManager1.createChannel()
//                notificationManager1.send(true);
//
//                val notificationManager  = RNNotificationManager(_context!!);
//                notificationManager.createChannel()
//                notificationManager.send(true)
//                Log.d("Notification Trigger" ,"true")

            }
        }
    }

    //Foreground
    private fun isAppOnForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName: String = context.getPackageName()
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName
            ) {
                return true
            }
        }
        return false
    }

    //Background
    private fun isAppOnBackground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName: String = context.getPackageName()
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED &&
                appProcess.processName == packageName
            ) {
                return true
            }
        }
        return false
    }
    }