package cn.cunmo.bootstrap;

import cn.cunmo.infrastructure.config.WechatProperties;
import cn.cunmo.infrastructure.config.MembershipProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 存魔后端唯一启动入口。
 */
@SpringBootApplication(scanBasePackages = "cn.cunmo")
@MapperScan("cn.cunmo.infrastructure.persistence")
@EnableConfigurationProperties({
    WechatProperties.class,
    MembershipProperties.class
})
@EnableScheduling
public class CunmoApplication {

    /**
     * 启动存魔 Spring Boot 后端。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CunmoApplication.class, args);
    }
}
