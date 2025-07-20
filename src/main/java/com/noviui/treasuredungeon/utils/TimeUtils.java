package com.noviui.treasuredungeon.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");
    
    public static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return 0;
        }
        
        long totalMillis = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeStr.toLowerCase());
        
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "d":
                    totalMillis += value * 24 * 60 * 60 * 1000L;
                    break;
                case "h":
                    totalMillis += value * 60 * 60 * 1000L;
                    break;
                case "m":
                    totalMillis += value * 60 * 1000L;
                    break;
                case "s":
                    totalMillis += value * 1000L;
                    break;
            }
        }
        
        return totalMillis;
    }
    
    public static long parseTimeToTicks(String timeStr) {
        return parseTimeToMillis(timeStr) / 50; // 20 ticks per second = 50ms per tick
    }
    
    public static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append("d ");
            hours %= 24;
        }
        if (hours > 0) {
            result.append(hours).append("h ");
            minutes %= 60;
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
            seconds %= 60;
        }
        if (seconds > 0 || result.length() == 0) {
            result.append(seconds).append("s");
        }
        
        return result.toString().trim();
    }
}