package com.example.uttoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    BluetoothAdapter bluetoothAdapter;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch bluetoothSwitch;
    EditText deviceName;
    Button sendFileBtn;
    ListView pairedList, availableList;
    ArrayAdapter<String> pairedAdapter, availableAdapter;

    // Track selected paired device for unpairing
    BluetoothDevice selectedPairedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_activity);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        deviceName = findViewById(R.id.deviceName);
        pairedList = findViewById(R.id.pairedList);
        sendFileBtn = findViewById(R.id.buttonSendFile);
        availableList = findViewById(R.id.availableList);

        pairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        availableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedList.setAdapter(pairedAdapter);
        availableList.setAdapter(availableAdapter);

        availableList.setOnItemClickListener((parent, view, position, id) -> {
            String item = availableAdapter.getItem(position);
            if (item == null || !item.contains(" - ")) return;

            String address = item.substring(item.lastIndexOf(" - ") + 3);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            new AlertDialog.Builder(BluetoothActivity.this)
                    .setTitle("Pair Device")
                    .setMessage("Do you want to pair with " + device.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (pairDevice(device)) {
                            Toast.makeText(BluetoothActivity.this, "Pairing initiated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BluetoothActivity.this, "Pairing failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) ->
                            Toast.makeText(BluetoothActivity.this, "Pairing cancelled", Toast.LENGTH_SHORT).show()
                    )
                    .show();
        });


        // Request permissions at start
        checkPermissions();
        // Switch for Bluetooth On/Off
        bluetoothSwitch.setChecked(bluetoothAdapter.isEnabled());
        updateBluetoothUI();
        bluetoothSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    loadPairedDevices();
                }

            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                    return;
                }
                bluetoothAdapter.disable();
                pairedAdapter.clear();
                Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show();
            }
            updateBluetoothUI();
        });

        ImageButton discoverBtn = findViewById(R.id.imageButtonDiscover);
        discoverBtn.setOnClickListener(v -> startDiscovery());

        // Image Button Back
        ImageButton backBtn = findViewById(R.id.imageButtonBack);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(BluetoothActivity.this, MainActivity.class);
            startActivity(intent);
        });

        Button sendFileBtn = findViewById(R.id.buttonSendFile);
        sendFileBtn.setOnClickListener(v -> openFileChooser());

        loadPairedDevices();

        pairedList.setOnItemLongClickListener((parent, view, position, id) -> {
            String item = pairedAdapter.getItem(position);
            if (item == null || !item.contains(" - ")) {
                return true; // skip invalid row
            }

            String address = item.substring(item.lastIndexOf(" - ") + 3);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            new AlertDialog.Builder(BluetoothActivity.this)
                    .setTitle("Unpair Device")
                    .setMessage("Do you want to unpair " + device.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (unpairDevice(device)) {
                            Toast.makeText(BluetoothActivity.this, "Device unpaired", Toast.LENGTH_SHORT).show();
                            loadPairedDevices();
                        } else {
                            Toast.makeText(BluetoothActivity.this, "Failed to unpair", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) ->
                            Toast.makeText(BluetoothActivity.this, "Unpair cancelled", Toast.LENGTH_SHORT).show()
                    )
                    .show();

            return true;
        });

        // Editable device name
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            deviceName.setText(bluetoothAdapter.getName());
        }

        deviceName.setOnEditorActionListener((v, actionId, event) -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
                return false;
            }
            bluetoothAdapter.setName(deviceName.getText().toString());
            Toast.makeText(this, "Device name updated", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Register receivers
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        IntentFilter finishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, finishedFilter);

        // Register bond state change receiver
        IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondReceiver, bondFilter);

        IntentFilter statefilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, statefilter);


    }

    private static final int PICK_FILE_REQUEST = 100;

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // allow any file type
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file to send"), PICK_FILE_REQUEST);
    }


    private void loadPairedDevices() {
        pairedAdapter.clear();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            String name = (device.getName() != null) ? device.getName() : "Unnamed Device";
            pairedAdapter.add(name + " - " + device.getAddress());
        }
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {

                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        return;
                    }

                    String name = (device.getName() != null) ? device.getName() : "Unnamed Device";
                    String deviceInfo = name + " - " + device.getAddress();
                    System.out.println("Discovered: " + deviceInfo);

                    if (availableAdapter.getPosition(deviceInfo) == -1) {
                        availableAdapter.add(deviceInfo);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private final BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (bondState == BluetoothDevice.BOND_BONDED) {
                    if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Toast.makeText(context, "Paired with " + device.getName(), Toast.LENGTH_SHORT).show();
                    loadPairedDevices(); // refresh list automatically
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(context, "Unpaired from " + device.getName(), Toast.LENGTH_SHORT).show();
                    loadPairedDevices();
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    updateBluetoothUI();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    updateBluetoothUI();
                }
            }
        }
    };


    private void updateBluetoothUI() {
        boolean isBtOn = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        deviceName.setEnabled(isBtOn);
        sendFileBtn.setEnabled(isBtOn);

        if (isBtOn) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                deviceName.setText(bluetoothAdapter.getName());
            }
        } else {
            deviceName.setText(""); // or leave previous name
        }
    }

    private boolean pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.setAccessible(true);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // failed → will trigger fallback
    }

    private void startDiscovery() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please turn on Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        availableAdapter.clear();
        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Discovering nearby devices...", Toast.LENGTH_SHORT).show();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                loadPairedDevices();
                startDiscovery();
            } else {
                Toast.makeText(this, "Bluetooth & Location permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                bluetoothSwitch.setChecked(true);
                loadPairedDevices();
            } else {
                bluetoothSwitch.setChecked(false);
                Toast.makeText(this, "Bluetooth enabling canceled", Toast.LENGTH_SHORT).show();
            }
            updateBluetoothUI();
        }

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                sendFileViaBluetooth(fileUri);
            }
        }
    }

    private void sendFileViaBluetooth(Uri fileUri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(getContentResolver().getType(fileUri)); // detect MIME type
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);

        // Restrict intent to Bluetooth
        intent.setPackage("com.android.bluetooth");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Bluetooth not available for file transfer", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(discoveryReceiver);
        unregisterReceiver(bondReceiver);
        unregisterReceiver(bluetoothStateReceiver);

    }

}