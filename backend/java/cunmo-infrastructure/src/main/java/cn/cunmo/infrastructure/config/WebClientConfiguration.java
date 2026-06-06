package cn.cunmo.infrastructure.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 微信 HTTP 客户端配置。
 */
@Configuration
public class WebClientConfiguration {

    /**
     * 创建具有连接和响应超时限制的微信 HTTP 客户端。
     */
    @Bean
    WebClient wechatWebClient(WechatProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(properties.connectTimeout().toMillis()))
                .responseTimeout(properties.responseTimeout());
        return WebClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
