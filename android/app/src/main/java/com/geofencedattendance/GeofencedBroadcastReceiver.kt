package com.geofencedattendance
import android.app.ActivityManager
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import java.text.SimpleDateFormat
import java.util.*
import android.location.Geocoder
import android.location.Address


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("broadcastfirst","true")
        val notificationManager1  = RNNotificationManager(context!!);
        notificationManager1.createChannel()
        notificationManager1.send(true);
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        if(geofencingEvent == null){
            return;
        }
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            return
        }
        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            return // Not interested in DWELL
        }

        //Save login into Database
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||  geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                val triggeringLocation = geofencingEvent.triggeringLocation
                val latitude = triggeringLocation?.latitude
                val longitude = triggeringLocation?.longitude
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                val currentTime = timeFormat.format(Date())

            if (latitude != null && longitude != null) {
                lateinit var fullAddress: String

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address: Address = addresses[0]
                    val city = address.locality ?: ""
                    val state = address.adminArea ?: ""
                    val country = address.countryName ?: ""
                    val addressLine = address.getAddressLine(0) ?: ""

                     fullAddress = "$addressLine, $city, $state, $country"
                    Log.d("LocationAddress", fullAddress)
                }
                // Send event to React Native.
                sendEventToReactNative(context, geofenceTransition, latitude, longitude ,currentDate,currentTime ,fullAddress)
            }
        }

        val notificationManager  = RNNotificationManager(context!!);
        notificationManager.createChannel()
        notificationManager.send(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER);
    }

    //Save Event To Shared Preference when in kill mode
    private fun saveEventToPreferences(context: Context, eventType: String, latitude: Double, longitude: Double ,currentDate: String,currentTime: String ,fullAddress:String) {
        Log.d("SharedStarting..", "Event saved successfully: $eventType|$latitude|$longitude")
        val preferences = context.getSharedPreferences("GeofenceEvents", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("lastEvent", "$eventType|$latitude|$longitude|$currentDate|$currentTime|$fullAddress")
        editor.apply()
    }
        //Send Data to React Native
    private fun sendEventToReactNative(context: Context, transitionType: Int, latitude: Double, longitude: Double ,currentDate:String,currentTime:String ,fullAddress:String){
            val reactContext  = context.applicationContext as ReactContext
            val appState = (context.applicationContext as MainApplication).getAppState()
            val eventType =  if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
                "geofenceEnter"
            }else{
                "geofenceExit"
            }

            Log.d("BroadcastReceiver", "App State: $appState")
            val data = "${eventType}|${latitude}|${longitude}|${currentDate}|${currentTime}|${fullAddress}"
            if(isAppOnForeground(context) || isAppOnBackground(context)){
                Log.d("AppRunInMode","Foreground or Background is running")
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("GeofenceEvent", data)
            }else{
                Log.d("AppRunInMode","Kill Mode is running")
                saveEventToPreferences(context, eventType, latitude, longitude ,currentDate,currentTime ,fullAddress);
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
