package com.warriorssmp.woodcutting.model;

public final class PointsUtil {

    private PointsUtil() {}

    public static String format(long points) {
        return String.format("%,d", points) + " Points";
    }

    public static String formatShort(long points) {
        return String.format("%,d", points);
    }
}
