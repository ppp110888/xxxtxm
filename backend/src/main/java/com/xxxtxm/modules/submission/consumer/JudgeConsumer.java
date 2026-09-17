package com.xxxtxm.modules.submission.consumer;

import com.rabbitmq.client.Channel;
import com.xxxtxm.modules.submission.judge.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 判题消费者 — 从 RabbitMQ 拉取判题任务并执行
 *
 * <p>仅在非 dev 环境激活。dev 环境使用同步判题（不走 MQ）。</p>
 *
 * <p>并发控制: 通过 RabbitMQ prefetch 限制单实例同时处理的判题数，
 * 多实例部署时通过 concurrency 参数控制消费线程数。</p>
 */
@Slf4j
@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class JudgeConsumer {

    private final JudgeService judgeService;

    /**
     * 监听判题队列，执行判题任务
     *
     * <p>手动 ACK 模式：判题成功后再确认，避免判题过程中宕机丢失消息</p>
     */
    @RabbitListener(
            queues = "#{rabbitMQConfig.judgeQueue().getName()}",
            concurrency = "3-5",  // 3~5 个消费线程
            ackMode = "MANUAL"
    )
    public void onJudgeMessage(Long submissionId, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received judge task: submissionId={}", submissionId);
        try {
            judgeService.judge(submissionId);
            // 手动确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Judge failed for submission {}: {}", submissionId, e.getMessage());
            try {
                // 拒绝消息并重新入队（最多重试 3 次由消息头 x-death 控制）
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("Failed to nack message", ex);
            }
        }
    }
}
