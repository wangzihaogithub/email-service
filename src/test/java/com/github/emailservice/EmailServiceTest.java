package com.github.emailservice;

import org.junit.jupiter.api.Test;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * 邮件测试
 */
class EmailServiceTest {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private EmailService emailService = new EmailService();

    @Test
    void selectEmailList() throws MessagingException, ParseException {
        // imap 是读邮件
        EmailService.Account account = new EmailService.Account();
        account.setHost("imap.exmail.qq.com");
        account.setUsername("账号@qq.com");
        account.setPassword("密码");
        SearchTerm query = EmailService.buildTimeRangeQuery(dateFormat.parse("2022-01-01"), null);
        boolean readOnly = true;

        // 这样写自动close
        try (EmailService.EmailList<EmailService.EmailMessage> list = emailService.selectEmailList(account, query, readOnly)) {
            System.out.println("list = " + list);
        }
    }

    @Test
    void selectFolderList() throws MessagingException {
        // imap 是读
        EmailService.Account account = new EmailService.Account();
        account.setHost("imap.exmail.qq.com");
        account.setUsername("账号@qq.com");
        account.setPassword("密码");

        // 这样写自动close
        try (EmailService.FolderList list = emailService.selectFolderList(account)) {
            for (Folder folder : list) {
                System.out.println("folder.getFullName() = " + folder.getFullName());
                Folder[] childList = folder.list();
            }
            System.out.println("list = " + list);
        }
    }

    @Test
    void send() throws MessagingException, IOException {
        // smtp 是写邮件
        EmailService.Sender sender = emailService.newSender(
                "smtp.exmail.qq.com",
                "账号@qq.com",
                "密码123"
        );
        EmailService.MimeMessage message = sender.newMessage();
        message.setFrom(sender.getUsername(), "测试名称");
        message.setTo("收件人@qq.com");
        message.setSubject("测试");
        message.setText(html(), true);
        message.send();
    }

    private String html() throws IOException {
        InputStream stream = getClass().getResourceAsStream("test1.html");
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }
}