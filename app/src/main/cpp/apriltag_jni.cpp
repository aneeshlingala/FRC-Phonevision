#include <jni.h>
extern "C" {
#include "apriltag.h"
#include "tag36h11.h"
#include "common/image_u8.h"
#include "common/zarray.h"
}
struct Ctx { apriltag_detector_t* td; apriltag_family_t* tf; };

extern "C" {

JNIEXPORT jlong JNICALL Java_org_phonevision_AprilTagNative_create(JNIEnv*, jclass, jint threads, jfloat decimate, jfloat sigma) {
    Ctx* c = new Ctx();
    c->tf = tag36h11_create();
    c->td = apriltag_detector_create();
    apriltag_detector_add_family_bits(c->td, c->tf, 1);
    c->td->nthreads = threads;
    c->td->quad_decimate = decimate;
    c->td->quad_sigma = sigma;
    c->td->refine_edges = true;
    return (jlong)c;
}

JNIEXPORT void JNICALL Java_org_phonevision_AprilTagNative_configure(JNIEnv*, jclass, jlong p, jint threads, jfloat decimate, jfloat sigma) {
    Ctx* c = (Ctx*)p;
    c->td->nthreads = threads;
    c->td->quad_decimate = decimate;
    c->td->quad_sigma = sigma;
}

JNIEXPORT void JNICALL Java_org_phonevision_AprilTagNative_destroy(JNIEnv*, jclass, jlong p) {
    Ctx* c = (Ctx*)p;
    apriltag_detector_destroy(c->td);
    tag36h11_destroy(c->tf);
    delete c;
}

// Returns 11 doubles per detection: id, hamming, decisionMargin, then 4 corners (x,y) x4
JNIEXPORT jdoubleArray JNICALL Java_org_phonevision_AprilTagNative_detect(JNIEnv* env, jclass, jlong p, jobject buf, jint w, jint h, jint stride) {
    Ctx* c = (Ctx*)p;
    // image_u8_t has const members, so it must be aggregate-initialised
    image_u8_t img = { (int32_t)w, (int32_t)h, (int32_t)stride, (uint8_t*)env->GetDirectBufferAddress(buf) };
    zarray_t* dets = apriltag_detector_detect(c->td, &img);
    int n = zarray_size(dets);
    jdoubleArray out = env->NewDoubleArray(n * 11);
    for (int i = 0; i < n; i++) {
        apriltag_detection_t* d;
        zarray_get(dets, i, &d);
        double v[11] = { (double)d->id, (double)d->hamming, (double)d->decision_margin,
            d->p[0][0], d->p[0][1], d->p[1][0], d->p[1][1], d->p[2][0], d->p[2][1], d->p[3][0], d->p[3][1] };
        env->SetDoubleArrayRegion(out, i * 11, 11, v);
    }
    apriltag_detections_destroy(dets);
    return out;
}

}
