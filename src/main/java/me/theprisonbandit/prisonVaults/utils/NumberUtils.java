package me.theprisonbandit.prisonVaults.utils;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class NumberUtils {

    private static final NavigableMap<Double, String> suffixes = new TreeMap<>();
    private static final DecimalFormat df = new DecimalFormat("#.##");

    static {
        suffixes.put(1_000D, "k");         // Thousands
        suffixes.put(1_000_000D, "M");     // Millions
        suffixes.put(1_000_000_000D, "B"); // Billions
        suffixes.put(1_000_000_000_000D, "T"); // Trillions
        suffixes.put(1_000_000_000_000_000D, "q"); // Quadrillions
        suffixes.put(1_000_000_000_000_000_000D, "Q"); // Quintillions

        // --- NEW HIGHER TIERS ---
        suffixes.put(1_000_000_000_000_000_000_000D, "s"); // Sextillion
        suffixes.put(1_000_000_000_000_000_000_000_000D, "S"); // Septillion
        suffixes.put(1_000_000_000_000_000_000_000_000_000D, "O"); // Octillion
        suffixes.put(1_000_000_000_000_000_000_000_000_000_000D, "N"); // Nonillion
        suffixes.put(1_000_000_000_000_000_000_000_000_000_000_000D, "D"); // Decillion
    }

    public static String format(double value) {
        // Handle Edge Cases
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity";
        if (value < 0) return "-" + format(-value);

        // If small number, just show it normally
        if (value < 1000) return df.format(value);

        // Find the suffix
        Map.Entry<Double, String> e = suffixes.floorEntry(value);

        // Safety check: if the number is somehow bigger than our list (Decillion+),
        // we default to the highest one we have to prevent crashing.
        if (e == null) {
            return df.format(value);
        }

        Double divideBy = e.getKey();
        String suffix = e.getValue();

        double truncated = value / divideBy;

        return df.format(truncated) + suffix;
    }
}