package com.yamada;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

public class GDUTLoginUtil {

    private volatile static GDUTLoginUtil instance;

    private volatile String cookie;

    private RestTemplate restTemplate;

    private GDUTLoginUtil() {

    }

    public static GDUTLoginUtil newInstance(boolean enableSsLCheck) {
        return newInstance(null, enableSsLCheck, -1, -1);
    }

    public static GDUTLoginUtil newInstance(Proxy proxy, boolean enableSsLCheck) {
        return newInstance(proxy, enableSsLCheck, -1, -1);
    }

    /**
     *
     * @param proxy 代理类
     * @param enableSsLCheck 是否开启SSL验证，未配置证书建议使用false
     * @param connTimeout
     * @param readTimeout
     * @return
     */
    public static GDUTLoginUtil newInstance(Proxy proxy, boolean enableSsLCheck, int connTimeout, int readTimeout) {
        if (instance == null) {
            synchronized (GDUTLoginUtil.class) {
                if (instance == null) {
                    instance = new GDUTLoginUtil();
                    instance.buildRestTemplate(proxy, connTimeout, readTimeout, enableSsLCheck);
                    instance.refreshCookie();
                }
            }
        }
        return instance;
    }

    private void buildRestTemplate(Proxy proxy, int connTimeout, int readTimeout, boolean enableSslCheck) {
        restTemplate = new RestTemplate();
        // sslIgnore
        SimpleClientHttpRequestFactory requestFactory;
        if (!enableSslCheck) {
            requestFactory = getUnsafeClientHttpRequestFactory();
        } else {
            requestFactory = new SimpleClientHttpRequestFactory();
        }

        // proxy
        if (proxy != null) {
            requestFactory.setProxy(proxy);
        }

        // timeout
        requestFactory.setConnectTimeout(connTimeout);
        requestFactory.setReadTimeout(readTimeout);

        restTemplate.setRequestFactory(requestFactory);
    }

    private SimpleClientHttpRequestFactory getUnsafeClientHttpRequestFactory() {
        TrustManager[] byPassTrustManagers = new TrustManager[]{new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
            sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        return new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection,
                                             @NotNull String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(
                            sslContext.getSocketFactory());
                }
            }
        };
    }

    public static GDUTLoginUtil getInstance() {
        return instance;
    }

    public String getCookie() {
        if (cookie == null) {
            refreshCookie();
        }
        return cookie;
    }

    /**
     * 刷新cookie
     * @return
     */
    public String refreshCookie() {
        String url = "https://jxfw.gdut.edu.cn/";
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        List<String> cookies = responseEntity.getHeaders().get("Set-Cookie");
        String cookie = "";
        if (cookies != null && cookies.size() > 0) {
            cookie = cookies.get(0).split(";")[0].split("=")[1];
        }
        this.cookie = cookie;
        return cookie;
    }

    /**
     * 获取该cookie下的验证码并输出到response
     * @param response
     */
    public void getImageCode(HttpServletResponse response) {
        if (StringUtils.isNotBlank(cookie)) {
            String url = "https://jxfw.gdut.edu.cn/yzm";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cookie", "JSESSIONID=" + cookie);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            byte[] bytes = (byte[]) responseEntity.getBody();
//            将请求的验证码图片用输出流方式输出
            OutputStream out = null;
            try {
                out = response.getOutputStream();
                response.setContentType("image/jpeg");
                response.setHeader("Content-Type","image/jpeg");
                if (bytes != null) {
                    out.write(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 登录操作
     * @param account 学号
     * @param password 密码，需事先在前端做加密处理
     * @param verifycode 验证码
     * @return
     */
    public String login(String account, String password, String verifycode) {
        if (StringUtils.isNotBlank(cookie)) {
            String url = "https://jxfw.gdut.edu.cn/new/login";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cookie", "JSESSIONID=" + cookie);
            headers.add("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.add("Accept-Encoding", "gzip, deflate, br");
            headers.add("Accept-Language", "zh-CN,zh;q=0.9");
            headers.add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            headers.add("Host", "jxfw.gdut.edu.cn");
            headers.add("Origin", "https://jxfw.gdut.edu.cn");
            headers.add("Referer", "https://jxfw.gdut.edu.cn/");
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/" +
                    "537.36 (KHTML, like Gecko) Chrome/74.0.3710.0 Safari/537.36");
            headers.add("X-Requested-With", "XMLHttpRequest");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("account", account);
            params.add("pwd", password);
            params.add("verifycode", verifycode);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            return responseEntity.getBody();
        }
        return null;
    }

    /**
     * 验证登录是否成功
     * @param s login()返回的字符串
     * @return
     */
    public boolean isLoginSuccess(String s) {
        JSONObject jsonObject = JSONObject.parseObject(s);
        return (Integer) jsonObject.get("code") >= 0;
    }

    /**
     * 获取登录返回的信息
     * @param s login()返回的字符串
     * @return
     */
    public String getLoginMessage(String s) {
        JSONObject jsonObject = JSONObject.parseObject(s);
        return (String)jsonObject.get("message");
    }
}
