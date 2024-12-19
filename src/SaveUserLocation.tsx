import { View, Text } from 'react-native';
import React, { useEffect } from 'react';
import { DeviceEventEmitter, Alert } from 'react-native';

const SaveUserLocation = () => {
    //get Data from When User Exit & Enter
    useEffect(() => {
        const subscription = DeviceEventEmitter.addListener('GeofenceEvent', (eventData) => {
            // Access the eventData
            console.log('Geofence Event Data:', eventData);
            Alert.alert(
                'Geofence Event Data Saved',
                `Event: ${eventData.event}\nLatitude: ${eventData.latitude}\nLongitude: ${eventData.longitude}`
            );
        });

        return () => {
            subscription.remove();
        };
    }, []);

    return (
        <View>
            <Text>SaveUserLocation</Text>
        </View>
    );
};

export default SaveUserLocation;
