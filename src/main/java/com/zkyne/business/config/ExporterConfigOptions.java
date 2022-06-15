package com.zkyne.business.config;

import com.google.common.collect.Lists;
import com.zkyne.business.collector.DefaultCollector;
import com.zkyne.business.collector.ICollector;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * @className: ExporterConfigOptions
 * @description:
 * @author: zkyne
 * @date: 2020/11/18 9:51
 * @see <a href=""></a>
 */
@ConfigurationProperties(prefix = "spring.business.exporter")
public class ExporterConfigOptions {

    /**
     * 是否启用内置http服务对外暴露数据
     */
    private boolean builtInHttpEnabled = false;
    /**
     * 指标采集客户端配置
     */
    private ExporterClientOptions client;
    /**
     * 是否自动将采集相关配置同步到prometheus服务端,默认不开启同步
     */
    private boolean syncConfigToServerEnabled = false;

    /**
     * 指标采集同步到服务端prometheus配置
     */
    private PrometheusOptions prometheus;
    /**
     * 指标采集配置
     */
    private Map<String, MetricConfigOptions> metrics;

    public ExporterConfigOptions() {

    }

    private ExporterConfigOptions(Builder builder) {
        this.builtInHttpEnabled = builder.builtInHttpEnabled;
        this.client = builder.client;
        this.syncConfigToServerEnabled = builder.syncConfigToServerEnabled;
        this.prometheus = builder.prometheus;
        metrics = new HashMap<>();
        if(builder.metrics == null || builder.metrics.isEmpty()){
            return;
        }
        for (MetricConfigOptions metric : builder.metrics) {
            metrics.put(metric.name, metric);
        }
        if(this.builtInHttpEnabled && this.client == null){
            throw new IllegalArgumentException("Exporter Config error, when builtInHttpEnabled is true, the client must not be null");
        }
        if(this.syncConfigToServerEnabled && this.prometheus == null){
            throw new IllegalArgumentException("Exporter Config error, when syncConfigToServerEnabled is true, but prometheus must not be null");
        }
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean builtInHttpEnabled = false;

        private ExporterClientOptions client;

        private boolean syncConfigToServerEnabled = false;

        private PrometheusOptions prometheus;

        private List<MetricConfigOptions> metrics;

        public Builder builtInHttpEnabled(boolean builtInHttpEnabled){
            this.builtInHttpEnabled = builtInHttpEnabled;
            return this;
        }
        public Builder client(ExporterClientOptions client){
            this.client = client;
            return this;
        }

        public Builder syncConfigToServerEnabled(boolean syncConfigToServerEnabled){
            this.syncConfigToServerEnabled = syncConfigToServerEnabled;
            return this;
        }

        public Builder prometheus(PrometheusOptions prometheus){
            this.prometheus = prometheus;
            return this;
        }

        public Builder metrics(List<MetricConfigOptions> metrics){
            this.metrics = metrics;
            return this;
        }

        public ExporterConfigOptions build(){
            return new ExporterConfigOptions(this);
        }

    }

    public static class PrometheusOptions {
        /**
         * prometheus服务的namespace
         */
        private String namespace;

        /**
         * prometheus采集任务名称,默认UUID生成
         */
        private String jobName = UUID.randomUUID().toString().replaceAll("-", "");
        /**
         * prometheus采集间隔,格式同prometheus的scrape_interval配置,默认为1m
         */
        private String scrapeInterval="1m";
        /**
         * prometheus采集超时,格式同prometheus的scrape_timeout配置,默认为15s
         */
        private String scrapeTimeout = "15s";
        /**
         * prometheus静态采集配置的targets
         */
        private String targets;

        public PrometheusOptions() {

        }

        private PrometheusOptions(Builder builder) {
            this.namespace = builder.namespace;
            this.jobName = builder.jobName;
            this.scrapeInterval = builder.scrapeInterval;
            this.scrapeTimeout = builder.scrapeTimeout;
            this.targets = builder.targets;
        }
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String namespace;
            private String jobName = UUID.randomUUID().toString().replaceAll("-", "");
            private String scrapeInterval="1m";
            private String scrapeTimeout = "15s";
            private String targets;

            public Builder namespace(String namespace){
                this.namespace = namespace;
                return this;
            }
            public Builder jobName(String jobName){
                this.jobName = jobName;
                return this;
            }

            public Builder scrapeInterval(String scrapeInterval){
                this.scrapeInterval = scrapeInterval;
                return this;
            }

            public Builder scrapeTimeout(String scrapeTimeout){
                this.scrapeTimeout = scrapeTimeout;
                return this;
            }

            public Builder targets(String targets){
                this.targets = targets;
                return this;
            }

            public PrometheusOptions build(){
                return new PrometheusOptions(this);
            }

        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getScrapeInterval() {
            return scrapeInterval;
        }

        public void setScrapeInterval(String scrapeInterval) {
            this.scrapeInterval = scrapeInterval;
        }

        public String getScrapeTimeout() {
            return scrapeTimeout;
        }

        public void setScrapeTimeout(String scrapeTimeout) {
            this.scrapeTimeout = scrapeTimeout;
        }

        public String getTargets() {
            return targets;
        }

        public void setTargets(String targets) {
            this.targets = targets;
        }
    }

    public static class ExporterClientOptions{
        /**
         * prometheus采集路径,默认已支持"/metric"
         */
        private String requestUri;
        /**
         * 内置http服务prometheus数据采集端口号,默认9093
         */
        private int clientPort = 9093;

        public ExporterClientOptions() {

        }

        private ExporterClientOptions(Builder builder) {
            this.requestUri = builder.requestUri;
            this.clientPort = builder.clientPort;
        }
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String requestUri;
            private int clientPort = 9093;

