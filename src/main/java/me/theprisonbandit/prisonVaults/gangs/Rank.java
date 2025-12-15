package me.theprisonbandit.prisonVaults.gangs;

public enum Rank {
    LEADER(7, "Leader"),
    CO_LEADER(6, "Co-Leader"),
    ELITE(5, "Elite"),
    SHOT_CALLER(4, "Shot-Caller"),
    THUG(3, "Thug"),
    BRUTE(2, "Brute"),
    HUSTLER(1, "Hustler"),
    MEMBER(0, "Member");

    public final int weight;
    public final String display;

    Rank(int weight, String display) {
        this.weight = weight;
        this.display = display;
    }

    public boolean isAtLeast(Rank other) {
        return this.weight >= other.weight;
    }
}