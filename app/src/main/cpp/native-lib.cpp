#include <jni.h>
#include <string>

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_LyricBox_utils_SecureStorage_getNativeDefaultKey(JNIEnv *env, jclass) {
    const char *key = "244394";
    return env->NewStringUTF(key);
}

}
