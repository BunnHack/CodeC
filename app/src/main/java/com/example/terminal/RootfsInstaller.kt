package com.example.terminal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

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

        onProgress("Preparing Alpine rootfs extraction, please wait...")
        Log.d(TAG, "Starting Rootfs installation...")

        // Determine the actual asset name packed in APK (could be .tar or .tar.gz)
        val assetsList = context.assets.list("") ?: emptyArray()
        val assetName = when {
            assetsList.contains("alpine-rootfs.tar") -> "alpine-rootfs.tar"
            assetsList.contains("alpine-rootfs.tar.gz") -> "alpine-rootfs.tar.gz"
            else -> {
                assetsList.firstOrNull { it.startsWith("alpine-rootfs") } ?: "alpine-rootfs.tar"
            }
        }
        val isGz = assetName.endsWith(".gz")

        val tempTarFile = File(context.cacheDir, assetName)
        try {
            // Copy asset to cache file
            context.assets.open(assetName).use { input ->
                FileOutputStream(tempTarFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied $assetName to cache: ${tempTarFile.length()} bytes")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy rootfs asset $assetName: ${e.message}", e)
            onProgress("Error: Failed to open $assetName from assets.")
            return false
        }

        onProgress("Unpacking rootfs files (using pure Kotlin extractor)...")
        try {
            val fis = FileInputStream(tempTarFile)
            val inputStream = if (isGz) GZIPInputStream(fis) else fis
            
            inputStream.use { stream ->
                TarExtractor.extractTar(stream, rootfsDir)
            }
            
            Log.d(TAG, "Tar extraction finished successfully!")
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
        
        // Also write initial resolv.conf
        updateResolvConf(context)
        
        return true
    }

    fun updateResolvConf(context: Context) {
        val rootfsDir = File(context.filesDir, "containers/alpine")
        val etcDir = File(rootfsDir, "etc")
        if (!etcDir.exists()) {
            etcDir.mkdirs()
        }
        val resolvConf = File(etcDir, "resolv.conf")
        
        val dnsList = mutableListOf<String>()
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val linkProperties = cm.getLinkProperties(activeNetwork)
                    if (linkProperties != null) {
                        for (dnsServer in linkProperties.dnsServers) {
                            val ip = dnsServer?.hostAddress
                            if (!ip.isNullOrBlank()) {
                                dnsList.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting DNS servers from ConnectivityManager", e)
        }

        // Fallbacks if list is empty
        if (dnsList.isEmpty()) {
            dnsList.add("1.1.1.1")
            dnsList.add("8.8.8.8")
        }

        val content = dnsList.joinToString("\n") { "nameserver $it" } + "\n"
        try {
            resolvConf.writeText(content)
            Log.d(TAG, "Successfully updated resolv.conf with dns: $dnsList")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write resolv.conf", e)
        }
    }
}
