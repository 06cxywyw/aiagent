package com.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文分词器（简化版）
 * 生产环境建议使用 IK Analyzer 或 jieba
 */
@Slf4j
@Component
public class ChineseTokenizer {

    /**
     * 简单中文分词（基于词典）
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // 1. 提取中文词组（2-4个字）
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        // 2. 提取单个汉字
        pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        // 3. 提取英文单词
        pattern = Pattern.compile("[a-zA-Z]+");
        matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase());
        }

        // 4. 提取数字
        pattern = Pattern.compile("\\d+");
        matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }

    /**
     * 提取关键词（基于词频）
     */
    public List<String> extractKeywords(String text, int topN) {
        List<String> tokens = tokenize(text);

        // 统计词频
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }

        // 过滤停用词
        Set<String> stopWords = getStopWords();
        freq.keySet().removeIf(stopWords::contains);

        // 按词频排序
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 停用词表
     */
    private Set<String> getStopWords() {
        return new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
        ));
    }

    /**
     * 同义词扩展
     */
    public List<String> expandSynonyms(String word) {
        Map<String, List<String>> synonyms = getSynonymDict();
        return synonyms.getOrDefault(word, Collections.singletonList(word));
    }

    /**
     * 同义词词典（示例）
     */
    private Map<String, List<String>> getSynonymDict() {
        Map<String, List<String>> dict = new HashMap<>();
        dict.put("恋爱", Arrays.asList("恋爱", "爱情", "感情", "情感"));
        dict.put("技巧", Arrays.asList("技巧", "方法", "技能", "能力"));
        dict.put("提升", Arrays.asList("提升", "提高", "增强", "改善"));
        dict.put("沟通", Arrays.asList("沟通", "交流", "对话", "聊天"));
        return dict;
    }
}
