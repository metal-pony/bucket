package com.metal_pony.bucket.util;

public record IntCoordinates (int x, int y, int z) {

    public IntCoordinates(int x) {
        this(x, 0, 0);
    }

    public IntCoordinates(int x, int y) {
        this(x, y, 0);
    }

    public static double dist2D(IntCoordinates a, IntCoordinates b) {
        return java.lang.Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y));
    }

    public static double dist3D(IntCoordinates a, IntCoordinates b) {
        return java.lang.Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y) + (a.z-b.z)*(a.z-b.z));
    }
}
