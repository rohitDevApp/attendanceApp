import {NativeModules} from 'react-native';
const BridgeModule = NativeModules.ReactNativeBridge;

class NMBridge {
  async initialize(l: number, lg: number, r: number) {
    console.log(l, lg, r, 'Native bridge');
    await BridgeModule.initializeGeoFenceApplication(l, lg, r);
    console.log(l, lg, r, 'after Native bridge');
  }
}

export default new NMBridge();
