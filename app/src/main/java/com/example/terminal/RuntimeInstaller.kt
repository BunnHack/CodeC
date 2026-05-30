package com.example.terminal

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

object RuntimeInstaller {
    private const val TAG = "RuntimeInstaller"

    fun ensureInstalled(context: Context) {
        val prefixDir = File(context.filesDir, "usr")
        val binDir = File(prefixDir, "bin")
        val homeDir = File(context.filesDir, "home")
        val projectsDir = File(context.filesDir, "projects")
        val tmpDir = File(context.cacheDir, "tmp")

        if (!binDir.exists()) binDir.mkdirs()
        if (!homeDir.exists()) homeDir.mkdirs()
        if (!projectsDir.exists()) projectsDir.mkdirs()
        if (!tmpDir.exists()) tmpDir.mkdirs()

        // Clean up any historical/broken/32-bit binaries or symlinks inside binDir
        binDir.listFiles()?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean legacy file: ${file.name}", e)
            }
        }

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        Log.d(TAG, "nativeLibraryDir=${nativeLibDir}")
        File(nativeLibDir).listFiles()?.forEach {
            Log.d(TAG, "native lib: ${it.name}")
        }
        val sourceBusybox = File(nativeLibDir, "libbusybox.so")
        
        if (!sourceBusybox.exists()) {
            Log.e(TAG, "libbusybox.so not found in nativeLibraryDir")
            return
        }

        val profileFile = File(homeDir, ".profile")
        try {
            val profileContent = "export PS1='\\w \\$ '\n"
            profileFile.writeText(profileContent)
            Log.d(TAG, ".profile written successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create .profile: ${e.message}")
        }
    }
}