            public Builder requestUri(String requestUri){
                this.requestUri = requestUri;
                return this;
            }
            public Builder clientPort(int clientPort){
                this.clientPort = clientPort;
                return this;
            }

            public ExporterClientOptions build(){
                return new ExporterClientOptions(this);
            }

        }

        public String getRequestUri() {
            return requestUri;
        }

        public void setRequestUri(String requestUri) {
            this.requestUri = requestUri;
        }

        public int getClientPort() {
            return clientPort;
        }

        public void setClientPort(int clientPort) {
            this.clientPort = clientPort;
        }
    }


    public static class MetricConfigOptions{

        /**
         * 是否启用自定义底层数据采集,默认为false,直接使用默认采集器直接通过excuteSql查询采集
         */
        private boolean customCollectorEnabled = false;
        /**
         * 自定义数据采集器
         */
        private Class<? extends ICollector> customCollector = DefaultCollector.class;

        /**
         * 采集数据sql,当未启用自定义Collector时需要配置
         */
        private String excuteSql;

        /**
         * 指标名称,必传且不重复
         */
        private String name;
        /**
         * 指标描述信息,非必传,默认为help
         */
        private String help = "help";
        /**
         * 指标值对应的key,指的是源数据Map中哪个key的值作为该指标的指标值,不配置,指标值默认为1
         */
        private String valueKey;
        /**
         * 指标的label对应的keys,指的是以源数据map中选取哪些key作为指标的label
         */
        private Set<String> tagKeys;

        public MetricConfigOptions() {
        }

        private MetricConfigOptions(Builder builder) {
            this.customCollectorEnabled = builder.customCollectorEnabled;
            this.customCollector = builder.customCollector;
            this.excuteSql = builder.excuteSql;
            this.name = builder.name;
            this.help = builder.help;
            this.valueKey = builder.valueKey;
            this.tagKeys = builder.tagKeys;
            if(this.name == null || "".equals(this.name.trim())){
                throw new IllegalArgumentException("Exporter Config error, the metric name must not be null or blank");
            }
            if(this.customCollectorEnabled){
                if(this.customCollector == DefaultCollector.class){
                    throw new IllegalArgumentException("Exporter Config error, when customCollectorEnabled is true, the customCollector must not be DefaultCollector.class");
                }
            }else {
                if(this.excuteSql == null || "".equals(this.excuteSql.trim())){
                    throw new IllegalArgumentException("Exporter Config error, when customCollectorEnabled is false, the excuteSql must not be null or blank");
                }
            }
        }

        public static Builder builder() {
            return new Builder();
        }
        public static class Builder {
            private boolean customCollectorEnabled = false;
            private Class<? extends ICollector> customCollector = DefaultCollector.class;
            private String excuteSql;
            private String name;
            private String help = "help";
            private String valueKey;
            private Set<String> tagKeys;

            public Builder customCollectorEnabled(boolean customCollectorEnabled){
                this.customCollectorEnabled = customCollectorEnabled;
                return this;
            }

            public Builder customCollector(Class<? extends ICollector> customCollector){
                this.customCollector = customCollector;
                return this;
            }
            public Builder excuteSql(String excuteSql){
                this.excuteSql = excuteSql;
                return this;
            }
            public Builder name(String name){
                this.name = name;
                return this;
            }
            public Builder help(String help){
                this.help = help;
                return this;
            }
            public Builder valueKey(String valueKey){
                this.valueKey = valueKey;
                return this;
            }
            public Builder tagKeys(Set<String> tagKeys){
                this.tagKeys = tagKeys;
                return this;
            }

            public MetricConfigOptions build(){
                return new MetricConfigOptions(this);
            }

        }

        public boolean isCustomCollectorEnabled() {
            return customCollectorEnabled;
        }

        public void setCustomCollectorEnabled(boolean customCollectorEnabled) {
            this.customCollectorEnabled = customCollectorEnabled;
        }

        public Class<? extends ICollector> getCustomCollector() {
            return customCollector;
        }

        public void setCustomCollector(Class<? extends ICollector> customCollector) {
            this.customCollector = customCollector;
        }

        public String getExcuteSql() {
            return excuteSql;
        }

        public void setExcuteSql(String excuteSql) {
            this.excuteSql = excuteSql;
        }

        public String getName() {
            if(name != null){
                return name.trim();
            }
            return null;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHelp() {
            if(help == null){
                return "help";
            }
            return help;
        }

        public void setHelp(String help) {
            this.help = help;
        }

        public String getValueKey() {
            return valueKey;
        }

        public void setValueKey(String valueKey) {
            this.valueKey = valueKey;
        }

        public List<String> getTagKeys() {
            if(tagKeys == null){
                return Lists.newArrayList();
            }
            return Lists.newArrayList(tagKeys);
        }

        public void setTagKeys(Set<String> tagKeys) {
            this.tagKeys = tagKeys;
        }

    }

    public boolean isBuiltInHttpEnabled() {
        return builtInHttpEnabled;
    }

    public void setBuiltInHttpEnabled(boolean builtInHttpEnabled) {
        this.builtInHttpEnabled = builtInHttpEnabled;
    }

    public ExporterClientOptions getClient() {
        return client;
    }

    public void setClient(ExporterClientOptions client) {
        this.client = client;
    }

    public boolean isSyncConfigToServerEnabled() {
        return syncConfigToServerEnabled;
    }

    public void setSyncConfigToServerEnabled(boolean syncConfigToServerEnabled) {
        this.syncConfigToServerEnabled = syncConfigToServerEnabled;
    }

    public PrometheusOptions getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(PrometheusOptions prometheus) {
        this.prometheus = prometheus;
    }

    public Map<String, MetricConfigOptions> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, MetricConfigOptions> metrics) {
        this.metrics = metrics;
    }
}
