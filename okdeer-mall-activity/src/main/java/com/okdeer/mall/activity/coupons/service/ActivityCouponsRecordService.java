package com.okdeer.mall.activity.coupons.service;

import java.util.List;
import java.util.Map;

import com.okdeer.base.common.exception.ServiceException;
import com.okdeer.base.common.utils.PageUtils;
import com.okdeer.mall.activity.bo.FavourParamBO;
import com.okdeer.mall.activity.coupons.bo.ActivityCouponsRecordParamBo;
import com.okdeer.mall.activity.coupons.bo.UserCouponsBo;
import com.okdeer.mall.activity.coupons.entity.ActivityCollectCoupons;
import com.okdeer.mall.activity.coupons.entity.ActivityCoupons;
import com.okdeer.mall.activity.coupons.entity.ActivityCouponsRecord;
import com.okdeer.mall.activity.coupons.entity.ActivityCouponsRecordQueryVo;
import com.okdeer.mall.activity.coupons.entity.ActivityCouponsRecordVo;
import com.okdeer.mall.activity.coupons.entity.CouponsFindVo;
import com.okdeer.mall.activity.dto.ActivityCouponsRecordQueryParamDto;
import com.okdeer.mall.common.enums.UseUserType;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.vo.Coupons;

import net.sf.json.JSONObject;

/**
 * @DESC:
 * @author YSCGD
 * @date 2016-04-08 19:39:19
 * @version 1.0.0
 * @copyright ©2005-2020 yschome.com Inc. All rights reserved
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿2.1 			2017年2月15日			 maojj			新增查询用户有效代金券接口
 */
public interface ActivityCouponsRecordService {

	/**
	 * 领取记录列表
	 * 
	 * @param activityCouponsRecordVo
	 * @param pageNum
	 * @param pageSize
	 * @return
	 * @throws ServiceException
	 */
	PageUtils<ActivityCouponsRecordVo> getAllRecords(ActivityCouponsRecordVo activityCouponsRecordVo, int pageNum,
			int pageSize) throws ServiceException;

	/**
	 * 导出领取记录信息
	 * 
	 * @param paraMap
	 * @return
	 */
	List<ActivityCouponsRecordVo> getRecordExportData(Map<String, Object> paraMap);

	/**
	 * 
	 * 根据代金券id、活动id、活动类型和领取人id，查询代金券领取记录信息
	 * 
	 * @author wusw
	 * @param couponsCollectRecord
	 * @return
	 */
	int selectCountByParams(ActivityCouponsRecordQueryParamDto activityCouponsRecordQueryParam) throws ServiceException;


	/**
	 * 
	 * 插入代金券领取记录
	 * 
	 * @author wusw
	 * @param couponsId
	 * @param currentOperatUserId
	 * @throws ServiceException
	 */
	JSONObject addRecordForRecevie(String couponsId, String currentOperatUserId) throws ServiceException;

	/**
	 * DESC: 领取活动优惠券
	 * 
	 * @author LIU.W
	 * @param coll
	 * @param activityCouponsType
	 * @param userId
	 * @throws ServiceException
	 */
	void drawCouponsRecord(ActivityCollectCoupons coll,String userId) throws ServiceException;

	/**
	 * 
	 * 输入优惠码，插入代金券领取记录
	 * 
	 * @author wusw
	 * @param exchangeCode
	 * @param currentOperatUserId
	 * @return
	 * @throws ServiceException
	 */
	JSONObject addRecordForExchangeCode(Map<String, Object> params, String exchangeCode, String currentOperatUserId) throws ServiceException;

	/**
	 * 根据领取人id、领取状态，店铺ID，查询领取的代金券详细信息
	 * 
	 * @author yangq
	 * @param couponsCollectRecord
	 * @return
	 */
	List<ActivityCouponsRecordQueryVo> selectCouponsDetailByStoreId(ActivityCouponsRecord activityCouponsRecord)
			throws Exception;

	/**
	 * 查询代金券具体金额
	 * </p>
	 * 
	 * @author yangq
	 * @param couponsFindVo
	 * @return
	 */
	ActivityCoupons selectCouponsItem(CouponsFindVo couponsFindVo) throws Exception;

