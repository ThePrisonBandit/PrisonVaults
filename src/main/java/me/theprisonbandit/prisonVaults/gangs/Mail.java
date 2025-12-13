package me.theprisonbandit.prisonVaults.gangs;

public class Mail {
    private final String sender;
    private final long timestamp;
    private final String message;

    public Mail(String sender, long timestamp, String message) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
}