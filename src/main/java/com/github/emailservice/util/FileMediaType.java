package com.github.emailservice.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 文件媒体类型获取
 *
 * @author wangzihao
 */
public class FileMediaType {

    public static final FileMediaType APPLICATION_ATOM_XML;
    public static final FileMediaType APPLICATION_CBOR;
    public static final FileMediaType APPLICATION_FORM_URLENCODED;
    public static final FileMediaType APPLICATION_JSON;
    public static final FileMediaType APPLICATION_OCTET_STREAM;
    public static final FileMediaType APPLICATION_PDF;
    public static final FileMediaType APPLICATION_PROBLEM_JSON;
    public static final FileMediaType APPLICATION_PROBLEM_XML;
    public static final FileMediaType APPLICATION_RSS_XML;
    public static final FileMediaType APPLICATION_NDJSON;
    public static final FileMediaType APPLICATION_XHTML_XML;
    public static final FileMediaType APPLICATION_XML;
    public static final FileMediaType IMAGE_GIF;
    public static final FileMediaType IMAGE_JPEG;
    public static final FileMediaType IMAGE_PNG;
    public static final FileMediaType MULTIPART_FORM_DATA;
    public static final FileMediaType MULTIPART_MIXED;
    public static final FileMediaType MULTIPART_RELATED;
    public static final FileMediaType TEXT_EVENT_STREAM;
    public static final FileMediaType TEXT_HTML;
    public static final FileMediaType TEXT_MARKDOWN;
    public static final FileMediaType TEXT_PLAIN;
    public static final FileMediaType TEXT_XML;
    public static final FileMediaType UNKOWN;
    public static final FileMediaType APPLICATION_MSWORD;

    private static final Map<String, FileMediaType> FILE_EXT_TYPE_MAP = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<String, FileMediaType> FILE_TYPE_MAP = new LinkedHashMap<>(16, 0.75F, true);

