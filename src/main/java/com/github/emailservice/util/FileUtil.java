package com.github.emailservice.util;/**
 * Created by ITerGet-Tech on 2019/3/22.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class FileUtil {

    private static Pattern PATTERN_ENG = Pattern.compile("^[0-9a-zA-Z]+$");

    public static String getFileName(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        boolean isUrl;
        String url0;
        try {
            isUrl = true;
            url0 = new URL(url).getPath();
        } catch (MalformedURLException e) {
            isUrl = false;
            url0 = url;
        }
        if (url0 == null || url0.isEmpty()) {
            url0 = url;
        }

        String substring = url0;
        if (isUrl) {
            int index = url0.lastIndexOf("/");
            if (index != -1) {
                substring = url0.substring(Math.min(index + 1, url0.length()));
            }
        }
        String name;
        int index1 = substring.lastIndexOf('.');
        if (index1 != -1) {
            name = substring.substring(0, index1);
        } else {
            name = substring;
        }
        if (isUrl) {
            name = filterSymbol(name);
        }
        return name.replaceFirst("[.]", "");
    }

    public static String filterSymbol(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String s = name.replaceAll("[“<'>\"| %/?#:;,”+*]", "");
        // 不能超过文件名的长度上限, 否则导致文件打不开
        return s.substring(0, Math.min(s.length(), 255));
    }

    public static String getFileExtension(String fileName) {
        return getFileExtension(fileName, "");
    }

    public static String getFileExtension(String fileName, String def) {
        if (fileName == null || fileName.isEmpty()) {
            return def;
        }
        String fileName0;
        try {
            if (fileName.contains(":")) {
                fileName0 = new URL(fileName).getPath();
            } else {
                fileName0 = fileName;
            }
        } catch (MalformedURLException e) {
            fileName0 = fileName;
        }
        if (fileName0 == null || fileName0.isEmpty()) {
            fileName0 = fileName;
        }
        int index = fileName0.lastIndexOf('.');
        if (index != -1) {
            String s = fileName0.substring(index).replaceFirst("\\.", "");
            if (PATTERN_ENG.matcher(s).matches()) {
                return s.toLowerCase(Locale.ENGLISH);
            } else {
                return null;
            }
        } else {
            return def;
        }
    }
    public static synchronized File cacheTempFile(String cacheFileName, IOSupplier<InputStream> inputStream, boolean deleteOnExit) throws IOException {
        if (cacheFileName.length() >= 255) {
            cacheFileName = cacheFileName.hashCode() + "." + FileUtil.getFileExtension(cacheFileName);
        }
        String cacheKey = FileUtil.filterSymbol(cacheFileName);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), cacheKey);
        Path path = tempFile.toPath();
        if (tempFile.exists()) {
            // 校验文件完整性
            String completeLength = getFileAttribute(path, "complete_length");
            if (completeLength != null
                    && Objects.equals(completeLength, String.valueOf(tempFile.length()))) {
                return tempFile;
            }
        }
        long size = copy(inputStream.get(), tempFile.toPath(), 1024 * 1024, StandardCopyOption.REPLACE_EXISTING);
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        setFileAttribute(path, "complete_length", Long.toString(size));
        return tempFile;
    }

    public static String getFileAttribute(Path file, String attrName) {
        try {
            Object value = Files.getAttribute(file, "user:" + attrName);
            if (value instanceof byte[]) {
                return new String((byte[]) value, Charset.forName("utf-8"));
            }
        } catch (Exception e) {

        }
        return null;
    }

    public static boolean setFileAttribute(Path file, String attrName, String attrValue) {
        if (attrValue == null) {
            return false;
        }
        try {
            Files.setAttribute(file, "user:" + attrName, ByteBuffer.wrap(attrValue.getBytes("UTF-8")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long copy(InputStream in, Path target, int bufferSize, CopyOption... options)
            throws IOException {
        // ensure not null before opening file
        Objects.requireNonNull(in);

        // check for REPLACE_EXISTING
        boolean replaceExisting = false;
        for (CopyOption opt : options) {
            if (opt == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
            } else {
                if (opt == null) {
                    throw new NullPointerException("options contains 'null'");
                } else {
                    throw new UnsupportedOperationException(opt + " not supported");
                }
            }
        }

        // attempt to delete an existing file
        SecurityException se = null;
        if (replaceExisting) {
            try {
                Files.deleteIfExists(target);
            } catch (SecurityException x) {
                se = x;
            }
        }

        // attempt to create target file. If it fails with
        // FileAlreadyExistsException then it may be because the security
        // manager prevented us from deleting the file, in which case we just
        // throw the SecurityException.
        OutputStream ostream;
        try {
            ostream = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException x) {
            if (se != null)
                throw se;
            // someone else won the race and created the file
            throw x;
        }

        // do the copy
        try (OutputStream out = ostream) {
            return copy(in, out, bufferSize);
        }
    }

    public static long copy(InputStream source, OutputStream sink, int bufferSize)
            throws IOException {
        long nread = 0L;
        byte[] buf = new byte[bufferSize];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    @FunctionalInterface
    public interface IOSupplier<T> {

        /**
         * Gets the result.
         *
         * @return the result
         * @throws IOException if producing the result throws an {@link IOException}
         */
        T get() throws IOException;
    }

}
