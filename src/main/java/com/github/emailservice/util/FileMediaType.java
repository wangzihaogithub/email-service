package com.github.emailservice.util;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * 文件媒体类型获取
 *
 * @author wangzihao
 */
public class FileMediaType {
    private static int openFileConnectTimeout = 3000;
    private static int openFileStreamReadTimeout = 5000;
    private static Function<String, FileResponse> openFileMethod = url -> {
        URLConnection connection = openConnection(url, openFileConnectTimeout, openFileStreamReadTimeout);
        int status;
        if (connection instanceof HttpURLConnection) {
            status = ((HttpURLConnection) connection).getResponseCode();
        } else {
            status = 0;
        }
        return new FileResponse(url, connection.getInputStream(), connection.getHeaderFields(), status);
    };

    public static class FileResponse {
        private final String url;
        private final InputStream body;
        private final Map<String, List<String>> headers;
        private final int status;

        public FileResponse(String url, InputStream body, Map<String, List<String>> headers, int status) {
            this.url = url;
            this.body = body;
            this.headers = headers;
            this.status = status;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public int getStatus() {
            return status;
        }

        public InputStream getBody() {
            return body;
        }
    }

    public static void setOpenFileConnectTimeout(int connectTimeout) {
        FileMediaType.openFileConnectTimeout = connectTimeout;
    }

    public static void setOpenFileStreamReadTimeout(int readTimeout) {
        FileMediaType.openFileStreamReadTimeout = readTimeout;
    }

    public static Function<String, FileResponse> getOpenFileMethod() {
        return openFileMethod;
    }

    public static void setOpenFileMethod(Function<String, FileResponse> openFileMethod) {
        FileMediaType.openFileMethod = openFileMethod;
    }

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
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
    public static final FileMediaType IMAGE_ICO;
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
    public static final FileMediaType APPLICATION_MSWORD_DOC;
    public static final FileMediaType APPLICATION_MSWORD_DOCX;
    public static final FileMediaType APPLICATION_MSWORD_PPT;
    public static final FileMediaType APPLICATION_MSWORD_PPTX;
    public static final FileMediaType APPLICATION_MSWORD_XLS;
    public static final FileMediaType APPLICATION_OFD;
    public static final Map<String, FileMediaType> FILE_EXT_TYPE_MAP = new LinkedHashMap<>(16, 0.75F, true);
    public static final Map<String, FileMediaType> FILE_TYPE_MAP = new LinkedHashMap<>(16, 0.75F, true);
    public static final Map<String, FileMediaType> FILE_CONTENT_TYPE_MAP = new LinkedHashMap<>(16, 0.75F, true);