    static {
        APPLICATION_ATOM_XML = new FileMediaType("application", "atom+xml");
        APPLICATION_CBOR = new FileMediaType("application", "cbor");
        APPLICATION_FORM_URLENCODED = new FileMediaType("application", "x-www-form-urlencoded");
        APPLICATION_JSON = new FileMediaType("application", "json");
        APPLICATION_NDJSON = new FileMediaType("application", "x-ndjson");
        APPLICATION_OCTET_STREAM = new FileMediaType("application", "octet-stream");
        APPLICATION_PDF = new FileMediaType("application", "pdf");
        APPLICATION_PROBLEM_JSON = new FileMediaType("application", "problem+json");
        APPLICATION_PROBLEM_XML = new FileMediaType("application", "problem+xml");
        APPLICATION_RSS_XML = new FileMediaType("application", "rss+xml");
        APPLICATION_XHTML_XML = new FileMediaType("application", "xhtml+xml");
        APPLICATION_XML = new FileMediaType("application", "xml");
        IMAGE_GIF = new FileMediaType("image", "gif");
        IMAGE_JPEG = new FileMediaType("image", "jpeg");
        IMAGE_PNG = new FileMediaType("image", "png");
        MULTIPART_FORM_DATA = new FileMediaType("multipart", "form-data");
        MULTIPART_MIXED = new FileMediaType("multipart", "mixed");
        MULTIPART_RELATED = new FileMediaType("multipart", "related");
        TEXT_EVENT_STREAM = new FileMediaType("text", "event-stream");
        TEXT_HTML = new FileMediaType("text", "html");
        TEXT_MARKDOWN = new FileMediaType("text", "markdown");
        TEXT_PLAIN = new FileMediaType("text", "plain");
        TEXT_XML = new FileMediaType("text", "xml");
        UNKOWN = new FileMediaType("unkown", "*");
        APPLICATION_MSWORD = new FileMediaType("application", "msword");

        FILE_TYPE_MAP.put("%PDF", FileMediaType.APPLICATION_PDF); //Adobe Acrobat (pdf)
        FILE_TYPE_MAP.put("<!DOCTYPE HTM", FileMediaType.TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("<HTML", FileMediaType.TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("ffd8ff", FileMediaType.IMAGE_JPEG); //JPEG (jpg)
        FILE_TYPE_MAP.put("89504e47", FileMediaType.IMAGE_PNG); //PNG (png)
        FILE_TYPE_MAP.put("47494638", FileMediaType.IMAGE_GIF); //GIF (gif)
        FILE_TYPE_MAP.put("424d", FileMediaType.IMAGE_JPEG); //位图(bmp)
        FILE_TYPE_MAP.put("48544d4c207b0d0a0942", FileMediaType.TEXT_PLAIN); //css
        FILE_TYPE_MAP.put("696b2e71623d696b2e71", FileMediaType.TEXT_PLAIN); //js
        FILE_TYPE_MAP.put("46726f6d3a203d3f6762", FileMediaType.TEXT_PLAIN); //Email [Outlook Express 6] (eml)
        FILE_TYPE_MAP.put("D0CF11E0", APPLICATION_MSWORD); //doc xls.or MS Excel 注意：word、msi 和 excel的文件头一样  WPS文字wps、表格et、演示dps都是一样的
        FILE_TYPE_MAP.put("255044462d312e", FileMediaType.APPLICATION_PDF); //Adobe Acrobat (pdf)
        FILE_TYPE_MAP.put("3c25402070616765206c", FileMediaType.TEXT_PLAIN);//jsp文件
        FILE_TYPE_MAP.put("4d616e69666573742d56", FileMediaType.TEXT_PLAIN);//MF文件
        FILE_TYPE_MAP.put("3c3f786d6c2076657273", FileMediaType.APPLICATION_XML);//xml文件
        FILE_TYPE_MAP.put("494e5345525420494e54", FileMediaType.TEXT_PLAIN);//xml文件
        FILE_TYPE_MAP.put("7061636b616765207765", FileMediaType.TEXT_PLAIN);//java文件
        FILE_TYPE_MAP.put("406563686f206f66660d", FileMediaType.TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("6c6f67346a2e726f6f74", FileMediaType.TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("cafebabe0000002e0041", FileMediaType.TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("6431303a637265617465", FileMediaType.TEXT_PLAIN);

        FILE_TYPE_MAP.put("CFAD12FEC5FD746F", FileMediaType.TEXT_PLAIN); //Outlook Express (dbx)
        FILE_TYPE_MAP.put("2142444E", FileMediaType.TEXT_PLAIN); //Outlook (pst)
        FILE_TYPE_MAP.put("AC9EBD8F", FileMediaType.TEXT_PLAIN); //Quicken (qdf)
        FILE_TYPE_MAP.put("E3828596", FileMediaType.TEXT_PLAIN); //Windows Password (pwl)
        FILE_TYPE_MAP.put("pk\u0003\u0004\u0014\u0000\b\b\b\u0000", APPLICATION_MSWORD); // docx
        FILE_TYPE_MAP.put("pk\u0003\u0004", APPLICATION_MSWORD); // VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT

        FILE_EXT_TYPE_MAP.put("java", FileMediaType.TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("sql", FileMediaType.TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("txt", FileMediaType.TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("xml", FileMediaType.APPLICATION_XML);
        FILE_EXT_TYPE_MAP.put("json", FileMediaType.APPLICATION_JSON);
        FILE_EXT_TYPE_MAP.put("pdf", FileMediaType.APPLICATION_PDF);
        FILE_EXT_TYPE_MAP.put("html", FileMediaType.TEXT_HTML);
        FILE_EXT_TYPE_MAP.put("htm", FileMediaType.TEXT_HTML);
        FILE_EXT_TYPE_MAP.put("jpg", FileMediaType.IMAGE_JPEG);
        FILE_EXT_TYPE_MAP.put("png", FileMediaType.IMAGE_PNG);
        FILE_EXT_TYPE_MAP.put("gif", FileMediaType.IMAGE_GIF);
        FILE_EXT_TYPE_MAP.put("ico", FileMediaType.IMAGE_JPEG);
        FILE_EXT_TYPE_MAP.put("jpeg", FileMediaType.IMAGE_JPEG);
        FILE_EXT_TYPE_MAP.put("doc", APPLICATION_MSWORD);
        FILE_EXT_TYPE_MAP.put("docx", APPLICATION_MSWORD);
        FILE_EXT_TYPE_MAP.put("exe", FileMediaType.APPLICATION_OCTET_STREAM);
    }

    private final String type;
    private final String subtype;
    private final byte[] magicBytes;
    private final String magicString;
    private final String magicHex;
    private final Source source;
    private final IOException ioException;
    private final InputStream inputStream;
    private boolean known;
    private String url;

    private FileMediaType(String type, String subtype,
                          byte[] magicBytes, String magicHex, String magicString,
                          Source source, IOException ioException, InputStream inputStream) {
        this.type = type.toLowerCase(Locale.ENGLISH);
        this.subtype = subtype.toLowerCase(Locale.ENGLISH);
        this.magicBytes = magicBytes;
        this.magicHex = magicHex;
        this.magicString = magicString;
        this.known = !UNKOWN.type.equals(this.type);
        this.source = source;
        this.ioException = ioException;
        this.inputStream = inputStream;
    }

    public FileMediaType(FileMediaType other,
                         byte[] magicBytes, String magicHex, String magicString,
                         Source source, IOException ioException, InputStream inputStream) {
        this(other.type, other.subtype, magicBytes, magicHex, magicString,
                source, ioException, inputStream);
    }

    public FileMediaType(String type, String subtype) {
        this(type, subtype, null, null, null, null, null, null);
    }

    public FileMediaType(String type) {
        this(type, "*", null, null, null, null, null, null);
    }

    public static FileMediaType valueOf(ByteArrayInputStream inputStream, String urlOrFileName) throws IOException {
        return valueOf((InputStream) inputStream, urlOrFileName);
    }

    public static FileMediaType valueOf(InputStream inputStream, String urlOrFileName) throws IOException {
        FileMediaType fileMediaType = valueOf(inputStream, TEXT_PLAIN);
        if (!fileMediaType.isKnown()) {
            String fileExtension = getFileExtension(urlOrFileName);
            FileMediaType mediaType = FILE_EXT_TYPE_MAP.get(fileExtension);
            if (mediaType != null) {
                fileMediaType = fileMediaType.fork(mediaType, Source.EXT);
            } else {
                fileMediaType = fileMediaType.fork(TEXT_PLAIN, Source.DEFAULT);
            }
        }
        return fileMediaType;
    }

    public static FileMediaType valueOf(InputStream inputStream) throws IOException {
        return valueOf(inputStream, null, 20, true);
    }

    public static FileMediaType valueOf(InputStream inputStream, FileMediaType def) throws IOException {
        return valueOf(inputStream, def, 20, true);
    }

    public static FileMediaType valueOf(InputStream inputStream, FileMediaType def, int readSize) throws IOException {
        return valueOf(inputStream, def, readSize, true);
    }

    public static FileMediaType valueOf(InputStream inputStream, boolean close) throws IOException {
        return valueOf(inputStream, null, 20, close);
    }

    public static FileMediaType valueOf(InputStream inputStream, FileMediaType def, int readSize, boolean close) throws IOException {
        FileMediaType mediaType = null;
        byte[] magicBytes = new byte[readSize];
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, magicBytes.length);
        try {
            int read = pushbackInputStream.read(magicBytes);
            String magicHex = bytesToHexString(magicBytes).toLowerCase(Locale.ENGLISH);
            String magicString = filter(new String(magicBytes, Charset.forName("UTF-8"))).toLowerCase(Locale.ENGLISH);
            if (read != -1) {
                pushbackInputStream.unread(magicBytes, 0, read);
                // 有空后期优化成前缀树 O(N) -> O(1).  TrieMap.get(magicHex);
                for (Map.Entry<String, FileMediaType> entry : FILE_TYPE_MAP.entrySet()) {
                    String key = entry.getKey().toLowerCase(Locale.ENGLISH);
                    if (magicHex.startsWith(key) || magicString.startsWith(key)) {
                        mediaType = entry.getValue();
                        break;
                    }
                }
            }
            boolean known = true;
            if (mediaType == null) {
                known = false;
                mediaType = def == null ? UNKOWN : def;
            }
            if (close) {
                pushbackInputStream.close();
                pushbackInputStream = null;
            }
            FileMediaType fileMediaType = new FileMediaType(mediaType,
                    magicBytes, magicHex, magicString, Source.BODY, null, pushbackInputStream);
            fileMediaType.known = known;
            return fileMediaType;
        } catch (IOException e) {
            if (close) {
                pushbackInputStream.close();
                pushbackInputStream = null;
            }
            return new FileMediaType(UNKOWN, new byte[0],
                    "", "", Source.BODY, e, pushbackInputStream);
        }
    }

    private static String filter(String str) {
        return str.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .trim();
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder sb = new StringBuilder();
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                sb.append(0);
            }
            sb.append(hv);
        }
        return sb.toString();
    }

    public static FileMediaType valueOf(String url) {
        FileMediaType fileMediaType;
        if (url == null || url.isEmpty()) {
            fileMediaType = unkown();
            fileMediaType.url = url;
        } else {
            fileMediaType = valueOf(url, null);
        }
        return fileMediaType;
    }

    public static FileMediaType valueOf(String urlStr, FileMediaType def) {
        FileMediaType fileMediaType;
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36");
            fileMediaType = valueOf(connection.getInputStream(), urlStr, def);
        } catch (IOException e) {
            fileMediaType = unkown(e);
            fileMediaType.url = urlStr;
        }
        return fileMediaType;
    }

    public static FileMediaType valueOf(byte[] inputStream, String urlStr, FileMediaType def) {
        return valueOf(new ByteArrayInputStream(inputStream), urlStr, def);
    }

    public static FileMediaType valueOf(InputStream inputStream, String urlStr, FileMediaType def) {
        FileMediaType fileMediaType;
        try {
            fileMediaType = valueOf(inputStream);
        } catch (IOException e) {
            fileMediaType = unkown(e);
        }
        if (!fileMediaType.isKnown()) {
            String fileExtension = getFileExtension(urlStr);
            FileMediaType mediaType = FILE_EXT_TYPE_MAP.get(fileExtension);
            if (mediaType != null) {
                fileMediaType = fileMediaType.fork(mediaType, Source.EXT);
            } else if (def != null) {
                fileMediaType = fileMediaType.fork(def, Source.DEFAULT);
            }
        }
        fileMediaType.url = urlStr;
        return fileMediaType;
    }

    public static FileMediaType unkown(IOException exception) {
        return new FileMediaType(UNKOWN, new byte[0],
                "", "", Source.UNKOWN, exception, null);
    }

    public static FileMediaType unkown() {
        return unkown(null);
    }

    public static String getFileExtension(String fileName) {
        return FileUtil.getFileExtension(fileName, null);
    }

    public static void main(String[] args) throws IOException {
        FileMediaType mediaType = valueOf(new ByteArrayInputStream(("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "\t<head>\n" +
                "\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n").getBytes()));


        InputStream htmlInputStream = new URL("http://gllue.iterget.com/opt/upload/candidate/2016-05/ce71417c-98dc-4bd1-825e-3c9471a42e34.html").openStream();
        FileMediaType htmlMediaType = valueOf(htmlInputStream);
        System.out.println("htmlMediaType = " + htmlMediaType);

        InputStream pdfInputStream = new URL("https://iterget.oss-cn-beijing.aliyuncs.com/iterget-user/quake/talent84bd7d0909d346879dc35aac9f44fb2d.pdf").openStream();
        FileMediaType pdfMediaType = valueOf(pdfInputStream);
        System.out.println("pdfMediaType = " + pdfMediaType);
    }

    public boolean isKnown() {
        return known;
    }

    public byte[] getMagicBytes() {
        return magicBytes;
    }

    public String getMagicString() {
        return magicString;
    }

    public String getMagicHex() {
        return magicHex;
    }

    public Source getSource() {
        return source;
    }

    public IOException getIoException() {
        return ioException;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public boolean isMicrosoft() {
        String type = getType() + "/" + getSubtype();
        return type.contains("msword");
    }

    public boolean isHtml() {
        String type = getType() + "/" + getSubtype();
        return type.contains("html") || type.contains("xml");
    }

    public boolean isImage() {
        String type = getType() + "/" + getSubtype();
        return type.contains("image") || type.contains("jpg") || type.contains("png");
    }

    public boolean isPdf() {
        String type = getType() + "/" + getSubtype();
        return type.contains("pdf");
    }

    public boolean isText() {
        String type = getType() + "/" + getSubtype();
        return type.contains("text");
    }

    public FileMediaType fork(FileMediaType mediaType, Source source) {
        return new FileMediaType(mediaType,
                magicBytes, magicHex, magicString, source, ioException, inputStream);
    }

    public enum Source {
        /**
         * 根据扩展名识别
         */
        EXT,
        /**
         * 根据内容识别
         */
        BODY,
        /**
         * 是默认值
         */
        DEFAULT,
        /**
         * unkown
         */
        UNKOWN
    }
}
