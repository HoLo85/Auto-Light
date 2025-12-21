package com.mine.autolight;

import java.util.TreeMap;

public class BrightnessAlgorithm {
    // Internal map to hold the 4 lux/brightness pairs
    private static final TreeMap<Integer, Integer> pointsMap = new TreeMap<>();

    /**
     * Updates the internal points used for brightness calculation.
     * Called by MySettings whenever values are loaded or saved.
     */
    public static void setPoints(int l1, int b1, int l2, int b2, int l3, int b3, int l4, int b4) {
        pointsMap.clear();
        pointsMap.put(l1, b1);
        pointsMap.put(l2, b2);
        pointsMap.put(l3, b3);
        pointsMap.put(l4, b4);
    }

    /**
     * Calculates the target brightness based on current lux.
     * Uses linear interpolation between the nearest defined points.
     */
    public static int calculateBrightness(float lux) {
        if (pointsMap.isEmpty()) {
            return 128; // Default fallback (mid-brightness)
        }

        int luxInt = (int) lux;
        Integer lowerLux = pointsMap.floorKey(luxInt);
        Integer upperLux = pointsMap.ceilingKey(luxInt);

        // Case 1: Lux is below the lowest defined point
        if (lowerLux == null) return pointsMap.get(upperLux);
        
        // Case 2: Lux is above the highest defined point
        if (upperLux == null) return pointsMap.get(lowerLux);
        
        // Case 3: Exact match or Lux is between two points
        if (lowerLux.equals(upperLux)) return pointsMap.get(lowerLux);

        int lowerBri = pointsMap.get(lowerLux);
        int upperBri = pointsMap.get(upperLux);

        // Linear interpolation formula: y = y1 + (x - x1) * (y2 - y1) / (x2 - x1)
        float fraction = (lux - lowerLux) / (float) (upperLux - lowerLux);
        int targetBrightness = lowerBri + Math.round(fraction * (upperBri - lowerBri));

        // Ensure brightness stays within system limits (0-255)
        return Math.max(0, Math.min(255, targetBrightness));
    }
}
