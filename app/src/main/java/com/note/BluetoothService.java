package com.note;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.util.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothService {
    public final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a67");
    private final String TAG = "BLUETOOTH SERVICE";
    private final String NAME = "NOTE-APP";
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    MenuActivity menuActivity;


    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public BluetoothService(BluetoothAdapter adapter, Context ctx) {
        bluetoothAdapter = adapter;
        context = ctx;
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        private BluetoothSocket mmClientSocket = null;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
            Log.d(TAG, "AcceptThread was created");
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    Log.d(TAG, "AcceptThread tries to accept...");
                    socket = mmServerSocket.accept();
                    if (socket != null) {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        Log.d(TAG, "ACCEPTED");
                        mmClientSocket = socket;
                        manageAcceptedSocket(socket);
                        mmServerSocket.close();
                        break;
                    }
                    else
                        Log.d(TAG, "UNABLE TO ACCEPT");

                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
            }

            Log.d(TAG, "ACCEPT finished life");
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID uuid;

        public ConnectThread(BluetoothDevice device, UUID uid) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            uuid = uid;
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // uuid is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.d(TAG, "CONNECTED");
            } catch (IOException connectException) {
                Log.d(TAG, "UNABLE TO CONNECT");
                connectException.printStackTrace();
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageConnectedSocket(mmSocket);
            Log.d(TAG, "CONNECT finished life");
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                ByteBuffer byteBuffer = ByteBuffer.wrap(mmBuffer, 0, 4);
                Integer len = byteBuffer.getInt();

                Log.d(TAG, "LEN READ: " + len.toString() + " ");

                numBytes -= 4;
                len -= numBytes;
                StringBuilder totalMessage = new StringBuilder(new String(mmBuffer, 4, numBytes, Charset.defaultCharset()));

                // Send the obtained bytes to the UI activity.

                while (len > 0) {
                    numBytes = mmInStream.read(mmBuffer);
                    String incomingMessage = new String(mmBuffer, 0, numBytes, Charset.defaultCharset());

                    Log.d(TAG, "InputStream: " + incomingMessage);

                    totalMessage.append(incomingMessage);
                    len -= numBytes;
                }

                Log.d(TAG, "TOTAL MESSAGE of len: " + totalMessage.length() + " " + totalMessage.toString());

                Intent messageIntent = new Intent("message");
                messageIntent.putExtra("theMessage", totalMessage.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);

            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
            }

            Log.d(TAG, "CONNECTED finished life");
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Integer len = text.length();

            Log.d(TAG, "OutputStream: " + text);
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                byteBuffer.putInt(len);
                mmOutStream.write(byteBuffer.array());
                Log.d(TAG, "LENGTH SENT " + len.toString() + " byteBuffer len: " + byteBuffer.array().length);
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void manageAcceptedSocket(BluetoothSocket socket) {
        manageConnectedSocket(socket);

        byte[] bytes = MenuActivity.serialized.getBytes(Charset.defaultCharset());
        write(bytes);
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        Log.d(TAG, "Connected starting");
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    public void write(byte[] out) {
        Log.d(TAG, "write: write to connected thread");
        connectedThread.write(out);
    }

    public synchronized void startServer() {
        Log.d(TAG, "server start");

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "client start");

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    public void cancelAll() {
        if (acceptThread != null) {
            try { acceptThread.mmServerSocket.close(); } catch (IOException e) {}
            try { acceptThread.mmClientSocket.close(); } catch (IOException e) {}
            acceptThread.stop();
            //acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            try { connectThread.mmSocket.close(); } catch (IOException e) {}
            connectThread.stop();
            //connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            try { connectedThread.mmSocket.close(); } catch (IOException e) {}
            connectedThread.stop();
            //connectedThread.cancel();
            connectedThread = null;
        }
    }
}
