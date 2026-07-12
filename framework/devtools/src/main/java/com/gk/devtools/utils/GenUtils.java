package com.gk.devtools.utils;

import cn.hutool.core.map.MapUtil;
import com.gk.common.exception.GkException;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * 代码生成器工具类
 *
 * @author Lowen
 */
public class GenUtils {

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 获取模板渲染后的内容
     * @param content   模板内容
     * @param dataModel 数据模型
     */
    public static String getTemplateContent(String content, Map<String, Object> dataModel) {
        if(MapUtil.isEmpty(dataModel)){
            return content;
        }

        StringReader reader = new StringReader(content);
        StringWriter sw = new StringWriter();
        try {
            //渲染模板
            String templateName = dataModel.getOrDefault("templateName", "generator").toString();
            Template template = new Template(templateName, reader, null, "utf-8");
            template.process(dataModel, sw);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GkException("渲染模板失败，请检查模板语法", e);
        }

        content = sw.toString();

        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(sw);

        return content;
    }

    /**
     * 获取模板渲染后的内容
     * @param content   模板内容
     * @param dataModel 数据模型
     */
    public static String genTemplateString(String content, Map<String, Object> dataModel) {
        if(MapUtil.isEmpty(dataModel)){
            return content;
        }
        StringReader reader = new StringReader(content);
        StringWriter sw = new StringWriter();
        try {
            String templateName = dataModel.getOrDefault("name", "temp").toString();
            //渲染模板
            Template template = new Template(templateName, reader, null, "utf-8");
            template.process(dataModel, sw);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GkException("渲染模板失败，请检查模板语法", e);
        }
        return sw.toString();
    }

}