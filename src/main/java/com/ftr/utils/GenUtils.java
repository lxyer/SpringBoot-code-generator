package com.ftr.utils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.ftr.entity.ColumnEntity;
import com.ftr.entity.TableEntity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 * 
 * @author 郭光辉
 * @date 2017年8月10日 12:11:30
 */
public class GenUtils {
	//从自定义字典表字段备注中获取值选项的匹配正则
	private static Pattern decimalPattern = Pattern.compile("\\d+");

	//从内置字典表字段备注中获取字典表编码的匹配正则
	private static Pattern dicPattern = Pattern.compile("字典表\\((([_A-Z]?)+)\\)");

	public static List<String> getTemplates(){
		List<String> templates = new ArrayList<String>();
		templates.add("template/Entity.java.vm");
		templates.add("template/Dao.java.vm");
		templates.add("template/Dao.xml.vm");
		templates.add("template/Service.java.vm");
		templates.add("template/ServiceImpl.java.vm");
		templates.add("template/Controller.java.vm");
		templates.add("template/SaveVo.java.vm");
		templates.add("template/SearchVo.java.vm");
		templates.add("template/UpdateVo.java.vm");
		/*templates.add("template/list.html.vm");
        templates.add("template/list.js.vm");
        templates.add("template/menu.sql.vm");*/
		return templates;
	}
	
