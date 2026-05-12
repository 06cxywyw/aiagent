package com.example.aiagent.skill;

import lombok.Data;

import java.time.Instant;

/**
 * Skill 市场中的 Skill 信息
 */
@Data
public class SkillMarketItem {

    /**
     * Skill ID
     */
    private String id;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * 版本
     */
    private String version;

    /**
     * 简短描述
     */
    private String description;

    /**
     * 详细说明
     */
    private String readme;

    /**
     * 作者
     */
    private String author;

    /**
     * 标签
     */
    private String[] tags;

    /**
     * 下载次数
     */
    private int downloads;

    /**
     * 评分（1-5）
     */
    private double rating;

    /**
     * 评价数量
     */
    private int ratingCount;

    /**
     * 是否已安装
     */
    private boolean installed;

    /**
     * 下载 URL
     */
    private String downloadUrl;

    /**
     * 仓库 URL
     */
    private String repositoryUrl;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 图标 URL
     */
    private String iconUrl;

    /**
     * 截图 URLs
     */
    private String[] screenshots;

    /**
     * 依赖
     */
    private String[] dependencies;

    /**
     * 最低系统版本要求
     */
    private String minVersion;
}
