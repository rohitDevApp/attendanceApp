import React from 'react';
import {SafeAreaView, StatusBar} from 'react-native';
import Main from './src/Main';

function App(): React.JSX.Element {
  return (
    <SafeAreaView style={{flex: 1, backgroundColor: '#FFF'}}>
      <StatusBar barStyle={'dark-content'} />
      <Main />
    </SafeAreaView>
  );
}

export default App;
