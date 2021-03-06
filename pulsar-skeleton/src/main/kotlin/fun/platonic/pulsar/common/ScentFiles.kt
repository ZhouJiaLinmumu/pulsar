package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.config.CapabilityTypes.SCENT_TASK_IDENT
import `fun`.platonic.pulsar.persist.WebPage
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.SPACE
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ScentPaths {

    val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"),"scent-" + System.getenv("USER"))!!

    val rootDir = tmpDir

    val defaultTaskIdent = System.getProperty(SCENT_TASK_IDENT, DateTimeUtil.now("MMddHH"))!!

    private val rootDirStr get() = rootDir.toString()

    fun get(first: String, vararg more: String): Path {
        return Paths.get(rootDirStr, first, *more)
    }

    fun fromUri(uri: String, suffix: String = ""): String {
        val md5 = DigestUtils.md5Hex(uri)
        return if (suffix.isNotEmpty()) md5 + suffix else md5
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, rootDir.toString())
    }
}

object ScentFiles {

    fun save(page: WebPage, ident: String = ""): Path {
        val filename = page.headers.decodedDispositionFilename ?: ScentPaths.fromUri(page.baseUrl)
        var postfix = filename.substringAfter(".").toLowerCase()
        if (postfix.length > 5) {
            postfix = "other"
        }
        val path = ScentPaths.get("cache", "files", ident, postfix, filename)
        return saveTo(page, path)
    }

    fun saveTo(page: WebPage, path: Path): Path {
        if (!Files.exists(path)) {
            ScentFiles.saveTo(page.content?.array() ?: "(empty)".toByteArray(), path)
        }
        return path
    }

    fun save(doc: Document, ident: String = ""): Path {
        val path = ScentPaths.get("cache", "html", ident, ScentPaths.fromUri(doc.baseUri(), ".htm"))
        return saveTo(doc.outerHtml(), path)
    }

    fun save(content: String, ident: String, filename: String): Path {
        val path = ScentPaths.get(ident, filename)
        Files.deleteIfExists(path)
        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        return path
    }

    fun saveTo(any: Any, path: Path, deleteIfExists: Boolean = false): Path {
        return saveTo(any.toString().toByteArray(), path, deleteIfExists)
    }

    fun saveTo(content: String, path: Path, deleteIfExists: Boolean = false): Path {
        return saveTo(content.toByteArray(), path, deleteIfExists)
    }

    fun saveTo(content: ByteArray, path: Path, deleteIfExists: Boolean = false): Path {
        if (deleteIfExists) {
            Files.deleteIfExists(path)
        }

        Files.createDirectories(path.parent)
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

        return path
    }

    @JvmOverloads
    fun saveTo(content: ByteArray, relativePath: String, deleteIfExists: Boolean = false): Path {
        return saveTo(content, ScentPaths.get(relativePath), deleteIfExists)
    }

    fun appendLog(message: String, path: Path) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(
                    path.toFile(), DateTimeUtil.now() + SPACE + message, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun appendLog(message: String, file: String) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(
                    File(file), DateTimeUtil.now() + SPACE + message, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * list all files in a jar file
     * TODO : use spring.ResourceUtil instead
     */
    @Throws(IOException::class)
    fun <T> listJarDirectory(clazz: Class<T>): List<String> {
        val entries = mutableListOf<String>()

        val src = clazz.protectionDomain.codeSource
        val zip = ZipInputStream(src.location.openStream())

        var entry = zip.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zip.nextEntry
        }

        return entries
    }

    @Throws(IOException::class)
    fun <T> listJarDirectory(clazz: Class<T>, baseDirectory: String): List<String> {
        return listJarDirectory(clazz)
    }

    @Throws(IOException::class)
    fun <T> getJarEntries(clazz: Class<T>): List<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()

        val src = clazz.protectionDomain.codeSource
        val zip = ZipInputStream(src.location.openStream())

        var entry = zip.nextEntry
        while (entry != null) {
            entries.add(entry)
            entry = zip.nextEntry
        }

        return entries
    }
}
