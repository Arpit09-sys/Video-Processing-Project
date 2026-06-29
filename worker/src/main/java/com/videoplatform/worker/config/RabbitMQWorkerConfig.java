package com.videoplatform.worker.config;

import com.videoplatform.worker.dto.TranscodeMessage;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQWorkerConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.videoplatform.api.dto.TranscodeMessage", TranscodeMessage.class);
        typeMapper.setIdClassMapping(idClassMapping);
        
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
