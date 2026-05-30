package com.example.terminal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ContainerInstaller {
    private const val TAG = "ContainerInstaller"

    private val ASSETS_TO_UNPACK = listOf(
        "termux/usr/lib/libtalloc.so.2" to "usr/lib/libtalloc.so.2"
    )

    fun isInstalled(context: Context): Boolean {
        // Also check if native jniLibs exist at context.applicationInfo.nativeLibraryDir
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val libproot = File(nativeLibDir, "libproot.so")
        val libloader = File(nativeLibDir, "libproot_loader.so")
        
        if (!libproot.exists() || !libloader.exists()) {
            return false
        }

        for ((_, targetRelativePath) in ASSETS_TO_UNPACK) {
            val targetFile = File(context.filesDir, targetRelativePath)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                return false
            }
        }
        return true
    }

    fun ensureInstalled(context: Context): Boolean {
        val filesDir = context.filesDir

        // Create directory hierarchy
        val directories = listOf(
            File(filesDir, "usr/lib"),
            File(filesDir, "containers"),
            File(filesDir, "home")
        )

        for (dir in directories) {
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (created) {
                    Log.d(TAG, "Created directory: ${dir.absolutePath}")
                }
            }
        }

        // Copy asset files
        for ((assetPath, targetRelativePath) in ASSETS_TO_UNPACK) {
            val targetFile = File(filesDir, targetRelativePath)
            
            // For troubleshooting/reliability, we always unpack or only if model changes
            // To ensure any update to the binary is in place, we can always unpack.
            // Since this happens fast and files are small, always unpack or overwrite if size is incorrect.
            if (targetFile.exists() && targetFile.length() > 0L) {
                Log.d(TAG, "File already exists: $targetRelativePath")
                continue
            }

            Log.d(TAG, "Unpacking asset: $assetPath -> ${targetFile.absolutePath}")
            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                targetFile.setReadable(true, false)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to unpack asset $assetPath: ${e.message}", e)
                return false
            }
        }

        // Run background verification checks to validate proot --version and sh -c 'echo ok'
        Thread {
            try {
                val tests = ContainerRuntime.runMinimalValidation(context)
                for (t in tests) {
                    if (t.success) {
                        Log.i(TAG, "[VALIDATION SUCCESS] ${t.name}:\n${t.output}")
                    } else {
                        Log.e(TAG, "[VALIDATION FAILED] ${t.name}: output=${t.output}, error=${t.error}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed running background validation: ${e.message}")
            }
        }.start()

        return true
    }
}
