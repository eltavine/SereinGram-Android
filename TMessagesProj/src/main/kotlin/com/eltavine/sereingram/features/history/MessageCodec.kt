package com.eltavine.sereingram.features.history

import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC

/** Telegram's own wire form of a message, as its database stores it, local file path included. */
internal object MessageCodec {
    fun encode(message: TLRPC.Message): ByteArray {
        val data = SerializedData(message.objectSize)
        try {
            message.serializeToStream(data)
            return data.toByteArray()
        } finally {
            data.cleanup()
        }
    }

    /** Null when this build's TL schema cannot read [bytes], for example after a layer change. */
    fun decode(bytes: ByteArray, selfId: Long): TLRPC.Message? {
        val data = SerializedData(bytes)
        try {
            val message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false) ?: return null
            message.readAttachPath(data, selfId)
            return message
        } finally {
            data.cleanup()
        }
    }
}
