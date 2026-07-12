package com.gk.common.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception工具类
 *
 * @author Lowen
 */
public class ExceptionUtils {

    /**
     * 获取异常信息
     * @param ex  异常
     * @return    返回异常信息
     */
    public static String getErrorStackTrace(Exception ex){
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw, true);
            ex.printStackTrace(pw);
        }finally {
            try {
                if(pw != null) {
                    pw.close();
                }
            } catch (Exception e) {

            }
            try {
                if(sw != null) {
                    sw.close();
                }
            } catch (IOException e) {

            }
        }

        return sw.toString();
    }


    public static String getErrorStackTraceSrc(Throwable ex){
        List<String> result = new ArrayList<>();
        result.add(ex.toString());
        for (StackTraceElement element : ex.getStackTrace()) {
            if (element.getClassName().startsWith("com.gk")) {
                result.add("    at " + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        }
        return result.stream().collect(Collectors.joining("\n"));
    }


}