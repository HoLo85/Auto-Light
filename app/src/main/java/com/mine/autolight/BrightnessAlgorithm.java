package com.mine.autolight;

import java.util.Map;
import java.util.TreeMap;

public class BrightnessAlgorithm {

    /**
     * Uses the user-defined points from MySettings but connects them
     * with smooth transitions instead of harsh linear jumps.
     */
    public static int calculateBrightness(float ambientLux) {
        // 1. Get the points from your existing settings file
        TreeMap<Float, Integer> points = new TreeMap<>(MySettings.getPoints());

        // Handle edge cases (lux lower than your lowest point or higher than highest)
        if (ambientLux <= points.firstKey()) return points.firstEntry().getValue();
        if (ambientLux >= points.lastKey()) return points.lastEntry().getValue();

        // 2. Find the two points the current lux sits between (The "Segment")
        Map.Entry<Float, Integer> lower = points.floorEntry(ambientLux);
        Map.Entry<Float, Integer> upper = points.ceilingEntry(ambientLux);

        if (lower == null || upper == null) return 125; // Default fallback

        // 3. Apply Segmented Logarithmic Interpolation
        return interpolateLogarithmically(
                lower.getKey(), lower.getValue(), 
                upper.getKey(), upper.getValue(), 
                ambientLux
        );
    }

    private static int interpolateLogarithmically(float x1, int y1, float x2, int y2, float currentX) {
        // If the lux values are the same to avoid division by zero
        if (x1 == x2) return y1;

        // Formula: y = y1 + (y2 - y1) * [ log(currentX) - log(x1) ] / [ log(x2) - log(x1) ]
        // This ensures that within your custom points, the transition follows 
        // the logarithmic perception of the human eye.
        
        double logX = Math.log(currentX);
        double logX1 = Math.log(x1);
        double logX2 = Math.log(x2);

        double fraction = (logX - logX1) / (logX2 - logX1);
        double result = y1 + (y2 - y1) * fraction;

        return (int) Math.round(result);
    }
}