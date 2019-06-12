// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.buck_project_builder;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FileSystem {

  private FileSystem() {}

  /**
   * @return a mapping from absolute source path to absolute output path as specified by the sources
   *     object. Files are not guaranteed to exist.
   */
  public static Map<String, String> resolveSourceMapping(
      String sourceDirectory, String outputDirectory, Map<String, String> sources) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : sources.entrySet()) {
      String sourceFile = entry.getKey();
      String outputFile = entry.getValue();
      result.put(
          Paths.get(sourceDirectory, sourceFile).toString(),
          Paths.get(outputDirectory, outputFile).toString());
    }
    return result;
  }

  public static void addSymbolicLink(Path linkPath, Path actualPath) {
    linkPath.getParent().toFile().mkdirs();
    try {
      Files.deleteIfExists(linkPath);
      Files.createSymbolicLink(linkPath, actualPath);
    } catch (IOException exception) {
      Logger.getGlobal().log(Level.SEVERE, exception.getMessage());
    }
  }

  public static void unzipRemoteFile(String remoteUrl, File outputDirectory) throws IOException {
    URL url = new URL(remoteUrl);
    File temporaryZipFile = Files.createTempFile("remote-", ".zip").toFile();
    try (InputStream remoteInputStream = url.openStream()) {
      try (FileOutputStream zipFileOutputStream = new FileOutputStream(temporaryZipFile)) {
        IOUtils.copy(remoteInputStream, zipFileOutputStream);
      }
    }
    try (ZipFile zipFile = new ZipFile(temporaryZipFile)) {
      Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
      while (zipEntries.hasMoreElements()) {
        ZipEntry zipEntry = zipEntries.nextElement();
        if (!zipEntry.isDirectory()) {
          File outputFile = new File(outputDirectory, File.separator + zipEntry.getName());
          outputFile.getParentFile().mkdirs();
          IOUtils.copy(zipFile.getInputStream(zipEntry), new FileOutputStream(outputFile));
        }
      }
    }
    temporaryZipFile.delete();
  }
}