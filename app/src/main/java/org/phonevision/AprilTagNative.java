package org.phonevision;

import java.nio.ByteBuffer;

/** JNI wrapper around the AprilRobotics apriltag C library (tag36h11). */
public class AprilTagNative {
    static { System.loadLibrary("pvnative"); }
    public static native long create(int threads, float decimate, float sigma);
    public static native void configure(long handle, int threads, float decimate, float sigma);
    public static native void destroy(long handle);
    /** @return 11 doubles per tag: id, hamming, margin, 8 corner coords (apriltag order). */
    public static native double[] detect(long handle, ByteBuffer yPlane, int w, int h, int rowStride);
}
