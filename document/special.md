### 三.特殊场景处理

#### 场景1:我的指标没办法通过一个sql就能查询出来,需要我业务中进行汇总处理

需要自己实现ICollector接口,并使用该配置即可

```java
public class TestCollector implements ICollector {

    @Override
    public List<Map<String, Object>> collectData(String s) {
        List<Map<String,Object>> maps  = Lists.newArrayList();
        // 自行获取相关数据进行组装
        Map<String, Object> map1 = Maps.newHashMap();
        map1.put("userName", "张三");
        map1.put("age", 12);
        map1.put("gender", "男");
        maps.add(map1);
        Map<String, Object> map2 = Maps.newHashMap();
        map2.put("userName", "李四");
        map2.put("age", 11);
        map2.put("gender", "男");
        return maps;
    }
}
```

```java
....
MetricConfigOptions configOptions = MetricConfigOptions.builder()
            .customCollectorEnabled(false)
            .customCollector(TestCollector.class)
            .name("metric_name")
            .help("help")
            .valueKey("column1")
            .tagKeys(tagKeys)
            .build();
....
```

#### 场景2:我的多个指标的执行sql可能分别是从不同数据源获取而来的

只需要实例化两个多个BusinessCollector,即可

```java
@Configuration
public class ExporterAutoConfiguration {

    @Resource
    private DataSource dataSourceOne;
    @Resource
    private DataSource dataSourceTwo;
    
    @Bean("exporterOne")
    public BusinessCollector exporterOne(@Qualifier("jdbcTemplateOne")JdbcTemplate jdbcTemplate){
        // 初始化配置类
        省略相关配置类初始化
            ...
        return BusinessCollector.builder()
            .exporterConfigOptions(exporterConfigOptions)
            .jdbcTemplate(jdbcTemplate)
            .build();
    }
    
    @Bean("exporterTwo")
    public BusinessCollector exporterTwo(@Qualifier("jdbcTemplateTwo")JdbcTemplate jdbcTemplate){
        // 初始化配置类
        省略相关配置类初始化
            ...
        return BusinessCollector.builder()
            .exporterConfigOptions(exporterConfigOptions)
            .jdbcTemplate(jdbcTemplate)
            .build();
    }

    @Bean("jdbcTemplateOne")
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSourceOne);
    }
    
    @Bean("jdbcTemplateTwo")
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSourceTwo);
    }

}
```
