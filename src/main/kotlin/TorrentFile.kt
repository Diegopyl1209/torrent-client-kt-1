package me.diegopyl

import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest

class TorrentFile(path: String) {
    private val charset = Charset.defaultCharset()
    val announce: String
    val infoDictionary: InfoDictionary
    val infoHash: ByteArray
    val infoHashString: String


    init {
        val bencode = Bencode()
        val torrentFile = File(path)
        val torrentContent = torrentFile.readBytes()
        val torrentDic = bencode.decode(torrentContent) as BencodeValue.BDictionary

        announce = (torrentDic.value["announce"] as BencodeValue.BStringByte).value.toString(charset)
        infoDictionary = InfoDictionary((torrentDic.value["info"] as BencodeValue.BDictionary), charset)
        infoHash = sha1Hash(bencode.encode(infoDictionary.bencodeBDictionary))
        infoHashString = infoHash.joinToString("") { str -> "%02x".format(str) }
    }

    fun urlEncodeInfoHash(): String {
        // add % to every two characters
        val ihash = infoHash.joinToString("") { "%02x".format(it) }
        return ihash.chunked(2).joinToString("") { "%$it" }
    }

    private fun sha1Hash(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(data)
        return digest
    }

    /**
     * This class represent the info dictionary in a torrent file
     *
     * single-file mode properties:
     * - name (filename)
     * - length
     *
     * multi-file mode properties:
     * - name (directory)
     * - files (a list of dictionaries, one for each file)
     */
    class InfoDictionary(val bencodeBDictionary: BencodeValue.BDictionary, charset: Charset) {
        val pieceLength: Int = (bencodeBDictionary.value["piece length"] as BencodeValue.BInteger).value.toInt()
        val pieces: ByteArray = (bencodeBDictionary.value["pieces"] as BencodeValue.BStringByte).value
        val pieceHashes: List<String>
        val isSingleFile: Boolean

        val name: String
        val fileLength: Int?
        val files: List<BencodeValue>? // list of dictionaries who contains length and path for each file

        init {
            pieceHashes = pieceHashes(pieces)
            isSingleFile = bencodeBDictionary.value["files"] != null
            name = (bencodeBDictionary.value["name"] as BencodeValue.BStringByte).value.toString(charset)
            fileLength = (bencodeBDictionary.value["length"] as? BencodeValue.BInteger)?.value?.toInt()
            files = (bencodeBDictionary.value["files"] as? BencodeValue.BList)?.value
        }

        private fun pieceHashes(pieces: ByteArray): List<String> {
            // from spec: string consisting of the concatenation of all 20-byte SHA1 hash values
            return pieces.asIterable().chunked(20).map { it.joinToString("") { str -> "%02x".format(str) }  }
        }
    }
}