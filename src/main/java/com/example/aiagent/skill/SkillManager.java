package com.example.aiagent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 管理服务
 * 负责 skill 的加载、搜索、安装、卸载
 */
@Slf4j
@Service
public class SkillManager {

    /**
     * Skills 存储目录
     */
    private final String skillsDirectory;

    /**
     * 已加载的 skills
     */
    private final Map<String, SkillDefinition> loadedSkills = new HashMap<>();

    public SkillManager() {
        // 默认 skills 目录
        this.skillsDirectory = System.getProperty("user.home") + "/.aiagent/skills";
        initializeSkillsDirectory();
        loadAllSkills();
    }

    /**
     * 初始化 skills 目录
     */
    private void initializeSkillsDirectory() {
        try {
            Path path = Paths.get(skillsDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建 skills 目录: {}", skillsDirectory);
            }
        } catch (IOException e) {
            log.error("创建 skills 目录失败", e);
        }
    }

    /**
     * 加载所有 skills
     */
    public void loadAllSkills() {
        try {
            File dir = new File(skillsDirectory);
            File[] skillDirs = dir.listFiles(File::isDirectory);

            if (skillDirs == null) {
                log.warn("Skills 目录为空");
                return;
            }

            for (File skillDir : skillDirs) {
                try {
                    SkillDefinition skill = loadSkill(skillDir);
                    if (skill != null) {
                        loadedSkills.put(skill.getId(), skill);
                        log.info("加载 skill: {} v{}", skill.getName(), skill.getVersion());
                    }
                } catch (Exception e) {
                    log.error("加载 skill 失败: {}", skillDir.getName(), e);
                }
            }

            log.info("共加载 {} 个 skills", loadedSkills.size());

        } catch (Exception e) {
            log.error("加载 skills 失败", e);
        }
    }

    /**
     * 从目录加载单个 skill
     */
    private SkillDefinition loadSkill(File skillDir) {
        // TODO: 实现从 skill.json 或 skill.yml 加载配置
        // 这里返回 null，实际需要解析配置文件
        return null;
    }

    /**
     * 获取所有已安装的 skills
     */
    public List<SkillDefinition> getAllSkills() {
        return new ArrayList<>(loadedSkills.values());
    }

    /**
     * 根据 ID 获取 skill
     */
    public SkillDefinition getSkill(String skillId) {
        return loadedSkills.get(skillId);
    }

    /**
     * 搜索 skills
     */
    public List<SkillDefinition> searchSkills(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSkills();
        }

        String lowerKeyword = keyword.toLowerCase();

        return loadedSkills.values().stream()
                .filter(skill ->
                    skill.getName().toLowerCase().contains(lowerKeyword) ||
                    skill.getDescription().toLowerCase().contains(lowerKeyword) ||
                    (skill.getTags() != null && skill.getTags().stream()
                            .anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword)))
                )
                .collect(Collectors.toList());
    }

    /**
     * 按标签搜索
     */
    public List<SkillDefinition> searchByTag(String tag) {
        return loadedSkills.values().stream()
                .filter(skill -> skill.getTags() != null && skill.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * 执行 skill
     */
    public String executeSkill(String skillId, Map<String, Object> inputs) {
        SkillDefinition skill = loadedSkills.get(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillId);
        }

        log.info("执行 skill: {} with inputs: {}", skill.getName(), inputs);

        // 根据 skill 类型执行
        switch (skill.getType()) {
            case PROMPT:
                return executePromptSkill(skill, inputs);
            case TOOL:
                return executeToolSkill(skill, inputs);
            case CODE:
                return executeCodeSkill(skill, inputs);
            case COMPOSITE:
                return executeCompositeSkill(skill, inputs);
            default:
                throw new UnsupportedOperationException("Unsupported skill type: " + skill.getType());
        }
    }

    /**
     * 执行 Prompt 类型的 skill
     */
    private String executePromptSkill(SkillDefinition skill, Map<String, Object> inputs) {
        // 替换 prompt 模板中的变量
        String prompt = skill.getPromptTemplate();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }

        // TODO: 调用 LLM
        log.info("执行 prompt: {}", prompt);
        return "Prompt skill result";
    }

    /**
     * 执行 Tool 类型的 skill
     */
    private String executeToolSkill(SkillDefinition skill, Map<String, Object> inputs) {
        // TODO: 调用配置的工具
        log.info("执行 tool skill: {}", skill.getTools());
        return "Tool skill result";
    }

    /**
     * 执行 Code 类型的 skill
     */
    private String executeCodeSkill(SkillDefinition skill, Map<String, Object> inputs) {
        // TODO: 动态加载并执行处理器类
        log.info("执行 code skill: {}", skill.getHandlerClass());
        return "Code skill result";
    }

    /**
     * 执行 Composite 类型的 skill
     */
    private String executeCompositeSkill(SkillDefinition skill, Map<String, Object> inputs) {
        // TODO: 依次执行依赖的 skills
        log.info("执行 composite skill with dependencies: {}", skill.getDependencies());
        return "Composite skill result";
    }

    /**
     * 安装 skill
     */
    public void installSkill(String skillId, String downloadUrl) {
        log.info("安装 skill: {} from {}", skillId, downloadUrl);
        // TODO: 实现下载和安装逻辑
        // 1. 下载 skill 包
        // 2. 解压到 skills 目录
        // 3. 验证 skill 配置
        // 4. 加载 skill
    }

    /**
     * 卸载 skill
     */
    public void uninstallSkill(String skillId) {
        log.info("卸载 skill: {}", skillId);
        SkillDefinition skill = loadedSkills.remove(skillId);
        if (skill != null) {
            // TODO: 删除 skill 目录
            log.info("Skill {} 已卸载", skill.getName());
        }
    }

    /**
     * 更新 skill
     */
    public void updateSkill(String skillId) {
        log.info("更新 skill: {}", skillId);
        // TODO: 实现更新逻辑
        // 1. 检查新版本
        // 2. 下载新版本
        // 3. 替换旧版本
        // 4. 重新加载
    }

    /**
     * 获取 skills 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", loadedSkills.size());
        stats.put("byType", loadedSkills.values().stream()
                .collect(Collectors.groupingBy(
                        skill -> skill.getType().name(),
                        Collectors.counting()
                )));
        return stats;
    }
}
