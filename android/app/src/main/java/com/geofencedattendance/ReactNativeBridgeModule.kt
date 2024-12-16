package com.geofencedattendance

import GeofencedModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod


class ReactNativeBridgeModule internal constructor(context: ReactApplicationContext?) :
    ReactContextBaseJavaModule(context){
        val geoFence =  GeofencedModule(context!!);
    override fun getName(): String {
        return "ReactNativeBridge"
    }

    @ReactMethod
    fun initializeGeoFenceApplication(promise: Promise){
        try {
            geoFence.initialize();
            promise.resolve("INITIALIZED")
        }
        catch (e:Exception){
            promise.reject(e)
        }
    }
    }