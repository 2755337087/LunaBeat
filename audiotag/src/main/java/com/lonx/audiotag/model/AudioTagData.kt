package com.lonx.audiotag.model

import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

data class AudioTagData(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val date: String? = null,
    val trackNumber: String? = null,
    val discNumber: Int? = null,

    val composer: String? = null,
    val lyricist: String? = null,
    val comment: String? = null,
    val lyrics: String? = null,
    val fileName: String = "",
    val durationMilliseconds: Int = 0,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,

    val rawProperties: Map<String, Array<String>>? = null,

    val pictures: List<AudioPicture> = ArrayList(),
    val picUrl: String? = null
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readHashMap(Array<String>::class.java.classLoader) as? Map<String, Array<String>>,
        parcel.createTypedArrayList(AudioPicture.CREATOR) ?: ArrayList(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeString(albumArtist)
        parcel.writeString(genre)
        parcel.writeString(date)
        parcel.writeString(trackNumber)
        parcel.writeValue(discNumber)
        parcel.writeString(composer)
        parcel.writeString(lyricist)
        parcel.writeString(comment)
        parcel.writeString(lyrics)
        parcel.writeString(fileName)
        parcel.writeInt(durationMilliseconds)
        parcel.writeInt(bitrate)
        parcel.writeInt(sampleRate)
        parcel.writeInt(channels)
        parcel.writeMap(rawProperties)
        parcel.writeTypedList(pictures)
        parcel.writeString(picUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AudioTagData> {
        override fun createFromParcel(parcel: Parcel): AudioTagData {
            return AudioTagData(parcel)
        }

        override fun newArray(size: Int): Array<AudioTagData?> {
            return arrayOfNulls(size)
        }
    }
}

data class AudioPicture(
    val data: ByteArray,
    val mimeType: String = "image/jpeg",
    val description: String = "",
    val pictureType: String = "Front Cover"
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createByteArray() ?: byteArrayOf(),
        parcel.readString() ?: "image/jpeg",
        parcel.readString() ?: "",
        parcel.readString() ?: "Front Cover"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(data)
        parcel.writeString(mimeType)
        parcel.writeString(description)
        parcel.writeString(pictureType)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioPicture) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    companion object CREATOR : Parcelable.Creator<AudioPicture> {
        override fun createFromParcel(parcel: Parcel): AudioPicture {
            return AudioPicture(parcel)
        }

        override fun newArray(size: Int): Array<AudioPicture?> {
            return arrayOfNulls(size)
        }
    }
}
