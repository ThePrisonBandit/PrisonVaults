package me.theprisonbandit.prisonVaults.utils;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;

public class GradientUtils {

    public static String getGradient(String text, String preset) {
        if (preset == null) return text;

        switch (preset.toUpperCase()) {
            // --- ORIGINAL & NEW REQUESTS ---
            case "RAINBOW": return rainbow(text);
            case "BLURPLE": return rgbGradient(text, "#5865F2", "#A020F0");
            case "REDINK": return rgbGradient(text, "#FF0000", "#FF69B4");
            case "LEMONLIME": return rgbGradient(text, "#FFF700", "#00FF00");

            // --- ALL OTHER PRESETS ---
            case "SUNSET": return rgbGradient(text, "#FF512F", "#DD2476");
            case "DAWNINGDAY": return rgbGradient(text, "#4DA0B0", "#D39D38");
            case "MIDNIGHT": return rgbGradient(text, "#0F2027", "#2C5364");
            case "GRASSLANDS": return rgbGradient(text, "#D4FC79", "#96E6A1");
            case "SWAMP": return rgbGradient(text, "#556B2F", "#8B4513");
            case "STEEL": return rgbGradient(text, "#2C3E50", "#BDC3C7");
            case "GOLDEN": return rgbGradient(text, "#FFD700", "#FDB931");
            case "FOOLSGOLD": return rgbGradient(text, "#C5A059", "#998455");
            case "DIAMOND": return rgbGradient(text, "#00C6FF", "#0072FF");
            case "EMERALD": return rgbGradient(text, "#50C878", "#004225");
            case "AMETHYST": return rgbGradient(text, "#9D50BB", "#6E48AA");
            case "WOODEN": return rgbGradient(text, "#935D37", "#5C3A21");
            case "SUNNYDAY": return rgbGradient(text, "#F2994A", "#F2C94C");
            case "CYBERLORD": return rgbGradient(text, "#CC2B5E", "#753A88");
            case "VOLCANO": return rgbGradient(text, "#ED213A", "#93291E");
            case "BLAZINGFIRE": return rgbGradient(text, "#FF416C", "#FF4B2B");
            case "DESERT": return rgbGradient(text, "#F7971E", "#FFD200");

            default: return text;
        }
    }

    // --- MATH ENGINE ---

    private static String rgbGradient(String text, String startHex, String endHex) {
        Color start = hexToColor(startHex);
        Color end = hexToColor(endHex);
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            float ratio = (float) i / (float) (chars.length - 1);
            if (chars.length == 1) ratio = 0;

            int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
            int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
            int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);

            ChatColor color = ChatColor.of(new Color(red, green, blue));
            result.append(color).append(chars[i]);
        }
        return result.toString();
    }

    private static String rainbow(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
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

    private static Color hexToColor(String hex) {
        return new Color(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }
}