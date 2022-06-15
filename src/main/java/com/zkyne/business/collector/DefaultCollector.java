package com.zkyne.business.collector;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * @className: DefaultCollector
 * @description:
 * @author: zkyne
 * @date: 2020/11/17 8:47
 * @see <a href=""></a>
 */
public class DefaultCollector implements ICollector {

    private final JdbcTemplate jdbcTemplate;

    public DefaultCollector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> collectData(String excuteSql) {
        return jdbcTemplate.queryForList(excuteSql);
    }
}
