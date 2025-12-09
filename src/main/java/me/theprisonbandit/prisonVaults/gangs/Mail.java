package me.theprisonbandit.prisonVaults.gangs;

public class Mail {
    private final String sender;
    private final String message;
    private final long timestamp;

    public Mail(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}