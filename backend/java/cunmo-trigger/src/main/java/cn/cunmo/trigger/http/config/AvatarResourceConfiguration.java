package cn.cunmo.trigger.http.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地头像静态资源映射配置。
 */
@Configuration
public class AvatarResourceConfiguration implements WebMvcConfigurer {
    private final String resourceLocation;

    /**
     * 创建头像资源映射配置。
     *
     * @param uploadDirectory 本地头像根目录
     */
    public AvatarResourceConfiguration(
            @Value("${storage.avatar-directory:./data/uploads/avatars}")
            String uploadDirectory) {
        String location = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        this.resourceLocation = location.endsWith("/")
                ? location
                : location + "/";
    }

    /**
     * 将头像访问路径映射到本地文件目录。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(resourceLocation);
    }
}
