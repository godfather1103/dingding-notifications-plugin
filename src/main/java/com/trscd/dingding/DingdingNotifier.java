package com.trscd.dingding;


import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Marvin on 16/8/25.
 */
public class DingdingNotifier extends Notifier {

    private String accessToken;

    private boolean onStart;

    private boolean onSuccess;

    private boolean onFailed;

    public String getJenkinsURL() {
        return jenkinsURL;
    }

    private String jenkinsURL;

    private String projectIndexURL;

    private String appDownloadURL;

    private String GITLOG;

    private String VersionInfo;

    private String customContent;

    public String getCustomContent() {
        return customContent;
    }

    public String getVersionInfo() {
        return VersionInfo;
    }

    public String getGITLOG() {
        return GITLOG;
    }

    public void setGITLOG(String GITLOG) {
        this.GITLOG = GITLOG;
    }


    public String getProjectIndexURL() {
        return projectIndexURL;
    }

    public String getAppDownloadURL() {
        return appDownloadURL;
    }

    public boolean isOnStart() {
        return onStart;
    }

    public boolean isOnSuccess() {
        return onSuccess;
    }


    public boolean isOnFailed() {
        return onFailed;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @DataBoundConstructor
    public DingdingNotifier(String accessToken, String projectIndexURL, String appDownloadURL, String GITLOG, String VersionInfo, String customContent, boolean onStart, boolean onSuccess, boolean onFailed, String jenkinsURL) {
        super();
        this.accessToken = accessToken;
        this.onStart = onStart;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.jenkinsURL = jenkinsURL;
        this.projectIndexURL = projectIndexURL;
        this.appDownloadURL = appDownloadURL;
        this.GITLOG = GITLOG;
        this.VersionInfo = VersionInfo;
        this.customContent = customContent;
    }

    public DingdingService newDingdingService(AbstractBuild build, TaskListener listener) {
        String pGitLog="";
        String pVersionInfo="";
        String pCustomContent="";
        try {
            //相关输入框的数据需要去转义一些变量
            pGitLog = build.getEnvironment(listener).expand(GITLOG);
            pVersionInfo = build.getEnvironment(listener).expand(VersionInfo);
            pCustomContent = build.getEnvironment(listener).expand(customContent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new DingdingServiceImpl(jenkinsURL, accessToken, projectIndexURL, appDownloadURL, pGitLog, pVersionInfo, pCustomContent, onStart, onSuccess, onFailed, listener, build);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }


    @Override
    public DingdingNotifierDescriptor getDescriptor() {
        return (DingdingNotifierDescriptor) super.getDescriptor();
    }

    @Extension
    public static class DingdingNotifierDescriptor extends BuildStepDescriptor<Publisher> {


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "钉钉通知器配置-TRS";
        }

        public String getDefaultURL() {
            Jenkins instance = Jenkins.getInstance();
            assert instance != null;
            if(instance.getRootUrl() != null){
                return instance.getRootUrl();
            }else{
                return "";
            }
        }

    }
}
