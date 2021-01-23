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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class GenericTarDecompressor {
    public static void decompress(File input, File target) {
        Instant start = Instant.now();

        try (InputStream fileInputStream = new FileInputStream(input);
             InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             InputStream compressorInputStream = getCompressorInputStream(bufferedInputStream);
             TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)) {
            TarArchiveEntry entry;

            while ((entry = archiveInputStream.getNextTarEntry()) != null) {
                if(!archiveInputStream.canReadEntryData(entry)) {
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
                } else {
                    File parent = file.getParentFile();

                    try {
                        Files.createDirectories(parent.toPath());
                    } catch (IOException e) {
                        Statics.LOGGER.error("An exception occurred when trying to create {}", file, e);
                    }
                    /*
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        Statics.LOGGER.error("Failed to create {}", parent);
                        Statics.LOGGER.error("Skipping: {}", file);
                        continue;
                    }*/

                    try (OutputStream outputStream = Files.newOutputStream(file.toPath());
                         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                        IOUtils.copy(archiveInputStream, bufferedOutputStream);
                    } catch (IOException e) {
                        Statics.LOGGER.error("An exception occurred while trying to decompress file: {}", file.getName(), e);
                    }
                }
            }
        } catch (IOException | CompressorException e) {
            Statics.LOGGER.error("An exception occurred! ", e);
        }

        Statics.LOGGER.info("Decompression took {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }

    private static InputStream getCompressorInputStream(InputStream inputStream) throws CompressorException {
        try {
            return new CompressorStreamFactory().createCompressorInputStream(inputStream);
        } catch (CompressorException e) {
            final byte[] tarHeader = new byte[512];
            int signatureLength;

            inputStream.mark(tarHeader.length);

            try {
                signatureLength = IOUtils.readFully(inputStream, tarHeader);
                inputStream.reset();
            } catch (IOException e1) {
                throw new CompressorException("IOException while reading tar signature", e1);
            }

            if(TarArchiveInputStream.matches(tarHeader, signatureLength)) return inputStream;

            throw e;
        }
    }
}