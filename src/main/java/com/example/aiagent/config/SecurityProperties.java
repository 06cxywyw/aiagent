package com.example.aiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置属性类
 * 从环境变量或 application.yml 读取配置
 *
 * 安全说明：
 * - 敏感信息（API Key、密码）优先从环境变量读取
 * - 环境变量未设置时使用配置文件的默认值（仅用于开发）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * API Key 配置
     */
    private ApiKeys apiKeys = new ApiKeys();

    /**
     * 数据库配置
     */
    private Datasource datasource = new Datasource();

    /**
     * Redis 配置
     */
    private Redis redis = new Redis();

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultRequestsPerMinute = 60;
        private int authenticatedRequestsPerMinute = 120;
        private int windowSizeSeconds = 60;
    }

    @Data
    public static class ApiKeys {
        /**
         * DashScope API Key
         * 优先级：环境变量 DASHSCOPE_API_KEY > 配置文件
         */
        private String dashscopeKey;

        /**
         * SearchAPI Key
         * 优先级：环境变量 SEARCH_API_KEY > 配置文件
         */
        private String searchApiKey;

        public String getDashscopeKey() {
            // 优先从环境变量读取
            return dashscopeKey != null ? dashscopeKey :
                   System.getenv("DASHSCOPE_API_KEY");
        }

        public String getSearchApiKey() {
            // 优先从环境变量读取
            return searchApiKey != null ? searchApiKey :
                   System.getenv("SEARCH_API_KEY");
        }
    }

    @Data
    public static class Datasource {
        /**
         * 数据库 URL
         * 优先级：环境变量 DB_URL > 配置文件
         */
        private String url;

        /**
         * 数据库用户名
         * 优先级：环境变量 DB_USERNAME > 配置文件
         */
        private String username;

        /**
         * 数据库密码
         * 优先级：环境变量 DB_PASSWORD > 配置文件
         */
        private String password;

        public String getUrl() {
            return url != null ? url : System.getenv("DB_URL");
        }

        public String getUsername() {
            return username != null ? username : System.getenv("DB_USERNAME");
        }

        public String getPassword() {
            return password != null ? password : System.getenv("DB_PASSWORD");
        }
    }

    @Data
    public static class Redis {
        /**
         * Redis 主机
         * 优先级：环境变量 REDIS_HOST > 配置文件
         */
        private String host;

        /**
         * Redis 端口
         * 优先级：环境变量 REDIS_PORT > 配置文件
         */
        private Integer port;

        /**
         * Redis 密码
         * 优先级：环境变量 REDIS_PASSWORD > 配置文件
         */
        private String password;

        /**
         * Redis 数据库
         * 优先级：环境变量 REDIS_DATABASE > 配置文件
         */
        private Integer database;

        public String getHost() {
            return host != null ? host : System.getenv("REDIS_HOST");
        }

        public Integer getPort() {
            return port != null ? port :
                   parseEnvInteger("REDIS_PORT", 6379);
        }

        public String getPassword() {
            return password != null ? password : System.getenv("REDIS_PASSWORD");
        }

        public Integer getDatabase() {
            return database != null ? database :
                   parseEnvInteger("REDIS_DATABASE", 0);
        }

        private Integer parseEnvInteger(String envName, Integer defaultValue) {
            String value = System.getenv(envName);
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }
    }
}
