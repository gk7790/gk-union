package com.gk.devtools.controller;

import cn.hutool.core.io.IoUtil;
import com.gk.devtools.entity.ProjectEntity;
import com.gk.devtools.utils.ProjectUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 项目名修改
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools/project")
@Tag(name = "开发工具-下载代码")
public class ProjectController {

    @GetMapping
    public void project(ProjectEntity project, HttpServletResponse response) throws Exception {
        byte[] data = ProjectUtils.download(project);

        response.setHeader("Content-Disposition", "attachment; filename=\"" + project.getNewProjectName() + ".zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IoUtil.write(response.getOutputStream(), false, data);
    }
}