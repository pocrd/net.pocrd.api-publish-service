package com.pocrd.api_publish_service.client;

import com.pocrd.clientsdk.ApiMethod;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Codegen SDK 的 HTTP 客户端
 * <p>
 * 用于通过 Higress 网关调用服务，使用生成的 ApiMethod 类构建请求
 * 支持 HTTPS + mTLS（客户端证书认证）和 --resolve 功能（域名到IP映射）
 * <p>
 * 参考实现: HttpsGreet2Test
 */
public class CodegenHttpClient {

    private final String baseUrl;
    private final String resolveHost;
    private final String resolveIp;
    private final int resolvePort;
    private final String clientCertPath;
    private final String clientKeyPath;

    /**
     * 创建 HTTP 客户端（HTTP 模式）
     *
     * @param baseUrl 基础 URL，如 http://localhost:8080
     */
    public CodegenHttpClient(String baseUrl) {
        this(baseUrl, null, 0, null, null);
    }

    /**
     * 创建 HTTPS 客户端（HTTPS + mTLS 模式，支持 --resolve）
     *
     * @param baseUrl        基础 URL，如 https://api.caringfamily.cn:30443
     * @param resolveIp      实际连接的 IP 地址（模拟 curl --resolve），如 127.0.0.1
     * @param resolvePort    实际连接的端口
     * @param clientCertPath 客户端证书路径（PEM 格式）
     * @param clientKeyPath  客户端私钥路径（PEM 格式，支持 EC PRIVATE KEY）
     */
    public CodegenHttpClient(String baseUrl, String resolveIp, int resolvePort,
                             String clientCertPath, String clientKeyPath) {
        this.baseUrl = baseUrl;
        this.resolveHost = extractHost(baseUrl);
        this.resolveIp = resolveIp;
        this.resolvePort = resolvePort > 0 ? resolvePort : extractPort(baseUrl);
        this.clientCertPath = clientCertPath;
        this.clientKeyPath = clientKeyPath;
    }

    /**
     * 执行 API 调用
     *
     * @param apiMethod API 方法对象
     * @param <R>       返回类型
     * @return 响应字符串（JSON）
     * @throws Exception 执行异常
     */
    public <R> String execute(ApiMethod<R> apiMethod) throws Exception {
        String path = apiMethod.buildUrl(""); // 获取路径部分
        String requestBody = apiMethod.buildRequestBody();
        boolean isPost = apiMethod.isPost();

        // 构建完整 URL
        String url = baseUrl + path;

        // 确定连接目标
        String connectHost = resolveIp != null ? resolveIp : resolveHost;
        int connectPort = resolvePort;
        String sniHost = resolveHost; // SNI 始终使用原始域名

        // 发送 HTTP 请求
        return sendRequest(connectHost, connectPort, sniHost, path, isPost, requestBody);
    }

    /**
     * 发送 HTTP/HTTPS 请求
     */
    private String sendRequest(String connectHost, int connectPort, String sniHost,
                               String path, boolean isPost, String requestBody) throws Exception {
        // 建立 TCP 连接
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(connectHost, connectPort), 10000);

        // 如果是 HTTPS，包装为 SSLSocket
        SSLSocket sslSocket = null;
        Socket socket;
        if (baseUrl.startsWith("https://")) {
            SSLContext sslContext = createSSLContext();
            javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    plainSocket, sniHost, connectPort, true);

            // 设置 SNI
            SSLParameters sslParams = sslSocket.getSSLParameters();
            sslParams.setServerNames(Collections.singletonList(new SNIHostName(sniHost)));
            sslSocket.setSSLParameters(sslParams);