	/**
	 * 定时更新代金券领取记录状态
	 *
	 */
	void updateStatusByJob() throws Exception;

	/**
	 * 定时更新代金券活动 退款状态 退钱
	 */
	void updateRefundStatus(List<ActivityCouponsRecordVo> couponsRecordVoList, String id) throws Exception;

	/**
	 * 订单更新代金卷状态
	 * @param orderId
	 */
	void updateUseStatus(String orderId);
	
	// Begin　V2.5 added by maojj 2017-06-24
	/**
	 * @Description: 释放代金券
	 * @param tradeOrder   
	 * @author maojj
	 * @date 2017年6月24日
	 */
	void releaseConpons(TradeOrder tradeOrder);
	// End V2.5 added by maojj 2017-06-24

	ActivityCouponsRecord selectByPrimaryKey(String id);
	
	void updateActivityCouponsStatus(Map<String,Object> params);
	
	/**
	 * 执行代金劵提醒JOB
	 * @Description:   
	 * @return void  
	 * @throws
	 * @author tuzhd
	 * @date 2016年11月21日
	 */
	void procesRecordNoticeJob();
	
	/**
	 * 根据活动ID领取代金劵
	 * @param collectId 活动id集合
	 * @param userId 用户id
	 * @param activityCouponsType 活动类型
	 * @return tuzhiding 
	 * @throws ServiceException
	 */
	JSONObject addRecordsByCollectId(String collectId, String userId)throws Exception;
	
	/**
	 * @Description: 邀新活动 被邀用户下单完成后给 邀请人送代金劵及抽奖次数 
	 * 1、是否完成首单
	 * 2、活动是否未结束
	 * @param userId 被邀请人id
	 * @param collectCouponsIds 邀请人获得的代金劵奖励id
	 * @author tuzhd
	 * @date 2016年12月13日
	 */
	void addInviteUserHandler(String userId,String[] collectCouponsIds)  throws Exception;
	
	/**
	 * @Description:  tuzhd根据用户id查询其是否存在已使用的新用户专享代金劵 用于首单条件判断
	 * @param useUserType 使用用户类型
	 * @param userId 用户id
	 * @return int  统计结果
	 * @author tuzhd
	 * @date 2016年12月31日
	 */
	int findCouponsCountByUser(UseUserType useUserType,String userId);
	
	// Begin V2.1 added by maojj 2017-02-15
	/**
	 * @Description: 查询用户有效的代金券领取记录
	 * @param paramBo 查询条件参数
	 * @param favourFilter 优惠过滤算法
	 * @return   
	 * @author maojj
	 * @date 2017年2月15日
	 */
	List<Coupons> findValidCoupons(FavourParamBO paramBo) throws Exception;
	// End V2.1 added by maojj 2017-02-15
	
	/**
	 * 根据活动ID领取代金劵
	 * @param collectId 活动id集合
	 * @param userId 用户id
	 * @param limitOne 是否限制只领取一次
	 * @return tuzhiding 
	 * @throws ServiceException
	 */
	public JSONObject addRecordsByCollectId(String collectId, String userId,String invitaUserId,boolean limitOne)throws Exception;

	JSONObject addBeforeRecordsForWechatActivity(String collectId, String phone, String advertId)
			throws ServiceException;
	
	/**
	 * @Description: 添加代价劵领取记录
	 * @param activityCouponsRecord
	 * @author zengjizu
	 * @date 2017年8月25日
	 */
	void add(ActivityCouponsRecord activityCouponsRecord);
	
	// Begin V2.6.4 added by maojj 2017-11-07
	/**
	 * @Description: 查询用户代金券列表
	 * @param paramBo
	 * @return   
	 * @author maojj
	 * @date 2017年11月7日
	 */
	List<UserCouponsBo> findUserCouponsList(ActivityCouponsRecordParamBo paramBo);
	
	/**
	 * 统计用户代金券使用记录
	 * @param paramBo
	 * @param userCouponsList   
	 * @author maojj
	 * @date 2017年11月9日
	 */
	void countUseRecord(FavourParamBO paramBo, List<UserCouponsBo> userCouponsList);
	// End V2.6.4 added by maojj 2017-11-07
}