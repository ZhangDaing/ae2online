package com.zhangdaing.ae2online.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 探测本机可用于局域网访问的 IPv4 地址。
 * 用于在玩家保存终端配置后，直接把「客户端该填的地址」显示到聊天栏，
 * 省得玩家自己去 ipconfig 找 IP。
 */
public final class NetworkAddress {

    private NetworkAddress() {}

    /** 虚拟网卡/容器网卡的名称特征，这些地址对局域网玩家通常不可用，排到后面 */
    private static final Pattern VIRTUAL_HINTS = Pattern.compile(
            "(?i)(virtual|vmware|virtualbox|vethernet|hyper-v|docker|wsl|tailscale|zerotier|radmin|"
            + "hamachi|loopback|tap|tun|vpn|utun|bridge|meta|vmnet)");

    /**
     * 按「最可能是玩家需要的」顺序返回本机 IPv4 地址。
     * 优先：私有网段（192.168/10.x/172.16-31）且非虚拟网卡。
     */
    public static List<String> localIpv4Addresses() {
        List<Candidate> candidates = new ArrayList<>();
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nif == null || !nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                boolean virtual = VIRTUAL_HINTS.matcher(nif.getName() + " " + safeName(nif)).find();
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip == null || ip.isBlank()) continue;
                    boolean privateNet = isPrivate(ip);
                    // 排序权重：私有网段优先、非虚拟网卡优先
                    int score = (privateNet ? 0 : 100) + (virtual ? 10 : 0);
                    candidates.add(new Candidate(ip, score));
                }
            }
        } catch (SocketException e) {
            // 探测失败不抛给玩家，返回空列表由调用方降级处理
            return List.of();
        }
        candidates.sort((a, b) -> Integer.compare(a.score, b.score));
        List<String> result = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            if (!result.contains(c.ip)) result.add(c.ip);
        }
        return result;
    }

    private static String safeName(NetworkInterface nif) {
        try {
            String dn = nif.getDisplayName();
            return dn == null ? "" : dn;
        } catch (Exception e) {
            return "";
        }
    }

    /** 判断是否属于 RFC1918 私有网段 */
    private static boolean isPrivate(String ip) {
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private record Candidate(String ip, int score) {}
}
