package fr.russki.app.data.online.download

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object Checksums {
    fun sha256Hex(file: File): String =
        file.inputStream().use { sha256Hex(it) }

    fun sha256Hex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun matches(file: File, expectedSha256: String, expectedBytes: Long): Boolean {
        if (!file.exists() || file.length() != expectedBytes) return false
        return sha256Hex(file).equals(expectedSha256, ignoreCase = true)
    }
}
