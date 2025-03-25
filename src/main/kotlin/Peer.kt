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
    private val BLOCK_SIZE = 16*1024L
    private val currentBlockIndexDownloaded: MutableList<Boolean> = mutableListOf()

    private var socket: Socket? = null
    private var tx: ByteWriteChannel? = null
    private var rx: ByteReadChannel? = null

    var bitfield: List<Boolean>? = null
    var chocked: Boolean = false

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

    suspend fun downloadPiece(index: Int): ByteArray {
        if (tx == null || rx == null) {
            throw IllegalStateException("Socket is closed")
        }

        // send interested
        tx!!.writeInt(1)
        tx!!.writeByte(2)

        var blocksRequested = false
        var blocksQuantity = 0
        var blocksDownloaded = 0

        var pieceDownloaded = byteArrayOf()

        while(true) {
            if (blocksQuantity in 1..blocksDownloaded) { // blocksDownloaded >= blocksQuantity
                break
            }
            val messageLength = rx!!.readByte()
            val messageId = rx!!.readByte()

            when(messageLength) {
                1.toByte() -> { // unchocke
                    chocked = false

                    if (!blocksRequested) {
                        // request blocks for this piece index
                        var offset = 0L
                        while (offset < torrentFile.infoDictionary.pieceLength) {
                            val size = if ((offset + BLOCK_SIZE) > torrentFile.infoDictionary.pieceLength) torrentFile.infoDictionary.pieceLength - offset else BLOCK_SIZE
                            tx!!.writeInt(13)
                            tx!!.writeByte(6)
                            tx!!.writeInt(index)
                            tx!!.writeInt(offset.toInt())
                            tx!!.writeInt(size.toInt())
                            offset+= size
                            blocksQuantity++
                        }
                        blocksRequested = true
                    }
                }
                7 .toByte() -> { // piece
                    val pieceIndex = rx!!.readInt()
                    val offset = rx!!.readInt()
                    println("responded offset: $offset")
                    //println("responded index: $pieceIndex")
                    val block2 = rx!!.readByteArray(messageLength - 9)
                    blocksDownloaded++
                    pieceDownloaded += block2
                }
                0.toByte() -> { // chocked
                    chocked = true
                    println("Peer Chocked; current downloaded blocks of piece $index : $blocksDownloaded, total: $blocksQuantity")
                }
                else -> {}
            }
        }
        return pieceDownloaded
    }


}