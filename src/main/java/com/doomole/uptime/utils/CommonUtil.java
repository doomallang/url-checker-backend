package com.doomole.uptime.utils;

public final class CommonUtil {

    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String t = rawUrl.trim();
        if (!t.startsWith("http://") && !t.startsWith("https://")) {
            t = "https://" + t;
        }
        try {
            var uri = new java.net.URI(t);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Only http/https allowed: " + rawUrl);
            }
            // 호스트 소문자, 기본 포트 제거, 트레일링 슬래시 정리 정도
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : null;
            int port = uri.getPort();
            boolean dropPort = (port == 80 && "http".equalsIgnoreCase(uri.getScheme()))
                    || (port == 443 && "https".equalsIgnoreCase(uri.getScheme()));
            String path = (uri.getPath() == null || uri.getPath().isBlank()) ? "/" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length()-1);

            var rebuilt = new java.net.URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    host,
                    dropPort ? -1 : port,
                    path,
                    uri.getQuery(),
                    null // fragment 제거
            );
            return rebuilt.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl, e);
        }
    }
}
