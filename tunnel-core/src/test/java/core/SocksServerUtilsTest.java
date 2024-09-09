package core;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
class SocksServerUtilsTest {

    @Test
    @SneakyThrows
    void startJdkHttpClient() {
        ProxySelector proxySelector = new ProxySelector() {
            private List<Proxy> proxies;

            {
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 30808));
                proxies = Collections.singletonList(proxy);
            }

            @Override
            public List<Proxy> select(URI uri) {
                return proxies;
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                throw new RuntimeException(ioe);
            }
        };

        try (HttpClient client = HttpClient.newBuilder()
//            .version(HttpClient.Version.HTTP_1_1)
            .proxy(proxySelector)
            .build();) {
            HttpRequest request = HttpRequest
                .newBuilder(new URI("https://www.baidu.com/"))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String verificationText = "Example Domain";
            // Should print "true" otherwise the request failed
            System.out.println(response.body().contains(verificationText));
        }


    }

    @Test
    void xx() {
        String[] a = StringUtils.delimitedListToStringArray("1,2,3", ",");
        String[] b = StringUtils.delimitedListToStringArray("1,2,3,", ",");
        String[] c = StringUtils.delimitedListToStringArray(",3,", ",");
        String[] d = StringUtils.delimitedListToStringArray("3,", ",");
        String[] e = StringUtils.delimitedListToStringArray(",3", ",");
        String[] f = StringUtils.delimitedListToStringArray("3", ",");
        String[] g = StringUtils.delimitedListToStringArray(",", ",");
        String[] h = StringUtils.delimitedListToStringArray("", ",");
        String[] i = StringUtils.delimitedListToStringArray(" ", ",");

        log.info("i:{}", Arrays.toString(i));

    }

    @Test
    void testApacheHttpClient() {
        //创建连接池管理器
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //设置最大连接
        cm.setMaxTotal(100);
        //设置每个主机（域名）的最大连接数（并发量大时，避免某一个使用过大而其他过小）
        cm.setDefaultMaxPerRoute(10);

        //通过连接池管理器获取链接
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        HttpPost httpPost = new HttpPost("https://www.baidu.com");

        //参数定义
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("keyword", "电影"));

        try {
            //创建提交的表单对象
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);

            httpPost.setEntity(formEntity);
            //发起请求
            httpClient.execute(httpPost, new HttpClientResponseHandler<Void>() {
                @Override
                public Void handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
                    if (classicHttpResponse.getCode() != 200) {
                        log.error("none 200 ok found");
                    }
                    HttpEntity entity = classicHttpResponse.getEntity();
                    String html = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    System.out.println(html);
                    return null;
                }
            });

        } catch (Exception e) {
            log.error("send request failed");
        } finally {

        }

    }
}