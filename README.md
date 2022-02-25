# email-service

#### 介绍

 * 邮箱服务
 * 功能1. 读取邮件 {@link #selectEmailList(Account, SearchTerm, boolean)}
 * 功能2. 读取后, 修改邮件 {@link EmailMessage#getEmail()}, 改为已读 {@link Message#setFlags(Flags, boolean)}
 * 功能3. 发送邮件 {@link #newSender(String, String, String)} {@link #sendHtmlMail(String, String, String)} {@link #sendSimpleMail(String, String, String)}
 * 功能4. 测试账号密码是否正确 {@link #testConnection(String, String, String)}
 * 功能5. 获取文件夹列表 {@link #selectFolderList(Account)}
 * 功能6. 获取附件 {@link EmailMessage#getContentList()} {@link FileContent,HtmlContent,PdfContent,ImageContent,TextContent,WordContent,UnkownContent}
 * 功能7. 处理html附件dom元素，类似JQuery {@link HtmlContent#getQuery()} 后链式操作处理
 
#### 软件架构
软件架构说明


#### 安装教程

1.  添加依赖

            <dependency>
                <groupId>com.github.wangzihaogithub</groupId>
                <artifactId>email-service</artifactId>
                <version>1.0.0</version>
            </dependency>
            
2.  注入对象
    
        @Autowired
        private EmailService emailService;

3.  开始读写修改邮件吧

            EmailService.Account account = new EmailService.Account();
            account.setHost("imap.exmail.qq.com");
            account.setUsername("账号@qq.com");
            account.setPassword("密码");
            SearchTerm query = EmailService.buildTimeRangeQuery(dateFormat.parse("2022-01-01"), null);
            boolean readOnly = true;
    
            EmailService.EmailList<EmailService.EmailMessage> list = emailService.selectEmailList(account, query, readOnly);
        

#### 使用说明

1.  /src/test/java 里面有用例

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
