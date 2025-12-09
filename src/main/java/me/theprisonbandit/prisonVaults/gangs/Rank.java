package me.theprisonbandit.prisonVaults.gangs;

public enum Rank {
    LEADER(4, "Leader"),
    CO_LEADER(3, "Co-Leader"),
    ELITE(2, "Elite"),
    THUG(1, "Thug"),
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