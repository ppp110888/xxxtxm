package com.xxxtxm.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ 异步判题队列配置
 *
 * <p>仅在非 dev 环境生效。dev 环境使用同步判题（不走 MQ）。</p>
 *
 * <p>架构:
 *   生产者: SubmissionController.submit() → rabbitTemplate.convertAndSend("judge.queue", submissionId)
 *   消费者: JudgeConsumer.onMessage() → judgeService.judge(submissionId) → WebSocket 推送结果
 * </p>
 */
@Configuration
@Profile("!dev & !test")
public class RabbitMQConfig {

    // ── 交换机 ──
    public static final String JUDGE_EXCHANGE = "codemate.judge.exchange";
    public static final String JUDGE_QUEUE = "codemate.judge.queue";
    public static final String JUDGE_ROUTING_KEY = "codemate.judge.submit";

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(JUDGE_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeQueue() {
        return QueueBuilder.durable(JUDGE_QUEUE)
                .withArgument("x-message-ttl", 60000)       // 消息 TTL: 60s
                .withArgument("x-max-length", 10000)         // 最大长度
                .withArgument("x-overflow", "reject-publish") // 溢出拒绝
                .build();
    }

    @Bean
    public Binding judgeBinding() {
        return BindingBuilder.bind(judgeQueue())
                .to(judgeExchange())
                .with(JUDGE_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器（替代默认的 Java 序列化）
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        // 发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && cause != null) {
                // 生产环境应接入告警
            }
        });
        return template;
    }
}