    static {
        APPLICATION_ATOM_XML = new FileMediaType("application", "atom+xml");
        APPLICATION_MSWORD_DOC = new FileMediaType("application", "msworddoc");
        APPLICATION_MSWORD_DOCX = new FileMediaType("application", "msworddocx");
        APPLICATION_MSWORD_PPT = new FileMediaType("application", "mswordppt");
        APPLICATION_MSWORD_PPTX = new FileMediaType("application", "mswordpptx");
        APPLICATION_MSWORD_XLS = new FileMediaType("application", "mswordxls");

        APPLICATION_OFD = new FileMediaType("application", "ofd");
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
        IMAGE_ICO = new FileMediaType("image", "ico");
        MULTIPART_FORM_DATA = new FileMediaType("multipart", "form-data");
        MULTIPART_MIXED = new FileMediaType("multipart", "mixed");
        MULTIPART_RELATED = new FileMediaType("multipart", "related");
        TEXT_EVENT_STREAM = new FileMediaType("text", "event-stream");
        TEXT_HTML = new FileMediaType("text", "html");
        TEXT_MARKDOWN = new FileMediaType("text", "markdown");
        TEXT_PLAIN = new FileMediaType("text", "plain");
        TEXT_XML = new FileMediaType("text", "xml");
        UNKOWN = new FileMediaType("unkown", "*");

        FILE_TYPE_MAP.put("<html xmlns", APPLICATION_MSWORD_DOC);
        FILE_TYPE_MAP.put("<meta charset", TEXT_HTML);
        FILE_TYPE_MAP.put("from:<", APPLICATION_MSWORD_DOC); // docx
        FILE_TYPE_MAP.put("%PDF", APPLICATION_PDF); //Adobe Acrobat (pdf)
        FILE_TYPE_MAP.put("3c6c696e6b2072656c3d", TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("<!DOCTYPE HTM", TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("<HTML", TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("ffd8ff", IMAGE_JPEG); //JPEG (jpg)
        FILE_TYPE_MAP.put("89504e47", IMAGE_PNG); //PNG (png)
        FILE_TYPE_MAP.put("47494638", IMAGE_GIF); //GIF (gif)
        FILE_TYPE_MAP.put("424d", IMAGE_JPEG); //位图(bmp)
        FILE_TYPE_MAP.put("48544d4c207b0d0a0942", TEXT_PLAIN); //css
        FILE_TYPE_MAP.put("696b2e71623d696b2e71", TEXT_PLAIN); //js
        FILE_TYPE_MAP.put("46726f6d3a203d3f6762", TEXT_PLAIN); //Email [Outlook Express 6] (eml)
        FILE_TYPE_MAP.put("D0CF11E0", APPLICATION_MSWORD_DOC); //doc xls.or MS Excel 注意：word、msi 和 excel的文件头一样  WPS文字wps、表格et、演示dps都是一样的
        FILE_TYPE_MAP.put("255044462d312e", APPLICATION_PDF); //Adobe Acrobat (pdf)
        FILE_TYPE_MAP.put("3c25402070616765206c", TEXT_PLAIN);//jsp文件
        FILE_TYPE_MAP.put("4d616e69666573742d56", TEXT_PLAIN);//MF文件
        FILE_TYPE_MAP.put("3c3f786d6c2076657273", APPLICATION_XML);//xml文件
        FILE_TYPE_MAP.put("494e5345525420494e54", TEXT_PLAIN);//xml文件
        FILE_TYPE_MAP.put("7061636b616765207765", TEXT_PLAIN);//java文件
        FILE_TYPE_MAP.put("406563686f206f66660d", TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("6c6f67346a2e726f6f74", TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("cafebabe0000002e0041", TEXT_PLAIN);//bat文件
        FILE_TYPE_MAP.put("6431303a637265617465", TEXT_PLAIN);
        FILE_TYPE_MAP.put("0a202020203c6d657461", TEXT_HTML); //HTM (htm)
        FILE_TYPE_MAP.put("CFAD12FEC5FD746F", TEXT_PLAIN); //Outlook Express (dbx)
        FILE_TYPE_MAP.put("2142444E", TEXT_PLAIN); //Outlook (pst)
        FILE_TYPE_MAP.put("AC9EBD8F", TEXT_PLAIN); //Quicken (qdf)
        FILE_TYPE_MAP.put("E3828596", TEXT_PLAIN); //Windows Password (pwl)
        FILE_TYPE_MAP.put("pk\u0003\u0004\u0014\u0000\b\b\b\u0000", APPLICATION_MSWORD_DOCX); // docx
        FILE_TYPE_MAP.put("pk\u0003\u0004", APPLICATION_MSWORD_DOCX); // VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT

        FILE_EXT_TYPE_MAP.put("ppt", APPLICATION_MSWORD_PPT);
        FILE_EXT_TYPE_MAP.put("pptx", APPLICATION_MSWORD_PPTX);
        FILE_EXT_TYPE_MAP.put("xls", APPLICATION_MSWORD_XLS);
        FILE_EXT_TYPE_MAP.put("xlsx", APPLICATION_MSWORD_XLS);
        FILE_EXT_TYPE_MAP.put("java", TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("sql", TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("txt", TEXT_PLAIN);
        FILE_EXT_TYPE_MAP.put("xml", APPLICATION_XML);
        FILE_EXT_TYPE_MAP.put("json", APPLICATION_JSON);
        FILE_EXT_TYPE_MAP.put("pdf", APPLICATION_PDF);
        FILE_EXT_TYPE_MAP.put("html", TEXT_HTML);
        FILE_EXT_TYPE_MAP.put("htm", TEXT_HTML);
        FILE_EXT_TYPE_MAP.put("jpg", IMAGE_JPEG);
        FILE_EXT_TYPE_MAP.put("png", IMAGE_PNG);
        FILE_EXT_TYPE_MAP.put("gif", IMAGE_GIF);
        FILE_EXT_TYPE_MAP.put("ico", IMAGE_ICO);
        FILE_EXT_TYPE_MAP.put("jpeg", IMAGE_JPEG);
        FILE_EXT_TYPE_MAP.put("doc", APPLICATION_MSWORD_DOC);
        FILE_EXT_TYPE_MAP.put("docx", APPLICATION_MSWORD_DOCX);
        FILE_EXT_TYPE_MAP.put("exe", APPLICATION_OCTET_STREAM);
        FILE_EXT_TYPE_MAP.put("ofd", APPLICATION_OFD);
    }

    private final String type;
    private final String subtype;
    private final byte[] magicBytes;
    private final String magicString;
    private final String magicHex;
    private final Source source;
    private IOException ioException;
    private InputStream inputStream;
    private final boolean known;
    private String url;

    private FileMediaType(String type, String subtype,
                          byte[] magicBytes, String magicHex, String magicString,
                          Source source, IOException ioException, InputStream inputStream) {
        this.type = type.toLowerCase(Locale.ENGLISH);
        this.subtype = subtype.toLowerCase(Locale.ENGLISH);
        this.magicBytes = magicBytes;
        this.magicHex = magicHex;
        this.magicString = magicString;
        this.known = !"unkown".equals(this.type);
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

    public static FileMediaType valueOf(InputStream inputStream, String urlOrFileName, boolean close) throws IOException {
        FileMediaType fileMediaType = valueOf(inputStream, close);
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

    public static FileMediaType valueOf(InputStream inputStream, String urlOrFileName) throws IOException {
        FileMediaType fileMediaType = valueOfCantOpen(urlOrFileName);
        if (fileMediaType.isKnown()) {
            inputStream.close();
        } else {
            fileMediaType = valueOf(inputStream, TEXT_PLAIN);
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
            String magicString = trim(new String(magicBytes, DEFAULT_CHARSET)).toLowerCase(Locale.ENGLISH);
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
            if (mediaType == null) {
                mediaType = def == null ? UNKOWN : def;
            }
            if (close) {
                pushbackInputStream.close();
                pushbackInputStream = null;
            }
            return new FileMediaType(mediaType, magicBytes, magicHex, magicString,
                    Source.BODY, null, pushbackInputStream);
        } catch (IOException e) {
            if (close) {
                pushbackInputStream.close();
                pushbackInputStream = null;
            }
            return new FileMediaType(UNKOWN, new byte[0],
                    "", "", Source.BODY, e, pushbackInputStream);
        }
    }

    public static FileMediaType valueOfCantOpen(String urlOrNameOrExt) {
        String fileExtension = getFileExtension(urlOrNameOrExt);
        FileMediaType mediaType = FILE_EXT_TYPE_MAP.get(fileExtension);
        if (mediaType == null) {
            return unkown();
        } else {
            return new FileMediaType(mediaType, new byte[0],
                    "", "", Source.EXT, null, null);
        }
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

    private static FileMediaType valueOfContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String contentTypeLowerCase = contentType.toLowerCase(Locale.ENGLISH);
        return FILE_CONTENT_TYPE_MAP.getOrDefault(contentTypeLowerCase, UNKOWN);
    }

    public static FileMediaType valueOf(String urlStr, FileMediaType def) {
        FileMediaType fileMediaType;
        try {
            FileResponse fileResponse = openFileMethod.apply(urlStr);
            if (fileResponse == null) {
                fileMediaType = unkown();
            } else {
                fileMediaType = valueOf(fileResponse, def);
            }
        } catch (IOException e) {
            fileMediaType = unkown(e);
            fileMediaType.url = urlStr;
        }
        return fileMediaType;
    }

    public static FileMediaType valueOf(byte[] inputStream, String urlStr, FileMediaType def) {
        return valueOf(new ByteArrayInputStream(inputStream), urlStr, def);
    }

    public static FileMediaType valueOf(FileResponse fileResponse, FileMediaType def) {
        FileMediaType fileMediaType;
        try {
            fileMediaType = valueOf(fileResponse.getBody());
        } catch (IOException e) {
            fileMediaType = unkown(e);
        }
        if (!fileMediaType.isKnown()) {
            int status = fileResponse.getStatus();
            if (status == 0 || (status >= 200 && status < 300)) {
                Map<String, List<String>> headers = fileResponse.getHeaders();
                List<String> contentTypeList;
                if (headers == null) {
                    contentTypeList = null;
                } else {
                    contentTypeList = headers.get("Content-Type");
                    if (contentTypeList == null) {
                        contentTypeList = headers.get("content-type");
                    }
                }
                if (contentTypeList != null && !contentTypeList.isEmpty()) {
                    fileMediaType = valueOfContentType(contentTypeList.get(0));
                }
            }
        }
        if (!fileMediaType.isKnown()) {
            String fileExtension = getFileExtension(fileResponse.getUrl());
            FileMediaType mediaType = FILE_EXT_TYPE_MAP.get(fileExtension);
            if (mediaType != null) {
                fileMediaType = fileMediaType.fork(mediaType, Source.EXT);
            } else if (def != null) {
                fileMediaType = fileMediaType.fork(def, Source.DEFAULT);
            }
        }
        fileMediaType.url = fileResponse.getUrl();
        return fileMediaType;
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
        if (exception == null) {
            return UNKOWN;
        } else {
            return new FileMediaType(UNKOWN, new byte[0],
                    "", "", Source.UNKOWN, exception, null);
        }
    }

    public static FileMediaType unkown() {
        return unkown(null);
    }

    private static String getFileExtension(String fileName) {
        return FileUtil.invokeFileExtensionMethod(fileName);
    }

    public void release() {
        InputStream inputStream = this.inputStream;
        if (inputStream != null) {
            try {
                inputStream.close();
                this.inputStream = null;
            } catch (Exception ignored) {
            }
        }
        this.ioException = null;
    }

    public boolean isXml() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("xml");
    }

    public boolean isDoc() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("doc");
    }

    public boolean isDocx() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("docx");
    }

    public boolean isPpt() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("ppt");
    }

    public boolean isXls() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("xls");
    }

    public boolean isPptx() {
        String subtype = getSubtype();
        return subtype.toLowerCase().endsWith("pptx");
    }

    public boolean isMicrosoft() {
        String subtype = getSubtype();
        return subtype.toLowerCase().startsWith("msword");
    }

    public boolean isHtml() {
        return "html".equalsIgnoreCase(getSubtype());
    }

    public boolean isImage() {
        return "image".equalsIgnoreCase(getType());
    }

    public boolean isPdf() {
        return "pdf".equalsIgnoreCase(getSubtype());
    }

    public boolean isText() {
        return "text".equalsIgnoreCase(getType())
                && "plain".equalsIgnoreCase(getSubtype())
                ;
    }

    public boolean isOfd() {
        // 电子发票
        return "odf".equalsIgnoreCase(getSubtype());
    }

    public boolean isSafeMedia() {
        return isImage() || isXml() || isPdf() || isDocx() || isDoc() || isText() || isOfd();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileMediaType) {
            FileMediaType that = (FileMediaType) obj;
            return Objects.equals(that.type, this.type)
                    && Objects.equals(that.subtype, this.subtype);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subtype);
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
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

    @FunctionalInterface
    public interface Function<T, R> {
        R apply(T t) throws IOException;
    }

    private static class DisableValidationTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class TrustAllHostnames implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static URLConnection openConnection(String urlStr, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(urlEncoder(urlStr));
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{new DisableValidationTrustManager()}, new SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(ctx.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(new TrustAllHostnames());
            } catch (Exception e) {
                throw new IOException(e.toString(), e);
            }
        }
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");
        return connection;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private static String urlEncoder(String paramUrl) {
        if (paramUrl == null || paramUrl.isEmpty()) {
            return "";
        }
        char[] ch = paramUrl.toCharArray();
        StringBuilder result = new StringBuilder(ch.length);
        for (char temp : ch) {
            if (temp == '+') {
                result.append("%20");
            } else if (temp == '·') {
                result.append("%C2%B7");
            } else if (isChinese(temp)) {
                try {
                    String encode = URLEncoder.encode(String.valueOf(temp), "utf-8");
                    result.append(encode);
                } catch (UnsupportedEncodingException e) {
                    return paramUrl.replace("+", "%20").replace("·", "%C2%B7");
                }
            } else {
                result.append(temp);
            }
        }
        return result.toString();
    }

    private static String trim(String str) {
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

}
