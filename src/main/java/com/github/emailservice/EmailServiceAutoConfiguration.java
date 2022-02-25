package com.github.emailservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置
 *
 * @author wangzihao
 */
@Configuration(proxyBeanMethods = false)
public class EmailServiceAutoConfiguration {

    @Bean("emailService")
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService emailService() {
        return new EmailService();
    }
}
