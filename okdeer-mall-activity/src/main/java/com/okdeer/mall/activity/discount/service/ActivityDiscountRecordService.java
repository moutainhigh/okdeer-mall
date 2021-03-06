package com.okdeer.mall.activity.discount.service;

import com.okdeer.base.service.IBaseService;
import com.okdeer.mall.activity.coupons.bo.ActivityRecordParamBo;

/**
 * @DESC: 
 * @author yangq
 * @date  2016-03-25 20:00:26
 * @version 1.0.0
 * @copyright ©2005-2020 yschome.com Inc. All rights reserved
 * 
 */
public interface ActivityDiscountRecordService extends IBaseService{
	
	/**
	 * @Description: 统计用户参与活动的总次数
	 * @param userId
	 * @param activityId
	 * @return   
	 * @author maojj
	 * @date 2017年4月21日
	 */
	int countTotalFreq(ActivityRecordParamBo paramBo);
	
	/**
	 * @Description: 根据订单删除用户使用活动记录
	 * @param record   
	 * @author maojj
	 * @date 2017年4月22日
	 */
	void deleteByOrderId(String orderId);
}