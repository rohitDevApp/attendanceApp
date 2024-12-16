import {NativeModules} from 'react-native';
const BridgeModule = NativeModules.ReactNativeBridge;

class NMBridge {
  async initialize() {
    await BridgeModule.initializeGeoFenceApplication();
  }
}

export default new NMBridge();
