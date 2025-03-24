package me.diegopyl

import java.security.MessageDigest

fun ByteArray.formatHexToString(): String {
    return this.joinToString("") { "%02x".format(it) }
}

fun sha1Hash(data: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(data)
    return digest
}