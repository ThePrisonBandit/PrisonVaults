package me.theprisonbandit.prisonVaults.utils;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientUtils {

    // --- PRESET DEFINITIONS ---
    // Format: "HEX_START:HEX_END"
    // Rainbow is special, so we handle it separately

    public static String getGradient(String text, String preset) {
        switch (preset.toLowerCase()) {
            case "blurple":
                return rgbGradient(text, new Color(88, 101, 242), new Color(0, 0, 255)); // Discord Blurple -> Blue
            case "lemonlime":
                return rgbGradient(text, new Color(50, 205, 50), new Color(255, 255, 0)); // Lime -> Yellow
            case "redink":
                return rgbGradient(text, new Color(255, 0, 0), new Color(255, 105, 180)); // Red -> Hot Pink
            case "rainbow":
                return rainbow(text);
            default:
                return text; // Return plain text if preset not found
        }
    }

    // --- MATH ENGINE ---

    private static String rgbGradient(String text, Color start, Color end) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            float ratio = (float) i / (float) (chars.length - 1);
            if (chars.length == 1) ratio = 0; // Fix for single letter names

            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            ChatColor color = ChatColor.of(new Color(red, green, blue));
            result.append(color).append(chars[i]);
        }
        return result.toString();
    }

    private static String rainbow(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        // Define rainbow colors
        Color[] colors = {
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                Color.BLUE, new Color(75, 0, 130), new Color(148, 0, 211)
        };

        int step = 0;
        for (char c : chars) {
            result.append(ChatColor.of(colors[step % colors.length])).append(c);
            step++;
        }
        return result.toString();
    }
}