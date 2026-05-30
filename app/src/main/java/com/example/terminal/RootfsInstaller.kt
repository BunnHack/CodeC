package com.example.terminal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object RootfsInstaller {
    private const val TAG = "RootfsInstaller"

    fun isInstalled(context: Context): Boolean {
        val rootfsDir = File(context.filesDir, "containers/alpine")
        val shFile = File(rootfsDir, "bin/sh")
        val etcDir = File(rootfsDir, "etc")
        val usrDir = File(rootfsDir, "usr")
        return shFile.exists() && etcDir.exists() && usrDir.exists()
    }

    fun ensureInstalled(context: Context, onProgress: (String) -> Unit = {}): Boolean {
        if (isInstalled(context)) {
            Log.d(TAG, "Rootfs already installed.")
            return true
        }

        // Diagnostic listing of assets to make sure we can find things
        try {
            context.assets.list("")?.forEach { Log.d(TAG, "Asset found in root: $it") }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing assets: ${e.message}")
        }

        val rootfsDir = File(context.filesDir, "containers/alpine")
        if (!rootfsDir.exists()) {
            rootfsDir.mkdirs()
        }

        onProgress("Extracting Alpine rootfs, please wait...")
        Log.d(TAG, "Starting Rootfs installation...")

        val tempTarFile = File(context.cacheDir, "alpine-rootfs.tar.gz")
        try {
            // Copy asset to cache file
            context.assets.open("alpine-rootfs.tar.gz").use { input ->
                FileOutputStream(tempTarFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied alpine-rootfs.tar.gz to cache: ${tempTarFile.length()} bytes")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy rootfs asset: ${e.message}", e)
            onProgress("Error: Failed to open alpine-rootfs.tar.gz from assets.")
            return false
        }

        // Use busybox tar to extract
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val busyboxFile = File(nativeLibDir, "libbusybox.so")

        if (!busyboxFile.exists()) {
            Log.e(TAG, "libbusybox.so not found at $nativeLibDir")
            onProgress("Error: libbusybox.so not found.")
            return false
        }

        onProgress("Unpacking rootfs files...")
        try {
            val processBuilder = ProcessBuilder(
                busyboxFile.absolutePath,
                "tar",
                "-zxf",
                tempTarFile.absolutePath,
                "-C",
                rootfsDir.absolutePath
            )
            processBuilder.directory(context.filesDir)
            
            // Set basic environment variables
            val env = processBuilder.environment()
            env["PATH"] = "/system/bin:/system/xbin"

            val process = processBuilder.start()

            // Read potential error streams in background to avoid pipe blocking
            val errReader = Thread {
                try {
                    process.errorStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.e(TAG, "[tar err] $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in tar error stream reader thread", e)
                }
            }
            errReader.start()

            val stdoutReader = Thread {
                try {
                    process.inputStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.d(TAG, "[tar out] $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error in tar output stream reader thread", e)
                }
            }
            stdoutReader.start()

            val exitCode = process.waitFor()
            Log.d(TAG, "Tar extraction finished with exit code $exitCode")

            if (exitCode != 0) {
                onProgress("Extraction failed with exit code $exitCode.")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during tar extraction", e)
            onProgress("Error: ${e.message}")
            return false
        } finally {
            if (tempTarFile.exists()) {
                tempTarFile.delete()
            }
        }

        // Make sure minimal directories exist
        val essentialDirs = listOf("tmp", "root", "dev", "proc", "sys")
        for (dirName in essentialDirs) {
            val dir = File(rootfsDir, dirName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        // Fix permissions inside rootfs binaries if necessary (like alpine sh)
        val binSh = File(rootfsDir, "bin/sh")
        if (binSh.exists()) {
            binSh.setExecutable(true, false)
            binSh.setReadable(true, false)
        }

        onProgress("Rootfs installed successfully!")
        Log.i(TAG, "Rootfs installed successfully!")
        return true
    }
}
