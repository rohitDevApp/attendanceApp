import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.datatypes.ExerciseRoute.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.style.ComputedBorderRadius
import com.geofencedattendance.GeofenceBroadcastReceiver
import com.geofencedattendance.GeofenceDto
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.UUID
import com.geofencedattendance.MainActivity
import com.geofencedattendance.RNNotificationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.facebook.react.bridge.Callback
import android.app.Activity
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnSuccessListener

class GeofencedModule(context: ReactApplicationContext) {
    lateinit var geoFencingClient: GeofencingClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    val _context: ReactApplicationContext = context;
    var geoFencedList : MutableList<Geofence> = mutableListOf()
    var geoFenceDtoList = mutableListOf<GeofenceDto>()
    val _notificationResponisveness = 1 * 60 * 1000;
    val sharedPref = context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
    val _sharedPrefInitializedKey = "initialized";

    fun initialize(l: Double, lg: Double, r: Int){
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(_context);

        if (ActivityCompat.checkSelfPermission(
                _context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                _context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw IllegalStateException("Permission to access fine location is not granted.");
        }

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,CancellationTokenSource().token).addOnCompleteListener({
            val lat = it.result.latitude;
            val long = it.result.longitude;
            geoFenceDtoList.add(GeofenceDto(lat,long, r))
            geoFencingClient = LocationServices.getGeofencingClient(_context);
            subscribeToLocation()
        })
    }


    fun subscribeToLocation(){
        for (geoFenceDto in geoFenceDtoList) {
            val requestId = UUID.randomUUID().toString()
            val geoFence = Geofence.Builder()
                    .setRequestId(requestId)
                    .setCircularRegion(
                            geoFenceDto.latitude,
                            geoFenceDto.longitude,
                            geoFenceDto.radius.toFloat()
                    )
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(_notificationResponisveness)
                    .setLoiteringDelay(10000)
                    .build()

            geoFencedList.add(geoFence)

            // Save request ID in shared preferences
            sharedPref.edit()
                    .putString("Location_$requestId", requestId)
                    .putBoolean(_sharedPrefInitializedKey, true)
                    .apply()
        }
        getGeofencingRequest()
    }


    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(_context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        PendingIntent.getBroadcast(_context, 12345, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun getGeofencingRequest() {
        val geoFencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofences(geoFencedList)
        }.build();

        if (ActivityCompat.checkSelfPermission(
                _context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw IllegalStateException("Permission to access fine location is not granted.");
        }

        try{
            geoFencingClient.addGeofences(geoFencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d("Geofence add Successfully ","Done")
                }
                addOnFailureListener { e ->
                    e.printStackTrace()
                    Log.d("GeoFencedModule",e.toString())
                    throw e
                }
            }
        }catch (err:Exception){
            Log.e("ErrorWhengGeoFenceReques",err.toString())
            sendNotify("Error")
        }
    }

    // Define the LocationCallback
    private val stopLocationTrack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            Log.d("Location", "Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
        }
    }

    @ReactMethod
    fun getSavedGeofenceEvent(promise: Promise) {
        try {
            val preferences = _context.getSharedPreferences("GeofenceEvents", Context.MODE_PRIVATE)
            val allEvents = preferences.getString("allEvents", null)

            if (!allEvents.isNullOrEmpty()) {

                val mapResult: WritableMap = Arguments.createMap()
                mapResult.putString("userData", allEvents)

                // Resolve the promise with the map
                promise.resolve(mapResult)
            }
        } catch (err: Exception) {
            Log.e("GeoFencedCallError", "Error retrieving geofence events", err)
            promise.reject("NO_EVENT", "No saved geofence events found", err)
        }
    }

    @ReactMethod
    fun getAllLocationData(promise: Promise) {
        try {
            val sharedPref = _context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
            val allEntries = sharedPref.all
            val locationData = mutableMapOf<String, String>()

            // Filter keys that start with "Location_" and collect their values
            for ((key, value) in allEntries) {
                if (key.startsWith("Location_") && value is String) {
                    locationData[key] = value
                }
            }

            // Return the data as a JSON-like map to JavaScript
            promise.resolve(locationData.toString())
        } catch (e: Exception) {
            Log.e("GetAllLocationDataError", "Error fetching location data", e)
            promise.reject("FETCH_ERROR", "Failed to fetch location data", e)
        }
    }

    @ReactMethod
    fun clearSavedGeofenceEvents(promise: Promise) {
        try {
            val context = _context
            val preferences = context.getSharedPreferences("GeofenceEvents", Context.MODE_PRIVATE)
            val editor = preferences.edit()

            // Clear the specific key or all data
            editor.remove("allEvents") // Removes only the geofence events
            editor.apply()

            // Optionally, resolve a success message
            promise.resolve("Geofence events cleared successfully")
        } catch (err: Exception) {
            Log.e("GeoFencedClearError", "Error clearing geofence events", err)
            promise.reject("CLEAR_ERROR", "Failed to clear geofence events", err)
        }
    }

    @ReactMethod
    fun stopGeofencing(promise: Promise) {
        try {
            geoFencingClient = LocationServices.getGeofencingClient(_context);

            //getShared Prefer Data from Location
            val sharedPref = _context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
            val allEntries = sharedPref.all
            val geofenceIds = mutableListOf<String>()
            val geoPrefIds = mutableListOf<String>()
            for ((key,value) in allEntries) {
                if (key.startsWith("Location_") && value is String) {
                    geofenceIds.add(value)
                    geoPrefIds.add(key)
                }
            }

            if (geofenceIds.isEmpty()) {
                promise.resolve("Empty")
                return
            }

            // Remove all geofence using the geofenceIds
            geoFencingClient?.removeGeofences(geofenceIds)?.run {
                addOnSuccessListener {
                    //  clear the stopped geofence from shared preferences
                    val editor = sharedPref.edit()
                    for (id in geoPrefIds) {
                        editor.remove(id.toString())
                    }
                    editor.apply()

                    // Remove the corresponding geofences from geoFencedList
                    geoFenceDtoList.clear()
                    geoFencedList.clear()

                    promise.resolve("Geofencing stopped successfully")
                }
                addOnFailureListener { e ->
                    Log.e("GeoFencedStopError", "Error stopping geofence", e)
                    promise.reject("STOP_ERROR", "Failed to stop geofencing", e)
                }
            }

            // Stop location updates
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(_context)
            try {
                fusedLocationClient.removeLocationUpdates(stopLocationTrack)
            }catch (err:Exception){
                Log.e("FusedError","true");
            }

        } catch (e: Exception) {
            Log.e("GeoFencedStopException", "Exception stopping geofencing", e)
            promise.reject("STOP_EXCEPTION", "Exception occurred while stopping geofencing", e)
        }
    }

    @ReactMethod
    fun sendNotify(message:String){
        val notificationManager1  = RNNotificationManager(_context);
        notificationManager1.createChannel()
        // Create an Intent that will open the app when the notification is clicked
//        val intent = Intent(_context, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            _context,
//            54321,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
        val builder = NotificationCompat.Builder(_context, "GEOFENCE_CHANNEL")
            .setContentTitle("Geofence Alert")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)

        val notificationManager =
            _context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        notificationManager.notify(1, builder.build())
    }



}