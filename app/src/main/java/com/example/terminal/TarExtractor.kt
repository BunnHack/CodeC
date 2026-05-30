package com.example.terminal

import android.os.Build
import android.system.Os
import android.util.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream
import java.io.IOException

object TarExtractor {
    private const val TAG = "TarExtractor"

    fun extractTar(inputStream: InputStream, destDir: File) {
        val buffer = ByteArray(512)
        while (true) {
            val bytesRead = readFully(inputStream, buffer)
            if (bytesRead < 512) break
            
            // Check if it's the EOF blocks (two blocks of 512 zeroes)
            if (buffer.all { it == 0.toByte() }) {
                // Read next block to see if it's also zero
                val nextBuffer = ByteArray(512)
                readFully(inputStream, nextBuffer)
                break
            }

            // Extract entry details
            var name = parseString(buffer, 0, 100).trim()
            if (name.isEmpty()) continue

            // Parse USTAR prefix if present to support longer paths
            val prefix = parseString(buffer, 345, 155).trim()
            if (prefix.isNotEmpty()) {
                name = "$prefix/$name"
            }

            // Parse entry size (octal)
            val size = parseOctal(buffer, 124, 12)

            // Parse link indicator / type flag
            val typeFlag = buffer[156].toChar()

            val targetFile = File(destDir, name)

            Log.d(TAG, "Tar entry: name=$name, size=$size, type=$typeFlag")

            when (typeFlag) {
                '5' -> { // Directory
                    targetFile.mkdirs()
                }
                '2' -> { // Symbolic link
                    val linkTarget = parseString(buffer, 157, 100).trim()
                    createSymbolicLink(targetFile, linkTarget)
                }
                '0', '\u0000', '7' -> { // Normal file
                    val parentFile = targetFile.parentFile
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs()
                    }
                    
                    // Copy file data
                    FileOutputStream(targetFile).use { out ->
                        copyBytes(inputStream, out, size)
                    }

                    // Set executable permission if mode matches
                    val mode = parseOctal(buffer, 100, 8).toInt()
                    if ((mode and 0x49) != 0) { // check if executable (user or group or other exec bit)
                        targetFile.setExecutable(true, false)
                    }
                }
                else -> {
                    // Unknown or unhandled type (like hardlink 1), skip content
                    skipBytes(inputStream, size)
                }
            }

            // TAR files pad file data blocks to 512 byte boundaries
            val padding = (512 - (size % 512)) % 512
            if (padding > 0) {
                skipBytes(inputStream, padding)
            }
        }
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray): Int {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = inputStream.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return totalRead
    }

    private fun parseString(buffer: ByteArray, offset: Int, length: Int): String {
        var end = offset
        while (end < offset + length && buffer[end] != 0.toByte()) {
            end++
        }
        return String(buffer, offset, end - offset, Charsets.UTF_8)
    }

    private fun parseOctal(buffer: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        var start = offset
        // Skip leading space or zeroes
        while (start < offset + length && (buffer[start] == ' '.toByte() || buffer[start] == '0'.toByte())) {
            start++
        }
        for (i in start until offset + length) {
            val b = buffer[i]
            if (b == 0.toByte() || b == ' '.toByte()) break
            if (b in '0'.toByte()..'7'.toByte()) {
                result = (result shl 3) + (b - '0'.toByte())
            } else {
                break
            }
        }
        return result
    }

    private fun copyBytes(input: InputStream, output: OutputStream, size: Long) {
        val buffer = ByteArray(4096)
        var remaining = size
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read == -1) {
                throw IOException("Unexpected EOF while reading entry data")
            }
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun skipBytes(input: InputStream, amount: Long) {
        var remaining = amount
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                // Fallback to read if skip is not working or returns 0
                val block = minOf(4096L, remaining).toInt()
                val temp = ByteArray(block)
                val read = input.read(temp, 0, block)
                if (read == -1) {
                    throw IOException("Unexpected EOF while skipping padding")
                }
                remaining -= read
            } else {
                remaining -= skipped
            }
        }
    }

    private fun createSymbolicLink(targetFile: File, linkTarget: String) {
        try {
            // Delete existing file if any
            if (targetFile.exists() || isSymlink(targetFile)) {
                targetFile.delete()
            }
            val parent = targetFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            // On API 21+ we can use android.system.Os.symlink
            Os.symlink(linkTarget, targetFile.absolutePath)
            Log.d(TAG, "Created symlink: ${targetFile.absolutePath} -> $linkTarget")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create symlink ${targetFile.absolutePath} to $linkTarget", e)
        }
    }

    private fun isSymlink(file: File): Boolean {
        return try {
            val canon: File = if (file.parent == null) file else File(file.parentFile!!.canonicalFile, file.name)
            !canon.canonicalFile.equals(canon.absoluteFile)
        } catch (e: IOException) {
            false
        }
    }
}
