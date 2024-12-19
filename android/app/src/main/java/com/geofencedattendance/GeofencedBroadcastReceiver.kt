package com.geofencedattendance
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

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
                if (latitude != null && longitude != null) {
                Log.d("GeofenceBroadcastReceiver", "Lat: $latitude, Long: $longitude")
                // Send event to React Native.
                sendEventToReactNative(context, geofenceTransition, latitude, longitude)
            }
        }

        val notificationManager  = RNNotificationManager(context!!);
        notificationManager.createChannel()
        notificationManager.send(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER);
    }

    //Send Data to React Native
    private fun sendEventToReactNative(context: Context, transitionType: Int, latitude: Double, longitude: Double){
        val reactContext = context.applicationContext as? ReactContext
           val eventType =  if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
                "geofenceEnter"
            }else{
                "geofenceExit"
            }

            //Create Object 
            val eventData = mapOf(
                "event" to eventType,
                "latitude" to latitude,
                "longitude" to longitude
            )

            if (reactContext != null) {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("GeofenceEvent", eventData)
            } else {
                Log.e("GeofenceBroadcastReceiver", "React context not initialized or inactive")
            }
    }
}
