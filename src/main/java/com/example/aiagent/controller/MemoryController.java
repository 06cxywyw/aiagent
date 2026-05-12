package com.example.aiagent.controller;

import com.example.aiagent.memory.LongTermMemory;
import com.example.aiagent.memory.MemoryCleanupService;
import com.example.aiagent.memory.MixedMemory;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆管理接口
 */
@RestController
@RequestMapping("/memory")
public class MemoryController {

    @Resource
    private LongTermMemory longTermMemory;

    @Resource
    private MixedMemory mixedMemory;

    @Resource
    private MemoryCleanupService memoryCleanupService;

    /**
     * 添加长期记忆
     */
    @PostMapping("/add")
    public Map<String, Object> addMemory(@RequestBody AddMemoryRequest request) {
        longTermMemory.addMemory(request.getUserId(), request.getContent());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "记忆已添加");
        return result;
    }

    /**
     * 检索记忆
     */
    @GetMapping("/search")
    public Map<String, Object> searchMemory(
            @RequestParam String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        List<String> memories = longTermMemory.retrieveMemories(userId, query, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", memories.size());
        result.put("memories", memories);
        return result;
    }

    /**
     * 获取用户实体信息
     */
    @GetMapping("/entities/{userId}")
    public Map<String, Object> getEntities(@PathVariable String userId) {
        Map<String, String> entities = mixedMemory.getEntityInfo(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("userId", userId);
        result.put("entities", entities);
        return result;
    }

    /**
     * 清空用户记忆
     */
    @DeleteMapping("/clear/{userId}")
    public Map<String, Object> clearMemory(@PathVariable String userId) {
        mixedMemory.clear(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户记忆已清空");
        return result;
    }

    /**
     * 手动触发记忆清理（用于测试）
     */
    @PostMapping("/cleanup")
    public Map<String, Object> triggerCleanup() {
        memoryCleanupService.manualCleanup();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "记忆清理任务已触发");
        return result;
    }

    /**
     * 记忆统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "记忆统计功能待实现");
        // TODO: 实现统计功能
        // - 总记忆数量
        // - 各重要性等级分布
        // - 最近访问记录
        // - 存储空间占用
        return result;
    }

    @Data
    public static class AddMemoryRequest {
        private String userId;
        private String content;
    }
}
