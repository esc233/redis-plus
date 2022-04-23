package org.whale.cbc.redis.testBean;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * @Author yakik
 * @DATE 2018/9/16
 * @DESCRIPTION :
 */
public class RedisMessageListener implements MessageListener {
    Random random = new Random();
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            /*System.out.println("channel:" + new String(message.getChannel())
                    + ",message:" + new String(message.getBody(), "utf-8").substring(7));*/
            Long time =1L+random.nextInt(1000);
            System.out.println("begin-->timeout:"+time+" channel:" + new String(message.getChannel())
                    + ",message:" + new String(message.getBody(), "utf-8"));
            Thread.sleep(1000);
            System.out.println("end-->timeout:"+time+" channel:" + new String(message.getChannel())
                    + ",message:" + new String(message.getBody(), "utf-8"));
//            new MessageListenerAdapter();
        } catch (UnsupportedEncodingException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
