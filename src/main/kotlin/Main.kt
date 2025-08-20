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
        torrent.fetchPeers()
        println(torrent.getPeers())
        //val peeer = listOf("79.117.18.139:2201", "79.127.207.163:51413", "80.57.226.158:50413", "83.37.248.45:4577", "95.216.116.214:51065", "102.182.93.216:44313", "139.218.235.105:57890", "149.36.49.226:55202", "154.16.192.31:10399", "157.100.105.159:64483", "176.113.74.87:59000", "177.20.214.179:57475", "185.56.20.243:53298", "186.189.96.103:6881", "186.223.24.147:17359", "189.154.73.161:8999", "191.101.160.61:53259", "212.102.39.68:47100", "212.102.63.88:31444", "46.6.6.48:48404")
        val peer1 = torrent.getPeers()!![3]
        //val peer1 = peeer[2]
        val ip = peer1.substringBefore(":")
        val port = peer1.substringAfter(":").toInt()
        val peer = Peer(torrentFile, ip, port)
        peer.connect()
        println("Connected to $ip:$port")
        peer.handshake()


    }


}
