package cn.cunmo.infrastructure.storage;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.user.port.AvatarStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 本地磁盘头像存储适配器。
 *
 * <p>生产环境可将该端口替换为对象存储实现。</p>
 */
@Component
public class LocalAvatarStorage implements AvatarStorage {
    private static final Logger log =
            LoggerFactory.getLogger(LocalAvatarStorage.class);

    private final Path rootDirectory;

    /**
     * 创建本地头像存储适配器。
     *
     * @param uploadDirectory 头像文件根目录
     */
    public LocalAvatarStorage(
            @Value("${storage.avatar-directory:./data/uploads/avatars}")
            String uploadDirectory) {
        this.rootDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 将头像写入用户独立目录。
     */
    @Override
    public String store(
            long userId,
            byte[] content,
            String contentType) {
        String extension = extensionOf(contentType);
        String fileName = UUID.randomUUID() + extension;
        Path userDirectory = rootDirectory.resolve(Long.toString(userId));
        Path target = userDirectory.resolve(fileName).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new ApplicationException(
                    "AVATAR_STORAGE_FAILED",
                    "头像存储路径无效");
        }
        try {
            Files.createDirectories(userDirectory);
            Files.write(
                    target,
                    content,
                    StandardOpenOption.CREATE_NEW);
            log.debug(
                    "event=user_profile stage=avatar_file_stored userId={} fileName={}",
                    userId,
                    fileName);
            return "/uploads/avatars/" + userId + "/" + fileName;
        } catch (IOException error) {
            log.error(
                    "event=user_profile stage=avatar_file_store_failed userId={} exceptionType={}",
                    userId,
                    error.getClass().getSimpleName());
            throw new ApplicationException(
                    "AVATAR_STORAGE_FAILED",
                    "头像保存失败",
                    error);
        }
    }

    /**
     * 按允许的媒体类型返回安全文件扩展名。
     */
    private String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
