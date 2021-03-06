/** 
 *@Project: okdeer-mall-order 
 *@Author: guocp
 *@Date: 2017年8月11日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */
package com.okdeer.mall.order.api;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.redis.util.RedisLockRegistry;

import com.alibaba.dubbo.config.annotation.Service;
import com.okdeer.base.common.utils.PageUtils;
import com.okdeer.base.common.utils.UuidUtils;
import com.okdeer.base.common.utils.mapper.BeanMapper;
import com.okdeer.base.common.utils.mapper.JsonMapper;
import com.okdeer.mall.activity.dto.ActivityPinMoneyDto;
import com.okdeer.mall.activity.dto.ActivityPinMoneyQueryDto;
import com.okdeer.mall.order.dto.TradePinMoneyObtainDto;
import com.okdeer.mall.order.dto.TradePinMoneyQueryDto;
import com.okdeer.mall.order.dto.TradePinMoneyUseDto;
import com.okdeer.mall.order.entity.TradePinMoneyObtain;
import com.okdeer.mall.order.service.TradePinMoneyApi;
import com.okdeer.mall.order.service.TradePinMoneyObtainService;
import com.okdeer.mall.order.service.TradePinMoneyUseService;

/**
 * ClassName: TradePinMoneyApiImpl 
 * @Description: 零花钱服务API
 * @author guocp
 * @date 2017年8月11日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *
 */
@Service(version="1.0.0")
public class TradePinMoneyApiImpl implements TradePinMoneyApi {
	/**
	 *日志管理类。
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TradePinMoneyApiImpl.class);
	
	@Autowired
	private TradePinMoneyObtainService tradePinMoneyObtainService;

	@Autowired
	private TradePinMoneyUseService tradePinMoneyUseService;
	
	@Resource
	private RedisLockRegistry redisLockRegistry;
	
	/**
	 * 查询我的零花钱余额
	 */
	@Override
	public BigDecimal findMyUsableTotal(String userId, Date nowDate) {
		return tradePinMoneyObtainService.findMyRemainTotal(userId, nowDate);
	}

	/**
	 * @Description: 查询用户领取记录
	 * @param userId
	 * @param date
	 * @return   
	 * @author guocp
	 * @date 2017年8月10日
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PageUtils<TradePinMoneyObtainDto> findObtainList(String userId, int pageNumber, int pageSize) {
		return tradePinMoneyObtainService.findPage(userId, pageNumber, pageSize).toBean(TradePinMoneyObtainDto.class);

	}

	/**
	 * @Description: 查询用户领取记录
	 * @param userId
	 * @param date
	 * @return   
	 * @author guocp
	 * @date 2017年8月10日
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PageUtils<TradePinMoneyUseDto> findUseList(String userId, int pageNumber, int pageSize) {
		return tradePinMoneyUseService.findPage(userId, pageNumber, pageSize).toBean(TradePinMoneyUseDto.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PageUtils<TradePinMoneyObtainDto> findObtainPageList(TradePinMoneyQueryDto paramDto, int pageNumber,
			int pageSize) {
		return tradePinMoneyObtainService.findObtainPageList(paramDto, pageNumber, pageSize).toBean(TradePinMoneyObtainDto.class);
	}

	@Override
	public Integer findObtainListCount(TradePinMoneyQueryDto paramDto) {
		return tradePinMoneyObtainService.findObtainListCount(paramDto);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PageUtils<TradePinMoneyUseDto> findUsePageList(TradePinMoneyQueryDto paramDto, int pageNumber,
			int pageSize) {
		return tradePinMoneyUseService.findUsePageList(paramDto, pageNumber, pageSize).toBean(TradePinMoneyUseDto.class);
	}

	@Override
	public Integer findUseListCount(TradePinMoneyQueryDto paramDto) {
		return tradePinMoneyUseService.findUseListCount(paramDto);
	}

	@Override
	public void addObtainRecord(ActivityPinMoneyQueryDto dto, String deviceId, ActivityPinMoneyDto moneyDto, BigDecimal pinMoney) throws Exception {
		LOGGER.info("零花钱领取客户端设备deviceId===：{}" ,deviceId);
		
		TradePinMoneyObtain obtain = BeanMapper.map(dto, TradePinMoneyObtain.class);
		obtain.setId(UuidUtils.getUuid());
		obtain.setDeviceId(deviceId);
		obtain.setStatus(0);
		obtain.setAmount(pinMoney);
		obtain.setRemainAmount(pinMoney);
		Date date = new Date();
		obtain.setCreateTime(date);
		obtain.setUpdateTime(date);
		obtain.setActivityId(moneyDto.getId());
		
		if(moneyDto.getValidDayType() == 0){
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(calendar.DATE,moneyDto.getEffectDay());
			Date effectTime = calendar.getTime();
			//生效时间
			obtain.setEffectTime(effectTime);
			calendar.add(calendar.DATE,moneyDto.getValidDay());
			//有效时间
			obtain.setValidTime(calendar.getTime());
		}else{
			obtain.setEffectTime(moneyDto.getValidTimeStart());
			obtain.setValidTime(moneyDto.getValidTimeEnd());
		}
		
		LOGGER.info("零花钱领取对象参数===：{}" ,JsonMapper.nonDefaultMapper().toJson(obtain));
		
		// 已活动和用户id为key 进行加锁
		String lockKey = String.format("%s:%s", moneyDto.getId(),dto.getUserId());
		Lock lock = redisLockRegistry.obtain(lockKey);
		try {
			if(lock.tryLock(5,TimeUnit.SECONDS)){
				tradePinMoneyObtainService.add(obtain);
			}
		}finally {
		    lock.unlock();
		}
	}

	@Override
	public BigDecimal findPinMoneyObtainAmount(TradePinMoneyQueryDto queryDto) {
		return tradePinMoneyObtainService.findPinMoneyObtainAmount(queryDto);
	}
	
}
