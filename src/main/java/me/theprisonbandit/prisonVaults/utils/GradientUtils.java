package me.theprisonbandit.prisonVaults.utils;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;

public class GradientUtils {

    // --- STATIC GRADIENT ---
    public static String getGradient(String text, String preset) {
        if (preset == null) return text;

        Color[] colors = getColors(preset);
        if (colors != null) {
            // Special handling for Rainbow
            if (preset.equalsIgnoreCase("RAINBOW")) {
                return rainbow(text);
            }
            // For BlueCrew or any multi-color preset, use the first and last colors for static gradient
            // Or interpolate across all if you prefer, but standard 2-point is usually sufficient for static
            // Using first and last ensures BlueCrew (Blue -> Sky -> Blue) looks just Blue, so let's use Index 0 and 1
            if (preset.equalsIgnoreCase("BLUECREW")) {
                return rgbGradient(text, toHex(colors[0]), toHex(colors[1]));
            }

            // Standard 2-color logic
            return rgbGradient(text, toHex(colors[0]), toHex(colors[colors.length - 1]));
        }
        return text;
    }

    // --- ANIMATED GRADIENT ---
    public static String getAnimatedGradient(String text, String preset, int step) {
        Color[] colors = getColors(preset);
        if (colors == null) return getGradient(text, preset);

        StringBuilder builder = new StringBuilder();
        double stepSize = 1.0 / (text.length());

        // Cycle the offset from 0.0 to 1.0 over 50 ticks (adjust speed here)
        double offset = (step % 50) / 50.0;

        for (int i = 0; i < text.length(); i++) {
            double ratio = (i * stepSize) + offset;
            // Wrap ratio to stay within 0.0 - 1.0 bounds for cycling
            while (ratio > 1.0) ratio -= 1.0;

            Color color = interpolate(colors, ratio);
            builder.append(ChatColor.of(color)).append(text.charAt(i));
        }
        return builder.toString();
    }

    // --- COLOR DEFINITIONS (UPDATED) ---
    private static Color[] getColors(String preset) {
        if (preset == null) return null;
        switch (preset.toUpperCase()) {
            // NEW: BlueCrew (RGB: 0,0,255 with shades of Light Blue)
            case "BLUECREW": return new Color[]{
                    new Color(0, 0, 255),    // Pure Blue
                    new Color(135, 206, 250), // Light Sky Blue
                    new Color(0, 0, 255)     // Back to Pure Blue (for smooth looping)
            };

            // Special
            case "RAINBOW": return new Color[]{
                    Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                    Color.BLUE, new Color(75, 0, 130), new Color(148, 0, 211)
            };

            // Blues / Purples
            case "BLURPLE": return new Color[]{new Color(0x5865F2), new Color(0xA020F0)};
            case "DIAMOND": return new Color[]{new Color(0x00C6FF), new Color(0x0072FF)};
            case "MIDNIGHT": return new Color[]{new Color(0x0F2027), new Color(0x2C5364)};
            case "STEEL": return new Color[]{new Color(0x2C3E50), new Color(0xBDC3C7)};
            case "AMETHYST": return new Color[]{new Color(0x9D50BB), new Color(0x6E48AA)};
            case "CYBERLORD": return new Color[]{new Color(0xCC2B5E), new Color(0x753A88)};

            // Reds / Oranges / Yellows
            case "REDINK": return new Color[]{new Color(0xFF0000), new Color(0xFF69B4)};
            case "SUNSET": return new Color[]{new Color(0xFF512F), new Color(0xDD2476)};
            case "VOLCANO": return new Color[]{new Color(0xED213A), new Color(0x93291E)};
            case "BLAZINGFIRE": return new Color[]{new Color(0xFF416C), new Color(0xFF4B2B)};
            case "DESERT": return new Color[]{new Color(0xF7971E), new Color(0xFFD200)};
            case "SUNNYDAY": return new Color[]{new Color(0xF2994A), new Color(0xF2C94C)};
            case "GOLDEN": return new Color[]{new Color(0xFFD700), new Color(0xFDB931)};
            case "FOOLSGOLD": return new Color[]{new Color(0xC5A059), new Color(0x998455)};
            case "WOODEN": return new Color[]{new Color(0x935D37), new Color(0x5C3A21)};
            case "DAWNINGDAY": return new Color[]{new Color(0x4DA0B0), new Color(0xD39D38)};

            // Greens
            case "LEMONLIME": return new Color[]{new Color(0xFFF700), new Color(0x00FF00)};
            case "EMERALD": return new Color[]{new Color(0x50C878), new Color(0x004225)};
            case "GRASSLANDS": return new Color[]{new Color(0xD4FC79), new Color(0x96E6A1)};
            case "SWAMP": return new Color[]{new Color(0x556B2F), new Color(0x8B4513)};

            default: return null;
        }
    }

    // --- MATH HELPERS ---

    private static Color interpolate(Color[] colors, double ratio) {
        int length = colors.length;
        // Adjust progress to fit array length
        double progress = ratio * (length - 1);
        int low = (int) progress;
        int high = low + 1;

        if (high >= length) return colors[length - 1];

        return blend(colors[low], colors[high], progress - low);
    }

    private static Color blend(Color c1, Color c2, double ratio) {
        int r = (int) (c1.getRed() + ratio * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    private static String rgbGradient(String text, String startHex, String endHex) {
        Color start = hexToColor(startHex);
        Color end = hexToColor(endHex);
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            float ratio = (float) i / (float) (chars.length - 1);
            if (chars.length == 1) ratio = 0;
            Color c = blend(start, end, ratio);
            result.append(ChatColor.of(c)).append(chars[i]);
        }
        return result.toString();
    }

    private static String rainbow(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        Color[] colors = getColors("RAINBOW");
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

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}