package com.okdeer.mall.order.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.okdeer.base.dal.IBaseMapper;
import com.okdeer.base.service.BaseServiceImpl;
import com.okdeer.mall.order.entity.TradeOrderGroupRelation;
import com.okdeer.mall.order.mapper.TradeOrderGroupMapper;
import com.okdeer.mall.order.mapper.TradeOrderGroupRelationMapper;
import com.okdeer.mall.order.service.TradeOrderGroupRelationService;

@Service
public class TradeOrderGroupRelationServiceImpl extends BaseServiceImpl implements TradeOrderGroupRelationService {

	@Resource
	private TradeOrderGroupMapper tradeOrderGroupMapper;
	
	@Resource
	private TradeOrderGroupRelationMapper tradeOrderGroupRelationMapper;
	
	public IBaseMapper getBaseMapper() {
		return tradeOrderGroupRelationMapper;
	}

	@Override
	public List<TradeOrderGroupRelation> findByGroupOrderId(String groupOrderId) {
		return tradeOrderGroupRelationMapper.findByGroupOrderId(groupOrderId);
	}

	@Override
	public int countSuccessJoinNum(String groupOrderId) {
		return tradeOrderGroupRelationMapper.countSuccessJoinNum(groupOrderId);
	}
}
