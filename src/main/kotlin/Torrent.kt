package me.diegopyl

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*

class Torrent(val torrentFile: TorrentFile) {
    private val httpClient = HttpClient()
    private val bencode = Bencode()
    private var peers: List<String>? = null

    /**
     * get the current fetched peers,
     * can be null if peers has been not fetched
     */
    fun getPeers(): List<String>? {
        return peers
    }

    suspend fun fetchPeers() {
        val uri = torrentFile.announce + "?" + buildString {
            append("info_hash=", torrentFile.urlEncodeInfoHash())
            append("&peer_id=", "IWOWLAQPZLSO2IDJ3OA9") // todo: randomize
            append("&port=", 6881.toString()) // don't matter because upload content is not supported
            append("&uploaded=", 0.toString())
            append("&downloaded=", 0.toString())
            append("&left=", 0.toString())
            append("&compact=", 1.toString())
        }
        val response = httpClient.get(uri)
        val responseDic = (bencode.decode(response.bodyAsBytes()) as BencodeValue.BDictionary).value
        val interval = responseDic["interval"]
        val peers = responseDic["peers"] as BencodeValue.BStringByte // each peer is represented using 6 bytes, 4 for host and 2 for port
        val peersDecoded = peers.value.asIterable().chunked(6).map {
            val ip = it.take(4).joinToString(".") { byte -> (byte.toInt() and 0xFF).toString() }
            val port = it.takeLast(2).let { byte -> (byte[0].toInt() and 0xFF) shl 8 or (byte[1].toInt() and 0xFF) }
            "$ip:$port"
        }
        this.peers = peersDecoded
    }
}