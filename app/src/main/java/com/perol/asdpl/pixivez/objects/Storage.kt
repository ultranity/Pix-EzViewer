package com.perol.asdpl.pixivez.objects

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.edit
import com.perol.asdpl.pixivez.services.PxEZApp
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * One node of the download storage, either a [java.io.File] (`file://`) or a SAF document
 * (`content://` tree document) — callers never branch on which.
 */
data class Entry(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

/**
 * Download storage root and all IO below it.
 *
 * The root is either a folder granted through the Storage Access Framework
 * (ACTION_OPEN_DOCUMENT_TREE, persisted permission) — which also reaches SD cards/USB
 * drives and keeps working under scoped storage — or the legacy `/sdcard/PxEz` path
 * that relies on WRITE_EXTERNAL_STORAGE.
 *
 * SAF is driven through [DocumentsContract] directly: a child is resolved by building its
 * document id (ExternalStorageProvider ids are paths), and a directory is listed with one
 * query carrying every column, instead of DocumentFile's one-query-per-attribute model.
 */
object Storage {
    private const val TAG = "Storage"
    const val KEY_ROOT = "storage_root"
    /** folder browsed by the image manager */
    const val KEY_BROWSE_DIR = "ImgManagerDir"
    private val GRANT_KEYS = arrayOf(KEY_ROOT, KEY_BROWSE_DIR)
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val OCTET_STREAM = "application/octet-stream"
    private val PROJECTION = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE,
        Document.COLUMN_LAST_MODIFIED,
    )
    private const val PERMISSION_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private val context: Context get() = PxEZApp.instance
    private val resolver: ContentResolver get() = context.contentResolver

    val defaultRoot: Uri
        get() = Uri.fromFile(File(Environment.getExternalStorageDirectory(), "PxEz"))

    /** `file://` dir or SAF tree document uri; set by [init]. */
    @Volatile
    lateinit var root: Uri
        private set

    val isSaf: Boolean get() = root.scheme == ContentResolver.SCHEME_CONTENT

    /** Ketch downloads land here first, then Works.commitDownload moves them below [root]. */
    val stagingRoot: File by lazy { File(context.noBackupFilesDir, "downloads") }

    fun init() {
        root = PxEZApp.instance.pre.getString(KEY_ROOT, null)?.let { treeRoot(Uri.parse(it)) }
            ?: defaultRoot
    }

    /** Takes over a folder returned by ACTION_OPEN_DOCUMENT_TREE as the new root. */
    fun setRoot(treeUri: Uri) {
        grant(KEY_ROOT, treeUri)
        root = treeRoot(treeUri)
    }

    /**
     * Persists access to a picked tree under pref [key] and releases every grant that no
     * [GRANT_KEYS] pref refers to anymore.
     */
    fun grant(key: String, treeUri: Uri) {
        require(key in GRANT_KEYS)
        resolver.takePersistableUriPermission(treeUri, PERMISSION_FLAGS)
        val pre = PxEZApp.instance.pre
        pre.edit(commit = true) { putString(key, treeUri.toString()) }
        val kept = GRANT_KEYS.mapNotNull { pre.getString(it, null) }.toSet()
        resolver.persistedUriPermissions
            .filter { it.uri.toString() !in kept }
            .forEach { resolver.releasePersistableUriPermission(it.uri, PERMISSION_FLAGS) }
    }

    /** Document uri of the tree saved under [key], if its grant is still held. */
    fun granted(key: String): Uri? {
        val uri = PxEZApp.instance.pre.getString(key, null)?.let(Uri::parse) ?: return null
        return if (resolver.persistedUriPermissions.any { it.uri == uri }) treeRoot(uri) else null
    }

    val displayPath: String get() = filePath(root) ?: root.toString()

    /** Splits user formats such as `{userid}/{title}` into path segments. */
    fun relPath(vararg parts: String): List<String> =
        parts.flatMap { it.split('/') }.filter { it.isNotEmpty() }

    fun stat(uri: Uri): Entry? = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        File(uri.path!!).takeIf { it.exists() }?.toEntry()
    } else {
        query(uri)?.firstOrNull()
    }

    fun list(dir: Uri): List<Entry> = if (dir.scheme == ContentResolver.SCHEME_FILE) {
        File(dir.path!!).listFiles()?.map { it.toEntry() } ?: emptyList()
    } else {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            dir, DocumentsContract.getDocumentId(dir)
        )
        query(children, parent = dir) ?: emptyList()
    }

    fun child(dir: Uri, name: String): Entry? {
        if (dir.scheme == ContentResolver.SCHEME_FILE) {
            return File(dir.path!!, name).takeIf { it.exists() }?.toEntry()
        }
        if (dir.authority == EXTERNAL_STORAGE_AUTHORITY) {
            val docId = DocumentsContract.getDocumentId(dir)
            val childId = if (docId.endsWith(':')) docId + name else "$docId/$name"
            return stat(DocumentsContract.buildDocumentUriUsingTree(dir, childId))
        }
        // other providers use opaque ids: fall back to listing
        val children = list(dir)
        return children.firstOrNull { it.name == name }
            ?: children.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun find(rel: List<String>, from: Uri = root): Entry? {
        var node = stat(from) ?: return null
        for (name in rel) {
            if (!node.isDirectory) return null
            node = child(node.uri, name) ?: return null
        }
        return node
    }

    fun mkdirs(rel: List<String>, from: Uri = root): Uri {
        if (from.scheme == ContentResolver.SCHEME_FILE) {
            val dir = rel.fold(File(from.path!!), ::File)
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("mkdirs failed: $dir")
            return Uri.fromFile(dir)
        }
        return rel.fold(from) { dir, name ->
            val existing = child(dir, name)
            when {
                existing?.isDirectory == true -> existing.uri
                existing != null -> throw IOException("not a directory: ${existing.uri}")
                else -> DocumentsContract.createDocument(resolver, dir, Document.MIME_TYPE_DIR, name)
                    ?: throw IOException("createDirectory failed: $name in $dir")
            }
        }
    }

    /** Creates (replacing any existing file) `rel` under [from] and lets [writer] fill it. */
    fun write(rel: List<String>, from: Uri = root, writer: (OutputStream) -> Unit): Uri {
        require(rel.isNotEmpty())
        val dir = mkdirs(rel.dropLast(1), from)
        val name = rel.last()
        val target = if (dir.scheme == ContentResolver.SCHEME_FILE) {
            Uri.fromFile(File(dir.path!!, name))
        } else {
            // "w" does not truncate on every provider, so replace instead of overwrite
            child(dir, name)?.let { delete(it.uri) }
            // full display name + matching mime keeps the extension as given
            DocumentsContract.createDocument(resolver, dir, mimeOf(name), name)
                ?: throw IOException("createFile failed: $name in $dir")
        }
        try {
            openOutput(target).use(writer)
        } catch (e: Exception) {
            delete(target)
            throw e
        }
        return target
    }

    fun copyIn(src: File, rel: List<String>, from: Uri = root): Uri =
        write(rel, from) { out -> src.inputStream().use { it.copyTo(out) } }

    fun openInput(uri: Uri): InputStream =
        resolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())

    /** Display name of any openable uri (picker results, shares). */
    fun displayName(uri: Uri): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment ?: "file"

    private fun openOutput(uri: Uri): OutputStream = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        File(uri.path!!).outputStream()
    } else {
        resolver.openOutputStream(uri, "w") ?: throw FileNotFoundException(uri.toString())
    }

    /** @return the uri of the renamed node, which changes for SAF documents */
    fun rename(uri: Uri, name: String): Uri? = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        val src = File(uri.path!!)
        val dest = File(src.parentFile, name)
        if (src.renameTo(dest)) Uri.fromFile(dest) else null
    } else try {
        DocumentsContract.renameDocument(resolver, uri, name) ?: uri
    } catch (e: Exception) {
        Log.w(TAG, "rename $uri -> $name", e)
        null
    }

    fun delete(uri: Uri): Boolean = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        File(uri.path!!).delete()
    } else try {
        DocumentsContract.deleteDocument(resolver, uri)
    } catch (e: Exception) {
        Log.w(TAG, "delete $uri", e)
        false
    }

    /** Best-effort real path, for display and for MediaScanner. */
    fun filePath(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.path
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }
            .getOrElse { DocumentsContract.getTreeDocumentId(uri) }
        val volume = docId.substringBefore(':')
        val path = docId.substringAfter(':')
        val base = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory().path
        } else {
            "/storage/$volume"
        }
        return if (path.isEmpty()) base else "$base/$path"
    }

    /** Lets the gallery index a saved file; SAF writes on Android 11+ are indexed by FUSE. */
    fun scan(uri: Uri) {
        val path = filePath(uri) ?: return
        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeOf(path)), null)
    }

    /** Hides a directory from the gallery. */
    fun ensureNoMedia(dir: Uri) {
        if (child(dir, ".nomedia") == null) write(listOf(".nomedia"), dir) {}
    }

    fun mimeOf(name: String): String =
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: OCTET_STREAM

    private fun treeRoot(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )

    private fun File.toEntry() = Entry(Uri.fromFile(this), name, isDirectory, length(), lastModified())

    /** @param parent set when [uri] is a children uri, to build child document uris */
    private fun query(uri: Uri, parent: Uri? = null): List<Entry>? = try {
        resolver.query(uri, PROJECTION, null, null, null)?.use { c ->
            buildList(c.count) {
                while (c.moveToNext()) add(c.toEntry(parent ?: uri, isChild = parent != null))
            }
        }
    } catch (e: Exception) {
        // missing documents throw FileNotFoundException/IllegalArgumentException
        null
    }

    private fun Cursor.toEntry(base: Uri, isChild: Boolean) = Entry(
        uri = if (isChild) DocumentsContract.buildDocumentUriUsingTree(base, getString(0)) else base,
        name = getString(1) ?: "",
        isDirectory = getString(2) == Document.MIME_TYPE_DIR,
        size = if (isNull(3)) 0 else getLong(3),
        lastModified = if (isNull(4)) 0 else getLong(4),
    )
}
