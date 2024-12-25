import { View, Text, NativeModules, TouchableOpacity, StyleSheet } from 'react-native';
import React, { useEffect, useState } from 'react';
import { Alert } from 'react-native';
import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { ReactNativeBridge } = NativeModules;
const SaveUserLocation = ({ onPress }: { onPress: (act: string) => void; }) => {
    const [isset, setIsSet] = useState<string | null>('1');

    useEffect(() => {
        const fetchAndSaveLocation = async () => {
            try {
                //Permise
                const savedState = await AsyncStorage.getItem('isSet');
                setIsSet(savedState);

                if (savedState !== null && savedState === '1') {
                    // Call the native method to get saved geofence event
                    const savedEvent = await ReactNativeBridge.getSavedGeofenceEventFromBridge();
                    // console.log('Saved Geofence Event:', savedEvent?.userData);
                    if (savedEvent?.userData !== 'null') {
                        const data = JSON.parse(savedEvent?.userData);
                        console.log(data[0], 'parts');
                        if (data?.length > 0) {
                            data?.map(async (d: any, index: Number) => {
                                // Send data to the API
                                const res = await axios.post(
                                    'https://app.cheransoftwares.com/api/app/staff_attendance/clock_store',
                                    {
                                        employee_id: 'abcde1234',
                                        latitude: d?.latitude ?? '',
                                        longitude: d?.longitude ?? '',
                                        address: d?.fullAddress ?? '',
                                        premises: '1',
                                    }
                                );
                                console.log(res?.status, 'update REsponse');
                                if (res?.status === 200) {
                                    if (d?.eventType === 'geofenceEnter') {
                                        ReactNativeBridge.showNotification(
                                            'Your Attendance is Successfully updated.',
                                        );
                                    } else {
                                        ReactNativeBridge.showNotification('You are logout.');
                                    }

                                    // Check if this is the last item
                                    if (index === data?.length - 1) {
                                        try {
                                            ReactNativeBridge.clearSavedGeofenceEvents().then((r: any) => {
                                                console.log(r, 'removeResult');
                                            }).catch((err: any) => {
                                                console.log(err, 'when we clear sharedPreffer');
                                            });
                                            // console.log(removeRes, 'removeResult');
                                        } catch (err) {
                                            console.log(err, 'update Clear Geofencing data');
                                        }

                                    }
                                } else {
                                    console.log('Not Updated');
                                }
                            });
                        } else {
                            console.log('empty Data');
                        }

                    }
                }
            } catch (error) {
                console.error('Error fetching or posting data:', error);
                Alert.alert('Error', 'Failed to update location.');
            }
        };

        // Call the async function
        fetchAndSaveLocation();
    }, [isset]);

    //handleToggerle
    const handleToggerle = async () => {
        if (isset === '0') {
            setIsSet('1');
            await AsyncStorage.setItem('isSet', '1');
            onPress('1');
        } else {
            try {
                const res = await ReactNativeBridge.stopGeofencing();
                console.log(res, 'stop response');
                setIsSet('0');
                await AsyncStorage.setItem('isSet', '0');
                onPress('0');
                ReactNativeBridge.showNotification('Geofenced Successfully stoped.');
            } catch (err) {
                console.log('Error when we toggle Set & isSet', err);
                ReactNativeBridge.showNotification('Geofenced not Successfully stoped.');
            }
        }
    };

    return (
        <View style={[styles.main]}>
            <TouchableOpacity onPress={handleToggerle} style={styles.buttonContainer}>
                <View
                    style={[
                        styles.buttonBackground,
                        { backgroundColor: isset !== '0' ? '#34c759' : '#ff5e57' },
                    ]}
                >
                    <Text style={styles.buttonText}>{isset !== '0' ? 'Set' : 'Unset'}</Text>
                </View>
            </TouchableOpacity>
        </View>
    );
};

const styles = StyleSheet.create({
    main: {
        width: '100%',
        alignItems: 'center',
    },
    buttonContainer: {
        borderRadius: 25,
        overflow: 'hidden',
        width: '50%',
        marginVertical: 14,
    },
    buttonBackground: {
        padding: 15,
        alignItems: 'center',
        borderRadius: 25,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
        elevation: 5, // Adds shadow on Android
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold',
    },
});
export default SaveUserLocation;
