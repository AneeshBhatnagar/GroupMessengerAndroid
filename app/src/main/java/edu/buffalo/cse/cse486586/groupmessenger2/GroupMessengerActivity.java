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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    private static ArrayList<Integer> remotePorts;
    private static HashMap<Integer, Integer> portOrdering;
    private static DatabaseHelper databaseHelper;
    private static HashMap<String, ArrayList<Float>> proposedList;
    private final int SEND_ALL = 1;
    private final int SEND_PROPOSED = 2;
    private ServerTask serverTask;
    private ServerSocket serverSocket;
    private int deliveredMsgCounter;
    private ContentResolver contentResolver;
    private int own_port;
    private Uri uri;
    private int sentMsgCounter;
    private TextView tv;
    private int maxSeenPriority = 0;
    private static PriorityBlockingQueue<Message> priorityQueue;
    private static HashMap<String,Message> backupQueueList;

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
        portOrdering = new HashMap<Integer, Integer>();
        proposedList = new HashMap<String, ArrayList<Float>>();
        backupQueueList = new HashMap<String, Message>();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        own_port = (Integer.parseInt(portStr) * 2);

        int i = 5554;
        int j = 1;
        while (i < 5564) {
            remotePorts.add(i * 2);
            portOrdering.put(i * 2, j);
            i += 2;
            j++;
        }

        //Creating a custom comparator for the message class and initializing the queue

        final Comparator<Message> messageComparator = new Comparator<Message>() {
            @Override
            public int compare(Message msg1, Message msg2) {
                if(msg1.getPriority() < msg2.getPriority())
                    return -1;
                if(msg1.getPriority() == msg2.getPriority())
                    return 0;
                return 1;
            }
        };

        priorityQueue = new PriorityBlockingQueue<Message>(11,messageComparator);
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
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, SEND_ALL);
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

    private class ServerTask extends AsyncTask<ServerSocket, Message, Void> {

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
                    socket.close();
                    Log.d("MSG RECEIVED", msg.getMessage());
                    int msgType = msg.getMessageType();
                    if (msgType == 0) {
                        //Original Message is Sent and priority must be proposed
                        float priority = (float) ((++maxSeenPriority) + (portOrdering.get(own_port) * 0.1));
                        msg.setProposed(priority, own_port);
                        priorityQueue.add(msg);
                        backupQueueList.put(msg.getMessageID(),msg);
                        publishProgress(msg);
                    } else if (msgType == 1) {
                        //Proposed Priority received, must be added to HashMap for this message and handled appropriately
                        Log.d("PROPOSED", msg.getJson());
                        if (proposedList.containsKey(msg.getMessageID())) {
                            //Fetch current Array List and add current priority to it
                            ArrayList<Float> list = proposedList.get(msg.getMessageID());
                            list.add(msg.getPriority());
                            proposedList.remove(msg.getMessageID());
                            proposedList.put(msg.getMessageID(), list);
                        } else {
                            //New ArrayList and add to HashMap
                            ArrayList<Float> newList = new ArrayList<Float>();
                            newList.add(msg.getPriority());
                            proposedList.put(msg.getMessageID(), newList);
                        }
                        ArrayList<Float> arrayList = proposedList.get(msg.getMessageID());
                        if (arrayList.size() == 5) {
                            //Received from all
                            Log.d("Proposed", "Got all");
                            float finalPriority = Collections.max(arrayList);
                            proposedList.remove(msg.getMessageID());
                            msg.setAccepted(finalPriority);
                            publishProgress(msg);
                        } else {
                            Log.d("Proposed", "Got only " + Integer.toString(arrayList.size()));
                        }
                    } else if (msgType == 3) {
                        //Accepted Priority received, must be added to queue and published to text view
                        Message originalMsg = backupQueueList.get(msg.getMessageID());
                        priorityQueue.remove(originalMsg);
                        backupQueueList.remove(msg.getMessageID());
                        priorityQueue.add(msg);
                        if(msg.getPriority()>maxSeenPriority){
                            maxSeenPriority = (int)Math.ceil(msg.getPriority()) + 1;
                        }
                        publishProgress(msg);
                    } else {
                        Log.d("ELSE","INVALID");
                        //Invalid message type encountered
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }


            return null;
        }

        protected void onProgressUpdate(Message... messages) {
            Message msg = messages[0];
            if (msg.getMessageType() == 1) {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, SEND_PROPOSED);
            } else if (msg.getMessageType() == 2) {
                msg.clearProposer();
                //Log.d("Hello","hey there");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, SEND_ALL);
            } else if (msg.getMessageType() == 3) {
                Message x;
                while((x = priorityQueue.peek())!=null){
                    if(x.getMessageType() == 3){
                        x = priorityQueue.poll();
                        ContentValues values = new ContentValues();
                        values.put("key", Integer.toString(deliveredMsgCounter));
                        values.put("value", x.getMessage());
                        deliveredMsgCounter++;
                        contentResolver.insert(uri, values);
                        tv.append(x.getMessage() + Float.toString(x.getPriority()) + "\n");
                    }else{
                        break;
                    }
                }
            }
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
            ArrayList<Integer> sendingPorts = new ArrayList<Integer>(remotePorts);
            int whoToSend = (Integer) msg[1];
            if (whoToSend == SEND_PROPOSED) {
                sendingPorts.clear();
                sendingPorts.add((int) msgToSend.getSenderID());
            }else{
                sendingPorts = remotePorts;
            }
            try {
                for (int port : sendingPorts) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            port);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(msgToSend.getJson());
                    dataOutputStream.flush();
                    Log.d("MSG SENT", msgToSend.getJson());
                    Log.d("Message Type", Integer.toString(whoToSend));
                    Log.d("Length of Remote Ports", Integer.toString(sendingPorts.size()));
                    Log.d("REMOTE PORT", Integer.toString(port));
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
