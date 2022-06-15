### 二.接入使用(spring boot)

#### step1.引入依赖

```java
 <dependency>
    <groupId>com.zkyne</groupId>
    <artifactId>business-exporter</artifactId>
    <version>1.0.0</version>
  </dependency>
```

#### step2.初始化配置

核心是需要初始化BusinessExporter(配置类的说明参考spring-boot-starter-business-exporter)

```java
@Configuration
public class ExporterAutoConfiguration {

    @Resource
    private DataSource dataSource;
    @Bean
    public BusinessExporter singleCollector(JdbcTemplate jdbcTemplate){
        // 初始化配置类
        ExporterClientOptions clientOptions = ExporterClientOptions.builder()
            .requestUri("/prometheus/metrics")
            .clientPort(9093)
            .build();
        PrometheusOptions prometheusOptions = PrometheusOptions.builder()
            .namespace("namespace")
            .jobName("jobName")
            .scrapeInterval("1m")
            .scrapeTimeout("15s")
            .targets("www.xxx.com")
            .build();
        List<MetricConfigOptions> metrics = Lists.newArrayList();
        Set<String> tagKeys = Sets.newHashSet();
        tagKeys.add("column1");
        tagKeys.add("column2");
        tagKeys.add("AliasColumn");
        tagKeys.add("column4");
        MetricConfigOptions configOptions = MetricConfigOptions.builder()
            .customCollectorEnabled(false)
            .customCollector(DefaultCollector.class)
            .excuteSql("select column1,column2,column3 as AliasColumn,column4 from tableName")
            .name("metric_name")
            .help("help")
            .valueKey("column1")
            .tagKeys(tagKeys)
            .build();
        metrics.add(configOptions);
        ExporterConfigOptions exporterConfigOptions = ExporterConfigOptions.builder()
            .builtInHttpEnabled(true)
            .client(clientOptions)
            .syncConfigToServerEnabled(false)
            .prometheus(prometheusOptions)
            .metrics(metrics)
            .build(); 
        
        return BusinessExporter.builder()
            .exporterConfigOptions(exporterConfigOptions)
            .jdbcTemplate(jdbcTemplate)
            .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSource);
    }

}
```

#### step3.对外提供prometheus服务拉取指标的接口

通过配置即可内置http服务对外暴露,如果想要指标暴露端口与项目端口一致,需要项目中对外暴露相关接口

```java
	@RequestMapping("/exporter")
    public void exporter(HttpServletResponse response) throws IOException {
        OutputStreamWriter osw = null;
        try{
            response.setContentType(TextFormat.CONTENT_TYPE_004);
            osw = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            TextFormat.write004(osw, CollectorRegistry.defaultRegistry.metricFamilySamples());
            osw.flush();
            osw.close();
        }catch (Exception e){
            //
        }finally {
            if(osw != null){
                osw.close();
            }
        }
    }
```
