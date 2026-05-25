package com.example.terminal

import android.content.Context
import java.io.File

object TerminalRuntime {
    fun buildSystemShell(context: Context): TerminalLaunchSpec {
        // Ensure installer runs first
        RuntimeInstaller.ensureInstalled(context)

        val prefix = File(context.filesDir, "usr")
        val home = File(context.filesDir, "home")
        val tmp = File(context.cacheDir, "tmp")
        
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val sourceBusybox = File(nativeLibDir, "libbusybox.so")
        val binPath = File(prefix, "bin").absolutePath

        val executable = if (sourceBusybox.exists()) {
            sourceBusybox.absolutePath
        } else {
            "/system/bin/sh"
        }

        val args = if (sourceBusybox.exists()) {
            arrayOf("sh", "-l")
        } else {
            arrayOf("-l")
        }

        return TerminalLaunchSpec(
            executable = executable,
            workingDirectory = home.absolutePath,
            args = args,
            environment = arrayOf(
                "PREFIX=${prefix.absolutePath}",
                "HOME=${home.absolutePath}",
                "TMPDIR=${tmp.absolutePath}",
                "PATH=$binPath:/system/bin:/system/xbin",
                "TERM=xterm-256color",
                "BUSYBOX_EXEC=${sourceBusybox.absolutePath}"
            )
        )
    }
}
