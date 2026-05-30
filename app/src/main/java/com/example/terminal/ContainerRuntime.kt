package com.example.terminal

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ContainerRuntime {
    private const val TAG = "ContainerRuntime"

    data class TestResult(
        val name: String,
        val success: Boolean,
        val output: String,
        val error: String? = null
    )

    fun runMinimalValidation(context: Context): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val filesDir = context.filesDir
        val prefix = File(filesDir, "usr")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val prootFile = File(nativeLibDir, "libproot.so")

        if (!prootFile.exists()) {
            results.add(TestResult("Check proot binary", false, "", "proot executable not found at ${prootFile.absolutePath}"))
            return results
        }

        // Test 1: files/usr/bin/proot --version
        results.add(runCommand(
            name = "Test 1: proot --version",
            command = listOf(prootFile.absolutePath, "--version"),
            prefix = prefix,
            context = context
        ))

        // Test 2: files/usr/bin/proot /system/bin/sh -c 'echo ok'
        results.add(runCommand(
            name = "Test 2: proot /system/bin/sh -c 'echo ok'",
            command = listOf(prootFile.absolutePath, "/system/bin/sh", "-c", "echo ok"),
            prefix = prefix,
            context = context
        ))

        return results
    }

    private fun runCommand(
        name: String,
        command: List<String>,
        prefix: File,
        context: Context
    ): TestResult {
        Log.d(TAG, "Running validation test: $name")
        return try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(context.filesDir)
            
            val env = processBuilder.environment()
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            env["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib:${nativeLibDir}"
            env["PROOT_LOADER"] = File(nativeLibDir, "libproot_loader.so").absolutePath
            env["HOME"] = File(context.filesDir, "home").absolutePath
            env["TMPDIR"] = File(context.cacheDir, "tmp").absolutePath
            env["PATH"] = "/system/bin:/system/xbin"

            val process = processBuilder.start()
            
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stdout.append(line).append("\n")
                }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stderr.append(line).append("\n")
                }
            }

            val exitCode = process.waitFor()
            Log.d(TAG, "Test completed with exit code: $exitCode. stdout=$stdout, stderr=$stderr")
            
            if (exitCode == 0) {
                TestResult(name, true, stdout.toString().trim())
            } else {
                val errorDetails = if (stderr.isNotEmpty()) stderr.toString().trim() else "Exit code: $exitCode"
                TestResult(name, false, stdout.toString().trim(), errorDetails)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing validation command $name", e)
            TestResult(name, false, "", e.message ?: "Unknown Exception")
        }
    }
}
