package com.zkyne.business.collector;

import java.util.List;
import java.util.Map;

/**
 * @className: ICollector
 * @description:
 * @author: zkyne
 * @date: 2020/10/12 14:10
 * @see <a href=""></a>
 */
public interface ICollector {
    /**
     * 收集数据
     * @param excuteSql
     * @return
     */
    List<Map<String, Object>> collectData(String excuteSql);

}
