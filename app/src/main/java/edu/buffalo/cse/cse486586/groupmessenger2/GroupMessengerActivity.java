package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    private static ArrayList<Integer> remotePorts;
    private static DatabaseHelper databaseHelper;
    private ServerTask serverTask;
    private ServerSocket serverSocket;
    private int deliveredMsgCounter;
    private ContentResolver contentResolver;
    private int own_port;
    private Uri uri;
    private int sentMsgCounter;
    private final int SEND_MESSAGE = 1;
    private final int SEND_PROPOSED = 2;
    private final int SEND_ACCEPTED = 3;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        databaseHelper = new DatabaseHelper(getApplicationContext());
        deliveredMsgCounter = 0;
        sentMsgCounter = 0;
        contentResolver = getContentResolver();
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        uri = uriBuilder.build();

        //Initializing Own Port and calculating ports for other devices
        remotePorts = new ArrayList<Integer>();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        own_port = (Integer.parseInt(portStr) * 2);

        int i = 5554;
        while (i < 5564) {
            remotePorts.add(i * 2);
            i += 2;
        }

        //Setting the serverSocket
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        serverTask = new ServerTask();
        serverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        final EditText editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msgText = editText.getText().toString();
                String msgId = Integer.toString(own_port) + "-" + Integer.toString(++sentMsgCounter);
                Message msg = new Message(msgText, own_port, msgId);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, SEND_MESSAGE);
                editText.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        ServerSocket serverSocket;
        SQLiteDatabase sqLiteDatabase;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            serverSocket = sockets[0];
            sqLiteDatabase = databaseHelper.getWritableDatabase();
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    String jsonString = dataInputStream.readUTF();
                    Message msg = new Message(jsonString);
                    dataOutputStream.writeUTF("OK");
                    Log.d("MSG RECEIVED", msg.getMessage());
                    publishProgress(msg.getMessage());
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }


            return null;
        }

        protected void onProgressUpdate(String... strings) {
            ContentValues values = new ContentValues();
            values.put("key", Integer.toString(deliveredMsgCounter));
            values.put("value", strings[0]);
            deliveredMsgCounter++;
            contentResolver.insert(uri, values);
            tv.append(strings[0] + "\n");
            return;
        }

        @Override
        protected void onCancelled() {
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sqLiteDatabase.close();
            super.onCancelled();
        }
    }

    private class ClientTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... msg) {
            Message msgToSend = (Message) msg[0];
            int whoToSend = (Integer) msg[1];
            try {
                for (int port : remotePorts) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            port);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(msgToSend.getJson());
                    dataOutputStream.flush();
                    Log.d("MSG SENT", msgToSend.getJson());
                    Log.d("Message Type", Integer.toString(whoToSend));
                    DataInputStream dataInputStream = new DataInputStream((socket.getInputStream()));
                    String resp = dataInputStream.readUTF();
                    if (resp.equals("OK"))
                        socket.close();

                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }


}