            // 开始 TLS 握手
            sslSocket.startHandshake();
            socket = sslSocket;
        } else {
            socket = plainSocket;
        }

        try {
            // 发送 HTTP 请求
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            // 构建 HTTP 请求
            StringBuilder request = new StringBuilder();
            request.append(isPost ? "POST " : "GET ").append(path).append(" HTTP/1.1\r\n");
            request.append("Host: ").append(sniHost).append("\r\n");
            request.append("Content-Type: application/json\r\n");
            if (isPost && requestBody != null && !requestBody.isEmpty()) {
                request.append("Content-Length: ").append(requestBody.getBytes().length).append("\r\n");
            }
            request.append("Connection: close\r\n");
            request.append("\r\n");
            if (isPost && requestBody != null && !requestBody.isEmpty()) {
                request.append(requestBody);
            }

            os.write(request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.flush();

            // 读取 HTTP 响应（使用 UTF-8 编码）
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            // 解析响应
            return parseResponseBody(response.toString());
        } finally {
            socket.close();
        }
    }

    /**
     * 解析 HTTP 响应体（跳过头部）
     */
    private String parseResponseBody(String rawResponse) throws IOException {
        // 找到响应体开始位置（\r\n\r\n 之后）
        int bodyStart = rawResponse.indexOf("\r\n\r\n");
        if (bodyStart >= 0) {
            String body = rawResponse.substring(bodyStart + 4).trim();
            // 处理 chunked 编码（简单处理，去除长度标记）
            return stripChunkedEncoding(body);
        }
        // 如果没有找到 \r\n\r\n，尝试 \n\n
        bodyStart = rawResponse.indexOf("\n\n");
        if (bodyStart >= 0) {
            String body = rawResponse.substring(bodyStart + 2).trim();
            return stripChunkedEncoding(body);
        }
        return rawResponse.trim();
    }

    /**
     * 去除 chunked 编码的长度标记
     */
    private String stripChunkedEncoding(String body) {
        // 简单处理：去除十六进制长度行（如 "60\r\n"）和结尾的 "0\r\n"
        StringBuilder result = new StringBuilder();
        String[] lines = body.split("\r\n");
        for (String line : lines) {
            // 跳过纯十六进制数字的行（长度标记）
            if (line.matches("^[0-9a-fA-F]+$")) {
                continue;
            }
            // 跳过 "0" 结束标记
            if (line.equals("0")) {
                break;
            }
            result.append(line);
        }
        return result.toString().trim();
    }

    /**
     * 创建 SSLContext（支持 mTLS）
     */
    private SSLContext createSSLContext() throws Exception {
        // 如果没有配置证书，创建默认的 SSLContext
        if (clientCertPath == null || clientKeyPath == null) {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{createTrustAllManager()}, null);
            return sslContext;
        }

        // 验证证书文件存在
        if (!new File(clientCertPath).exists()) {
            throw new RuntimeException("客户端证书不存在：" + clientCertPath);
        }
        if (!new File(clientKeyPath).exists()) {
            throw new RuntimeException("客户端私钥不存在：" + clientKeyPath);
        }

        // 加载客户端证书链
        List<Certificate> clientCerts = loadCertificates(clientCertPath);

        // 加载客户端私钥
        PrivateKeyInfo keyInfo = loadPrivateKey(clientKeyPath);

        // 创建 KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        Certificate[] chain = clientCerts.toArray(new Certificate[0]);
        keyStore.setKeyEntry("client", keyInfo.key, new char[0], chain);

        // 创建 KeyManager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);

        // 创建 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[]{createTrustAllManager()}, null);

        return sslContext;
    }

    /**
     * 创建信任所有证书的 TrustManager（测试环境）
     */
    private TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        };
    }

    /**
     * 从 PEM 文件加载证书链
     */
    private List<Certificate> loadCertificates(String certPath) throws Exception {
        List<Certificate> certs = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        String content = readFile(certPath);
        String[] certBlocks = content.split("(?=-----BEGIN CERTIFICATE-----)");
        for (String block : certBlocks) {
            if (block.trim().isEmpty()) continue;
            byte[] certBytes = block.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            certs.add(cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes)));
        }
        return certs;
    }

    /**
     * 从 PEM 文件加载私钥（支持 EC PRIVATE KEY 和 PRIVATE KEY 格式）
     */
    private PrivateKeyInfo loadPrivateKey(String keyPath) throws Exception {
        String content = readFile(keyPath);

        // 如果是 EC PRIVATE KEY 格式（SEC1），需要转换
        if (content.contains("BEGIN EC PRIVATE KEY")) {
            File pkcs8File = File.createTempFile("key", ".pkcs8");
            pkcs8File.deleteOnExit();

            ProcessBuilder pb = new ProcessBuilder(
                    "openssl", "pkcs8", "-topk8", "-nocrypt",
                    "-in", keyPath,
                    "-out", pkcs8File.getAbsolutePath()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("openssl 转换私钥失败，退出码: " + exitCode);
            }

            content = readFile(pkcs8File.getAbsolutePath());
            pkcs8File.delete();
        }

        // 解析 PRIVATE KEY 格式（PKCS#8）
        String base64 = content
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
        return new PrivateKeyInfo(kf.generatePrivate(spec), "EC");
    }

    /**
     * 读取文件内容
     */
    private String readFile(String path) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 从 URL 中提取主机名
     */
    private String extractHost(String url) {
        if (url.startsWith("http://")) {
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        int colonIndex = url.indexOf(':');
        if (colonIndex > 0) {
            return url.substring(0, colonIndex);
        }
        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            return url.substring(0, slashIndex);
        }
        return url;
    }

    /**
     * 从 URL 中提取端口
     */
    private int extractPort(String url) {
        if (url.startsWith("http://")) {
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        int colonIndex = url.indexOf(':');
        if (colonIndex > 0) {
            String portStr = url.substring(colonIndex + 1);
            int slashIndex = portStr.indexOf('/');
            if (slashIndex > 0) {
                portStr = portStr.substring(0, slashIndex);
            }
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // 使用默认端口
            }
        }
        return url.startsWith("https") ? 443 : 80;
    }

    /**
     * 私钥信息包装类
     */
    private static class PrivateKeyInfo {
        final java.security.PrivateKey key;
        final String algorithm;

        PrivateKeyInfo(java.security.PrivateKey key, String algorithm) {
            this.key = key;
            this.algorithm = algorithm;
        }
    }
}
