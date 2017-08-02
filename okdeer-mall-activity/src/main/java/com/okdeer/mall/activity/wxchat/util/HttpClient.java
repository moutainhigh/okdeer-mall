
package com.okdeer.mall.activity.wxchat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {

	private static Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private static final String CHARSET_UTF8 = "UTF-8";

	public static String post(String url, String postData) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPost httpPost = new HttpPost(url);
		try {
			logger.info("API，POST过去的数据是：");
			logger.info(postData);
			// 得指明使用UTF-8编码，否则到API服务器XML的中文不能被成功识别
			StringEntity postEntity = new StringEntity(postData, CHARSET_UTF8);
			httpPost.addHeader("Content-Type", "text/xml");
			httpPost.setEntity(postEntity);
			HttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity, CHARSET_UTF8);
		} finally {
			httpPost.abort();
			try {
				httpclient.close();
			} catch (IOException e) {
				logger.error("关闭httpclient出错", e);
			}
		}
	}

	public static String get(String url) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpGet httpget = new HttpGet("http://httpbin.org/get");
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				HttpEntity entity = response.getEntity();
				return EntityUtils.toString(entity, CHARSET_UTF8);
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
	}

}
