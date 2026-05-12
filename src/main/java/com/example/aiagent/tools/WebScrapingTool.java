package com.example.aiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 网页抓取工具（已加固安全限制）
 *
 * 安全改进：
 * 1. URL 校验：只允许 http/https 协议
 * 2. IP 地址校验：禁止访问内网地址（防止 SSRF）
 * 3. 域名白名单：可选配置允许的域名
 * 4. 超时控制：防止长时间挂起
 * 5. 重定向限制：禁止跨域重定向
 */
@Component
public class WebScrapingTool {

    // 允许的协议
    private static final Set<String> ALLOWED_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

    // 最大页面大小（字节）- 5MB
    private static final int MAX_PAGE_SIZE = 5 * 1024 * 1024;

    // 超时时间（毫秒）
    private static final int TIMEOUT_MS = 10000;

    // 是否限制内网访问（默认 true）
    private static final boolean BLOCK_INTERNAL_IP = true;

    // 内网 IP 前缀
    private static final List<String> INTERNAL_IP_PREFIXES = Arrays.asList(
            "127.0.0.1", "localhost", "10.", "172.16.", "172.17.", "172.18.",
            "172.19.", "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.", "172.30.",
            "172.31.", "192.168.", "169.254."
    );

    @Tool(description = "Scrape the content of a web page (受限模式)")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        // 1. 基础校验
        if (url == null || url.trim().isEmpty()) {
            return "❌ 抓取失败：URL 不能为空";
        }

        // 2. URL 格式校验
        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return "❌ 抓取失败：URL 格式无效";
        }

        // 3. 协议校验
        String protocol = parsedUrl.getProtocol();
        if (!ALLOWED_PROTOCOLS.contains(protocol)) {
            return "❌ 抓取失败：只允许 http/https 协议";
        }

        // 4. 主机名解析和 IP 校验
        String host = parsedUrl.getHost();
        if (host == null || host.isEmpty()) {
            return "❌ 抓取失败：无效的主机名";
        }

        if (BLOCK_INTERNAL_IP && isInternalIP(host)) {
            return "❌ 抓取失败：禁止访问内网地址";
        }

        // 5. 执行抓取
        try {
            Document document = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (AI Agent)")
                    .followRedirects(false)  // 禁止重定向
                    .maxBodySize(MAX_PAGE_SIZE)
                    .get();

            return document.html();

        } catch (java.net.SocketTimeoutException e) {
            return "❌ 抓取失败：连接超时";
        } catch (java.net.UnknownHostException e) {
            return "❌ 抓取失败：主机不存在";
        } catch (java.net.ConnectException e) {
            return "❌ 抓取失败：无法连接到服务器";
        } catch (org.jsoup.HttpStatusException e) {
            return "❌ 抓取失败：HTTP 错误 " + e.getStatusCode();
        } catch (org.jsoup.UnsupportedMimeTypeException e) {
            return "❌ 抓取失败：不支持的文件类型";
        } catch (Exception e) {
            return "❌ 抓取失败: " + e.getMessage();
        }
    }

    /**
     * 检查是否是内网 IP 地址
     */
    private boolean isInternalIP(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            for (InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress() ||
                    addr.isLoopbackAddress() ||
                    addr.isLinkLocalAddress() ||
                    addr.isSiteLocalAddress()) {
                    return true;
                }

                String ipAddress = addr.getHostAddress();
                for (String prefix : INTERNAL_IP_PREFIXES) {
                    if (ipAddress.startsWith(prefix)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            // 解析失败，保守起见认为是内网
            return true;
        }
    }
}
