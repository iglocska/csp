package com.intrasoft.csp.conf.server.utils;


import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileHelper {

    public static void removeFile(String filePath, String fileName) throws IOException {
        File f = new File(filePath + fileName);
        Path path = Paths.get(f.getAbsolutePath());
        Files.delete(path);
    }

    public static String getFileFromHash(String filePath, String hash) throws FileNotFoundException {
        File folder = new File(filePath);
        File[] files = folder.listFiles();
        String fileName;
        int lastPeriodPos;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                fileName = files[i].getName();
                lastPeriodPos = fileName.lastIndexOf('.');
                if (lastPeriodPos > 0 && fileName.substring(0, lastPeriodPos).equals(hash)) {
                    return fileName;
                }
            }
        }

        return null;
    }

    public static String saveUploadedFile(String fileTemp, String fileRepository, MultipartFile file, String digestAlgorithm) throws IOException, NoSuchAlgorithmException {
        /*
        Create temp directory if does not exist
         */
        createDirectory(fileTemp);

        /*
        Create data directory of does not exist
         */
        createDirectory(fileRepository);

        /*
        Save file to temp
         */
        final File f = new File(fileTemp + file.getOriginalFilename());
        final String hash; // to be initialized further down

        try (InputStream inputStream = file.getInputStream();
             DigestOutputStream outputStream = new DigestOutputStream(
                     new FileOutputStream(f.getAbsoluteFile()),
                     MessageDigest.getInstance(digestAlgorithm)) ) {
            IOUtils.copy(inputStream, outputStream );
            // copy is done, digest is ready.
            hash = Hex.encodeHexString(outputStream.getMessageDigest().digest());
        }

        move(f.getParent(), f.getName(), fileTemp, hash + "." + FilenameUtils.getExtension(f.getAbsolutePath()));

        return hash;
    }

    public static void copyFromTempToRepo(String fileTemp, String fileRepository, String hash) throws IOException {
        String fileName = FileHelper.getFileFromHash(fileTemp, hash);

        /*
        Clean up files
         */
        move(fileTemp, fileName, fileRepository, fileName);
    }

    private static void move(String fromDir, String fromName, String toDir, String toName) throws IOException {
        //overwrite existing file, if exists
        CopyOption[] options = new CopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
//                StandardCopyOption.COPY_ATTRIBUTES
        };
        Path FROM = Paths.get(fromDir, fromName);
        Path TO = Paths.get(toDir, toName);
        Files.move(FROM, TO, options);
    }

    private static void createDirectory(String directory) throws IOException {
        File tempDir = new File(directory);
        if (!tempDir.exists()) {
            try{
                tempDir.mkdirs();
            }
            catch(SecurityException se){
                throw new IOException();
            }
        }
    }
}