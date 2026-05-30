package com.example.terminal

import android.content.Context
import java.io.File

object TerminalRuntime {
    fun buildSystemShell(context: Context, useProot: Boolean = false): TerminalLaunchSpec {
        // Ensure installer runs first
        RuntimeInstaller.ensureInstalled(context)

        val prefix = File(context.filesDir, "usr")
        val home = File(context.filesDir, "home")
        val tmp = File(context.cacheDir, "tmp")
        
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val sourceBusybox = File(nativeLibDir, "libbusybox.so")

        if (useProot) {
            // Unpack PRoot assets if needed
            ContainerInstaller.ensureInstalled(context)

            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val prootFile = File(nativeLibDir, "libproot.so")
            if (prootFile.exists()) {
                val args = arrayOf("/system/bin/sh")
                return TerminalLaunchSpec(
                    executable = prootFile.absolutePath,
                    workingDirectory = home.absolutePath,
                    args = args,
                    environment = arrayOf(
                        "HOME=${home.absolutePath}",
                        "TMPDIR=${tmp.absolutePath}",
                        "TERM=xterm-256color",
                        "LD_LIBRARY_PATH=${prefix.absolutePath}/lib:${nativeLibDir}",
                        "PROOT_LOADER=${File(nativeLibDir, "libproot_loader.so").absolutePath}",
                        "PATH=/system/bin:/system/xbin"
                    )
                )
            }
        }

        val executable = if (sourceBusybox.exists()) {
            sourceBusybox.absolutePath
        } else {
            "/system/bin/sh"
        }

        val args = if (sourceBusybox.exists()) {
            arrayOf("sh")
        } else {
            arrayOf("-l")
        }

        return TerminalLaunchSpec(
            executable = executable,
            workingDirectory = home.absolutePath,
            args = args,
            environment = arrayOf(
                "HOME=${home.absolutePath}",
                "TMPDIR=${tmp.absolutePath}",
                "TERM=xterm-256color",
                "PATH=/system/bin:/system/xbin"
            )
        )
    }
}

