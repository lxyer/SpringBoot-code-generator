package com.ftr.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.ftr.entity.CoonEntity;
import com.ftr.service.SysGeneratorService;
import com.ftr.utils.PageUtils;
import com.ftr.utils.Query;
import com.ftr.utils.R;

/**
 * 代码生成器
 * 
 * @author 郭光辉
 * @date 2017年8月10日 12:09:29
 */
@Controller
@RequestMapping("/sys/generator")
public class SysGeneratorController {
	@Autowired
	private SysGeneratorService sysGeneratorService;
	
    private static final Logger logger = LoggerFactory.getLogger(SysGeneratorController.class);
	
	/**用于文件下载的常量对象*/
	public static final String  USER_AGENT =  "User-Agent";
	public  static final String  MSIE= "MSIE";
	
    
    
	/**
	 * 列表
	 */
	@ResponseBody
	@RequestMapping("/list")
	public R list(@RequestParam Map<String, Object> params) {
		//查询列表数据
		Query query = new Query(params);
		try {
			List<Map<String, Object>> list = sysGeneratorService.queryList(query);
			int total = sysGeneratorService.queryTotal(query);

			PageUtils pageUtil = new PageUtils(list, total, query.getLimit(), query.getPage());

			return R.ok().put("page", pageUtil);
		} catch (Exception e) {
			return R.error(e.getMessage());
		}
	}
	
	/**
	 * 生成代码
	 */
	@RequestMapping("/code")
	public void code(HttpServletRequest request, HttpServletResponse response) throws Exception{
		String[] tableNames = new String[]{};
		String moduleName = request.getParameter("moduleName");
		String author = request.getParameter("author");
		String email = request.getParameter("email");
		String tables = request.getParameter("tables");
		tableNames = JSON.parseArray(tables).toArray(tableNames);

		CoonEntity entity=new CoonEntity();
		entity.setCoon(request.getParameter("coon"));
		entity.setUser(request.getParameter("user"));
		entity.setPwd(request.getParameter("pwd"));

		byte[] data = sysGeneratorService.generatorCode(tableNames, entity, moduleName, author, email);

		response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"jz.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IOUtils.write(data, response.getOutputStream());
	}
	

	
	
	/**
	 * 下载附件
	 * @param id
	 * @param res
	 * @param request
	 */
	@GetMapping(value="/download", produces = "application/json")
	public void downloadFile(HttpServletResponse res, HttpServletResponse request) {
		//获取要下载的文件路径
		String filePath = System.getProperty("user.dir") + File.separator + "demo";
			try {
				downloadResultFile(res, request, filePath, "see-demo-service.zip");
				logger.info("下载成功！");
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("下载文件异常");
			}
		
	}
	
	/**
	 * 下载文件的方法
	 * @param res response对象
	 * @param filePath 待下载的文件名
	 * @param downloadName 下载给用户的文件名
	 */
	private void downloadResultFile(HttpServletResponse res, HttpServletResponse request, String filePath, String downloadName) {

		try {
			if (request.getHeader(USER_AGENT) != null && request.getHeader(USER_AGENT).toUpperCase().indexOf(MSIE) > 0) {
				downloadName = URLEncoder.encode(downloadName, "UTF-8");
			} else {
				downloadName = new String(downloadName.getBytes("UTF-8"), "ISO-8859-1");
			}
		}
		catch (UnsupportedEncodingException ue) {
			logger.error("download filename convert encoding exception ", ue);
		}

		res.setContentType("application/octet-stream");
		res.addHeader("Content-Disposition", "attachment; filename=" + downloadName);
		BufferedInputStream bis = null;
		BufferedOutputStream out = null;
		File file = new File(filePath+File.separator+downloadName);
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			out = new BufferedOutputStream(res.getOutputStream());
			byte[] buff = new byte[bis.available()];
			while (true) {
				int bytesRead;
				if (-1 == (bytesRead = bis.read(buff, 0, buff.length))) {
					break;
				}
				out.write(buff, 0, bytesRead);
			}
			out.flush();
		} catch (IOException e) {
			logger.error("output template file exception", e);
		} finally {
			IOUtils.closeQuietly(bis);
			IOUtils.closeQuietly(out);
		}
	
	}
	
	
	

	
}
