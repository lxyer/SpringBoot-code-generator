package com.ftr.dao;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 * 
 * @author 郭光辉
 * @date 2017年8月10日 12:10:07
 */
public interface SysGeneratorDao {
	
	List<Map<String, Object>> queryList(Map<String, Object> map);
	
	int queryTotal(Map<String, Object> map);
	
	Map<String, String> queryTable(String tableName);
	
	List<Map<String, String>> queryColumns(String tableName);
}
