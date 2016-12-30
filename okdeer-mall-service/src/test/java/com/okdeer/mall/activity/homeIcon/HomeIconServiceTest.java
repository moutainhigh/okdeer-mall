/** 
 *@Project: okdeer-third-service 
 *@Author: tangzj02
 *@Date: 2016年12月30日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */
package com.okdeer.mall.activity.homeIcon;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.okdeer.mall.Application;
import com.okdeer.mall.activity.dto.HomeIconParamDto;
import com.okdeer.mall.activity.entity.HomeIcon;
import com.okdeer.mall.activity.service.HomeIconService;

/**
 * ClassName: HomeIconApiTest 
 * @Description: 首页ICON服务测试
 * @author tangzj02
 * @date 2016年12月30日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 * 	        友门鹿2.0       2016-12-30         tangzj02                        添加
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class HomeIconServiceTest {

	/** 日志对象 */
	private static final Logger log = LoggerFactory.getLogger(HomeIconServiceTest.class);

	@Autowired
	private HomeIconService homeIconService;

	@Test
	public void testFindList() {
		try {
			log.info("测试获取首页ICON列表");
			HomeIconParamDto paramDto = new HomeIconParamDto();
			paramDto.setAddStartTime(new Date());
			paramDto.setAddEndTime(new Date());
			paramDto.setUpdateStartTime(new Date());
			paramDto.setUpdateEndTime(new Date());
			List<HomeIcon> list = homeIconService.findList(paramDto);
			log.info("list.size : ", list.size());
		} catch (Exception e) {
			log.error("查询首页ICON列表异常:{}", e);
		}
	}
}
