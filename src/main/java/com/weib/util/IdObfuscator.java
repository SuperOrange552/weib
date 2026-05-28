package com.weib.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 防止 IDOR（不安全的直接对象引用）攻击：
 * 将自增数字 ID 混淆为不可预测的字符串，用于 URL 中。
 *
 * 算法：HMAC-SHA256(secret, id) 取前 6 字节 + id 的 8 字节 → Base64URL 编码
 * 攻击者无法通过遍历 URL 来枚举资源。
 */
@Component
public class IdObfuscator {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int TAG_LEN = 6;

    private final byte[] secretBytes;

    public IdObfuscator(@Value("${jwt.secret:weib-default-secret}") String secret) {
        // 对 secret 做一次哈希，得到固定长度的密钥材料
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec("id-obfuscator-salt".getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            this.secretBytes = mac.doFinal(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to init IdObfuscator", e);
        }
    }

    /**
     * 编码：Long → 不可预测的 URL 安全字符串
     */
    public String encode(Long id) {
        if (id == null) return null;
        try {
            byte[] idBytes = longToBytes(id);
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            byte[] tag = mac.doFinal(idBytes);

            // 合并 tag(前6字节) + id(8字节) = 14字节
            byte[] combined = new byte[TAG_LEN + 8];
            System.arraycopy(tag, 0, combined, 0, TAG_LEN);
            System.arraycopy(idBytes, 0, combined, TAG_LEN, 8);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            return String.valueOf(id);
        }
    }

    /**
     * 解码：URL 安全字符串 → Long
     * 如果签名不匹配（伪造/篡改），返回 null
     */
    public Long decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            byte[] combined = Base64.getUrlDecoder().decode(encoded);
            if (combined.length != TAG_LEN + 8) return null;

            byte[] tag = new byte[TAG_LEN];
            byte[] idBytes = new byte[8];
            System.arraycopy(combined, 0, tag, 0, TAG_LEN);
            System.arraycopy(combined, TAG_LEN, idBytes, 0, 8);

            // 验证签名
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            byte[] expectedTag = mac.doFinal(idBytes);

            // 常量时间比较前 TAG_LEN 字节
            for (int i = 0; i < TAG_LEN; i++) {
                if (tag[i] != expectedTag[i]) return null;
            }

            return bytesToLong(idBytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] longToBytes(long value) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(value);
        return buf.array();
    }

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return buf.getLong();
    }
}
