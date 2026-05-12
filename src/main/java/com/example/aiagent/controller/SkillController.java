package com.example.aiagent.controller;

import com.example.aiagent.skill.SkillDefinition;
import com.example.aiagent.skill.SkillManager;
import com.example.aiagent.skill.SkillMarketItem;
import com.example.aiagent.skill.SkillMarketService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 管理接口
 */
@RestController
@RequestMapping("/skills")
public class SkillController {

    @Resource
    private SkillManager skillManager;

    @Resource
    private SkillMarketService skillMarketService;

    /**
     * 获取所有已安装的 skills
     */
    @GetMapping("/installed")
    public Map<String, Object> getInstalledSkills() {
        List<SkillDefinition> skills = skillManager.getAllSkills();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", skills.size());
        result.put("skills", skills);
        return result;
    }

    /**
     * 搜索已安装的 skills
     */
    @GetMapping("/search")
    public Map<String, Object> searchInstalledSkills(@RequestParam String keyword) {
        List<SkillDefinition> skills = skillManager.searchSkills(keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", skills.size());
        result.put("skills", skills);
        return result;
    }

    /**
     * 获取 skill 详情
     */
    @GetMapping("/{skillId}")
    public Map<String, Object> getSkillDetail(@PathVariable String skillId) {
        SkillDefinition skill = skillManager.getSkill(skillId);

        Map<String, Object> result = new HashMap<>();
        if (skill != null) {
            result.put("success", true);
            result.put("skill", skill);
        } else {
            result.put("success", false);
            result.put("message", "Skill not found");
        }
        return result;
    }

    /**
     * 执行 skill
     */
    @PostMapping("/{skillId}/execute")
    public Map<String, Object> executeSkill(
            @PathVariable String skillId,
            @RequestBody Map<String, Object> inputs) {

        try {
            String output = skillManager.executeSkill(skillId, inputs);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("output", output);
            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 搜索 skill 市场
     */
    @GetMapping("/market/search")
    public Map<String, Object> searchMarket(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<SkillMarketItem> items = skillMarketService.searchMarket(
                keyword != null ? keyword : "",
                tag,
                page,
                pageSize
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", items.size());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("items", items);
        return result;
    }

    /**
     * 获取热门 skills
     */
    @GetMapping("/market/popular")
    public Map<String, Object> getPopularSkills(@RequestParam(defaultValue = "10") int limit) {
        List<SkillMarketItem> items = skillMarketService.getPopularSkills(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    /**
     * 获取市场 skill 详情
     */
    @GetMapping("/market/{skillId}")
    public Map<String, Object> getMarketSkillDetail(@PathVariable String skillId) {
        SkillMarketItem item = skillMarketService.getSkillDetail(skillId);

        Map<String, Object> result = new HashMap<>();
        if (item != null) {
            result.put("success", true);
            result.put("item", item);
        } else {
            result.put("success", false);
            result.put("message", "Skill not found in market");
        }
        return result;
    }

    /**
     * 安装 skill
     */
    @PostMapping("/install")
    public Map<String, Object> installSkill(@RequestBody InstallRequest request) {
        try {
            skillManager.installSkill(request.getSkillId(), request.getDownloadUrl());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Skill installed successfully");
            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 卸载 skill
     */
    @DeleteMapping("/{skillId}")
    public Map<String, Object> uninstallSkill(@PathVariable String skillId) {
        try {
            skillManager.uninstallSkill(skillId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Skill uninstalled successfully");
            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 更新 skill
     */
    @PutMapping("/{skillId}")
    public Map<String, Object> updateSkill(@PathVariable String skillId) {
        try {
            skillManager.updateSkill(skillId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Skill updated successfully");
            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 获取所有标签
     */
    @GetMapping("/market/tags")
    public Map<String, Object> getAllTags() {
        List<String> tags = skillMarketService.getAllTags();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tags", tags);
        return result;
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = skillManager.getStats();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stats", stats);
        return result;
    }

    @Data
    public static class InstallRequest {
        private String skillId;
        private String downloadUrl;
    }
}
