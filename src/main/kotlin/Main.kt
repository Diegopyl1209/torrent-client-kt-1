package me.diegopyl

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.*

fun main() {
    val torrentFile = TorrentFile("./test2.torrent")
    println(torrentFile.announce)
    println(torrentFile.infoHashString)
    println(torrentFile.infoDictionary.pieceLength)
    println(torrentFile.infoDictionary.pieceHashes)
    println(torrentFile.infoDictionary.name)
    println(torrentFile.infoDictionary.fileLength)

    runBlocking {
        val torrent = Torrent(torrentFile)
        //torrent.fetchPeers()
        //println(torrent.getPeers())
        val peeer = listOf("79.117.18.139:2201", "79.127.207.163:51413", "80.57.226.158:50413", "83.37.248.45:4577", "95.216.116.214:51065", "102.182.93.216:44313", "139.218.235.105:57890", "149.36.49.226:55202", "154.16.192.31:10399", "157.100.105.159:64483", "176.113.74.87:59000", "177.20.214.179:57475", "185.56.20.243:53298", "186.189.96.103:6881", "186.223.24.147:17359", "189.154.73.161:8999", "191.101.160.61:53259", "212.102.39.68:47100", "212.102.63.88:31444", "46.6.6.48:48404")
        //val peer1 = torrent.getPeers()!![3]
        val peer1 = peeer[12]
        val ip = peer1.substringBefore(":")
        val port = peer1.substringAfter(":").toInt()
        val socket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(ip, port)
        val tx = socket.openWriteChannel(autoFlush = true)
        val rx = socket.openReadChannel()
        println("ip: $ip ; port: $port")

        val protocol = "BitTorrent protocol"
        val reserved = ByteArray(8) { 0 }

        with(tx) {
            writeByte(protocol.length.toByte())
            writeByteArray(protocol.toByteArray())
            writeByteArray(reserved)
            writeByteArray(torrentFile.infoHash)
            writeByteArray("W01LCOAPWKS92MCUAOKS".toByteArray())
            flush()
        }

        with(rx) {
            val responseProtocolLength = readByte()
            val responseProtocol = readByteArray(protocol.length)
            val responseReserved = readByteArray(reserved.size)
            val responseInfoHash = readByteArray(20)
            val responsePeerId = readByteArray(20)

            val responsePeerIdFormated = responseInfoHash.formatHexToString()
            println("responseInfoHash: $responsePeerIdFormated")
        }

        // Peer messages consist of a message length prefix (4 bytes), message id (1 byte) and a payload (variable size)

        // bitfield
        val messageLength = rx.readInt() // 4 bytes
        val messageId = rx.readByte()
        val payload = rx.readByteArray(messageLength - 1) // - 1 because the messageId
        val parsedBitfield = payload.flatMap { byte -> (0 until 8).map { (byte.toInt() shr (7 - it)) and 1 } }
        println(parsedBitfield)

        // send interested
        tx.writeInt(1)
        tx.writeByte(2)

        //wait for unchoke
        val messageLength1 = rx.readInt() // 4 bytes
        val messageId1 = rx.readByte()

        println(messageId1.toInt())
        val blockSize = 16 * 1024L
        val downloadFile = File("./output")
        val downloadFileStream = downloadFile.outputStream()
        parsedBitfield.forEachIndexed { index, _ ->
            var offset = 0L
            var pieceQuantity = 0
            while (offset < torrentFile.infoDictionary.pieceLength) {
                val size = if ((offset + blockSize) > torrentFile.infoDictionary.pieceLength) torrentFile.infoDictionary.pieceLength - offset else blockSize
                tx.writeInt(13)
                tx.writeByte(6)
                tx.writeInt(index)
                tx.writeInt(offset.toInt())
                tx.writeInt(size.toInt())
                offset+= size
                pieceQuantity++
            }
            //println("pieces quantiy: ${pieceQuantity}")
            for (i in 0 until pieceQuantity) {
                val messageLength2 = rx.readInt() // 4 bytes
                val messageId2 = rx.readByte()
                val index2 = rx.readInt()
                val offset2 = rx.readInt()
                println("responded offset: $offset2")
                //println("responded index: $index2")
                val block2 = rx.readByteArray(messageLength2 - 9)
                downloadFileStream.write(block2)
            } // TODO: handle chocked

        }




        /*
        while(fileIsDownloading) {
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            val peerConnections: MutableList<Peer> = mutableListOf()
            val numofpeerconnections = 4;
            val peerIndex = 0
            for(i in 1..numofpeerconnections) {
                peerConnections.add(Peer(i, peeer[peerIndex].substringBefore(":"), peeer[peerIndex].substringAfter(":").toInt()))
                coroutineScope.launch {
                    peerConnections
                }
            }

        }

         */

    }


}