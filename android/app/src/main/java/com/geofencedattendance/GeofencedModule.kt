import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.geofencedattendance.GeofenceBroadcastReceiver
import com.geofencedattendance.GeofenceDto
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.UUID

class GeofencedModule(context: ReactApplicationContext) {
    lateinit var geoFencingClient: GeofencingClient
    val _context: ReactApplicationContext = context;
    var geoFencedList : MutableList<Geofence> = mutableListOf()
    var geoFenceDtoList = mutableListOf<GeofenceDto>()
    val _notificationResponisveness = 2 * 60 * 1000;
    val sharedPref = context.getSharedPreferences("locationSharedPref", Context.MODE_PRIVATE)
    val _sharedPrefInitializedKey = "initialized";
    fun initialize(){
        Log.d("Initialized","true")
        val isInitialized = sharedPref.getBoolean(_sharedPrefInitializedKey,false);
        if(isInitialized){
            return; // initializing only once
        }
        geoFenceDtoList.add(GeofenceDto(11.1078938, 77.32728, 20))//Office (MSP)
        geoFenceDtoList.add(GeofenceDto(11.1120701,77.2746059, 70))//Cheran Nagar (Project
        geoFenceDtoList.add(GeofenceDto(11.0963805,77.3750278, 20))//Karthik home
        geoFenceDtoList.add(GeofenceDto(11.1015774, 77.3873048, 100))//Mahesh home
        geoFenceDtoList.add(GeofenceDto(28.41276922144451, 77.04381797704207, 50))//centocode office
        geoFenceDtoList.add(GeofenceDto(28.490043, 77.024092, 20))//Shantanu

        geoFencingClient = LocationServices.getGeofencingClient(_context);
        subscribeToLocation()
    }
    fun subscribeToLocation(){
        val requestId = UUID.randomUUID().toString();
      val geoFencedData =   Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(geoFenceDtoList[0].latitude,geoFenceDtoList[0].longitude,geoFenceDtoList[0].radius.toFloat())
            .setCircularRegion(geoFenceDtoList[1].latitude,geoFenceDtoList[1].longitude,geoFenceDtoList[1].radius.toFloat())
            .setCircularRegion(geoFenceDtoList[2].latitude,geoFenceDtoList[2].longitude,geoFenceDtoList[2].radius.toFloat())
            .setCircularRegion(geoFenceDtoList[3].latitude,geoFenceDtoList[3].longitude,geoFenceDtoList[3].radius.toFloat())
            .setCircularRegion(geoFenceDtoList[4].latitude,geoFenceDtoList[4].longitude,geoFenceDtoList[4].radius.toFloat())
            .setCircularRegion(geoFenceDtoList[5].latitude,geoFenceDtoList[5].longitude,geoFenceDtoList[5].radius.toFloat())
              .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
          .setExpirationDuration(Geofence.NEVER_EXPIRE)
          .setNotificationResponsiveness(_notificationResponisveness)
          .build()
       geoFencedList.add(geoFencedData);
        sharedPref.edit()
            .putString("Location1",requestId)
            .putBoolean(_sharedPrefInitializedKey,true)
            .apply();
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
        geoFencingClient.addGeofences(geoFencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
            }
            addOnFailureListener { e ->
                e.printStackTrace()
                Log.d("GeoFencedModule",e.toString())
                throw e
            }
        }
    }
}