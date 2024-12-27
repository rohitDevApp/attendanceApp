import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import {NativeModules} from 'react-native';

const {ReactNativeBridge} = NativeModules;

module.exports = async (geolocationData: any) => {
  console.log(geolocationData, 'data');
  const isSet = await AsyncStorage.getItem('isSet');
  if (isSet === '1') {
    console.log(geolocationData, 'geolocation');
    const data = geolocationData?.event?.split('|');
    console.log(data);
    // console.log(
    //   `Event: ${data[0]}\nLatitude: ${data[1]}\nLongitude: ${data[2]}\nDate: ${data[3]}\nTime : ${data[4]}\nAddress: ${data[5]}`,
    // );
    const res = await axios.post(
      'https://app.cheransoftwares.com/api/app/staff_attendance/clock_store',
      {
        employee_id: 'abcde1234',
        latitude: data[1],
        longitude: data[2],
        address: data[5],
        premises: '1',
      },
    );
    console.log(res?.status, 'response');
    if (res?.status === 200) {
      if (data[0] === 'geofenceEnter') {
        ReactNativeBridge.showNotification(
          'Your Attendance is Successfully updated.',
        );
      } else {
        ReactNativeBridge.showNotification('You are logout.');
      }
    } else {
      ReactNativeBridge.showNotification('Your Attendance is not updated.');
    }
  }
};
