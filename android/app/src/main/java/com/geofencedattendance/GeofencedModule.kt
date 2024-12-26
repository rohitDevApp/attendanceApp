import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.geofencedattendance.GeofenceBroadcastReceiver
import com.geofencedattendance.GeofenceDto
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.UUID
import com.geofencedattendance.MainActivity
import com.geofencedattendance.RNNotificationManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class GeofencedModule(context: ReactApplicationContext) {
    lateinit var geoFencingClient: GeofencingClient
    val _context: ReactApplicationContext = context;
    var geoFencedList : MutableList<Geofence> = mutableListOf()
    var geoFenceDtoList = mutableListOf<GeofenceDto>()
    val _notificationResponisveness = 1 * 60 * 1000;
    val sharedPref = context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
    val _sharedPrefInitializedKey = "initialized";
    fun initialize(l: Double, lg: Double, r: Int){
////        getLocation()
//        Log.d("Initialized","true")
        Log.d("latitude", l.toString())
        Log.d("longitude", lg.toString())  // Convert `lg` to String
        Log.d("radius", r.toString())

           // Check if already initialized
        val isInitialized = sharedPref.getBoolean(_sharedPrefInitializedKey, false)
//        if (isInitialized) {
//            Log.d("Geofence", "Already initialized")
//            return
//        }

    //     val isInitialized = sharedPref.getBoolean(_sharedPrefInitializedKey,false);
//        if(isInitialized){
//            return; // initializing only once
//        }
        // geoFenceDtoList.add(GeofenceDto(11.1078938, 77.32728, 20))//Office (MSP)
        // geoFenceDtoList.add(GeofenceDto(11.1120701,77.2746059, 70))//Cheran Nagar (Project
        // geoFenceDtoList.add(GeofenceDto(11.1015774, 77.3873048, 100))//Mahesh home
//         geoFenceDtoList.add(GeofenceDto(28.4135873, 77.0425976, 50))//centocode office
//         geoFenceDtoList.add(GeofenceDto(28.490043, 77.024092, 20))//Shantanu
//         geoFenceDtoList.add(GeofenceDto(28.4135431,77.0426261, 20))//Karthik home
        geoFenceDtoList.add(GeofenceDto(l,lg, r))//Dynamics
        Log.d("AddeddGeofecned", "true")
//        geoFenceDtoList.add(GeofenceDto(28.419934,77.0365344, 100))
        geoFencingClient = LocationServices.getGeofencingClient(_context);
        Log.d("PerfectSolution", geoFencingClient.toString())
        subscribeToLocation()
    }


    fun subscribeToLocation(){
        Log.d("subscribe","true")
        Log.d("GeoFencedDtoList",geoFenceDtoList.toString())
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
                    .build()

            geoFencedList.add(geoFence)

            // Save request ID in shared preferences
            sharedPref.edit()
                    .putString("Location_$requestId", requestId)
                    .putBoolean(_sharedPrefInitializedKey, true)
                    .apply()
        }
        Log.d("GeogeoFencedList",geoFencedList.toString())
        getGeofencingRequest()
    }


    private val geofencePendingIntent: PendingIntent by lazy {
        Log.d("PendingCalling","yes")
        val intent = Intent(_context, GeofenceBroadcastReceiver::class.java)
        Log.d("PendingMidCalling","yes")
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        PendingIntent.getBroadcast(_context, 12345, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun getGeofencingRequest() {
        Log.d("GeofecingRequesting...","true")
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
        Log.d("Enable Location is Done completed","Location")

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
        Log.d("GeofecingRequest completed","Last")
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
                Log.d("DataFromCurrentSharedFile", allEvents)

                // Create a WritableMap to return the data
                val mapResult: WritableMap = Arguments.createMap()
                mapResult.putString("userData", allEvents)

                // Resolve the promise with the map
                promise.resolve(mapResult)
            }
//            else {
//                Log.d("DataSharedFrom", "No events found")
//                promise.reject("NO_EVENT", "No saved geofence events found")
//            }
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
            Log.d("GeoFencedClear", "Clearing saved geofence events")
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
                Log.d("GeoFencedStop", "No geofence to stop")
                promise.resolve("Empty")
                return
            }

            Log.d("GeoFencedStop", "Stopping all geofence $geofenceIds")

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

                    Log.d("GeogeoFencedStopList",geoFencedList.toString())
                    Log.d("GeoFencedStopDtoList",geoFenceDtoList.toString())
                    Log.d("GeoFencedStop", "All geofence stopped successfully")
                    promise.resolve("Geofencing stopped successfully")
                }
                addOnFailureListener { e ->
                    Log.e("GeoFencedStopError", "Error stopping geofence", e)
                    promise.reject("STOP_ERROR", "Failed to stop geofencing", e)
                }
            }

            // Stop location updates
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(_context)
            fusedLocationClient.removeLocationUpdates(stopLocationTrack)

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