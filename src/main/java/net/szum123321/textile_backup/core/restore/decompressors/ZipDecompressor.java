/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup.core.restore.decompressors;

import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class ZipDecompressor {
    public static void decompress(File inputFile, File target) {
        Instant start = Instant.now();

        try (FileInputStream fileInputStream = new FileInputStream(inputFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream((bufferedInputStream))) {
            ZipArchiveEntry entry;

            while ((entry = zipInputStream.getNextZipEntry()) != null) {
                if(!zipInputStream.canReadEntryData(entry)){
                    Statics.LOGGER.error("Something when wrong while trying to decompress {}", entry.getName());
                    continue;
                }

                File file = target.toPath().resolve(entry.getName()).toFile();

                if(entry.isDirectory()) {
                    try {
                        Files.createDirectories(file.toPath());
                    } catch (IOException e) {
                        Statics.LOGGER.error("An exception occurred when trying to create {}", file, e);
                    }
                    //if(!file.isDirectory() && !file.mkdirs())
                      //  Statics.LOGGER.error("Failed to create: {}", file);
                } else {
                    File parent = file.getParentFile();

                    try {
                        Files.createDirectories(parent.toPath());
                    } catch (IOException e) {
                        Statics.LOGGER.error("An exception occurred when trying to create {}", file, e);
                    }

                    try (OutputStream outputStream = Files.newOutputStream(file.toPath());
                         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                        IOUtils.copy(zipInputStream, bufferedOutputStream);
                    } catch (IOException e) {
                        Statics.LOGGER.error("An exception occurred while trying to decompress file: {}", file.getName(), e);
                    }
                }
            }
        } catch (IOException e) {
            Statics.LOGGER.error("An exception occurred! ", e);
        }

        Statics.LOGGER.info("Decompression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}
