package com.example.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 去重工具类
 */
@Component
public class MyHashUtil {

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Document> deduplicate(List<Document> documents) {
        Set<String> seen = new HashSet<>();
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isEmpty()) continue;

            String hash = MyHashUtil.sha256(content);

            if (seen.add(hash)) {
                // 存一下 hash（后面有用）
                doc.getMetadata().put("hash", hash);
                result.add(doc);
            }
        }

        return result;
    }
}
