package fr.russki.app.data.online.download

import java.io.File

/**
 * Empêche la traversée de répertoires lors de l'écriture de fichiers issus du réseau.
 */
object PathSafety {

    private val SAFE_RELATIVE = Regex("^[A-Za-z0-9._\\-]+(/[A-Za-z0-9._\\-]+)*$")
    private val SAFE_ID = Regex("^[A-Za-z0-9._\\-]+$")

    fun isSafeAssetId(id: String): Boolean =
        id.isNotBlank() && SAFE_ID.matches(id)

    fun isSafeRelativePath(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/').trim()
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("..")) {
            return false
        }
        return SAFE_RELATIVE.matches(normalized)
    }

    /**
     * Résout [relativePath] sous [root]. Retourne null si le chemin sort du root
     * ou contient des segments dangereux.
     */
    fun resolveUnder(root: File, relativePath: String): File? {
        if (!isSafeRelativePath(relativePath)) return null
        val rootCanon = root.canonicalFile
        val target = File(root, relativePath.replace('\\', '/')).canonicalFile
        val prefix = rootCanon.path.trimEnd(File.separatorChar) + File.separator
        return if (target.path.startsWith(prefix) || target.path == rootCanon.path) {
            target
        } else {
            null
        }
    }
}
