package me.rerere.rikkahub.data.files

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

private const val MAX_EMOJI_FONT_FILE_SIZE = 40L * 1024L * 1024L
private val SUPPORTED_EMOJI_FONT_EXTENSIONS = setOf("ttf", "otf")
private val ANDROID_COLOR_FONT_TABLES = setOf("COLR", "CBDT", "sbix")

data class ImportedEmojiFont(
    val relativePath: String,
    val displayName: String,
)

fun importEmojiFontFromUri(context: Context, uri: Uri): ImportedEmojiFont {
    val displayName = FileUtils.getFileNameFromUri(context, uri)
        ?.takeIf { it.isNotBlank() }
        ?: "emoji_font.ttf"
    val extension = displayName.substringAfterLast('.', "ttf").lowercase()
    require(extension in SUPPORTED_EMOJI_FONT_EXTENSIONS) {
        "Only .ttf and .otf Emoji font files are supported"
    }

    val fontDir = File(context.filesDir, FileFolders.FONTS).apply { mkdirs() }
    val targetFile = File(fontDir, "emoji_font.${System.currentTimeMillis()}.$extension")
    val tempFile = File(fontDir, "emoji_font_import.tmp")

    try {
        tempFile.delete()
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                copyWithSizeLimit(input, output)
            }
        } ?: error("Unable to open the selected Emoji font")

        validateEmojiFont(tempFile)
        replaceEmojiFont(fontDir, tempFile, targetFile)
    } catch (error: Throwable) {
        tempFile.delete()
        throw error
    }

    val relativePath = FileUtils.getRelativePathInFilesDir(context.filesDir, targetFile)
        ?: "${FileFolders.FONTS}/${targetFile.name}"
    return ImportedEmojiFont(
        relativePath = relativePath,
        displayName = displayName,
    )
}

fun loadEmojiFontFile(context: Context, relativePath: String): File? =
    resolveEmojiFontFile(context, relativePath)?.takeIf { it.isFile }

fun deleteEmojiFont(context: Context, relativePath: String) {
    resolveEmojiFontFile(context, relativePath)?.delete()
}

private fun copyWithSizeLimit(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        totalBytes += read
        require(totalBytes <= MAX_EMOJI_FONT_FILE_SIZE) {
            "Emoji font files cannot exceed 40 MB"
        }
        output.write(buffer, 0, read)
    }
    require(totalBytes > 0) { "The selected Emoji font is empty" }
}

private fun validateEmojiFont(file: File) {
    val tableTags = readOpenTypeTableTags(file)
    require(tableTags.any { it in ANDROID_COLOR_FONT_TABLES }) {
        "This font does not contain an Android-compatible color Emoji table"
    }

    val typeface = runCatching { Typeface.createFromFile(file) }
        .getOrElse { error ->
            throw IllegalArgumentException(error.message ?: "Invalid Emoji font file", error)
        }
    val paint = Paint().apply { this.typeface = typeface }
    require(
        paint.hasGlyph("\uD83D\uDE00") &&
            paint.hasGlyph("\uD83D\uDE02") &&
            paint.hasGlyph("\uD83D\uDC4D")
    ) {
        "This font is missing common Emoji glyphs"
    }
}

private fun readOpenTypeTableTags(file: File): Set<String> = DataInputStream(
    FileInputStream(file).buffered()
).use { input ->
    val signature = ByteArray(4)
    input.readFully(signature)
    val signatureText = String(signature, Charsets.US_ASCII)
    require(signatureText != "ttcf") { ".ttc font collections are not supported" }
    require(
        signature.contentEquals(byteArrayOf(0x00, 0x01, 0x00, 0x00)) ||
            signatureText == "OTTO" ||
            signatureText == "true" ||
            signatureText == "typ1"
    ) {
        "Invalid OpenType font signature"
    }

    val tableCount = input.readUnsignedShort()
    require(tableCount in 1..4096) { "Invalid Emoji font table directory" }
    input.skipBytes(6)

    buildSet {
        repeat(tableCount) {
            val tag = ByteArray(4)
            input.readFully(tag)
            input.skipBytes(12)
            add(String(tag, Charsets.US_ASCII))
        }
    }
}

private fun replaceEmojiFont(fontDir: File, tempFile: File, targetFile: File) {
    val existingFiles = fontDir.listFiles { file ->
        file.isFile && file.name.startsWith("emoji_font.") && file != tempFile
    }?.toList().orEmpty()
    val backups = existingFiles.map { file ->
        file to File(fontDir, "previous_${file.name}").also { it.delete() }
    }

    try {
        backups.forEach { (file, backup) ->
            check(file.renameTo(backup)) { "Unable to prepare the existing Emoji font for replacement" }
        }
        check(tempFile.renameTo(targetFile)) { "Unable to save the selected Emoji font" }
        backups.forEach { (_, backup) -> backup.delete() }
    } catch (error: Throwable) {
        backups.forEach { (file, backup) ->
            if (!file.exists() && backup.exists()) {
                backup.renameTo(file)
            }
        }
        throw error
    }
}

private fun resolveEmojiFontFile(context: Context, relativePath: String): File? {
    if (relativePath.isBlank()) return null
    val filesDir = runCatching { context.filesDir.canonicalFile }.getOrNull() ?: return null
    val file = runCatching { File(filesDir, relativePath).canonicalFile }.getOrNull() ?: return null
    val fontDirPath = File(filesDir, FileFolders.FONTS).canonicalFile.path
    return file.takeIf {
        it.path.startsWith("$fontDirPath${File.separator}") &&
            it.name.startsWith("emoji_font.")
    }
}
