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

        val profileFile = File(homeDir, ".profile")
        if (!profileFile.exists()) {
            try {
                profileFile.writeText("export PS1='\\w \\$ '\n")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create .profile: ${e.message}")
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

        val destBusybox = File(binDir, "busybox")
        if (!destBusybox.exists()) {
            try {
                android.system.Os.symlink(sourceBusybox.absolutePath, destBusybox.absolutePath)
                Log.d(TAG, "Busybox symlink created.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to symlink busybox: ${e.message}")
            }
        }

        val shFile = File(binDir, "sh")
        if (!shFile.exists() && destBusybox.exists()) {
            try {
                Log.d(TAG, "Installing busybox symlinks...")
                // We run the SO file directly to install symlinks
                Runtime.getRuntime().exec(arrayOf(sourceBusybox.absolutePath, "--install", "-s", "."), null, binDir).waitFor()
            } catch(e: Exception) {
                Log.e(TAG, "Failed to create symlinks: ${e.message}")
            }
        }
    }
}
