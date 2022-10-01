/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
package com.hujiang.gradle.plugin.android.aspectjx.internal.utils

import com.google.common.io.Closer
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * class description here
 *
 * @author simon
 * @version 1.0.0
 * @since 2016-10-19
 */
@Suppress("UnstableApiUsage")
class JarMerger(private val jarFile: File) {
    private val buffer = ByteArray(8192)

    private val closer: Closer by lazy {
        FileUtils.forceMkdir(jarFile.parentFile)
        Closer.create()
    }
    private val jarOutputStream: JarOutputStream by lazy {
        val fos = closer.register(FileOutputStream(jarFile))
        closer.register(JarOutputStream(fos))
    }

    private var filter: IZipEntryFilter? = null

    /**
     * Sets a list of regex to exclude from the jar.
     */
    fun setFilter(filter: IZipEntryFilter) {
        this.filter = filter
    }

    fun addFolder(folder: File) {
        addFolderWithPath(folder, "")
    }

    private fun addFolderWithPath(folder: File, path: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (file.isFile) {
                val entryPath = path + file.name
                if (filter?.checkEntry(entryPath) == true) {
                    // new entry
                    jarOutputStream.putNextEntry(JarEntry(entryPath))

                    // put the file content
                    FileInputStream(file).use { fis ->
                        var count: Int
                        while ((fis.read(buffer).also { count = it }) != -1) {
                            jarOutputStream.write(buffer, 0, count)
                        }
                    }

                    // close the entry
                    jarOutputStream.closeEntry()
                }
            } else if (file.isDirectory) {
                addFolderWithPath(file, path + file.name + "/")
            }
        }
    }

    fun addJar(file: File) {
        addJar(file, false)
    }

    fun addJar(file: File, removeEntryTimestamp: Boolean) {
        val localCloser = Closer.create()
        localCloser.use {
            val fis = localCloser.register(FileInputStream(file))
            val zis = localCloser.register(ZipInputStream(fis))

            // loop on the entries of the jar file package and put them in the final jar
            var zipEntry: ZipEntry?
            while (zis.nextEntry.also { zipEntry = it } != null) {
                val entry = zipEntry!!
                // do not take directories or anything inside a potential META-INF folder.
                if (entry.isDirectory) {
                    continue
                }

                val name = entry.name
                if (filter?.checkEntry(name) == false) {
                    continue
                }

                // Preserve the STORED method of the input entry.
                val newEntry: JarEntry = if (entry.method == JarEntry.STORED) {
                    JarEntry(entry)
                } else {
                    // Create a new entry so that the compressed len is recomputed.
                    JarEntry(name)
                }
                if (removeEntryTimestamp) {
                    newEntry.time = 0
                }

                // add the entry to the jar archive
                jarOutputStream.putNextEntry(newEntry)

                // read the content of the entry from the input stream, and write it into the archive.
                var count: Int
                while ((zis.read(buffer).also { count = it }) != -1) {
                    jarOutputStream.write(buffer, 0, count)
                }

                // close the entries for this file
                jarOutputStream.closeEntry()
                zis.closeEntry()
            }
        }
    }

    fun addEntry(path: String, bytes: ByteArray) {
        jarOutputStream.putNextEntry(JarEntry(path))
        jarOutputStream.write(bytes)
        jarOutputStream.closeEntry()
    }

    fun close() {
        closer.close()
    }

    /**
     * Classes which implement this interface provides a method to check whether a file should
     * be added to a Jar file.
     */
    interface IZipEntryFilter {

        /**
         * Checks a file for inclusion in a Jar archive.
         * @param archivePath the archive file path of the entry
         * @return <code>true</code> if the file should be included.
         */
        fun checkEntry(archivePath: String): Boolean
    }
}