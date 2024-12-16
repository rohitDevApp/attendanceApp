import React, {useEffect, useState} from 'react';
import {
  Text,
  View,
  Platform,
  Alert,
  ActivityIndicator,
  PermissionsAndroid,
} from 'react-native';
import NMBridge from './NMBridge';
import Geolocation from '@react-native-community/geolocation';

export default () => {
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    if (Platform.OS === 'ios') {
      Alert.alert('Not supported', 'We are not currently supporting iOS');
      return;
    }
    Geolocation.getCurrentPosition(info => console.log(info));

    _initialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (Platform.OS === 'ios') {
    return null;
  }

  const _requestLocation = async () => {
    const hasFineLocationPermission = await PermissionsAndroid.check(
      'android.permission.ACCESS_FINE_LOCATION',
    );
    const hasBackgroundLocationAccess = await PermissionsAndroid.check(
      'android.permission.ACCESS_BACKGROUND_LOCATION',
    );
    if (hasFineLocationPermission && hasBackgroundLocationAccess) {
      return true;
    }
    const granted = await PermissionsAndroid.request(
      'android.permission.ACCESS_FINE_LOCATION',
      {
        message:
          'Permission to fine location is mandatory in order to work this application properly.',
        title: 'Ohhh :(',
        buttonPositive: 'OK',
      },
    );
    const isGranted = granted === PermissionsAndroid.RESULTS.GRANTED;
    if (!isGranted) {
      return false;
    }

    const backgroundPermissionRequest = await PermissionsAndroid.request(
      'android.permission.ACCESS_BACKGROUND_LOCATION',
      {
        message:
          'Permission to fine location is mandatory in order to work this application properly.',
        title: 'Ohhh :(',
        buttonPositive: 'OK',
      },
    );
    const isGrantedBackgroundPermissionAccess =
      backgroundPermissionRequest === PermissionsAndroid.RESULTS.GRANTED;
    if (!isGrantedBackgroundPermissionAccess) {
      return false;
    }
    const notificationRequestStatus = await PermissionsAndroid.request(
      'android.permission.POST_NOTIFICATIONS',
      {
        message:
          'Permission to fine location is mandatory in order to work this application properly.',
        title: 'Ohhh :(',
        buttonPositive: 'OK',
      },
    );
    return notificationRequestStatus === PermissionsAndroid.RESULTS.GRANTED;
  };

  const _initialize = async () => {
    try {
      const permission = await _requestLocation();
      if (!permission) {
        Alert.alert(
          'Location permission required',
          'Please grant location permission',
        );
        return;
      }
      await NMBridge.initialize();
      setLoaded(true);
    } catch (e: any) {
      Alert.alert('Error initializing', e.message);
    }
  };
  if (!loaded) {
    return (
      <View
        style={{
          flex: 1,
          justifyContent: 'center',
          alignItems: 'center',
        }}>
        <ActivityIndicator size={'large'} color={'#000'} />
        <Text style={{marginTop: 20}}>Initializing...</Text>
      </View>
    );
  }
  return (
    <View
      style={{
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'lightgray',
      }}>
      <Text>App is initialized for attendance. Well Done 🫡</Text>
    </View>
  );
};
