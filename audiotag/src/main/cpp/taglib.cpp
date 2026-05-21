#include "tfilestream.h"
#include "utils.h"

#include <mpegfile.h>
#include <vorbisfile.h>
#include <flacfile.h>
#include <opusfile.h>
#include <mp4file.h>
#include <wavfile.h>
#include <dsffile.h>
#include <dsdifffile.h>

#include <memory>
#include <stdexcept>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))

TagLib::File* createFileFromContent(TagLib::IOStream *stream,
                                    bool readAudioProperties,
                                    TagLib::AudioProperties::ReadStyle audioPropertiesStyle) {
    stream->seek(0);
    TagLib::File *file = nullptr;

    if (TagLib::MPEG::File::isSupported(stream))
        file = new TagLib::MPEG::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::Ogg::Vorbis::File::isSupported(stream))
        file = new TagLib::Ogg::Vorbis::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::FLAC::File::isSupported(stream))
        file = new TagLib::FLAC::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::Ogg::Opus::File::isSupported(stream))
        file = new TagLib::Ogg::Opus::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::MP4::File::isSupported(stream))
        file = new TagLib::MP4::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::RIFF::WAV::File::isSupported(stream))
        file = new TagLib::RIFF::WAV::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::DSF::File::isSupported(stream))
        file = new TagLib::DSF::File(stream, readAudioProperties, audioPropertiesStyle);
    else if (TagLib::DSDIFF::File::isSupported(stream))
        file = new TagLib::DSDIFF::File(stream, readAudioProperties, audioPropertiesStyle);

    if (file) {
        if (file->isValid()) {
            return file;
        }
        delete file;
    }

    return nullptr;
}

bool hasKeyMetadata(const TagLib::PropertyMap &props) {
    bool hasTitle = props.contains("TITLE") && !props["TITLE"].isEmpty() && !props["TITLE"][0].isEmpty();
    bool hasArtist = props.contains("ARTIST") && !props["ARTIST"].isEmpty() && !props["ARTIST"][0].isEmpty();
    bool hasAlbum = props.contains("ALBUM") && !props["ALBUM"].isEmpty() && !props["ALBUM"][0].isEmpty();
    return hasTitle || hasArtist || hasAlbum;
}

