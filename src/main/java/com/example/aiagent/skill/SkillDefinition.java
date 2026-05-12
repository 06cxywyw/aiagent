package com.example.aiagent.skill;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Skill 定义
 */
@Data
public class SkillDefinition {

    /**
     * Skill 唯一标识
     */
    private String id;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * Skill 版本
     */
    private String version;

    /**
     * Skill 描述
     */
    private String description;

    /**
     * Skill 作者
     */
    private String author;

    /**
     * Skill 标签
     */
    private List<String> tags;

    /**
     * Skill 类型
     */
    private SkillType type;

    /**
     * 输入参数定义
     */
    private List<Parameter> inputs;

    /**
     * 输出定义
     */
    private Output output;

    /**
     * Prompt 模板（对于 prompt 类型的 skill）
     */
    private String promptTemplate;

    /**
     * 工具调用配置（对于 tool 类型的 skill）
     */
    private List<String> tools;

    /**
     * 处理器类名（对于 code 类型的 skill）
     */
    private String handlerClass;

    /**
     * 依赖的其他 skills
     */
    private List<String> dependencies;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * Skill 类型
     */
    public enum SkillType {
        PROMPT,      // 基于 Prompt 的 skill
        TOOL,        // 调用工具的 skill
        CODE,        // 自定义代码的 skill
        COMPOSITE    // 组合多个 skills
    }

    /**
     * 参数定义
     */
    @Data
    public static class Parameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
    }

    /**
     * 输出定义
     */
    @Data
    public static class Output {
        private String type;
        private String description;
    }
}
