package edu.buffalo.cse.cse486586.groupmessenger2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aneesh on 3/10/17.
 */

public class Message {
    private String message;
    private long senderID;
    private String messageID;
    private float priority;
    private long proposerID;
    private boolean proposed;
    private boolean accepted;
    private static String[] jsonFields = {"message","senderID","messageID","priority","proposerID","proposed","accepted"};

    public Message(String message, long senderID, String messageID, float priority, boolean proposed, boolean accepted) {
        this.message = message;
        this.senderID = senderID;
        this.messageID = messageID;
        this.priority = priority;
        this.proposed = proposed;
        this.accepted = accepted;
        this.proposerID = -1;
    }

    public Message(Message msg){
        this.message = msg.message;
        this.senderID = msg.senderID;
        this.messageID = msg.messageID;
        this.priority = msg.priority;
        this.proposed = msg.proposed;
        this.accepted = msg.accepted;
        this.proposerID = msg.proposerID;
    }

    public Message(String message, long senderID, String messageID) {
        this.message = message;
        this.senderID = senderID;
        this.messageID = messageID;
        this.priority = -1;
        this.proposed = false;
        this.accepted = false;
        this.proposerID = -1;
    }

    public Message(String jsonString){
        //Used to convert JSON to class object
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            this.message = jsonObject.getString(jsonFields[0]);
            this.senderID = jsonObject.getLong(jsonFields[1]);
            this.messageID = jsonObject.getString(jsonFields[2]);
            this.priority = (float)jsonObject.getDouble(jsonFields[3]);
            this.proposerID = jsonObject.getLong(jsonFields[4]);
            this.proposed = jsonObject.getBoolean(jsonFields[5]);
            this.accepted = jsonObject.getBoolean(jsonFields[6]);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void setProposed(float priority, long proposerID) {
        this.priority = priority;
        this.proposerID = proposerID;
        this.proposed = true;
        this.accepted = false;
    }

    public void setToOriginal(){
        this.priority = -1;
        this.proposed = false;
        this.accepted = false;
        this.proposerID = -1;
    }

    public void setAccepted(float priority) {
        this.priority = priority;
        this.accepted = true;
    }

    public void clearProposer(){
        this.proposerID = -1;
        this.proposed = false;
    }

    public int getMessageType() {
        if (this.accepted == false && this.proposed == false) {
            return 0; //Initial message sent over network
        } else if (this.accepted == false && this.proposed == true) {
            return 1; //Priority proposed and message sent over network
        } else if (this.accepted == true && this.proposed == true){
            return 2; //Priority is finalised and message must be sent again over the network after clearing proposed
        } else if (this.accepted == true && this.proposed == false) {
            return 3; //Accepted priority and message sent finally by sender along with priority
        }
        return -1; //Invalid message type encountered as both can't be true
    }

    public String getMessage() {
        return message;
    }

    public long getSenderID() {
        return senderID;
    }

    public String getMessageID() {
        return messageID;
    }

    public float getPriority() {
        return priority;
    }

    public String getJson(){
        //Converting the class to a JSON format to send as a string across the network
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put(jsonFields[0],this.message);
            jsonObject.put(jsonFields[1],this.senderID);
            jsonObject.put(jsonFields[2],this.messageID);
            jsonObject.put(jsonFields[3],this.priority);
            jsonObject.put(jsonFields[4],this.proposerID);
            jsonObject.put(jsonFields[5],this.proposed);
            jsonObject.put(jsonFields[6],this.accepted);
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
        return jsonObject.toString();
    }

    public long getProposerID() {
        return proposerID;
    }

    public void setPriority(float priority){
        this.priority = priority;
        this.accepted = true;
        this.proposed = false;
        this.proposerID = -1;
    }
}
