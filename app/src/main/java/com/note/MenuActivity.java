package com.note;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.note.docstools.DocumentInfo;
import com.note.docstools.util.JPEGConverter;
import com.note.docstools.util.Serializer;

import org.bson.Document;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import io.realm.mongodb.App;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class MenuActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 1;

    ListView listView;
    ListView devicesListView;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    ArrayList<String> deviceNames = new ArrayList<>();
    BluetoothDevice bluetoothDevice;
    BluetoothService bluetoothService;
    private boolean sending = false;
    private MenuActivity menu = this;

    public static String workDocName = "";
    public static String serialized = "";
    public static boolean newlyCreated = false;
    public ArrayList<Document> documents = new ArrayList<>();
    public ArrayList<String> docNames = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    private DriveUploader driveUploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        listView = findViewById(R.id.listview);

        serialized = "";
        workDocName = "siema";
        newlyCreated = false;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        AppCompatButton bluetoothReceive = findViewById(R.id.bluetoothReceiveButton);
        bluetoothReceive.setOnClickListener(e -> {
            bluetoothAction(false);
        });
        bluetoothService = new BluetoothService(bluetoothAdapter, getApplicationContext());
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("message"));

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btEnableReceiver, filter);

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        registerReceiver(enDiscoverableReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveringReceiver, filter2);

        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondReceiver, filter3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        readDocuments(MainActivity.app);
    }

    public void createNote(View view) {
        EditText newDocName = findViewById(R.id.newnotetext);
        String docName = newDocName.getText().toString();
        if (!docName.equals("") && !docNames.contains(docName)) {
            workDocName = docName;
            serialized =  "";
            newlyCreated = true;
            openNote();
        }
        else {
            Toast.makeText(getApplicationContext(), "Already exists", Toast.LENGTH_SHORT).show();
        }
    }

    public void openNote() {
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    public void displayDocuments(MongoCursor<Document> docs) {
        documents.clear();
        docNames.clear();
        listView.clearChoices();

        while (docs.hasNext()) {
            Document doc = docs.next();
            Log.d("DOC NAMES", doc.get("name").toString());
            docNames.add(doc.get("name").toString());
            documents.add(doc);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.text_item, docNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                workDocName = docNames.get(position);
                serialized = documents.get(position).get("document").toString();
                newlyCreated = false;
                showPopupMenu();
            }
        });
    }

    public void showPopupMenu() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View contactPopupView = getLayoutInflater().inflate(R.layout.popup, null);
        AppCompatButton open = contactPopupView.findViewById(R.id.openButton);
        AppCompatButton download = contactPopupView.findViewById(R.id.downloadButton);
        AppCompatButton bluetoothShare = contactPopupView.findViewById(R.id.bluetoothShareButton);
        AppCompatButton driveUpload = contactPopupView.findViewById(R.id.driveUploadButton);
        AppCompatButton delete = contactPopupView.findViewById(R.id.deleteButton);
        AppCompatButton back = contactPopupView.findViewById(R.id.backButton);

        dialogBuilder.setView(contactPopupView);
        Dialog dialog = dialogBuilder.create();

        dialog.show();

        open.setOnClickListener(e -> {
            openNote();
            dialog.dismiss();
        });
        download.setOnClickListener(e -> {
            DocumentInfo documentInfo = Serializer.deserializeDocument(serialized);
            JPEGConverter.saveAsJPEG(documentInfo, workDocName, getApplicationContext());
            dialog.dismiss();
        });
        bluetoothShare.setOnClickListener(e -> {
            bluetoothAction(true);
        });
        driveUpload.setOnClickListener(e -> {
            requestSignIn();
        });
        delete.setOnClickListener(e -> {
            deleteDocument(MainActivity.app, workDocName);
            dialog.dismiss();
        });
        back.setOnClickListener(e -> {
            dialog.dismiss();
        });
    }

    public void readDocuments(App app) {
        User appUser = app.currentUser();
        MongoClient mongoClient = appUser.getMongoClient("mongodb-atlas");
        MongoDatabase db = mongoClient.getDatabase("NoteDatabase");
        MongoCollection<Document> collection = db.getCollection("NoteCollection");

        Document filter = new Document().append("userid", appUser.getId());
        RealmResultTask<MongoCursor<Document>> docs = collection.find(filter).iterator();

        docs.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                Log.d("DOCUMENT", "displaying");
                displayDocuments(results);
            }
            else {
                Log.d("DOCUMENT", task.getError().toString());
            }
        });
    }

    public void deleteDocument(App app, String name) {
        User appUser = app.currentUser();
        MongoClient mongoClient = appUser.getMongoClient("mongodb-atlas");
        MongoDatabase db = mongoClient.getDatabase("NoteDatabase");
        MongoCollection<Document> collection = db.getCollection("NoteCollection");

        Document filter = new Document().append("userid", appUser.getId());
        filter.append("name", name);

        collection.deleteOne(filter).getAsync(task -> {
            if (task.isSuccess()) {
                Log.d("DELETE DOCUMENT", appUser.getId() + " " + name);
            } else {
                Log.d("DELETE DOCUMENT", task.getError().toString());
            }
            readDocuments(app);
        });
    }

    // BLUETOOTH

    public void bluetoothAction(boolean send) {
        sending = send;
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

            if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                enableDiscoverable();
                return;
            }

            deviceNames.clear();
            devices.clear();
            showPopupBluetooth();
            bluetoothDiscover();
        }
        else
            enableBluetooth();
    }

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("theMessage");
            DocumentInfo info = Serializer.deserializeDocument(message);
            if (!docNames.contains(info.name)) {
                workDocName = info.name;
                serialized = message;
                newlyCreated = true;
                openNote();
            }
            else {
                Toast.makeText(getApplicationContext(), "Already exists", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver btEnableReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("BLUETOOTH", "onReceive: " + action);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                    Log.d("BLUETOOTH", "enabled bt");
                    enableDiscoverable();
                }
            }
        }
    };

    private final BroadcastReceiver enDiscoverableReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE.equals(action)) {
                Log.d("BLUETOOTH", "enabled discoverable");
                Toast.makeText(getApplicationContext(), "Enabled Bluetooth. Click again", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver discoveringReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("BLUETOOTH", "discovered " + deviceName);

                if (device != null && deviceName != null) {
                    devices.add(device);
                    deviceNames.add(deviceName);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.text_item, deviceNames);
                devicesListView.setAdapter(adapter);

                devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        bluetoothPair(devices.get(position));
                    }
                });
            }
        }
    };

    private final BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d("BLUETOOTH", "bonded " + deviceName);
                    if (!sending)
                        startWorkingAsClient(device);
                    else
                        startWorkingAsServer();
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d("BLUETOOTH", "bonding " + deviceName);
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d("BLUETOOTH", "bond broken " + deviceName);
                }
            }
        }
    };

    public void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Log.d("BLUETOOTH", "Device doesn't support bluetooth.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    public void enableDiscoverable() {
        Log.d("BLUETOOTH", "enable Discoverable");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);

        startActivity(discoverableIntent);
    }

    public void bluetoothDiscover() {

        Log.d("BLUETOOTH", "bluetooth Discover");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        checkBTPermissions();
        bluetoothAdapter.startDiscovery();
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d("BLUETOOTH", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    public void bluetoothPair(BluetoothDevice device) {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            if (!sending)
                startWorkingAsClient(device);
            else
                startWorkingAsServer();
            return;
        }
        device.createBond();
        Log.d("BLUETOOTH", "bluetooth paired with " + device.getName());
    }

    public void showPopupBluetooth() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View contactPopupView = getLayoutInflater().inflate(R.layout.bt_devices_popup, null);
        devicesListView = contactPopupView.findViewById(R.id.btlistview);

        dialogBuilder.setView(contactPopupView);
        Dialog dialog = dialogBuilder.create();
        dialog.show();
    }

    public void startWorkingAsServer() {
        bluetoothService.startServer();
    }

    public void startWorkingAsClient(BluetoothDevice device) {
        bluetoothService.startClient(device, bluetoothService.MY_UUID);
    }

    public void sendDocument(BluetoothDevice device) {
        byte[] bytes = serialized.getBytes(Charset.defaultCharset());
        bluetoothService.write(bytes);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(enDiscoverableReceiver);
        unregisterReceiver(discoveringReceiver);
        unregisterReceiver(btEnableReceiver);
        unregisterReceiver(bondReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }


    // GOOGLE DRIVE



    public void requestSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestScopes(new Scope(DriveScopes.DRIVE_FILE)).build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        Log.d("DRIVE", "STARTING ACTIVITY FOR RESULT");
        startActivityForResult(client.getSignInIntent(), 400);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("DRIVE", "HANDLING SIGN IN " + requestCode + " " + resultCode);

        if (requestCode == 400) {
            if (resultCode == RESULT_OK) {
                handleSignInIntent(data);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                Log.d("DRIVE", "onSuccess: ");
                // TODO
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(googleSignInAccount.getAccount());
                Drive driveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential
                ).setApplicationName("note-app").build();

                driveUploader = new DriveUploader(driveService, getApplicationContext());
                driveUploader.createFile(workDocName, Serializer.deserializeDocument(serialized))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                                  @Override
                                                  public void onSuccess(String s) {
                                                      Log.d("DRIVE", "Successful upload");
                                                      Toast.makeText(getApplicationContext(), "Uploaded to drive.", Toast.LENGTH_SHORT).show();
                                                  }
                                              }
                        ).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("DRIVE", "Failed to upload");
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Failed to upload.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("DRIVE", "onFailure: ");
            }
        });
    }
}