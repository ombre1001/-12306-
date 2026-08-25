package com.example.railgo.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
public class SmsConfig {

    @Bean
    public Client aliyunSmsClient(SmsProperties properties) throws Exception {
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret());
        config.endpoint = properties.getEndpoint();
        return new Client(config);
    }
}
