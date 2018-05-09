package com.ztbsuper.dingding;

import com.alibaba.fastjson.JSONObject;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * <p>Title:        TRS WCM</p>
 * <p>Copyright:    Copyright (c) 2004</p>
 * <p>Company:      www.trs.com.cn</p>
 * @author  作者: admin E-mail: chu.chuanbao@trs.com.cn
 * 创建时间：2018/5/8 16:56
 * @version 1.0
 */
public class DingdingServiceImpl implements DingdingService {

    private Logger logger = LoggerFactory.getLogger(DingdingService.class);

    private String jenkinsURL;

    private boolean onStart;

    private boolean onSuccess;

    private boolean onFailed;

    private String GITLOG;

    private String VersionInfo;

    private String projectIndexURL;

    private String appDownloadURL;

    private TaskListener listener;

    private AbstractBuild build;

    private static final String API_URL = "https://oapi.dingtalk.com/robot/send?access_token=";

    private String api;

    public DingdingServiceImpl(String jenkinsURL, String token, String projectIndexURL, String appDownloadURL, String GITLOG, String VersionInfo, boolean onStart, boolean onSuccess, boolean onFailed, TaskListener listener, AbstractBuild build) {
        this.jenkinsURL = jenkinsURL;
        this.onStart = onStart;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.listener = listener;
        this.build = build;
        this.api = API_URL + token;

        this.projectIndexURL = projectIndexURL;
        this.appDownloadURL = appDownloadURL;
        this.GITLOG = GITLOG;
        this.VersionInfo = VersionInfo;
    }

    @Override
    public void start() {
        String pic = "http://icon-park.com/imagefiles/loading7_gray.gif";
        String title = String.format("%s%s开始构建", build.getProject().getDisplayName(), build.getDisplayName());
        String content = String.format("项目[%s%s]开始构建", build.getProject().getDisplayName(), build.getDisplayName());

        String link = getBuildUrl();
        if (onStart) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }

    }

    private String getBuildUrl() {
        if (jenkinsURL.endsWith("/")) {
            return jenkinsURL + build.getUrl();
        } else {
            return jenkinsURL + "/" + build.getUrl();
        }
    }

    @Override
    public void success() {
        String title = String.format("%s%s构建成功", build.getProject().getDisplayName(), build.getDisplayName());
        String content = "# 项目[%s%s]构建成功\n" +
        "##### 主页地址：[%s](%s)\n" +
        "##### 下载地址：[%s](%s)\n"+
        "##### 构建时间：%s\n";
        content = String.format(content,
                build.getProject().getDisplayName(), build.getDisplayName(),
                projectIndexURL.length()>36?(projectIndexURL.substring(0,33)+"..."):projectIndexURL,
                projectIndexURL,
                appDownloadURL.length()>36?(appDownloadURL.substring(0,33)+"..."):appDownloadURL,
                appDownloadURL,
                build.getDurationString()
        );

        if (VersionInfo!=null&&VersionInfo.length()>0){
            content = content + "##### 版本信息："+VersionInfo+"\n";
        }

        if (GITLOG!=null&&GITLOG.length()>0){
            content = content + "##### 更新日志："+GITLOG+"\n";
        }
        
        if (onSuccess) {
            sendMarkdownMessage(content, title);
        }
    }

    @Override
    public void failed() {
        String pic = "http://www.iconsdb.com/icons/preview/soylent-red/x-mark-3-xxl.png";
        String title = String.format("%s%s构建失败", build.getProject().getDisplayName(), build.getDisplayName());
        String content = String.format("项目[%s%s]构建失败, summary:%s, duration:%s", build.getProject().getDisplayName(), build.getDisplayName(), build.getBuildStatusSummary().message, build.getDurationString());

        String link = getBuildUrl();
        logger.info(link);
        if (onFailed) {
            logger.info("send link msg from " + listener.toString());
            sendLinkMessage(link, content, title, pic);
        }
    }

    private void sendTextMessage(String msg) {

    }

    private void sendLinkMessage(String link, String msg, String title, String pic) {
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(api);

        JSONObject body = new JSONObject();
        body.put("msgtype", "link");


        JSONObject linkObject = new JSONObject();
        linkObject.put("text", msg);
        linkObject.put("title", title);
        linkObject.put("picUrl", pic);
        linkObject.put("messageUrl", link);

        body.put("link", linkObject);
        try {
            post.setRequestEntity(new StringRequestEntity(body.toJSONString(), "application/json", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("build request error", e);
        }
        try {
            client.executeMethod(post);
            logger.info(post.getResponseBodyAsString());
        } catch (IOException e) {
            logger.error("send msg error", e);
        }
        post.releaseConnection();
    }

    private void sendMarkdownMessage(String msg, String title) {
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(api);

        JSONObject body = new JSONObject();
        body.put("msgtype", "markdown");

        JSONObject linkObject = new JSONObject();
        linkObject.put("text", msg);
        linkObject.put("title", title);

        body.put("markdown", linkObject);
        try {
            post.setRequestEntity(new StringRequestEntity(body.toJSONString(), "application/json", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("build request error", e);
        }
        try {
            client.executeMethod(post);
            logger.info(post.getResponseBodyAsString());
        } catch (IOException e) {
            logger.error("send msg error", e);
        }
        post.releaseConnection();
    }


    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.proxy != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            if (proxy != null && client.getHostConfiguration() != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }
}
