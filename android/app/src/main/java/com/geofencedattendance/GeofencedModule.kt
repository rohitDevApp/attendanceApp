import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
import com.facebook.react.modules.core.DeviceEventManagerModule

class GeofencedModule(context: ReactApplicationContext) {
    lateinit var geoFencingClient: GeofencingClient
    val _context: ReactApplicationContext = context;
    var geoFencedList : MutableList<Geofence> = mutableListOf()
    var geoFenceDtoList = mutableListOf<GeofenceDto>()
    val _notificationResponisveness = 1 * 60 * 1000;
    val sharedPref = context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
    val _sharedPrefInitializedKey = "initialized";
    fun initialize(l: Double, lg: Double, r: Int){
//        getLocation()
        Log.d("Initialized","true")
        Log.d("latitude", l.toString())  // Convert `l` to String
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
        // geoFenceDtoList.add(GeofenceDto(28.41276922144451, 77.04381797704207, 50))//centocode office
//         geoFenceDtoList.add(GeofenceDto(28.490043, 77.024092, 20))//Shantanu
//         geoFenceDtoList.add(GeofenceDto(28.4135431,77.0426261, 20))//Karthik home
        geoFenceDtoList.add(GeofenceDto(l,lg, r))//Dynamics
        Log.d("AddeddGeofecned", "true")
//        geoFenceDtoList.add(GeofenceDto(28.419934,77.0365344, 100))
        geoFencingClient = LocationServices.getGeofencingClient(_context);
        Log.d("PerfectSolution", "true")
        subscribeToLocation()
    }


    fun subscribeToLocation(){
        Log.d("subscribe","true")
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

        getGeofencingRequest()
    }


    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(_context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(_context, 43853, intent,
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

    // Expose a method for React Native to fetch the saved event
    @ReactMethod
    fun getSavedGeofenceEvent(promise: Promise) {
        try {
            Log.d("GeoFencedCallSave","Yes")
            val context = _context
            val preferences = context.getSharedPreferences("GeofenceEvents", Context.MODE_PRIVATE)
            val lastEvent = preferences.getString("lastEvent", null)
            if(lastEvent.toString().isNotEmpty()){
                Log.d("DataFromCurrentSharedFile",lastEvent.toString());
                val mapResult: WritableMap = Arguments.createMap()
                mapResult.putString("userData", lastEvent.toString())
                promise.resolve(mapResult);
            }
        }catch(err: Exception){
            promise.reject("NO_EVENT", "No saved geofence events found",err)
        }
    }
}