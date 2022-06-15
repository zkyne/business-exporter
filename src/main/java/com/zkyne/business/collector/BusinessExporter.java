package com.zkyne.business.collector;

import com.google.common.collect.Lists;
import com.zkyne.business.config.ExporterConfigOptions;
import com.zkyne.business.config.ExporterConfigOptions.ExporterClientOptions;
import com.zkyne.business.config.ExporterConfigOptions.MetricConfigOptions;
import com.zkyne.business.http.HttpServer;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @className: BusinessExporter
 * @description:
 * @author: zkyne
 * @date: 2020/11/18 10:22
 * @see <a href=""></a>
 */
public class BusinessExporter extends Collector {

    private final ExporterConfigOptions exporterConfigOptions;
    private final JdbcTemplate jdbcTemplate;
    private static volatile HttpServer httpServer;

    @Override
    public <T extends Collector> T register(CollectorRegistry registry) {
        if(this.exporterConfigOptions.isBuiltInHttpEnabled()){
            BusinessExporter.initHttpServer(this.exporterConfigOptions.getClient());
        }
        return super.register(registry);
    }

    private double handleValue(Map<String, Object> data, String valueKey) {
        double value = 0.0D;
        if(valueKey != null && !"".equals(valueKey)){
            Object valueObj = data.get(valueKey);
            if(valueObj != null){
                String valueStr;
                if(valueObj instanceof Date){
                    Date date = (Date) valueObj;
                    valueStr = date.getTime() + "";
                }else{
                    valueStr = valueObj.toString();
                }
                value = Double.parseDouble(valueStr);
            }
        }else{
            value = 1.0D;
        }
        return value;
    }

    private List<String> bulidTagValues(Map<String, Object> data, List<String> tagKeys) {
        List<String> tagValues = Lists.newArrayList();
        if(tagKeys == null || tagKeys.isEmpty()){
            return tagValues;
        }
        for (String tagKey : tagKeys) {
            Object tagValueObj = data.get(tagKey);
            String tagValue;
            if(tagValueObj != null){
                if(tagValueObj instanceof Date){
                    Date date = (Date) tagValueObj;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                    tagValue = dateTime.format(formatter);
                }else{
                    tagValue = tagValueObj.toString();
                }
            }else{
                tagValue = "";
            }

            tagValues.add(tagValue);
        }
        return tagValues;
    }

    private static void initHttpServer(ExporterClientOptions exporterClientOptions){
        if(exporterClientOptions == null){
            throw new RuntimeException("Built in http config error");
        }
        if (BusinessExporter.httpServer == null) {
            synchronized (BusinessExporter.class) {
                if (BusinessExporter.httpServer == null) {
                    try {
                        BusinessExporter.httpServer = new HttpServer(exporterClientOptions.getClientPort(), exporterClientOptions.getRequestUri());
                    } catch (IOException e) {
                        throw new RuntimeException();
                    }
                }
            }
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> familySamples = Lists.newArrayList();
        if(this.exporterConfigOptions.getMetrics() == null){
            return familySamples;
        }
        Collection<MetricConfigOptions> metricConfigOptions = this.exporterConfigOptions.getMetrics().values();
        if(metricConfigOptions.isEmpty()){
            return familySamples;
        }
        try{
            for (MetricConfigOptions metricConfig : metricConfigOptions) {
                ICollector collector = null;
                if(!metricConfig.isCustomCollectorEnabled()){
                    Constructor<? extends ICollector> constructor =  metricConfig.getCustomCollector().getDeclaredConstructor(JdbcTemplate.class);
                    //设置允许访问，防止private修饰的构造方法
                    constructor.setAccessible(true);
                    collector = constructor.newInstance(this.jdbcTemplate);
                }else{
                    Constructor<? extends ICollector> constructor =  metricConfig.getCustomCollector().getDeclaredConstructor();
                    //设置允许访问，防止private修饰的构造方法
                    constructor.setAccessible(true);
                    collector = constructor.newInstance();
                }
                List<Map<String, Object>> originData = collector.collectData(metricConfig.getExcuteSql());
                if (originData == null || originData.isEmpty()) {
                    continue;
                }
                List<Sample> samples = Lists.newArrayList();
                for (Map<String, Object> data : originData) {
                    List<String> tagValues = bulidTagValues(data, metricConfig.getTagKeys());
                    double value = handleValue(data, metricConfig.getValueKey());
                    Sample sample = new Sample(metricConfig.getName(), metricConfig.getTagKeys(), tagValues, value);
                    samples.add(sample);
                }
                MetricFamilySamples metricFamilySamples = new MetricFamilySamples(metricConfig.getName(), Type.GAUGE, metricConfig.getHelp(), samples);
                familySamples.add(metricFamilySamples);
            }
        }catch (Exception e){
            throw new RuntimeException("Exporter collect error, error message" + e.getMessage());
        }
        return familySamples;
    }

    private BusinessExporter(Builder builder) {
        this.exporterConfigOptions = builder.exporterConfigOptions;
        this.jdbcTemplate = builder.jdbcTemplate;
        initVerify();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ExporterConfigOptions exporterConfigOptions = null;

        private JdbcTemplate jdbcTemplate = null;

        public Builder exporterConfigOptions(ExporterConfigOptions exporterConfigOptions){
            this.exporterConfigOptions = exporterConfigOptions;
            return this;
        }
        public Builder jdbcTemplate(JdbcTemplate jdbcTemplate){
            this.jdbcTemplate = jdbcTemplate;
            return this;
        }

        public BusinessExporter build(){
            return new BusinessExporter(this).register();
        }

    }


    private void initVerify() {
        if (this.exporterConfigOptions == null) {
            throw new RuntimeException("Exporter config error, ExporterConfigOptions must not be null");
        }

        if (this.exporterConfigOptions.isBuiltInHttpEnabled() && this.exporterConfigOptions.getClient() == null) {
            throw new RuntimeException("Exporter config error, when builtInHttpEnabled is true, the client config must not be null");
        }
        if(this.exporterConfigOptions.isSyncConfigToServerEnabled() && this.exporterConfigOptions.getPrometheus() == null){
            throw new RuntimeException("Exporter config error, when syncConfigToServerEnabled is true, the prometheus config must not be null");
        }
        if(this.exporterConfigOptions.getMetrics() == null){
            return;
        }
        boolean needJdbcTemplate = false;
        for (MetricConfigOptions metricConfigOptions : this.exporterConfigOptions.getMetrics().values()) {
            if(metricConfigOptions == null){
                continue;
            }
            if(metricConfigOptions.getName() == null || "".equals(metricConfigOptions.getName())){
                throw new RuntimeException("Exporter config error, the metrics config metric name must not be null or blank");
            }
            if (!metricConfigOptions.isCustomCollectorEnabled()){
                needJdbcTemplate = true;
                if (metricConfigOptions.getExcuteSql() == null || "".equals(metricConfigOptions.getExcuteSql().trim())) {
                    throw new RuntimeException("Exporter config error, when customCollectorEnabled is false, the excuteSql must not be null or blank");
                }
            }
            if(metricConfigOptions.isCustomCollectorEnabled() && metricConfigOptions.getCustomCollector() == DefaultCollector.class){
                throw new RuntimeException("Exporter config error, when customCollectorEnabled is true, the customCollector must not be DefaultCollector");
            }
        }
        if(needJdbcTemplate && this.jdbcTemplate == null){
            throw new RuntimeException("Exporter config error, when use DefaultCollector collect data, the jdbcTemplate config must not be null");
        }
    }
}
