import { View, Text, NativeModules, NativeEventEmitter } from 'react-native';
import React, { useEffect } from 'react';
import { Alert } from 'react-native';

const { ReactNativeBridge } = NativeModules;
const SaveUserLocation = () => {
    //get Data from When User Exit & Enter
    useEffect(() => {
        // Initialize the NativeEventEmitter for GeofenceModule
        const GeofenceEventEmitter = new NativeEventEmitter(NativeModules.GeofenceModule);
        console.log(GeofenceEventEmitter, 'geofenc');
        // Add the event listener
        const subscription = GeofenceEventEmitter.addListener('GeofenceEvent', (eventData) => {
            console.log('Geofence Event Data:', eventData);
            Alert.alert('Yes');
            Alert.alert(
                'Geofence Event Data Saved',
                `Event: ${eventData.event}\nLatitude: ${eventData.latitude}\nLongitude: ${eventData.longitude}`
            );
        });

        // Cleanup the listener when the component is unmounted
        return () => {
            subscription.remove();
        };
    }, []);

    useEffect(() => {
        // Call the native method when the app starts
        ReactNativeBridge.getSavedGeofenceEventFromBridge()
            .then((savedEvent: any) => {
                console.log('Saved Geofence Event:', savedEvent);
                if (savedEvent?.userData !== 'null') {
                    console.log(savedEvent?.userData, 'string');
                    const data = savedEvent?.userData.split('|');
                    console.log(data, 'parts');
                    Alert.alert(
                        'User Data Saved Successfully.',
                        `Event: ${data[0]}\nLatitude: ${data[1]}\nLongitude: ${data[2]}\nCurrentDate: ${data[3]}\nCurrentTime: ${data[4]}\nfullAddress: ${data[5]}`
                    );
                }

            })
            .catch((error: any) => {
                console.error('Error fetching saved event:', error);
            });
    }, []);

    return (
        <View>
            <Text>SaveUserLocation</Text>
        </View>
    );
};

export default SaveUserLocation;
