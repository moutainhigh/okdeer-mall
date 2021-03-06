/** 
 *@Project: okdeer-mall-activity 
 *@Author: xuzq01
 *@Date: 2017年4月12日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */    
package com.okdeer.mall.activity.staticFile.service;

import com.okdeer.base.common.utils.PageUtils;
import com.okdeer.base.service.IBaseService;
import com.okdeer.mall.activity.staticFile.dto.ActivityStaticFileDto;
import com.okdeer.mall.activity.staticFile.dto.ActivityStaticFileParamDto;

/**
 * ClassName: ActivityStaticFileService 
 * @Description:   前端模块管理service
 * @author xuzq01
 * @date 2017年4月12日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *	V2.2.0          2017年4月12日                       xuzq01          	     前端模块管理service
 */

public interface ActivityStaticFileService extends IBaseService {

	/**
	 * @Description: 获取列表
	 * @param activityStaticFileParamDto
	 * @param pageNumber
	 * @param pageSize
	 * @return   
	 * @author xuzq01
	 * @date 2017年4月12日
	 */
	PageUtils<ActivityStaticFileDto> findStaticFileList(ActivityStaticFileParamDto activityStaticFileParamDto, int pageNumber,
			int pageSize);

	/**
	 * @Description: 通过名称查询数量
	 * @param activityStaticFile
	 * @return   
	 * @author xuzq01
	 * @date 2017年4月12日
	 */
	int findCountByName(String name);

}
