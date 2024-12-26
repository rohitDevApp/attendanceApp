package com.geofencedattendance
import SaveDataWorker
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
import com.facebook.react.HeadlessJsTaskService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.internal.immutableListOf
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("broadcastfirst","true");
        if(context == null){
            return;
        }
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
        val triggeringLocation = geofencingEvent.triggeringLocation
        val latitude = triggeringLocation?.latitude
        val longitude = triggeringLocation?.longitude
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())

        if (latitude != null && longitude != null) {
            var fullAddress = "Not Found. GeoLocation from Address"
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
        val notificationManager  = RNNotificationManager(context);
        notificationManager.createChannel()
//        notificationManager.send(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER);
        HeadlessJsTaskService.acquireWakeLockNow(context)
    }

    //Save Event To Shared Preference when in kill mode
    private fun saveEventToPreferences(context: Context, eventType: String, latitude: Double, longitude: Double ,currentDate: String,currentTime: String ,fullAddress:String) {
        Log.d("SharedStarting..", "Event saved successfully: $eventType|$latitude|$longitude")
        val preferences = context.getSharedPreferences("GeofenceEvents", Context.MODE_PRIVATE)
        val editor = preferences.edit()

        val existingEventsJson = preferences.getString("allEvents", "[]")
        val eventsArray = JSONArray(existingEventsJson)
        val newEvent = JSONObject()

        newEvent.put("eventType", eventType)
        newEvent.put("latitude", latitude)
        newEvent.put("longitude", longitude)
        newEvent.put("currentDate", currentDate)
        newEvent.put("currentTime", currentTime)
        newEvent.put("fullAddress", fullAddress)

        // Append the new event to the array
        eventsArray.put(newEvent)
        editor.putString("allEvents", eventsArray.toString())
      //  editor.putString("lastEvent", "$eventType|$latitude|$longitude|$currentDate|$currentTime|$fullAddress")
        editor.apply()
    }
        //Send Data to React Native
    private fun sendEventToReactNative(context: Context, transitionType: Int, latitude: Double, longitude: Double ,currentDate:String,currentTime:String ,fullAddress:String){
        val eventType =  if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            "geofenceEnter"
        }else{
            "geofenceExit"
        }
            Log.d("transitionType",transitionType.toString())
        val data = "${eventType}|${latitude}|${longitude}|${currentDate}|${currentTime}|${fullAddress}"
        if(isAppReachable(context)){
            Log.d("AppRunInMode","Foreground or Background is running")
            val headlessJSIntent = Intent(context, HeadlessTaskService::class.java)
            headlessJSIntent.putExtra("event", data)
            context.startService(headlessJSIntent)
        }else{
            // Prepare input data for WorkManager
            val inputData = Data.Builder()
                .putString("eventType", eventType)
                .putDouble("latitude", latitude)
                .putDouble("longitude", longitude)
                .putString("currentDate", currentDate)
                .putString("currentTime", currentTime)
                .putString("fullAddress", fullAddress)
                .build()

            Log.d("AppRunInMode","Kill Mode is running")

            // Create a WorkRequest
            val saveDataWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<SaveDataWorker>()
                .setInputData(inputData)
                .build()

            // Enqueue the WorkRequest
            WorkManager.getInstance(context).enqueue(saveDataWorkRequest)
        }
    }

    // Call API Call
    private fun savedDataAPI(eventType: String, latitude: Double, longitude: Double, currentDate: String, currentTime: String, fullAddress: String) {
        // Create the OkHttpClient instance
        val client = OkHttpClient()
        val url = "https://app.cheransoftwares.com/api/app/staff_attendance/clock_store"

        // Prepare JSON data for the request body
        val jsonBody = """
        {
            "employee_id": "abide1234",
            "latitude": $latitude,
            "longitude": $longitude,
            "address": "$fullAddress"
        }
    """.trimIndent()

        // Create the RequestBody with the JSON data
        val body = jsonBody.toRequestBody("application/json".toMediaType())

        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Execute the API request in a background thread
        Thread {
            try {
                val response = client.newCall(request).execute()
                Log.d("Response In Kill mode",response.toString())
                if (response.isSuccessful) {
                    Log.d("API_CALL", "Successfully sent data to API")
                } else {
                    Log.d("API_CALL", "API call failed: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("API_CALL", "Error during API call", e)
            }
        }.start()
    }

    //check Foreground & background
    private fun isAppReachable(context: Context): Boolean {
        val interestImportances = immutableListOf<Int>(
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
        )

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName: String = context.getPackageName()
        for (appProcess in appProcesses) {
            if (appProcess.importance in interestImportances &&
                appProcess.processName == packageName
            ) {
                return true
            }
        }
        return false
    }
}