extern "C" {
JNIEXPORT jobject JNICALL
Java_com_lonx_audiotag_TagLib_getAudioProperties(
        JNIEnv *env, jclass, jint fd, jint read_style) {
    try {
        // 先用原方案尝试读取
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        const auto style = static_cast<TagLib::AudioProperties::ReadStyle>(read_style);

        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), true, style));

        if (file) {
            // 检查是否有音频属性（时长不为0）
            if (file->audioProperties() && file->audioProperties()->lengthInMilliseconds() > 0) {
                return getAudioProperties(env, file.get());
            }
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for audio properties");
        
        // 需要重新打开fd或重新定位到开头
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            LOGE("Failed to get path from fd");
            return nullptr;
        }
        
        const TagLibExt::FileRef f(path, stream.get(), true, style);
        
        if (!f.isNull()) {
            jobject result = getAudioProperties(env, f);
            free(path);
            return result;
        }
        
        free(path);
        return nullptr;
    } catch (const std::exception &e) {
        LOGE("Error reading audio properties: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_com_lonx_audiotag_TagLib_getMetadata(
        JNIEnv *env, jclass, jint fd, jboolean read_pictures) {
    try {
        // 先用原方案尝试读取
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (file) {
            // 检查是否有关键元数据
            const TagLib::PropertyMap &props = file->properties();
            if (hasKeyMetadata(props)) {
                jobject propertiesMap = getPropertyMap(env, file.get());
                jobjectArray pictures;
                if (read_pictures) {
                    pictures = getPictures(env, file.get());
                } else {
                    pictures = emptyPictureArray(env);
                }
                return env->NewObject(metadataClass, metadataConstructor, propertiesMap, pictures);
            }
            LOGI("Original method found no key metadata, trying alternative");
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for metadata");
        
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            LOGE("Failed to get path from fd");
            return nullptr;
        }
        
        const TagLibExt::FileRef f(path, stream.get(), false);
        
        if (!f.isNull()) {
            jobject propertiesMap = getPropertyMap(env, f);
            jobjectArray pictures;
            if (read_pictures) {
                pictures = getPictures(env, f);
            } else {
                pictures = emptyPictureArray(env);
            }
            jobject result = env->NewObject(metadataClass, metadataConstructor, propertiesMap, pictures);
            free(path);
            return result;
        }
        
        free(path);
        return nullptr;
    } catch (const std::exception &e) {
        LOGE("Error reading metadata: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_lonx_audiotag_TagLib_getMetadataPropertyValues(
        JNIEnv *env,
        jclass, jint fd, jstring property_name) {

    const char *propertyName = env->GetStringUTFChars(property_name, nullptr);
    if (propertyName == nullptr) return nullptr;

    try {
        // 先用原方案尝试读取
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (file) {
            const auto propertyMap = file->properties();
            auto it = propertyMap.find(TagLib::String(propertyName));

            if (it != propertyMap.end() && !it->second.isEmpty()) {
                const auto valueList = it->second;
                jobjectArray result = env->NewObjectArray(static_cast<jsize>(valueList.size()), stringClass, nullptr);

                int i = 0;
                for (const auto &value: valueList) {
                    jstring jValue = env->NewStringUTF(value.toCString(true));
                    env->SetObjectArrayElement(result, i, jValue);
                    env->DeleteLocalRef(jValue);
                    i++;
                }

                env->ReleaseStringUTFChars(property_name, propertyName);
                return result;
            }
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for property values");
        
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            env->ReleaseStringUTFChars(property_name, propertyName);
            return nullptr;
        }
        
        const TagLibExt::FileRef f(path, stream.get(), false);
        
        if (!f.isNull()) {
            const auto propertyMap = f.properties();
            auto it = propertyMap.find(TagLib::String(propertyName));

            if (it != propertyMap.end()) {
                const auto valueList = it->second;
                jobjectArray result = env->NewObjectArray(static_cast<jsize>(valueList.size()), stringClass, nullptr);

                int i = 0;
                for (const auto &value: valueList) {
                    jstring jValue = env->NewStringUTF(value.toCString(true));
                    env->SetObjectArrayElement(result, i, jValue);
                    env->DeleteLocalRef(jValue);
                    i++;
                }

                env->ReleaseStringUTFChars(property_name, propertyName);
                free(path);
                return result;
            }
        }
        
        env->ReleaseStringUTFChars(property_name, propertyName);
        free(path);
        return nullptr;

    } catch (const std::exception &e) {
        LOGE("Error reading property values: %s", e.what());
        env->ReleaseStringUTFChars(property_name, propertyName);
        return nullptr;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_lonx_audiotag_TagLib_getPictures(
        JNIEnv *env, jclass, jint fd) {
    try {
        // 先用原方案尝试读取
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (file) {
            // 检查是否有图片
            const auto pictures = getPictures(env, file.get());
            if (pictures != nullptr && env->GetArrayLength(pictures) > 0) {
                return pictures;
            }
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for pictures");
        
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            return emptyPictureArray(env);
        }
        
        const TagLibExt::FileRef f(path, stream.get(), false);
        
        if (!f.isNull()) {
            jobjectArray result = getPictures(env, f);
            free(path);
            return result;
        }
        
        free(path);
        return emptyPictureArray(env);
    } catch (const std::exception &e) {
        LOGE("Error reading pictures: %s", e.what());
        return emptyPictureArray(env);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_lonx_audiotag_TagLib_savePropertyMap(
        JNIEnv *env, jclass, jint fd, jobject property_map) {
    try {
        // 先用原方案尝试保存
        auto stream = std::make_unique<TagLib::FileStream>(fd, false);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (file) {
            TagLib::PropertyMap props = file->properties();
            const PropertyMap updates = JniHashMapToPropertyMap(env, property_map);

            for (const auto & update : updates) {
                const TagLib::String &key = update.first;
                const TagLib::StringList &values = update.second;

                if (values.isEmpty() || (values.size() == 1 && values.front().isEmpty())) {
                    props.erase(key);
                } else {
                    props.replace(key, values);
                }
            }

            file->setProperties(props);
            if (file->save()) {
                return true;
            }
            LOGW("Original save failed, trying alternative");
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for saving property map");
        
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            return false;
        }
        
        TagLibExt::FileRef f(path, stream.get(), false);
        
        if (!f.isNull()) {
            const PropertyMap propertyMap = JniHashMapToPropertyMap(env, property_map);
            f.setProperties(propertyMap);
            bool success = f.save();
            free(path);
            return success;
        }
        
        free(path);
        return false;

    } catch (const std::exception &e) {
        LOGE("Error saving property map: %s", e.what());
        return false;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_lonx_audiotag_TagLib_savePictures(
        JNIEnv *env, jclass, jint fd, jobjectArray pictures) {
    try {
        // 先用原方案尝试保存
        auto stream = std::make_unique<TagLib::FileStream>(fd, false);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (file) {
            auto pictureList = JniPictureArrayToPictureList(env, pictures);
            file->setComplexProperties("PICTURE", pictureList);
            if (file->save()) {
                return true;
            }
            LOGW("Original save failed, trying alternative");
        }

        // 原方案不行，尝试备选方案
        LOGI("Falling back to alternative FileRef for saving pictures");
        
        stream->seek(0);
        char *path = getRealPathFromFd(fd);
        
        if (path == nullptr) {
            return false;
        }
        
        TagLibExt::FileRef f(path, stream.get(), false);
        
        if (!f.isNull()) {
            auto pictureList = JniPictureArrayToPictureList(env, pictures);
            f.setComplexProperties("PICTURE", pictureList);
            bool success = f.save();
            free(path);
            return success;
        }
        
        free(path);
        return false;
    } catch (const std::exception &e) {
        LOGE("Error saving pictures: %s", e.what());
        return false;
    }
}

}
