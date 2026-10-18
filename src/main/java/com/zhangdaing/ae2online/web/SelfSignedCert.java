package com.zhangdaing.ae2online.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自签名证书的生成与加载。
 *
 * 为什么需要 HTTPS：
 *   浏览器把 https:// 和 localhost 视为「安全上下文」，只有安全上下文里
 *   AudioWorklet / SharedArrayBuffer 等 API 才可用。Godot Web 导出依赖这些，
 *   所以在局域网 IP + HTTP 下会拒绝启动。改用 HTTPS 即可从根上满足要求。
 *
 * 实现方式：
 *   调用 JDK 自带的 keytool（java.home/bin/keytool[.exe]）生成 PKCS12 证书。
 *   不依赖 BouncyCastle，也没有使用 sun.security.x509 这类内部 API，
 *   在只有 JRE 的环境里同样可用。
 *
 * 证书内容：
 *   自签名，10 年有效期，SAN 包含本机所有可用 IPv4 + 127.0.0.1 + localhost，
 *   这样浏览器访问任意本机地址时证书都「匹配」（仍会提示不受信任，属预期）。
 */
public final class SelfSignedCert {

    private static final Logger LOGGER = LoggerFactory.getLogger("AE2Online Cert");

    /** 证书文件（PKCS12） */
    public static final String STORE_FILE = "ae2online-cert.p12";
    /** 证书密码。本地自签名用途，硬编码可接受；证书仅用于局域网内加密传输。 */
    public static final char[] STORE_PASS = "ae2online".toCharArray();
    private static final String ALIAS = "ae2online";

    private SelfSignedCert() {}

    /**
     * 确保证书存在，返回可用的 KeyStore。
     * 首次调用会生成证书文件；之后直接复用。
     *
     * @param configDir 配置目录（证书保存在此）
     * @return 已加载的 KeyStore；生成或加载失败时返回 null
     */
    public static KeyStore ensure(Path configDir) {
        Path store = configDir.resolve(STORE_FILE);
        try {
            if (Files.exists(store)) {
                KeyStore ks = load(store);
                if (ks != null) {
                    LOGGER.info("复用已有证书: {}", store.toAbsolutePath());
                    return ks;
                }
                LOGGER.warn("已有证书无法加载，将重新生成: {}", store.toAbsolutePath());
                Files.deleteIfExists(store);
            }

            if (!generate(store)) {
                return null;
            }
            KeyStore ks = load(store);
            if (ks != null) {
                LOGGER.info("已生成自签名证书: {}", store.toAbsolutePath());
                LOGGER.info("浏览器首次访问会提示「连接不是私密连接」，选择「高级 → 继续访问」即可。");
            }
            return ks;
        } catch (Exception e) {
            LOGGER.error("准备证书失败: {}", store.toAbsolutePath(), e);
            return null;
        }
    }

    private static KeyStore load(Path store) {
        try (InputStream in = Files.newInputStream(store)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, STORE_PASS);
            // 校验确实有私钥条目
            if (!ks.isKeyEntry(ALIAS) && ks.size() > 0) {
                String first = ks.aliases().nextElement();
                if (!ks.isKeyEntry(first)) return null;
            }
            if (ks.size() == 0) return null;
            // 打印 SAN 供排错
            var it = ks.aliases();
            while (it.hasMoreElements()) {
                String alias = it.nextElement();
                Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate x) {
                    LOGGER.debug("证书 {} 有效期至 {}", alias, x.getNotAfter());
                }
            }
            return ks;
        } catch (Exception e) {
            LOGGER.debug("加载证书失败: {}", e.toString());
            return null;
        }
    }

    /** 调用 keytool 生成自签名证书 */
    private static boolean generate(Path store) {
        Path keytool = findKeytool();
        if (keytool == null) {
            LOGGER.error("未找到 keytool，无法生成证书。");
            LOGGER.error("如果服务端使用精简 JRE，请手动生成证书放到 config/{}", STORE_FILE);
            logManualCommand(store);
            return false;
        }

        List<String> cmd = new ArrayList<>(List.of(
                keytool.toString(), "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "3650",
                "-storetype", "PKCS12",
                "-keystore", store.toAbsolutePath().toString(),
                "-storepass", new String(STORE_PASS),
                "-keypass", new String(STORE_PASS),
                "-dname", "CN=AE2 Online, OU=Mod, O=AE2Online, L=LAN, ST=LAN, C=CN"
        ));

        // SAN：包含本机所有可用 IPv4，避免浏览器因地址不匹配直接拒绝
        String san = buildSan();
        if (san != null) {
            cmd.add("-ext");
            cmd.add(san);
        }

        try {
            Files.createDirectories(store.getParent());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                LOGGER.error("keytool 生成证书超时");
                return false;
            }
            if (p.exitValue() != 0) {
                LOGGER.error("keytool 生成证书失败（退出码 {}）:\n{}", p.exitValue(), out.toString().trim());
                logManualCommand(store);
                return false;
            }
            if (!Files.exists(store) || Files.size(store) == 0) {
                LOGGER.error("keytool 未产出证书文件");
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("调用 keytool 失败", e);
            logManualCommand(store);
            return false;
        }
    }

    /** 构造 subjectAltName 参数值 */
    private static String buildSan() {
        Set<String> entries = new LinkedHashSet<>();
        entries.add("IP:127.0.0.1");
        entries.add("DNS:localhost");
        for (String ip : NetworkAddress.localIpv4Addresses()) {
            if (!ip.equals("127.0.0.1")) {
                entries.add("IP:" + ip);
            }
        }
        return "SAN=" + String.join(",", entries);
    }

    /** 在 java.home/bin 下查找 keytool */
    private static Path findKeytool() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) return null;
        Path bin = Paths.get(javaHome, "bin");
        for (String name : new String[]{"keytool", "keytool.exe"}) {
            Path p = bin.resolve(name);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                return p;
            }
        }
        return null;
    }

    private static void logManualCommand(Path store) {
        LOGGER.error("可手动执行以下命令生成证书（把 IP 换成你的局域网地址）：");
        LOGGER.error("  keytool -genkeypair -alias {} -keyalg RSA -keysize 2048 -validity 3650 \\", ALIAS);
        LOGGER.error("    -storetype PKCS12 -keystore \"{}\" \\", store.toAbsolutePath());
        LOGGER.error("    -storepass {} -keypass {} -dname \"CN=AE2 Online\" \\", new String(STORE_PASS), new String(STORE_PASS));
        LOGGER.error("    -ext \"SAN=IP:127.0.0.1,DNS:localhost,IP:<你的局域网IP>\"");
    }

    /** 诊断用：返回证书文件的绝对路径 */
    public static Path storePath(Path configDir) {
        return configDir.resolve(STORE_FILE);
    }
}
