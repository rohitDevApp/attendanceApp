import React, { useState } from 'react';
import {
  Text,
  View,
  Platform,
  Alert,
  ActivityIndicator,
  PermissionsAndroid,
  StyleSheet,
  TouchableOpacity,
  TextInput,
} from 'react-native';
import NMBridge from './NMBridge';
import Geofencing from '@react-native-community/geolocation';
import SaveUserLocation from './SaveUserLocation';

export default () => {
  const [loaded, setLoaded] = useState(true);
  const [radius, setRadius] = useState(20);
  const [error, setError] = useState('');
  const [currentLocation, setCurrentLocation] = useState({
    lat: 0,
    long: 0,
  });

  // useEffect(() => {
  //   if (Platform.OS === 'ios') {
  //     Alert.alert('Not supported', 'We are not currently supporting iOS');
  //     return;
  //   }

  //   // _initialize();
  //   //  _initialize(currentLocation.lat, currentLocation.long, 20);
  //   // eslint-disable-next-line react-hooks/exhaustive-deps
  // }, []);

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

  const _initialize = async (l: number, lg: number, r: number) => {
    try {
      const permission = await _requestLocation();
      if (!permission) {
        Alert.alert(
          'Location permission required',
          'Please grant location permission',
        );
        return;
      }
      await NMBridge.initialize(l, lg, r);
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
        <Text style={{ marginTop: 20 }}>Initializing...</Text>
      </View>
    );
  }

  //get current location
  const getCurrentLocation = async () => {
    try {
      Geofencing.getCurrentPosition(info => {
        const { coords: { latitude, longitude } } = info;
        setCurrentLocation({ lat: latitude, long: longitude });
      },);
    } catch (err) {
      console.log(err, 'current location permission');
    }
  };

  //handler radius
  const handleRadiusChange = (value: any) => {
    if (value === '' || !isNaN(value)) {
      setRadius(value);
      setError('');
    } else {
      setError('Please enter a valid number');
    }
  };

  const handleSubmit = () => {
    console.log(currentLocation.lat, currentLocation.long, radius, 'submit', typeof Number(radius));
    _initialize(currentLocation.lat, currentLocation.long, Number(radius));
  };

  return (
    <View
      style={styles.container}>
      <Text>App is initialized for attendance. Well Done 🫡</Text>
      <TouchableOpacity style={styles.button} onPress={getCurrentLocation}>
        <Text>Get Location</Text>
      </TouchableOpacity>

      <View>
        <Text>Longitude: {currentLocation.lat}</Text>
        <Text>Longitude: {currentLocation.long}</Text>
      </View>

      <Text style={styles.label}>Enter Radius:</Text>
      <TextInput
        style={[styles.input, error && styles.errorInput]}
        placeholder="Enter radius"
        keyboardType="numeric"
        value={radius.toString()}
        onChangeText={handleRadiusChange}
      />
      <TouchableOpacity style={styles.geofencebutton} onPress={handleSubmit}>
        <Text>Get geofence</Text>
      </TouchableOpacity>
      <SaveUserLocation />
    </View >
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'lightgray',
  },
  button: {
    backgroundColor: '#4CAF50',
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 3,
    marginVertical: 12,
  },
  geofencebutton: {
    backgroundColor: 'red',
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 3,
  },
  buttonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold',
    textAlign: 'center',
  },
  label: {
    fontSize: 18,
    marginBottom: 10,
    marginTop: 12,
  },
  input: {
    height: 40,
    borderColor: 'black',
    borderWidth: 1,
    borderRadius: 5,
    width: '80%',
    paddingLeft: 10,
    marginBottom: 10,
  },
  errorInput: {
    borderColor: 'red',
  },
  errorText: {
    color: 'red',
    fontSize: 14,
    marginBottom: 10,
  },
});
