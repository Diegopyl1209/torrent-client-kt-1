package me.diegopyl

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers

class Peer(
    private val torrentFile: TorrentFile,
    private val ip: String,
    private val port: Int,
) {
    private var socket: Socket? = null
    private var tx: ByteWriteChannel? = null
    private var rx: ByteReadChannel? = null

    var bitfield: List<Boolean>? = null

    suspend fun connect() {
        socket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(ip, port)
        tx = socket!!.openWriteChannel(autoFlush = true)
        rx = socket!!.openReadChannel()
    }

    suspend fun handshake() {
        val protocol = "BitTorrent protocol"
        val reserved = ByteArray(8) { 0 }
        if (tx == null || rx == null) {
            throw IllegalStateException("Socket is closed")
        }

        tx!!.writeByte(protocol.length.toByte())
        tx!!.writeByteArray(protocol.toByteArray())
        tx!!.writeByteArray(reserved)
        tx!!.writeByteArray(torrentFile.infoHash)
        tx!!.writeByteArray("W01LCOAPWKS92MCUAOKS".toByteArray())

        val responseProtocolLength = rx!!.readByte()
        val responseProtocol = rx!!.readByteArray(protocol.length)
        val responseReserved = rx!!.readByteArray(reserved.size)
        val responseInfoHash = rx!!.readByteArray(20)
        val responsePeerId = rx!!.readByteArray(20)

        val responsePeerIdFormated = responseInfoHash.formatHexToString()
        println("responseInfoHash: $responsePeerIdFormated")

        // wait for bitfield
        val messageLength = rx!!.readInt() // 4 bytes
        val messageId = rx!!.readByte()
        val payload = rx!!.readByteArray(messageLength - 1) // - 1 because the messageId
        val parsedBitfield = payload.flatMap { byte -> (0 until 8).map { (byte.toInt() shr (7 - it)) and 1 } }
        bitfield = parsedBitfield.map { it and 1 == 0 }
        println(parsedBitfield)
    }

    fun downloadPiece(index: Int): ByteArray? {
        return null
    }


}