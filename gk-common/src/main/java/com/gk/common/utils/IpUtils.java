package com.gk.common.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * IP地址
 *
 * @author Lowen
 */
public class IpUtils {
	private static Logger logger = LoggerFactory.getLogger(IpUtils.class);

	/**
	 * 获取IP地址
	 *
	 * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
	 * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
	 */
	public static String getIpAddr(HttpServletRequest request) {
	    String unknown = "unknown";
    	String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StrUtil.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StrUtil.isEmpty(ip) || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StrUtil.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StrUtil.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StrUtil.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
        	logger.error("IPUtils ERROR ", e);
        }
        return ip;
    }

    /**
     * 获取客户端IP
     * @param request 请求
     * @return IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String unknown = "unknown";
        // Cloudflare CDN
        String ip = request.getHeader("cf-connecting-ip");
        if (StringUtils.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
            // 标准代理头，多级代理链
            ip = request.getHeader("x-forwarded-for");
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim(); // 取第一个
            }
        }
        if (StringUtils.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
            // Nginx 常用头
            ip = request.getHeader("x-real-ip");
        }
        if (StringUtils.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }



    /**
     * 获取IP地址
     * <p>getWebIpAddr
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getWebIpAddr(HttpServletRequest request) {
        String unknown = "unknown";
        String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isEmpty(ip) || unknown.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (StringUtils.isNotEmpty(ip) && ip.length() > 15) { //"***.***.***.***".length() = 15
                if (ip.indexOf(",") > 0) {
                    ip = ip.substring(0, ip.indexOf(","));
                }
            }
        } catch (Exception e) {
            logger.error("IPUtils ERROR ", e);
        }
        return ip;
    }

    /**
     * 根据IP,及可用Ip列表来判断ip是否包含在白名单之中
     *
     * @param ip     ip address
     * @param ipList ip address list
     * @return contain return true
     */
    public static boolean checkContainIp(String ip, Set<String> ipList) {
        if (ipList.contains(ip)) {
            return true;
        }
        return false;
    }

    /**
     * 根据IP白名单设置获取可用的IP范围
     *
     * @param allowIp 192.168.1.*
     * @return 192.168.1.1 - 192.168.1.255
     */
    public static String getAllowIpRange(String allowIp) {
        if (allowIp.contains("*")) {
            String[] ips = allowIp.split("\\.");
            String[] from = new String[]{"0", "0", "0", "0"};
            String[] end = new String[]{"255", "255", "255", "255"};
            List<String> tem = new ArrayList<>();
            for (int i = 0; i < ips.length; i++) {
                if (ips[i].contains("*")) {
                    tem = complete(ips[i]);
                    from[i] = null;
                    end[i] = null;
                } else {
                    from[i] = ips[i];
                    end[i] = ips[i];
                }
            }
            StringBuilder fromIP = new StringBuilder();
            StringBuilder endIP = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (from[i] != null) {
                    fromIP.append(from[i]).append(".");
                    endIP.append(end[i]).append(".");
                } else {
                    fromIP.append("[*].");
                    endIP.append("[*].");
                }
            }
            fromIP.deleteCharAt(fromIP.length() - 1);
            endIP.deleteCharAt(endIP.length() - 1);

            for (String s : tem) {
                String ip = fromIP.toString().replace("[*]",
                        s.split(";")[0]) + "-" + endIP.toString().replace("[*]",
                        s.split(";")[1]);
                if (validate(ip)) {
                    return ip;
                }
            }
        }

        return allowIp;
    }

    /**
     * 在添加至白名单时进行格式校验
     *
     * @param ip ip address
     * @return success true
     */
    public static boolean validate(String ip) {
        for (String s : ip.split("-")) {
            if (!PATTERN.matcher(s.trim()).matches()) {
                return false;
            }
        }

        return true;
    }

    /**
     * IP的正则
     */
    public static final Pattern PATTERN = Pattern.compile("(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
            + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
            + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
            + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");

    /**
     * 对单个IP节点进行范围限定
     *
     * @param arg ip address cell
     * @return 返回限定后的IP范围，格式为List[10;19, 100;199]
     */
    private static List<String> complete(String arg) {
        List<String> com = new ArrayList<>();
        if (arg.length() == 1) {
            com.add("0;255");
        } else if (arg.length() == 2) {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }

            String s2 = complete(arg, 2);
            if (s2 != null) {
                com.add(s2);
            }
        } else {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }
        }

        return com;
    }

    private static String complete(String arg, int length) {
        String from, end;
        if (length == 1) {
            from = arg.replace("*", "0");
            end = arg.replace("*", "9");
        } else {
            from = arg.replace("*", "00");
            end = arg.replace("*", "99");
        }

        if (Integer.valueOf(from) > 255) {
            return null;
        }
        if (Integer.valueOf(end) > 255) {
            end = "255";
        }

        return from + ";" + end;
    }


    /**
     * 判断是否是有效的 IPv4 地址
     */
    public static boolean isValidIpv4(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress instanceof java.net.Inet4Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断是否是有效的 IPv6 地址
     */
    public static boolean isValidIpv6(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
