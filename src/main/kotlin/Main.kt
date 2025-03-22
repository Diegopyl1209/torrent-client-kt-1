package me.diegopyl

import kotlinx.coroutines.runBlocking

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
    }


}