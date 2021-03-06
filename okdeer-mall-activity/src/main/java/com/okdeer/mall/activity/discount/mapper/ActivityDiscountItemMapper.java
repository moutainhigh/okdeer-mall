/*
 * Copyright(C) 2016-2021 okdeer.com Inc. All rights reserved
 * ActivityJoinRecordMapper.java
 * @Date 2017-10-10 Created
 * 注意：本内容仅限于友门鹿公司内部传阅，禁止外泄以及用于其他的商业目的
 */
package com.okdeer.mall.activity.discount.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.okdeer.base.dal.IBaseMapper;
import com.okdeer.mall.activity.discount.entity.ActivityDiscountItem;

public interface ActivityDiscountItemMapper extends IBaseMapper {

	/**
	 * @Description: 通过活动id删除
	 * @author zhangkn
	 * @date 2017年12月6日
	 */
	void deleteByActivityId(String activityId);
	
	/**
	 * @Description: 批量添加
	 * @param list
	 * @author zhangkn
	 * @date 2017年12月6日
	 */
	void addBatch(List<ActivityDiscountItem> list);
	
	/**
	 * @Description: 通过活动id加载列表
	 * @param activityId
	 * @return
	 * @author zhangkn
	 * @date 2017年12月6日
	 */
	List<ActivityDiscountItem> findByActivityId(String activityId);
	
	/**
	 * @Description: 根据id集合查询 按满多少元升序
	 * @param idList
	 * @author tuzhd
	 * @date 2017年7月27日
	 */
	List<ActivityDiscountItem> findByActivityIdList(@Param("idList")List<String> idList);
	
}