package org.whale.cbc.redis.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author thuglife
 * @DATE 2017/6/14
 * @DESCRIPTION :
 */
@Slf4j
public class PubSubService {
    private StringRedisTemplate redisTemplate;
    @Getter
    private RedisMessageListenerContainer redisMessageListenerContainer;

    public PubSubService(StringRedisTemplate redisTemplate,RedisMessageListenerContainer redisMessageListenerContainer){
        this.redisTemplate = redisTemplate;
        this.redisMessageListenerContainer =redisMessageListenerContainer;
    }

    /**
     * 订阅消息
     * @param listener
     * @param channels
     */
    public void subscribe(MessageListener listener,String... channels){
//        log.info("订阅channel{}",channel);
        List<Topic> topic = Arrays.stream(channels)
                .map(channel->new ChannelTopic(channel))
                .collect(Collectors.toList());
        redisMessageListenerContainer.addMessageListener(listener,topic);
    }

    /**
     * 发布消息
     * @param channel
     * @param message
     */
    public void publish(String channel,String message){
        log.info("发布message:{}至channel:{}",message,channel);
        redisTemplate.convertAndSend(channel,message);
    }
}
