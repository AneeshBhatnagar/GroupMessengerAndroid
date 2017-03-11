package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by aneesh on 3/10/17.
 */

public class Message {
    private String message;
    private long senderID;
    private long messageID;
    private float priority;
    private long proposerId;
    private boolean proposed;
    private boolean accepted;

    public Message(String message, long senderID, long messageID, float priority, boolean proposed, boolean accepted) {
        this.message = message;
        this.senderID = senderID;
        this.messageID = messageID;
        this.priority = priority;
        this.proposed = proposed;
        this.accepted = accepted;
        this.proposerId = -1;
    }

    public Message(String message, long senderID, long messageID) {
        this.message = message;
        this.senderID = senderID;
        this.messageID = messageID;
        this.priority = -1;
        this.proposed = false;
        this.accepted = false;
        this.proposerId = -1;
    }

    public void setProposed(float priority, long proposerId) {
        this.priority = priority;
        this.proposerId = proposerId;
        this.proposed = true;
        this.accepted = false;
    }

    public void setAccepted(float priority) {
        this.priority = priority;
        this.proposerId = -1;
        this.accepted = true;
        this.proposed = false;
    }

    public int getMessageType() {
        if (this.accepted == false && this.proposed == false) {
            return 0; //Initial message sent over network
        } else if (this.accepted == false && this.proposed == true) {
            return 1; //Priority proposed and message sent over network
        } else if (this.accepted == true && this.proposed == false) {
            return 2; //Accepted priority and message sent finally by sender along with priority
        }
        return -1; //Invalid message type encountered as both can't be true
    }

    public String getMessage() {
        return message;
    }

    public long getSenderID() {
        return senderID;
    }

    public long getMessageID() {
        return messageID;
    }

    public float getPriority() {
        return priority;
    }


}
