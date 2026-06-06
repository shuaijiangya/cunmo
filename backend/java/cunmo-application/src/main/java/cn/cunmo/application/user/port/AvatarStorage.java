package cn.cunmo.application.user.port;

/**
 * 用户头像文件存储端口。
 */
public interface AvatarStorage {

    /**
     * 保存头像二进制并返回应用内相对访问路径。
     *
     * @param userId 用户主键
     * @param content 文件内容
     * @param contentType 文件媒体类型
     * @return 以斜杠开头的相对访问路径
     */
    String store(long userId, byte[] content, String contentType);
}
