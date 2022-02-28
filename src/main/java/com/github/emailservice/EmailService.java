package com.github.emailservice;

import com.github.emailservice.util.FileMediaType;
import com.github.emailservice.util.FileUtil;
import com.github.emailservice.util.HtmlQuery;
import com.github.emailservice.util.ParameterParser;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.SortTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 邮箱服务
 * 功能1. 读取邮件 {@link #selectEmailList(Account, SearchTerm, boolean)}
 * 功能2. 读取后, 修改邮件 {@link EmailMessage#getEmail()}, 改为已读 {@link Message#setFlags(Flags, boolean)}
 * 功能3. 发送邮件 {@link #newSender(String, String, String)} {@link #sendHtmlMail(String, String, String)} {@link #sendSimpleMail(String, String, String)}
 * 功能4. 测试账号密码是否正确 {@link #testConnection(String, String, String)}
 * 功能5. 获取文件夹列表 {@link #selectFolderList(Account)}
 * 功能6. 获取附件 {@link EmailMessage#getContentList()} {@link FileContent,HtmlContent,PdfContent,ImageContent,TextContent,WordContent,UnkownContent}
 * 功能7. 处理html附件dom元素，类似JQuery {@link HtmlContent#getQuery()} 后链式操作处理
 * <p>
 *
 * @author wangzihao 2021年9月6日19:56:22
 * @see #buildTimeRangeQuery(Date, Date) 根据时间范围查询，传null等于不限制结束时间，至今或所有
 * @see #buildUnreadQuery() 构建未读邮件查询条件
 * <p>
 * Flag 类型列举如下
 * Flags.Flag.ANSWERED 邮件回复标记，标识邮件是否已回复。
 * Flags.Flag.DELETED 邮件删除标记，标识邮件是否需要删除。
 * Flags.Flag.DRAFT 草稿邮件标记，标识邮件是否为草稿。
 * Flags.Flag.FLAGGED 表示邮件是否为回收站中的邮件。
 * Flags.Flag.RECENT 新邮件标记，表示邮件是否为新邮件。
 * Flags.Flag.SEEN 邮件阅读标记，标识邮件是否已被阅读。
 * Flags.Flag.USER 底层系统是否支持用户自定义标记，只读。
 */
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final ContentType TYPE_IMAGE_WILDCARD = new ContentType("image", "*", null);
    private static final ContentType TYPE_TEXT_WILDCARD = new ContentType("text", "*", null);
    private static final ContentType TYPE_TEXT_HTML = new ContentType("text", "html", null);
    private static final ContentType TYPE_APPLICATION_PDF = new ContentType("application", "pdf", null);
    private static final ContentType TYPE_APPLICATION_VND = new ContentType("application", "VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT", null);
    private static final ContentType TYPE_APPLICATION_OCTET_STREAM = new ContentType("application", "octet-stream", null);

    /**
     * 可以为null
     */
    private MailProperties mailProperties;
    /**
     * 可以为null，为null时, 不能调用下面两个方法， 因为没有指定发件人
     *
     * @see #sendSimpleMail(String, String, String)
     * @see #sendHtmlMail(String, String, String)
     */
    private JavaMailSender mailSender;

    public EmailService() {
        this(null, null);
    }

    public EmailService(MailProperties mailProperties, JavaMailSender mailSender) {
        this.mailProperties = mailProperties;
        this.mailSender = mailSender;
    }

    public static SearchTerm buildTimeRangeQuery(Date beginTime, Date endTime) {
        List<ReceivedDateTerm> list = new ArrayList<>();
        if (beginTime != null) {
            list.add(new ReceivedDateTerm(ComparisonTerm.GE, beginTime));
        }
        if (endTime != null) {
            list.add(new ReceivedDateTerm(ComparisonTerm.LE, endTime));
        }
        if (list.isEmpty()) {
            return null;
        }
        return new AndTerm(list.toArray(new ReceivedDateTerm[0]));
    }

    public static SearchTerm buildUnreadQuery() {
        return new FlagTerm(new Flags(Flags.Flag.SEEN), false);
    }

    private static Content rejectParseEmailContent(Object body, FileMediaType mediaType, ContentType contentType) {
        return new UnkownContent(body);
    }

    private static ContentType parseContentType(String contentType) throws ParseException {
        return new ContentType(contentType);
    }

    private static List<Map<String, String>> parseDisposition(String... dispositions) {
        List<Map<String, String>> list = new ArrayList<>();
        if (dispositions != null) {
            for (String disposition : dispositions) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                list.add(parser.parse(disposition, ';'));
            }
        }
        return list;
    }

    private static Content parseContent(Part part, String sectionId, String messageId) throws MessagingException, IOException {
        String transferEncoding;
        if (part instanceof MimePart) {
            transferEncoding = ((MimePart) part).getEncoding();
        } else {
            transferEncoding = null;
        }

        ContentType contentType = parseContentType(part.getContentType());
        List<Map<String, String>> dispositions = parseDisposition(part.getHeader("Content-Disposition"));
        String fileName;
        if (dispositions.size() > 0) {
            List<Map<String, String>> list = parseDisposition(part.getHeader("content-type"));
            for (Map<String, String> map : list) {
                map.forEach((key, value) -> {
                    if (key != null && value != null) {
                        contentType.setParameter(key, value);
                    }
                });
            }
            fileName = dispositions.stream().map(e -> e.get("filename")).filter(Objects::nonNull).collect(Collectors.joining(";"));
        } else {
            fileName = part.getFileName();
        }

        Object body = part.getContent();
        FileMediaType mediaType;
        if (body instanceof InputStream) {
            InputStream inputStream = (InputStream) body;
            mediaType = FileMediaType.valueOf(inputStream, false);
            body = mediaType.getInputStream();
        } else if (body instanceof String) {
            mediaType = FileMediaType.valueOf(new ByteArrayInputStream(((String) body).getBytes(StandardCharsets.UTF_8)));
        } else {
            mediaType = FileMediaType.unkown();
        }

        Content result;
        if (body instanceof Multipart) {
            // 混合类型嵌套
            List<Content> contentList = new ArrayList<>();
            Multipart multipart = (Multipart) body;
            for (int i = 0; i < multipart.getCount(); i++) {
                String nextSectionId = sectionId == null ?
                        Integer.toString(i + 1) :
                        sectionId + "." + (i + 1);
                contentList.add(parseContent(multipart.getBodyPart(i), nextSectionId, messageId));
            }
            result = new MultiPartContent(contentList);
        } else if (body instanceof javax.mail.internet.MimeMessage) {
            // 邮件转发
            javax.mail.internet.MimeMessage mimeMessage = (javax.mail.internet.MimeMessage) body;
            String nextSectionId = sectionId == null ? "1" : sectionId + ".1";
            result = parseContent(mimeMessage, nextSectionId, messageId);
            result.setFromMessage(true);
        } else if (TYPE_TEXT_HTML.match(contentType)) {
            // html
            result = new HtmlContent(body, contentType.getParameter("charset"));
        } else if (TYPE_TEXT_WILDCARD.match(contentType)) {
            // 文本
            result = new TextContent(body, contentType.getParameter("charset"));
        } else if (TYPE_IMAGE_WILDCARD.match(contentType)) {
            // 图片
            result = new ImageContent(body);
        } else if (TYPE_APPLICATION_VND.match(contentType) || mediaType.isMicrosoft()) {
            // word
            result = new WordContent(body);
        } else if (TYPE_APPLICATION_PDF.match(contentType) || mediaType.isPdf()) {
            // pdf
            result = new PdfContent(body);
        } else if (TYPE_APPLICATION_OCTET_STREAM.match(contentType)) {
            // 其他文件
            if (mediaType.isImage()) {
                result = new ImageContent(body);
            } else if (mediaType.isPdf()) {
                result = new PdfContent(body);
            } else if (mediaType.isMicrosoft()) {
                result = new WordContent(body);
            } else {
                // 无法识别
                result = rejectParseEmailContent(body, mediaType, contentType);
            }
        } else {
            // 无法识别
            result = rejectParseEmailContent(body, mediaType, contentType);
        }

        result.setSectionId(sectionId);
        result.setMessageId(messageId);
        result.setEmailPart(part);
        result.setTransferEncoding(transferEncoding);
        result.setDispositions(dispositions);
        result.setHeaders(new Headers(part.getAllHeaders()));
        result.setDescription(part.getDescription());
        result.setFileName(fileName);
        result.setMediaType(mediaType);
        result.setContentType(contentType);
        return result;
    }

    private static List<Folder> flatFolderList(Folder root) throws MessagingException {
        List<Folder> result = new LinkedList<>();
        List<Folder> visitFolderList = new LinkedList<>();
        visitFolderList.add(root);
        while (!visitFolderList.isEmpty()) {
            Folder remove = visitFolderList.remove(0);
            Folder[] nextList = remove.list();
            if (nextList != null) {
                visitFolderList.addAll(Arrays.asList(nextList));
            }
            result.add(remove);
        }
        return result;
    }

    public MailProperties getMailProperties() {
        return mailProperties;
    }

    @Autowired(required = false)
    public void setMailProperties(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private EmailList<EmailMessage> selectEmailList(Folder folder, SearchTerm query, boolean useProtocolSort) throws MessagingException {
        EmailList<EmailMessage> list = new EmailList<>(folder);
        boolean isUnsorted;
        Message[] emails;
        if (useProtocolSort && folder instanceof IMAPFolder) {
            isUnsorted = false;
            if (query != null) {
                emails = ((IMAPFolder) folder).getSortedMessages(new SortTerm[]{SortTerm.ARRIVAL}, query);
            } else {
                emails = ((IMAPFolder) folder).getSortedMessages(new SortTerm[]{SortTerm.ARRIVAL});
            }
        } else {
            isUnsorted = true;
            if (query != null) {
                emails = folder.search(query);
            } else {
                emails = folder.getMessages();
            }
        }
        for (Message email : emails) {
            list.add(new EmailMessage(email, query));
        }
        if (isUnsorted) {
            list.sort(Comparator.comparing((Function<EmailMessage, Date>) m -> {
                try {
                    return m.getReceivedDate();
                } catch (MessagingException e) {
                    // 邮箱已断开连接
                    return new Timestamp(System.currentTimeMillis());
                }
            }).reversed());
        }
        for (int i = 0; i < list.size(); i++) {
            if ((i + 1) < list.size()) {
                list.get(i).next = list.get(i + 1);
                list.get(i + 1).prev = list.get(i);
            }
        }
        return list;
    }

    /**
     * 查询邮件 - 根据条件
     *
     * @param account  邮箱账号密码
     * @param query    查询条件 new FlagTerm(new Flags(Flags.Flag.SEEN), false)
     * @param readOnly true=不改邮件状态和信息， false=可以改邮件状态
     * @return 用完记得关闭，调用close方法
     */
    public EmailList<EmailMessage> selectEmailList(Account account, SearchTerm query, boolean readOnly) throws AuthenticationFailedException, MessagingException {
        log.info("selectEmailList start account = {} query = {}, readOnly = {}", account, query, readOnly);
        long startTimestamp = System.currentTimeMillis();
        EmailList<EmailMessage> resultList = new EmailList<>();
        try {
            Session session = Session.getInstance(new Properties());
            Store store = session.getStore(account.getProtocol());
            try {
                store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
            } catch (AuthenticationFailedException e) {
                // 账号密码错误
                throw e;
            }

            boolean isSupportProtocolSort = true;
            try {
                Folder defaultFolder = store.getDefaultFolder();
                Folder[] systemFolders = defaultFolder.list();
                // 系统根文件夹
                for (Folder systemFolder : systemFolders) {
                    Set<FolderEnum> skipFolders = EnumSet.of(FolderEnum.deleted, FolderEnum.sent, FolderEnum.junk, FolderEnum.drafts);
                    if (skipFolders.stream().anyMatch(e -> e.match(systemFolder))) {
                        continue;
                    }

                    // 用户文件夹
                    List<Folder> flatFolderList = flatFolderList(systemFolder);
                    for (Folder folder : flatFolderList) {
                        if (folder.getType() == Folder.HOLDS_FOLDERS) {
                            continue;
                        }
                        folder.open(readOnly ? Folder.READ_ONLY : Folder.READ_WRITE);
                        EmailList<EmailMessage> list;
                        // 查询
                        try {
                            // 优先用原生协议 如果支持。 按收件时间排序
                            if (isSupportProtocolSort) {
                                list = selectEmailList(folder, query, true);
                            } else {
                                // 在java内存里排序
                                list = selectEmailList(folder, query, false);
                            }
                        } catch (MessagingException e) {
                            isSupportProtocolSort = false;
                            if (e.getNextException() instanceof ProtocolException) {
                                // 在java内存里排序
                                list = selectEmailList(folder, query, false);
                            } else {
                                // 查询报错
                                try {
                                    folder.close(false);
                                } catch (MessagingException ignored) {
                                }
                                throw e;
                            }
                        }
                        resultList.addList(list);
                    }
                }
            } catch (Exception e) {
                store.close();
                resultList.close();
                throw e;
            }
            resultList.forEach(e -> e.username = account.getUsername());
            return resultList;
        } finally {
            log.info("selectEmailList end cost = {}/ms, account = {} query = {}, readOnly = {}, listSize = {}",
                    System.currentTimeMillis() - startTimestamp,
                    account, query, readOnly, resultList.size());
        }
    }


    /**
     * 获取文件夹
     *
     * @param account 账号
     * @return 文件夹。 用完记得关闭，调用close方法
     * @throws AuthenticationFailedException
     * @throws MessagingException
     */
    public FolderList selectFolderList(Account account) throws AuthenticationFailedException, MessagingException {
        Session session = Session.getInstance(new Properties());
        Store store = session.getStore(account.getProtocol());
        try {
            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
        } catch (AuthenticationFailedException e) {
            // 账号密码错误
            throw e;
        }
        return new FolderList(store);
    }

    /**
     * 发送简单文本的邮件方法
     *
     * @param to
     * @param subject
     * @param content
     */
    public boolean sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(mailProperties.getUsername());
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
        return true;
    }

    /**
     * 发送HTML邮件的方法
     *
     * @param to      邮箱
     * @param subject 标题
     * @param content html内容
     */
    public boolean sendHtmlMail(String to, String subject, String content) {
        javax.mail.internet.MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setFrom(mailProperties.getUsername());
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            log.error("发送html邮件失败：{}", e.toString(), e);
            return false;
        }
    }

    public Sender newSender(String host, String username, String password) {
        String protocol = "smtps";
        String charset = "UTF-8";
        Integer port = 465;
        if (mailProperties != null) {
            protocol = mailProperties.getProtocol();
            charset = mailProperties.getDefaultEncoding().toString();
            port = mailProperties.getPort();
        }
        return newSender(protocol, charset, port, host, username, password);
    }

    public Sender newSender(String protocol, String charset, Integer port, String host, String username, String password) {
        Sender sender = new Sender();
        sender.setHost(host);
        sender.setDefaultEncoding(charset);
        if (port != null) {
            sender.setPort(port);
        }
//        sender.getJavaMailProperties().put("mail." + protocol + ".ssl.enable", true);
        sender.setProtocol(protocol);
        sender.setUsername(username);
        sender.setPassword(password);
        return sender;
    }

    public boolean testConnection(String host, String username, String password) {
        try {
            newSender(host, username, password).testConnection();
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

    public enum FolderEnum {
        /**/
        inbox("收件"),
        sent("发送"),
        junk("垃圾"),
        deleted("删除"),
        drafts("草稿"),
        other("其他") {
            @Override
            public boolean match(Folder folder) {
                if (super.match(folder)) {
                    return true;
                }
                return !deleted.match(folder)
                        && !sent.match(folder)
                        && !inbox.match(folder)
                        && !junk.match(folder)
                        && !drafts.match(folder);
            }
        };

        private final String cnName;

        FolderEnum(String cnName) {
            this.cnName = cnName;
        }

        public boolean match(Folder folder) {
            String name = folder.getName();
            if (name == null) {
                return false;
            }
            String lowerCase = name.toLowerCase();
            return lowerCase.contains(name()) || lowerCase.contains(cnName);
        }
    }


    public interface Closeable extends AutoCloseable {
        @Override
        void close();
    }

    public static class FolderList extends ArrayList<Folder> implements Closeable {
        private final Store store;

        public FolderList() {
            this.store = null;
        }

        public FolderList(Store store) throws MessagingException {
            this.store = store;
            if (store != null) {
                Folder defaultFolder = store.getDefaultFolder();
                Folder[] folders = defaultFolder.list();
                if (folders != null) {
                    addAll(Arrays.asList(folders));
                }
            }
        }

        @Override
        public void close() {
            if (store == null) {
                return;
            }
            try {
                store.close();
            } catch (MessagingException e) {
                log.warn("FolderList store close error = {}", e.toString(), e);
            }
        }
    }

    public static class MimeMessage extends MimeMessageHelper {
        private final Sender sender;

        private MimeMessage(Sender sender) throws MessagingException {
            super(sender.createMimeMessage(), true, "UTF-8");
            this.sender = sender;
        }

        public void send() throws MailException {
            sender.send(this);
        }
    }

    public static class Sender extends JavaMailSenderImpl {
        private final List<MimeMessage> messageList = new ArrayList<>();

        public MimeMessage newMessage() throws MessagingException {
            MimeMessage message = new MimeMessage(this);
            message.setFrom(getUsername());
            messageList.add(message);
            return message;
        }

        public void send(MimeMessage message) throws MailException {
            if (messageList.remove(message)) {
                send(message.getMimeMessage());
            }
        }

        public int sendAll() {
            int i = 0;
            Iterator<MimeMessage> iterator = messageList.iterator();
            while (iterator.hasNext()) {
                send(iterator.next().getMimeMessage());
                iterator.remove();
                i++;
            }
            return i;
        }
    }

    public static class EmailList<T extends EmailMessage> extends ArrayList<T> implements Closeable {
        private final Set<Folder> folderList = Collections.newSetFromMap(new IdentityHashMap<>());
        private final AtomicBoolean closeFlag = new AtomicBoolean(false);

        public EmailList() {
        }

        public EmailList(Folder folder) {
            folderList.add(folder);
        }

        public EmailList(List<EmailList<T>> list) {
            list.forEach(this::addList);
        }

        public Set<Folder> getFolderList() {
            return folderList;
        }

        public void addList(EmailList<T> emailList) {
            folderList.addAll(emailList.folderList);
            this.addAll(emailList);
        }

        @Override
        public void close() {
            if (closeFlag.compareAndSet(false, true)) {
                for (T m : this) {
                    m.close();
                }
                for (Folder folder : folderList) {
                    if (folder != null) {
                        try {
                            if (folder.isOpen()) {
                                folder.close(false);
                            }
                        } catch (MessagingException e) {
                            log.warn("EmailList folder close error = {}", e.toString(), e);
                        }
                        try {
                            folder.getStore().close();
                        } catch (MessagingException e) {
                            log.warn("EmailList store close error = {}", e.toString(), e);
                        }
                    }
                }
            }
        }
    }

    public static class Account {
        private final String protocol = "imap";
        // hao.wang@xxx.com
        private String username = "";
        // 123456
        private String password = "";
        // imap.exmail.qq.com
        private String host = "";
        private int port = -1;

        public String getProtocol() {
            return protocol;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "Account{" +
                    "username='" + username + '\'' +
                    ", passwordLength='" + Objects.toString(password, "").length() + '\'' +
                    ", host='" + host + '\'' +
                    ", protocol='" + protocol + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class EmailMessage implements Closeable {
        private final Message email;
        private final SearchTerm query;
        private final String folderName;
        private String username;
        private String messageId;
        private String subject;
        private Address sender;
        private Address[] forms;
        private Date receivedDate;
        private Headers headers;
        private Content content;
        private ContentType contentType;
        private EmailMessage next;
        private EmailMessage prev;
        private boolean parseFormFlag = false;
        private Map<String, Object> attributeMap;
        /**
         * 是否已读
         */
        private Boolean seenFlag;

        public EmailMessage(Message email, SearchTerm query) {
            this.folderName = Optional.ofNullable(email).map(Message::getFolder).map(Folder::getFullName).orElse(null);
            this.email = email;
            this.query = query;
        }

        public Message getEmail() {
            return email;
        }

        public SearchTerm getQuery() {
            return query;
        }

        public String getFolderName() {
            return folderName;
        }

        public String getUsername() {
            return username;
        }

        public EmailMessage getNext() {
            return next;
        }

        public EmailMessage getPrev() {
            return prev;
        }

        public boolean isParseFormFlag() {
            return parseFormFlag;
        }

        public Map<String, Object> getAttributeMap() {
            return attributeMap;
        }

        public Boolean getSeenFlag() {
            return seenFlag;
        }

        public boolean isSeen() throws MessagingException {
            if (seenFlag == null) {
                seenFlag = email.getFlags().contains(Flags.Flag.SEEN);
            }
            return seenFlag;
        }

        public <T> T getAttribute(String key) {
            if (attributeMap == null) {
                return null;
            }
            return (T) attributeMap.get(key);
        }

        public <T> T setAttribute(String key, T value) {
            if (attributeMap == null) {
                attributeMap = new LinkedHashMap<>();
            }
            return (T) attributeMap.put(key, value);
        }

        public <T> T removeAttribute(String key) {
            if (attributeMap == null) {
                return null;
            }
            return (T) attributeMap.remove(key);
        }

        public String getMessageId() throws MessagingException {
            if (messageId == null) {
                String[] messageIds = email.getHeader("Message-ID");
                if (messageIds != null && messageIds.length > 0) {
                    for (String id : messageIds) {
                        if (id.length() > 0) {
                            this.messageId = id;
                            break;
                        }
                    }
                } else {
                    this.messageId = "";
                }
            }
            return messageId;
        }

        public Headers getHeaders() throws MessagingException {
            if (headers == null) {
                headers = new Headers(email.getAllHeaders());
            }
            return headers;
        }

        public Address getSender() throws MessagingException {
            getForms();
            return sender;
        }

        public Address[] getForms() throws MessagingException {
            if (!parseFormFlag) {
                javax.mail.Address[] mfroms = email.getFrom();
                if (mfroms != null) {
                    this.forms = new Address[mfroms.length];
                    if (mfroms.length > 0) {
                        this.sender = new Address(mfroms[0]);
                    } else {
                        this.sender = null;
                    }
                    for (int i = 0; i < mfroms.length; i++) {
                        forms[i] = new Address(mfroms[i]);
                    }
                } else {
                    this.forms = null;
                    this.sender = null;
                }
                parseFormFlag = true;
            }
            return forms;
        }

        public Date getReceivedDate() throws MessagingException {
            if (receivedDate == null) {
                this.receivedDate = new Timestamp(email.getReceivedDate().getTime());
            }
            return receivedDate;
        }

        public Content getContent() throws IOException, MessagingException {
            if (content == null) {
                String messageId = getMessageId();
                this.content = parseContent(email, null, messageId);
            }
            return content;
        }

        public ContentType getContentType() throws MessagingException {
            if (contentType == null) {
                this.contentType = parseContentType(email.getContentType());
            }
            return contentType;
        }

        public String getSubject() throws MessagingException {
            if (subject == null) {
                this.subject = email.getSubject();
            }
            return subject;
        }

        public String getSenderEmail() throws MessagingException {
            Address sender = getSender();
            return sender != null ? sender.getEmail() : null;
        }

        public <CONTENT extends Content> CONTENT getContent(Class<CONTENT> type, int index) throws IOException, MessagingException {
            List<CONTENT> list = getContentList(type);
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }

        public List<Content> getContentList() throws IOException, MessagingException {
            return getContentList(Content.class);
        }

        public <CONTENT extends Content> List<CONTENT> getContentList(Class<CONTENT> type) throws IOException, MessagingException {
            List<CONTENT> resultList = new ArrayList<>();
            List<Content> tempList = new LinkedList<>();
            tempList.add(getContent());
            while (!tempList.isEmpty()) {
                Content remove = tempList.remove(0);
                if (type.isAssignableFrom(remove.getClass())) {
                    resultList.add((CONTENT) remove);
                }
                if (remove instanceof MultiPartContent) {
                    tempList.addAll(((MultiPartContent) remove).getContentList());
                }
            }
            return resultList;
        }

        @Override
        public String toString() {
            try {
                return folderName + ": " + getSubject() + " / " + getMessageId() + " / " + getReceivedDate();
            } catch (Exception e) {
                return e.toString();
            }
        }

        @Override
        public void close() {
            Content content = this.content;
            if (content != null) {
                content.close();
            }
        }
    }

    public static class Address {
        private final String username;
        private final String email;

        public Address(javax.mail.Address address) {
            if (address instanceof InternetAddress) {
                this.username = ((InternetAddress) address).getPersonal();
                this.email = ((InternetAddress) address).getAddress();
            } else if (address instanceof NewsAddress) {
                this.username = ((NewsAddress) address).getNewsgroup();
                this.email = null;
            } else {
                this.username = null;
                this.email = null;
            }
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String toString() {
            return email;
        }
    }

    public static class FileContent extends Content {
        private InputStream stream;

        public FileContent(Object stream) {
            this.stream = toStream(stream);
        }

        private static InputStream toStream(Object o) {
            if (o instanceof InputStream) {
                return (InputStream) o;
            } else if (o instanceof byte[]) {
                return new ByteArrayInputStream((byte[]) o);
            } else {
                throw new IllegalStateException("toStream=" + o);
            }
        }

        public InputStream getStream() {
            return stream;
        }

        public void setStream(InputStream stream) {
            this.stream = stream;
        }

        public File getCacheFile() {
            String id = getId();
            InputStream stream = this.stream;
            String ext = getExt();
            String cacheId = "EmailBody_" + id + "." + ext;
            try {
                return FileUtil.cacheTempFile(cacheId, () -> stream, false);
            } catch (IOException e) {
                return null;
            }
        }

        public String getExt() {
            return FileUtil.getFileExtension(getFileName());
        }

        public InputStream getCacheStream() {
            try {
                File file = getCacheFile();
                return new FileInputStream(file);
            } catch (IOException e) {
                return stream;
            }
        }

        public boolean isPdf() {
            return false;
        }

        public boolean isImage() {
            return false;
        }

        public boolean isWord() {
            return false;
        }

        @Override
        public void close() {
            InputStream stream = this.stream;
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
            super.close();
        }

        @Override
        public String toString() {
            return stream.toString();
        }
    }

    public static class UnkownContent extends Content {
        private Object body;

        public UnkownContent(Object body) {
            this.body = body;
        }

        public Object getBody() {
            return body;
        }
    }

    public static class TextContent extends Content {
        private String text;
        private String charset;

        public TextContent(Object text, String charset) throws IOException {
            this.text = textToString(text, charset);
            this.charset = charset;
        }

        private static String textToString(Object o, String charset) throws IOException {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof InputStream) {
                return readInput((InputStream) o, charset == null || charset.isEmpty() ? "UTF-8" : charset);
            } else {
                return o.toString();
            }
        }

        public static String readInput(InputStream in, String encode) throws IOException {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, encode))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }
                return sb.toString();
            }
        }

        public String getText() {
            return text;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public void close() {
            text = null;
            super.close();
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static class ImageContent extends FileContent {
        public ImageContent(Object image) {
            super(image);
        }

        @Override
        public boolean isImage() {
            return true;
        }
    }

    public static class HtmlContent extends TextContent {
        private HtmlQuery query;

        public HtmlContent(Object html, String charset) throws IOException {
            super(html, charset);
        }

        public HtmlQuery getQuery() {
            if (query == null) {
                query = HtmlQuery.valueOf(getText());
            }
            return query;
        }

        @Override
        public void close() {
            query = null;
            super.close();
        }
    }

    public static class WordContent extends FileContent {
        public WordContent(Object word) {
            super(word);
        }

        @Override
        public boolean isWord() {
            return true;
        }
    }

    public static class PdfContent extends FileContent {
        public PdfContent(Object pdf) {
            super(pdf);
        }

        @Override
        public boolean isPdf() {
            return true;
        }
    }

    public static class MultiPartContent extends Content {
        private List<Content> contentList;

        public MultiPartContent(List<Content> contentList) {
            this.contentList = contentList;
        }

        public List<Content> getContentList() {
            return contentList;
        }

        @Override
        public void close() {
            for (Content content : contentList) {
                content.close();
            }
            super.close();
        }

        @Override
        public String toString() {
            return "size=" + contentList.size();
        }
    }

    public static class Content implements Closeable {
        private Part emailPart;
        private Headers headers;
        private FileMediaType mediaType;
        private ContentType contentType;
        private String description;
        /**
         * 文件名称
         */
        private String fileName;
        /**
         * 附件
         */
        private List<Map<String, String>> dispositions;
        private String transferEncoding;
        private boolean fromMessage;
        /**
         * 内容id imap 协议的 sectionId字段
         * this message's IMAP sectionId (null for toplevel message,
         * non-null for a nested message)
         */
        private String sectionId;
        /**
         * 邮件ID
         */
        private String messageId;

        public Part getEmailPart() {
            return emailPart;
        }

        public void setEmailPart(Part emailPart) {
            this.emailPart = emailPart;
        }

        public Headers getHeaders() {
            return headers;
        }

        public void setHeaders(Headers headers) {
            this.headers = headers;
        }

        public FileMediaType getMediaType() {
            return mediaType;
        }

        public void setMediaType(FileMediaType mediaType) {
            this.mediaType = mediaType;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public void setContentType(ContentType contentType) {
            this.contentType = contentType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public List<Map<String, String>> getDispositions() {
            return dispositions;
        }

        public void setDispositions(List<Map<String, String>> dispositions) {
            this.dispositions = dispositions;
        }

        public String getTransferEncoding() {
            return transferEncoding;
        }

        public void setTransferEncoding(String transferEncoding) {
            this.transferEncoding = transferEncoding;
        }

        public boolean isFromMessage() {
            return fromMessage;
        }

        public void setFromMessage(boolean fromMessage) {
            this.fromMessage = fromMessage;
        }

        public String getSectionId() {
            return sectionId;
        }

        public void setSectionId(String sectionId) {
            this.sectionId = sectionId;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getId() {
            return getMessageId() + "." + getSectionId();
        }

        @Override
        public void close() {

        }
    }

    public static class Headers extends LinkedHashMap<String, List<String>> {
        public Headers(Enumeration<Header> enumeration) {
            this(enumeration, null);
        }

        public Headers(Enumeration<Header> enumeration, Headers headers) {
            while (enumeration.hasMoreElements()) {
                Header header = enumeration.nextElement();
                computeIfAbsent(header.getName(), e -> new ArrayList<>())
                        .add(header.getValue());
            }
            if (headers != null) {
                headers.forEach((k, v) -> {
                    if (containsKey(k)) {
                        return;
                    }
                    for (String s : v) {
                        computeIfAbsent(k, e -> new ArrayList<>())
                                .add(s);
                    }
                });
            }
        }
    }
}
