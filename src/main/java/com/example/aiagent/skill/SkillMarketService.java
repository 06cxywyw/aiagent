package com.example.aiagent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 市场服务
 * 负责从远程市场搜索、下载 skills
 */
@Slf4j
@Service
public class SkillMarketService {

    private final RestTemplate restTemplate;

    /**
     * Skill 市场 API 地址
     */
    private static final String MARKET_API_URL = "https://api.aiagent-skills.com/v1";

    public SkillMarketService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 搜索市场中的 skills
     */
    public List<SkillMarketItem> searchMarket(String keyword, String tag, int page, int pageSize) {
        try {
            // 构建查询参数
            String url = String.format("%s/skills/search?keyword=%s&tag=%s&page=%d&size=%d",
                    MARKET_API_URL, keyword, tag != null ? tag : "", page, pageSize);

            log.info("搜索 skill 市场: {}", url);

            // TODO: 实际调用远程 API
            // SkillMarketItem[] items = restTemplate.getForObject(url, SkillMarketItem[].class);
            // return Arrays.asList(items);

            // 模拟数据
            return getMockSkills(keyword);

        } catch (Exception e) {
            log.error("搜索 skill 市场失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取热门 skills
     */
    public List<SkillMarketItem> getPopularSkills(int limit) {
        try {
            String url = String.format("%s/skills/popular?limit=%d", MARKET_API_URL, limit);
            log.info("获取热门 skills: {}", url);

            // TODO: 实际调用远程 API
            return getMockSkills("");

        } catch (Exception e) {
            log.error("获取热门 skills 失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取 skill 详情
     */
    public SkillMarketItem getSkillDetail(String skillId) {
        try {
            String url = String.format("%s/skills/%s", MARKET_API_URL, skillId);
            log.info("获取 skill 详情: {}", url);

            // TODO: 实际调用远程 API
            // return restTemplate.getForObject(url, SkillMarketItem.class);

            return getMockSkills("").get(0);

        } catch (Exception e) {
            log.error("获取 skill 详情失败", e);
            return null;
        }
    }

    /**
     * 下载 skill
     */
    public byte[] downloadSkill(String downloadUrl) {
        try {
            log.info("下载 skill: {}", downloadUrl);
            return restTemplate.getForObject(downloadUrl, byte[].class);
        } catch (Exception e) {
            log.error("下载 skill 失败", e);
            return null;
        }
    }

    /**
     * 获取所有标签
     */
    public List<String> getAllTags() {
        return Arrays.asList(
                "数据分析", "文本处理", "图像处理", "代码生成",
                "翻译", "总结", "问答", "搜索", "工具调用"
        );
    }

    /**
     * 模拟数据（用于测试）
     */
    private List<SkillMarketItem> getMockSkills(String keyword) {
        List<SkillMarketItem> skills = new ArrayList<>();

        // Skill 1: 代码审查
        SkillMarketItem skill1 = new SkillMarketItem();
        skill1.setId("code-review");
        skill1.setName("代码审查助手");
        skill1.setVersion("1.0.0");
        skill1.setDescription("自动审查代码质量、安全性和最佳实践");
        skill1.setAuthor("AI Agent Team");
        skill1.setTags(new String[]{"代码生成", "工具调用"});
        skill1.setDownloads(1250);
        skill1.setRating(4.8);
        skill1.setRatingCount(89);
        skill1.setInstalled(false);
        skill1.setDownloadUrl("https://cdn.aiagent-skills.com/code-review-1.0.0.zip");
        skill1.setRepositoryUrl("https://github.com/aiagent/skill-code-review");
        skills.add(skill1);

        // Skill 2: 文档总结
        SkillMarketItem skill2 = new SkillMarketItem();
        skill2.setId("doc-summarizer");
        skill2.setName("文档总结器");
        skill2.setVersion("2.1.0");
        skill2.setDescription("快速总结长文档，提取关键信息");
        skill2.setAuthor("Doc Team");
        skill2.setTags(new String[]{"文本处理", "总结"});
        skill2.setDownloads(3420);
        skill2.setRating(4.9);
        skill2.setRatingCount(156);
        skill2.setInstalled(false);
        skill2.setDownloadUrl("https://cdn.aiagent-skills.com/doc-summarizer-2.1.0.zip");
        skill2.setRepositoryUrl("https://github.com/aiagent/skill-doc-summarizer");
        skills.add(skill2);

        // Skill 3: SQL 生成
        SkillMarketItem skill3 = new SkillMarketItem();
        skill3.setId("sql-generator");
        skill3.setName("SQL 查询生成器");
        skill3.setVersion("1.5.2");
        skill3.setDescription("根据自然语言描述生成 SQL 查询");
        skill3.setAuthor("Data Team");
        skill3.setTags(new String[]{"数据分析", "代码生成"});
        skill3.setDownloads(2890);
        skill3.setRating(4.7);
        skill3.setRatingCount(203);
        skill3.setInstalled(true);
        skill3.setDownloadUrl("https://cdn.aiagent-skills.com/sql-generator-1.5.2.zip");
        skill3.setRepositoryUrl("https://github.com/aiagent/skill-sql-generator");
        skills.add(skill3);

        // Skill 4: 翻译助手
        SkillMarketItem skill4 = new SkillMarketItem();
        skill4.setId("translator");
        skill4.setName("多语言翻译助手");
        skill4.setVersion("3.0.1");
        skill4.setDescription("支持50+语言的高质量翻译");
        skill4.setAuthor("Translation Team");
        skill4.setTags(new String[]{"翻译", "文本处理"});
        skill4.setDownloads(5670);
        skill4.setRating(4.9);
        skill4.setRatingCount(412);
        skill4.setInstalled(false);
        skill4.setDownloadUrl("https://cdn.aiagent-skills.com/translator-3.0.1.zip");
        skill4.setRepositoryUrl("https://github.com/aiagent/skill-translator");
        skills.add(skill4);

        // Skill 5: 图片分析
        SkillMarketItem skill5 = new SkillMarketItem();
        skill5.setId("image-analyzer");
        skill5.setName("图片内容分析");
        skill5.setVersion("1.2.0");
        skill5.setDescription("识别图片内容、物体、场景和文字");
        skill5.setAuthor("Vision Team");
        skill5.setTags(new String[]{"图像处理", "工具调用"});
        skill5.setDownloads(1890);
        skill5.setRating(4.6);
        skill5.setRatingCount(78);
        skill5.setInstalled(false);
        skill5.setDownloadUrl("https://cdn.aiagent-skills.com/image-analyzer-1.2.0.zip");
        skill5.setRepositoryUrl("https://github.com/aiagent/skill-image-analyzer");
        skills.add(skill5);

        // 根据关键词过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            return skills.stream()
                    .filter(s -> s.getName().toLowerCase().contains(lowerKeyword) ||
                                 s.getDescription().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        return skills;
    }
}
