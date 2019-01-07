package com.ftr.service;

import com.ftr.entity.CoonEntity;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.ftr.dao.SysGeneratorDao;
import com.ftr.utils.GenUtils;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 *
 * @author 郭光辉
 * @date 2017年8月10日 12:10:47
 */
@Service
public class SysGeneratorService {
	@Autowired
	private SysGeneratorDao sysGeneratorDao;
	@Autowired
	private JdbcTemplate  jdbcTemplate;

	public List<Map<String, Object>> queryList(Map<String, Object> map) throws Exception {
		//jdbc:mysql://127.0.0.1:3306/demo
		String url= (String) map.get("coon");
		String user=(String) map.get("user");
		String pwd=(String) map.get("pwd");
		//1.加载驱动程序
		Class.forName("com.mysql.jdbc.Driver");
		//2.获得数据库链接
		Connection conn= DriverManager.getConnection(url, user, pwd);
		//3.通过数据库的连接操作数据库，实现增删改查（使用Statement类）
		Statement st=conn.createStatement();
		String sql="select table_name tableName, engine, table_comment tableComment, create_time createTime from information_schema.tables " +
				"  where table_schema = (select database()) ";
		if(map.get("tableName")!=null){
			sql+="and table_name like '%"+map.get("tableName")+"%' order by create_time desc ";
		}
		if(map.get("offset")!=null && map.get("limit")!=null){
			sql+=" limit  "+map.get("offset")+", "+map.get("limit")+"";
		}

		List<Map<String, Object>> list=new ArrayList<>();
		ResultSet rs=st.executeQuery(sql);
		while (rs.next()){
			Map<String,Object> data=new HashMap<>();
			data.put("tableName",rs.getString("tableName"));
			data.put("engine",rs.getString("engine"));
			data.put("tableComment",rs.getString("tableComment"));
			data.put("createTime",rs.getString("createTime"));
			list.add(data);
		}

		//关闭资源
		rs.close();
		st.close();
		conn.close();
		return  list;
		//return sysGeneratorDao.queryList(map);
	}

	public int queryTotal(Map<String, Object> map) throws Exception {
		String url= (String) map.get("coon");
		String user=(String) map.get("user");
		String pwd=(String) map.get("pwd");
		//1.加载驱动程序
		Class.forName("com.mysql.jdbc.Driver");
		//2.获得数据库链接
		Connection conn= DriverManager.getConnection(url, user, pwd);
		//3.通过数据库的连接操作数据库，实现增删改查（使用Statement类）
		Statement st=conn.createStatement();
		String sql="select count(*) num from information_schema.tables where table_schema = (select database())";
		if(map.get("tableName")!=null){
			sql+="and table_name like '%"+map.get("tableName")+"%' order by create_time desc ";
		}
		ResultSet rs=st.executeQuery(sql);
		int num=0;
		while(rs.next()){
			num=rs.getInt("num");
		}

		return num;
		//return sysGeneratorDao.queryTotal(map);
	}

	public Map<String, String> queryTable(String tableName) {
		return sysGeneratorDao.queryTable(tableName);
	}

	public List<Map<String, String>> queryColumns(String tableName) {
		return sysGeneratorDao.queryColumns(tableName);
	}

	public byte[] generatorCode(String[] tableNames,CoonEntity entity, String moduleName
			,String author, String email) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		//1.加载驱动程序
		Class.forName("com.mysql.jdbc.Driver");
		//2.获得数据库链接
		Connection conn= DriverManager.getConnection(entity.getCoon(), entity.getUser(), entity.getPwd());
		//3.通过数据库的连接操作数据库，实现增删改查（使用Statement类）
		Statement st=conn.createStatement();

		for(String tableName : tableNames){
			String sql="select table_name tableName, engine, table_comment tableComment, create_time createTime from " +
					"   information_schema.tables where table_schema = (select database()) and table_name = '"+tableName+"'";
			ResultSet rs=st.executeQuery(sql);

			Map<String,String> map=new HashMap<>();
			while(rs.next()){
				map.put("tableName",rs.getString("tableName"));
				map.put("engine",rs.getString("engine"));
				map.put("tableComment",rs.getString("tableComment"));
				map.put("createTime",rs.getString("createTime"));
			}

			//查询表信息
			//Map<String, String> table = queryTable(tableName);

			//查询列信息
			//List<Map<String, String>> columns = queryColumns(tableName);
			String sql1="select column_name columnName, data_type dataType, column_comment columnComment, column_key columnKey, extra, "
					+ "CHARACTER_MAXIMUM_LENGTH charLength,column_type columnType from information_schema.columns\n" 
					+ " \t\t\twhere table_name = '"+tableName+"' and table_schema = (select database()) order by ordinal_position";
			ResultSet query=st.executeQuery(sql1);
			List<Map<String, String>> list=new ArrayList<>();
			while(query.next()){
				Map<String,String> data=new HashMap<>();
				data.put("columnName",query.getString("columnName"));
				data.put("dataType",query.getString("dataType"));
				data.put("columnComment",query.getString("columnComment"));
				data.put("columnKey",query.getString("columnKey"));
				data.put("extra",query.getString("extra"));
				data.put("columnType",query.getString("columnType"));
				data.put("charLength",query.getString("charLength"));
				list.add(data);
			}

			//生成代码
			GenUtils.generatorCode(map, list, moduleName, author, email, zip);
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}
}
