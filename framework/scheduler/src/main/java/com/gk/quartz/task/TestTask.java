package com.gk.quartz.task;

import com.gk.common.task.ITask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 测试定时任务(演示Demo，可删除)
 * testTask为spring bean的名称
 * @author Lowen
 */
@Slf4j
@Component("testTask")
public class TestTask implements ITask {
	@Override
	public String run(String params){
        var record = execution().record("Run test task");
        record.step("LOG", "Task invoked with params=" + params);
        record.complete("Test task completed");
        log.debug("TestTask定时任务正在执行，参数为：{}", params);
		return "";
	}
}
