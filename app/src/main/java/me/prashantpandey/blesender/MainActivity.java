package me.prashantpandey.blesender;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_CONNECTION_CONGESTED;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity {
    final String[] bluetoothPersmissions = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION};
    final int bluetoothAccessCode = 5;
    final String TAG = this.getClass().getSimpleName();
    int currentCounterValue = 0;
    // bluetooth related variables
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    BluetoothGattServer gattServer;
    // advertising constants
    UUID SERVER_UUID = UUID.fromString("795090c7-420d-4048-a24e-18e60180e34d");
    UUID SERVICE_UUID = UUID.fromString("795090c7-420d-4048-a24e-18e60180e23c");
    UUID CHARACTERISTIC_COUNTER_UUID = UUID.fromString("31517c58-66bf-470c-b662-e352a6c80cba");
    UUID DESCRIPTOR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // ui elements
    TextView connectionStatus;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectionStatus = findViewById(R.id.connectionStatus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(bluetoothPersmissions[0])== PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(bluetoothPersmissions[1])== PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(bluetoothPersmissions[2]) == PackageManager.PERMISSION_GRANTED){
                // then scan for the devices
                Log.d(TAG, "onClick: permissions granted");
                // turn on the bluetooth and start advertising
                turnOnBluetooth();
            }else{
                requestPermissions(bluetoothPersmissions, bluetoothAccessCode);
            }
        }else{
            // turn on the bluetooth and start advertising
            turnOnBluetooth();
        }


    }

    private void turnOnBluetooth() {

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, bluetoothAccessCode);
        }else{
            // start advertising
            startAdvertising();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==bluetoothAccessCode && resultCode==RESULT_OK){
            Log.d(TAG, "onActivityResult: "+data.toString());
            // start Advertising
            startAdvertising();
        }else{
            turnOnBluetooth();
        }
    }

    private void startAdvertising(){
        // now start advertising
        bluetoothAdapter.getBluetoothLeScanner();
        bluetoothAdapter.setName("a");
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder().setConnectable(true)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(SERVER_UUID))
                .setIncludeDeviceName(true).setIncludeTxPowerLevel(false).build();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "onStartSuccess: Success to start");
                startGattServer();
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.d(TAG, "onStartFailure: Failed to start"+ errorCode);
            }
        };
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);

    }

    // creating gatt service
    private BluetoothGattService createService(){
        BluetoothGattService mBluetoothGattService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        // creating characteristic
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_COUNTER_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // characteristic descriptor
        BluetoothGattDescriptor mBluetoothGattDescriptor = new BluetoothGattDescriptor(DESCRIPTOR_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ|BluetoothGattDescriptor.PERMISSION_WRITE);
        mBluetoothGattCharacteristic.addDescriptor(mBluetoothGattDescriptor);

        mBluetoothGattService.addCharacteristic(mBluetoothGattCharacteristic);
        return mBluetoothGattService;
    }

    private void startGattServer(){
        BluetoothGattServerCallback mGattService = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                if (status==STATE_CONNECTED){
                    Log.d(TAG, "onConnectionStateChange: Connected to the client");
                    connectionStatus.setText("Connected");
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                if (characteristic.getUuid().equals(CHARACTERISTIC_COUNTER_UUID)){
                    BigInteger bigInt = BigInteger.valueOf(currentCounterValue);
                    byte[] res = bigInt.toByteArray();
                    gattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, res);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                if (characteristic.getUuid().equals(CHARACTERISTIC_COUNTER_UUID)){
                    currentCounterValue++;
                }
            }
        };
        gattServer =  bluetoothManager.openGattServer(this, mGattService);
        gattServer.addService(createService());
    }



}


