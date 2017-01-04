/** 
 *@Project: okdeer-mall-operate 
 *@Author: tangzj02
 *@Date: 2016年12月28日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */
package com.okdeer.mall.operate.service;

import java.util.List;

import com.okdeer.base.service.IBaseService;
import com.okdeer.common.utils.BaseResult;
import com.okdeer.mall.operate.dto.HomeIconDto;
import com.okdeer.mall.operate.dto.HomeIconParamDto;
import com.okdeer.mall.operate.entity.ColumnHomeIcon;
import com.okdeer.mall.operate.entity.ColumnSelectArea;

/**
 * ClassName: ColumnHomeIconService 
 * @Description: 首页ICON与商品关联服务
 * @author tangzj02
 * @date 2016年12月30日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 * 	        友门鹿2.0        2016-12-30        tangzj02                     添加
 */

public interface ColumnHomeIconService extends IBaseService {

	/**
	 * @Description: 查询首页ICON列表
	 * @param paramDto 查询参数
	 * @return List<ColumnHomeIcon> 首页ICON记录列表
	 * @throws Exception
	 * @author tangzj02
	 * @date 2016年12月28日
	 */
	List<ColumnHomeIcon> findList(HomeIconParamDto paramDto) throws Exception;

	/**
	 * @Description: 根据城市查询首页ICON记录列表
	 * @param provinceId 省ID
	 * @param cityId 城市ID
	 * @throws Exception   
	 * @return HomeIconDto  
	 * @author tangzj02
	 * @date 2017年01月04日
	 */
	List<HomeIconDto> findListByCityId(String provinceId, String cityId) throws Exception;

	/**
	 * @Description: 编辑或者添加首页ICON
	 * @param entity 首页ICON信息
	 * @param areaList 地区集合信息
	 * @param goodsList 推荐商品信息
	 * @return BaseResult  
	 * @throws Exception 
	 * @author tangzj02
	 * @date 2017年1月4日
	 */
	BaseResult save(ColumnHomeIcon entity, List<ColumnSelectArea> areaList, List<String> goodsIds) throws Exception;

}
