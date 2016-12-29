/** 
 *@Project: okdeer-mall-activity 
 *@Author: xuzq01
 *@Date: 2016年12月8日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */    
package com.okdeer.mall.activity.prize.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.okdeer.base.service.IBaseService;
import com.okdeer.mall.activity.prize.entity.ActivityPrizeWeight;

import net.sf.json.JSONObject;

/**
 * ClassName: ActivityPrizeWeightApiImpl 
 * @Description: 活动奖品权重表
 * @author xuzq01
 * @date 2016年12月8日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		v1.2.3			2016年12月8日		xuzq01				活动奖品权重表Service
 */

public interface ActivityPrizeWeightService extends IBaseService {
	/**
	 * 根据活动id查询所有奖品的比重信息 按顺序查询 顺序与奖品对应 
	 * @param activityId 活动id
	 * @return List<ActivityPrizeWeight>  
	 * @author tuzhd
	 * @date 2016年12月14日
	 */
	public List<ActivityPrizeWeight> findPrizesByactivityId(String activityId);
	
	/**
	 * 根据活动id扣减奖品数量
	 * @param activityId 活动id
	 * @return List<ActivityPrizeWeight>  
	 * @author tuzhd
	 * @date 2016年12月14日
	 */
	public JSONObject updatePrizesNumber(String id);
}