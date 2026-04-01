package com.pocrd.api_publish_service.client;

import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Higress Dubbo Triple HTTPS + mTLS 接口测试
 * 测试 GreeterServiceHttpExport.greet2 接口 (HTTPS + 客户端证书认证)
 * 参考: test_https_greet2.sh
 */
public class HttpsGreet2Test {

    private static final String HOST = "api.caringfamily.cn";
    private static final int PORT = 30443;
    private static final String SERVICE_PATH = "/dapi/com.pocrd.dubbo_demo.api.GreeterServiceHttpExport";
    private static final String METHOD = "greet2";

    // 证书路径
    private static final String CLIENT_CERT = "/Users/pocrd/workspace/deploy/higress/certs/files/bagua/testFactory/devices/device001/device001-fullchain.crt";
    private static final String CLIENT_KEY = "/Users/pocrd/workspace/deploy/higress/certs/files/bagua/testFactory/devices/device001/device001.key";

    @Test
    public void testGreet2WithMtls() throws Exception {
        System.out.println("==============================================");
        System.out.println("Higress Dubbo Triple HTTPS + mTLS 接口测试");
        System.out.println("==============================================");
        System.out.println();

        // 验证证书文件存在
        File certFile = new File(CLIENT_CERT);
        if (!certFile.exists()) {
            fail("❌ 错误：客户端证书不存在：" + CLIENT_CERT);
        }

        File keyFile = new File(CLIENT_KEY);
        if (!keyFile.exists()) {
            fail("❌ 错误：客户端私钥不存在：" + CLIENT_KEY);
        }

        System.out.println("证书配置:");
        System.out.println("  客户端证书：" + CLIENT_CERT);
        System.out.println();

        // 测试: HTTPS + mTLS + JSON 访问 greet2 接口
        System.out.println("测试: HTTPS + mTLS + JSON 请求 greet2 接口");
        System.out.println("----------------------------------------------");

        System.out.println("请求: POST https://" + HOST + ":" + PORT + SERVICE_PATH + "/" + METHOD);
        System.out.println("实际连接: 127.0.0.1:" + PORT + " (模拟 --resolve)");
        System.out.println("SNI: " + HOST);
        System.out.println("参数: name1=张三, name2=李四");
        System.out.println();

        String requestBody = "{\"name1\":\"张三\",\"name2\":\"李四\"}";
        System.out.println("请求 Body:");
        System.out.println(requestBody);
        System.out.println();

        // 创建 SSLContext
        SSLContext sslContext = createSSLContext(CLIENT_CERT, CLIENT_KEY);

        // 建立 TCP 连接到 127.0.0.1:30443（模拟 --resolve）
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress("127.0.0.1", PORT), 10000);

        // 创建 SSLSocket，并设置 SNI
        javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                plainSocket, HOST, PORT, true);

        // 设置 SSL 参数，添加 SNI
        SSLParameters sslParams = sslSocket.getSSLParameters();
        sslParams.setServerNames(Collections.singletonList(new SNIHostName(HOST)));
        sslSocket.setSSLParameters(sslParams);

        // 开始 TLS 握手
        sslSocket.startHandshake();

        // 发送 HTTP 请求
        OutputStream os = sslSocket.getOutputStream();
        InputStream is = sslSocket.getInputStream();

        // 构建 HTTP POST 请求
        StringBuilder request = new StringBuilder();
        request.append("POST ").append(SERVICE_PATH).append("/").append(METHOD).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(HOST).append("\r\n");
        request.append("Content-Type: application/json\r\n");
        request.append("Content-Length: ").append(requestBody.getBytes().length).append("\r\n");
        request.append("Connection: close\r\n");
        request.append("\r\n");
        request.append(requestBody);

        os.write(request.toString().getBytes());
        os.flush();

        // 读取 HTTP 响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }

        sslSocket.close();

        System.out.println("原始响应:");
        System.out.println(response);
        System.out.println();

        // 解析响应体（跳过 HTTP 头）
        String responseStr = response.toString();
        String body;
        int bodyStart = responseStr.indexOf("\r\n\r\n");
        if (bodyStart >= 0) {
            body = responseStr.substring(bodyStart + 4).trim();
        } else {
            body = responseStr.trim();
        }

        System.out.println("响应体:");
        System.out.println(body);
        System.out.println();

        // 验证响应是否包含预期内容
        if (body.contains("Hello 张三 and 李四")) {
            System.out.println("✅ 测试通过: 响应包含预期内容");
        } else {
            fail("❌ 测试失败: 响应不包含预期内容");
        }

        System.out.println();
        System.out.println("==============================================");
        System.out.println("所有测试通过!");
        System.out.println("==============================================");

        assertTrue(body.contains("Hello 张三 and 李四"));
    }

    private SSLContext createSSLContext(String clientCertPath, String clientKeyPath) throws Exception {
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

        // 创建信任所有证书的 TrustManager（测试环境）
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // 创建 SSLContext（使用 TLSv1.2）
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);

        return sslContext;
    }

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

    private PrivateKeyInfo loadPrivateKey(String keyPath) throws Exception {
        // 读取私钥文件内容
        String content = readFile(keyPath);

        // 判断是 PKCS#8 还是 PKCS#1 格式
        if (content.contains("-----BEGIN PRIVATE KEY-----")) {
            // PKCS#8 格式
            String base64 = content
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
            return new PrivateKeyInfo(kf.generatePrivate(spec), "EC");
        } else if (content.contains("-----BEGIN EC PRIVATE KEY-----")) {
            // PKCS#1 EC 格式，使用 openssl 转换为 PKCS#8
            return convertPkcs1ToPkcs8(keyPath, "EC");
        } else {
            throw new RuntimeException("不支持的私钥格式");
        }
    }

    private PrivateKeyInfo convertPkcs1ToPkcs8(String keyPath, String algorithm) throws Exception {
        // 使用 openssl 转换为 PKCS8 格式
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

        // 读取转换后的 PKCS8 私钥
        String content = readFile(pkcs8File.getAbsolutePath());
        pkcs8File.delete();

        String base64 = content
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance(algorithm);
        return new PrivateKeyInfo(kf.generatePrivate(spec), algorithm);
    }

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

    private static class PrivateKeyInfo {
        final java.security.PrivateKey key;
        final String algorithm;

        PrivateKeyInfo(java.security.PrivateKey key, String algorithm) {
            this.key = key;
            this.algorithm = algorithm;
        }
    }
}
