package me.diegopyl

import java.io.ByteArrayOutputStream

sealed class BencodeValue {
    data class BString(val value: String) : BencodeValue()
    class BStringByte(val value: ByteArray) : BencodeValue()
    data class BInteger(val value: Long) : BencodeValue()
    data class BList(val value: List<BencodeValue>) : BencodeValue()
    data class BDictionary(val value: Map<String, BencodeValue>) : BencodeValue()
}

private const val NUMBER = 'i'
private const val LIST = 'l'
private const val DICTIONARY = 'd'
private const val TERMINATOR = 'e'

class Bencode {

    fun decode(bytes: ByteArray): BencodeValue {
        return when (bytes.firstOrNull()?.toInt()?.toChar()) {
            in '0'..'9' -> decodeString(bytes).first
            'i' -> decodeInteger(bytes).first
            'l' -> decodeList(bytes).first
            'd' -> decodeDictionary(bytes).first
            else -> throw BencodeException("Unknown Bencode type")
        }
    }

    private fun decodeString(bytes: ByteArray): Pair<BencodeValue.BStringByte, ByteArray> {
        val length = String(bytes.takeWhile { it.toInt().toChar().isDigit() }.toByteArray()).toInt()
        val value = bytes.dropWhile { it.toInt().toChar().isDigit() }.drop(1).take(length).toByteArray()
        val remaining = bytes.drop(length + 1 + length.toString().length)
        return Pair(BencodeValue.BStringByte(value), remaining.toByteArray())
    }

    private fun decodeInteger(bytes: ByteArray): Pair<BencodeValue.BInteger, ByteArray> {
        val value = String(bytes.drop(1).takeWhile { it.toInt() != TERMINATOR.code }.toByteArray()).toLong()
        val remaining = bytes.dropWhile { it.toInt() != TERMINATOR.code }.drop(1)
        return Pair(BencodeValue.BInteger(value), remaining.toByteArray())
    }

    private fun decodeList(bytes: ByteArray): Pair<BencodeValue.BList, ByteArray> {
        val list = mutableListOf<BencodeValue>()
        var remaining = bytes.drop(1).toByteArray()
        while (remaining.first().toInt() != TERMINATOR.code) {
            val (value, newRemaining) = when (remaining.first().toInt().toChar()) {
                NUMBER -> decodeInteger(remaining)
                LIST -> decodeList(remaining)
                DICTIONARY -> decodeDictionary(remaining)
                else -> decodeString(remaining)
            }
            list.add(value)
            remaining = newRemaining
        }
        return Pair(BencodeValue.BList(list), remaining.drop(1).toByteArray())
    }

    private fun decodeDictionary(bytes: ByteArray): Pair<BencodeValue.BDictionary, ByteArray> {
        val dictionary = mutableMapOf<String, BencodeValue>()
        // take as list and then chunk it by 2
        val (list, remaining) = decodeList(bytes)
        val pairs = list.value.chunked(2)
        pairs.forEach { (key, value) ->
            if (key !is BencodeValue.BStringByte) {
                throw BencodeException("Dictionary key must be a string")
            }
            dictionary[key.value.toString(charset = Charsets.UTF_8)] = value
        }
        return Pair(BencodeValue.BDictionary(dictionary), remaining)
    }


    fun encode(bencodeValue: BencodeValue): ByteArray {
        return when (bencodeValue) {
            is BencodeValue.BString -> encodeString(bencodeValue)
            is BencodeValue.BStringByte -> encodeStringByte(bencodeValue)
            is BencodeValue.BInteger -> encodeNumber(bencodeValue)
            is BencodeValue.BList -> encodeList(bencodeValue)
            is BencodeValue.BDictionary -> encodeDictionary(bencodeValue)
        }
    }

    private fun encodeString(bencodeValue: BencodeValue.BString): ByteArray {
        val buffer = ByteArrayOutputStream()
        val bytes = bencodeValue.value.toByteArray()
        buffer.write(bytes.size.toString().toByteArray())
        buffer.write(':'.code)
        buffer.write(bytes)
        return buffer.toByteArray()
    }

    private fun encodeStringByte(bencodeValue: BencodeValue.BStringByte): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write(bencodeValue.value.size.toString().toByteArray())
        buffer.write(':'.code)
        buffer.write(bencodeValue.value)
        return buffer.toByteArray()
    }

    private fun encodeNumber(bencodeValue: BencodeValue.BInteger): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write('i'.code)
        buffer.write(bencodeValue.value.toString().toByteArray())
        buffer.write('e'.code)
        return buffer.toByteArray()
    }

    private fun encodeList(bencodeValue: BencodeValue.BList): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write('l'.code)
        bencodeValue.value.forEach {
            buffer.write(encode(it))
        }
        buffer.write('e'.code)
        return buffer.toByteArray()
    }

    private fun encodeDictionary(bencodeValue: BencodeValue.BDictionary): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write('d'.code)
        bencodeValue.value.forEach { (key, value) ->
            buffer.write(encodeString(BencodeValue.BString(key)))
            buffer.write(encode(value))
        }
        buffer.write('e'.code)
        return buffer.toByteArray()
    }
}

class BencodeException(message: String) : Exception(message)

