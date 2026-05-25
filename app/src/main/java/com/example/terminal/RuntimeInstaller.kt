package com.example.terminal

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object RuntimeInstaller {
    private const val TAG = "RuntimeInstaller"

    fun ensureInstalled(context: Context) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull { 
            it == "arm64-v8a" || it == "armeabi-v7a" || it == "x86_64" || it == "x86" 
        } ?: "arm64-v8a" // fallback

        Log.d(TAG, "Installing runtime for ABI: $abi")
        
        val prefixDir = File(context.filesDir, "usr")
        val binDir = File(prefixDir, "bin")
        val homeDir = File(context.filesDir, "home")
        val projectsDir = File(context.filesDir, "projects")
        val tmpDir = File(context.cacheDir, "tmp")

        if (!binDir.exists()) binDir.mkdirs()
        if (!homeDir.exists()) homeDir.mkdirs()
        if (!projectsDir.exists()) projectsDir.mkdirs()
        if (!tmpDir.exists()) tmpDir.mkdirs()

        val profileFile = File(homeDir, ".profile")
        if (!profileFile.exists()) {
            try {
                profileFile.writeText("export PS1='\\w \\$ '\n")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create .profile: ${e.message}")
            }
        }

        val busyboxFile = File(binDir, "busybox")
        if (!busyboxFile.exists()) {
            try {
                val assetPath = "runtime/$abi/busybox"
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(busyboxFile).use { output ->
                        input.copyTo(output)
                    }
                }
                busyboxFile.setExecutable(true, false)
                Log.d(TAG, "Busybox installed successfully.")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to install busybox: \${e.message}")
            }
        }

        // Create symlinks for all supported commands if sh doesn't exist
        val shFile = File(binDir, "sh")
        if (!shFile.exists() && busyboxFile.exists()) {
            try {
                Log.d(TAG, "Installing busybox symlinks...")
                Runtime.getRuntime().exec(arrayOf(busyboxFile.absolutePath, "--install", "-s", "."), null, binDir).waitFor()
            } catch(e: Exception) {
                Log.e(TAG, "Failed to create symlinks: ${e.message}")
            }
        }
    }
}