	/**
	 * 生成代码
	 */
	public static void generatorCode(Map<String, String> table,
			List<Map<String, String>> columns, String moduleName,String author,String email, ZipOutputStream zip){
		//配置信息
		Configuration config = getConfig();
		
		//表信息
		TableEntity tableEntity = new TableEntity();
		tableEntity.setTableName(table.get("tableName"));
		tableEntity.setComments(table.get("tableComment"));
		//表名转换成Java类名
		String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
		tableEntity.setClassName(className);
		tableEntity.setClassname(StringUtils.uncapitalize(className));

		boolean hasBigDecimal = false;
		//列信息
		List<ColumnEntity> columsList = new ArrayList<ColumnEntity>();

		for(Map<String, String> column : columns){
			ColumnEntity columnEntity = new ColumnEntity();
			columnEntity.setColumnName(column.get("columnName"));
			columnEntity.setDataType(column.get("dataType"));
			columnEntity.setComments(column.get("columnComment"));
			columnEntity.setExtra(column.get("extra"));
			columnEntity.setColumnType(column.get("columnType"));
			
			//列名转换成Java属性名
			String attrName = columnToJava(columnEntity.getColumnName());
			columnEntity.setAttrName(attrName);
			columnEntity.setAttrname(StringUtils.uncapitalize(attrName));
			
			//列的数据类型，转换成Java类型
			//String attrType = config.getString(columnEntity.getDataType(), "unknowType");
			String attrType = config.getString(columnEntity.getDataType(), "String");
			columnEntity.setAttrType(attrType);

			columnEntity.setCharLength(getIntValue(column.get("charLength"), 0));
			setColumnEntitytExample(columnEntity, attrType, config);

			//是否主键
			if("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null){
				tableEntity.setPk(columnEntity);
			}

			if("BigDecimal".equals(attrType)) {
				hasBigDecimal = true;
			}
			
			columsList.add(columnEntity);
		}
		tableEntity.setColumns(columsList);
		
		//没主键，则第一个字段为主键
		if(tableEntity.getPk() == null){
			tableEntity.setPk(tableEntity.getColumns().get(0));
		}
		
		//设置velocity资源加载器
		Properties prop = new Properties();  
		prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init(prop);

		String packageName = config.getString("package");

		if(StringUtils.isNotEmpty(moduleName)) {
			packageName += "." + moduleName;
		}
		
		//String author = config.getString("author");
		//String email = config.getString("email");
		//封装模板数据
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("tableName", tableEntity.getTableName());
		map.put("comments", tableEntity.getComments());
		map.put("pk", tableEntity.getPk());
		map.put("className", tableEntity.getClassName());
		map.put("classname", tableEntity.getClassname());
		map.put("pathName", tableEntity.getClassname().toLowerCase());
		map.put("columns", tableEntity.getColumns());
		map.put("package", packageName);
		map.put("author", author);
		map.put("email", email);
		map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
		map.put("hasBigDecimal", hasBigDecimal);
        VelocityContext context = new VelocityContext(map);
        
        //获取模板列表
		List<String> templates = getTemplates();
		for(String template : templates){
			//渲染模板
			StringWriter sw = new StringWriter();
			Template tpl = Velocity.getTemplate(template, "UTF-8");
			tpl.merge(context, sw);
			
			try {
				//添加到zip
				zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString("package"), moduleName)));
				IOUtils.write(sw.toString(), zip, "UTF-8");
				IOUtils.closeQuietly(sw);
				zip.closeEntry();
			} catch (IOException e) {
				throw new RRException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
			}
		}
	}

	private static int getIntValue(String value, int defaultV) {
		if (StringUtils.isEmpty(value)) {
			return defaultV;
		}
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return defaultV;
		}
	}

	/**
	 * 设置属性示例值
	 */
	private static  void setColumnEntitytExample(ColumnEntity columnEntity, String attrType,  Configuration config) {
		String example = null;
		Matcher matcher;

		if ("Date".equals(attrType)) {
			example = Long.toString(new Date().getTime());
		} else if ("String".equals(attrType)) {
			//处理内置字典表
			if("varchar(50)".equals(columnEntity.getColumnType()) && (matcher = dicPattern.matcher( columnEntity.getComments())).find()) {
				example = matcher.group(1) + "-1";
			} else {
				example = "示例：" + columnEntity.getComments();
			}
		} else {
			String columnName = columnEntity.getColumnName();

			//时间格式以long方式存储
			if("Long".equals(attrType) && (columnName.toLowerCase().contains("time") || columnName.toLowerCase().contains("date"))) {
				example = Long.toString(new Date().getTime());
			}

			//处理自定义字典
			if("int".equals(columnEntity.getDataType()) || "tinyint".equals(columnEntity.getDataType()) ) {
				try{
					matcher = decimalPattern.matcher(columnEntity.getComments());

					if (matcher.find()) {
						example = matcher.group(0);
					}
				} catch (Exception e) {

				}
			}

			if("BigDecimal".equals(attrType)) {
				String columnType = columnEntity.getColumnType();
				columnType = columnType.substring(8, columnType.length() - 1);

				String[] lens = columnType.split(",");

				String str = "";

				for(int i = 0; i < (Integer.parseInt(lens[0]) - Integer.parseInt(lens[1])); i++) {
					str += 9 - i % 10;
				}
				str += ".";
				for(int i = 0; i < Integer.parseInt(lens[1]); i++) {
					str += i%9 + 1;
				}

				example = new BigDecimal(str).toString();
			}
		}

		if(StringUtils.isEmpty(example)) {
			example = config.getString("example." + attrType, columnEntity.getComments());
		}

		//列示例值
		columnEntity.setExample(example);
	}
	
	/**
	 * 列名转换成Java属性名
	 */
	public static String columnToJava(String columnName) {
		return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
	}
	
	/**
	 * 表名转换成Java类名
	 */
	public static String tableToJava(String tableName, String tablePrefix) {
		if(StringUtils.isNotBlank(tablePrefix)){
			tableName = tableName.replace(tablePrefix, "");
		}
		return columnToJava(tableName);
	}
	
	/**
	 * 获取配置信息
	 */
	public static Configuration getConfig(){
		try {
			return new PropertiesConfiguration("generator.properties");
		} catch (ConfigurationException e) {
			throw new RRException("获取配置文件失败，", e);
		}
	}
	
	/**
	 * 获取文件名
	 */
	public static String getFileName(String template, String className, String packageName, String moduleName){
		String packagePath = "main" + File.separator + "java" + File.separator;
		if(StringUtils.isNotBlank(packageName)){
			packagePath += packageName.replace(".", File.separator) + File.separator;
		}

		if(StringUtils.isNotEmpty(moduleName)) {
			packagePath += moduleName.replace(".", File.separator) + File.separator;
		}
		
		if(template.contains("Entity.java.vm")){
			return packagePath + "entity" + File.separator + className + "Entity.java";
		}
		
		if(template.contains("Dao.java.vm")){
			return packagePath + "dao" + File.separator + className + "Dao.java";
		}

		if(template.contains("Service.java.vm")){
			return packagePath + "service" + File.separator + className + "Service.java";
		}
		
		if(template.contains("ServiceImpl.java.vm")){
			return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
		}
		
		if(template.contains("Controller.java.vm")){
			return packagePath + "controller" + File.separator + className + "Controller.java";
		}

		if(template.contains("Dao.xml.vm")){
			return "main" + File.separator + "resources" + File.separator + "mapper" +  File.separator + moduleName + File.separator  + className + "Dao.xml";
		}

		if(template.contains("SaveVo.java.vm")){
            return packagePath + "vo" + File.separator + className + "SaveVo.java";
        }
		
		if(template.contains("SearchVo.java.vm")){
            return packagePath + "vo" + File.separator + className + "SearchVo.java";
        }
		
		if(template.contains("UpdateVo.java.vm")){
            return packagePath + "vo" + File.separator + className + "UpdateVo.java";
        }
		
		return null;
	}
}
