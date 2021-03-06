package com.okdeer.mall.order.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.okdeer.archive.goods.dto.StoreSkuComponentDto;
import com.okdeer.archive.goods.dto.StoreSkuComponentParamDto;
import com.okdeer.archive.goods.service.StoreSkuApi;
import com.okdeer.archive.goods.spu.enums.SkuBindType;
import com.okdeer.archive.goods.spu.enums.SpuTypeEnum;
import com.okdeer.archive.goods.store.entity.GoodsStoreSku;
import com.okdeer.archive.stock.dto.StockUpdateDetailDto;
import com.okdeer.archive.stock.dto.StockUpdateDto;
import com.okdeer.archive.stock.enums.StockOperateEnum;
import com.okdeer.base.common.utils.UuidUtils;
import com.okdeer.mall.activity.coupons.entity.ActivitySale;
import com.okdeer.mall.activity.coupons.enums.ActivityTypeEnum;
import com.okdeer.mall.activity.coupons.mapper.ActivitySaleMapper;
import com.okdeer.mall.activity.coupons.service.ActivitySaleRecordService;
import com.okdeer.mall.activity.discount.entity.ActivityDiscount;
import com.okdeer.mall.activity.discount.entity.ActivityDiscountGroup;
import com.okdeer.mall.activity.discount.enums.ActivityDiscountStatus;
import com.okdeer.mall.activity.discount.mapper.ActivityDiscountGroupMapper;
import com.okdeer.mall.activity.discount.mapper.ActivityDiscountMapper;
import com.okdeer.mall.activity.seckill.entity.ActivitySeckill;
import com.okdeer.mall.activity.seckill.enums.SeckillStatusEnum;
import com.okdeer.mall.activity.seckill.service.ActivitySeckillService;
import com.okdeer.mall.common.dto.Request;
import com.okdeer.mall.order.bo.ComboSnapshotAdapter;
import com.okdeer.mall.order.bo.CurrentStoreSkuBo;
import com.okdeer.mall.order.bo.StoreSkuParserBo;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderComboSnapshot;
import com.okdeer.mall.order.entity.TradeOrderGroup;
import com.okdeer.mall.order.entity.TradeOrderItem;
import com.okdeer.mall.order.entity.TradeOrderRefunds;
import com.okdeer.mall.order.entity.TradeOrderRefundsItem;
import com.okdeer.mall.order.enums.OrderStatusEnum;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.mapper.TradeOrderComboSnapshotMapper;
import com.okdeer.mall.order.vo.ServiceOrderReq;
import com.okdeer.mall.order.vo.TradeOrderGoodsItem;

@Component
public class MallStockUpdateBuilder {
	
	@Resource
	private ActivitySaleRecordService activitySaleRecordService;
	
	@Resource
	private ActivitySaleMapper activitySaleMapper;
	
	/**
	 * 秒杀活动service
	 */
	@Autowired
	private ActivitySeckillService activitySeckillService;
	
	@Resource
	private TradeOrderComboSnapshotMapper tradeOrderComboSnapshotMapper;
	
	@Resource
	private ActivityDiscountMapper activityDiscountMapper;
	
	@Resource
	private ActivityDiscountGroupMapper activityDiscountGroupMapper;

	@Resource
	private ComboSnapshotAdapter comboSnapshotAdapter;
	
	@Reference(version="1.0.0",check=false)
	private StoreSkuApi storeSkuApi;
	
	/**
	 * @Description: V2.1版本。构建商品更新的Dto。仅用于秒杀订单
	 * @param order
	 * @param parserBo
	 * @return   
	 * @author maojj
	 * @date 2017年3月13日
	 */
	public StockUpdateDto build(TradeOrder order, StoreSkuParserBo parserBo) {
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(order.getId());
		stockUpdateDto.setStoreId(order.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.PLACE_ORDER);

		StockUpdateDetailDto updateDetail = null;
		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		for (CurrentStoreSkuBo storeSku : parserBo.getCurrentSkuMap().values()) {
			updateDetail = buildDetail(storeSku);
			if (updateDetail != null) {
				updateDetailList.add(updateDetail);
			}
		}
		if (CollectionUtils.isEmpty(updateDetailList)) {
			return null;
		}
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	/**
	 * @Description: 友门鹿V2.1以前的版本和鹿管家版本的构建秒杀库存变更
	 * @param order
	 * @param req
	 * @param rpcId
	 * @return   
	 * @author maojj
	 * @date 2017年3月15日
	 */
	public StockUpdateDto build(TradeOrder order, Request<ServiceOrderReq> req, String rpcId) {
		ServiceOrderReq reqData = req.getData();
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(rpcId);
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(order.getId());
		stockUpdateDto.setStoreId(order.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.PLACE_ORDER);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		GoodsStoreSku storeSku = (GoodsStoreSku) req.getContext().get("storeSku");
		StockUpdateDetailDto updateDetail = buildDetail(storeSku,ActivityTypeEnum.SECKILL_ACTIVITY,reqData.getSkuNum());
		updateDetailList.add(updateDetail);
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}

	/**
	 * @Description: 友门鹿V2.1以前的版本和鹿管家版本的构建服务商品库存变更
	 * @param order
	 * @param req
	 * @param resp
	 * @return   
	 * @author maojj
	 * @date 2017年3月15日
	 */
	@SuppressWarnings("unchecked")
	public StockUpdateDto build(TradeOrder order, Request<ServiceOrderReq> req) {
		Map<String, Object> context = req.getContext();
		ServiceOrderReq reqData = req.getData();
		List<GoodsStoreSku> storeSkuList = (List<GoodsStoreSku>) context.get("storeSkuList");
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(order.getId());
		stockUpdateDto.setStoreId(order.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.PLACE_ORDER);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		TradeOrderGoodsItem orderItem = null;
		StockUpdateDetailDto updateDetail = null;
		for (GoodsStoreSku storeSku : storeSkuList) {
			orderItem = reqData.findOrderItem(storeSku.getId());
			updateDetail = buildDetail(storeSku,ActivityTypeEnum.NO_ACTIVITY,orderItem.getSkuNum());
			updateDetailList.add(updateDetail);
		}
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	/**
	 * @Description: 1.0.0 版本服务店下单库存构建
	 * @param order
	 * @param req
	 * @return   
	 * @author maojj
	 * @date 2017年3月15日
	 */
	public StockUpdateDto build(TradeOrder order, GoodsStoreSku storeSku,ActivityTypeEnum actType, int buyNum) {
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(order.getId());
		stockUpdateDto.setStoreId(order.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.PLACE_ORDER);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		StockUpdateDetailDto updateDetail = buildDetail(storeSku,actType,buyNum);
		updateDetailList.add(updateDetail);
		
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	/**
	 * @Description: 根据订单构建库存更新信息。（取消订单、订单拒收）
	 * @param tradeOrder
	 * @return   
	 * @author maojj
	 * @throws Exception 
	 * @date 2017年3月15日
	 */
	public StockUpdateDto build(TradeOrder tradeOrder) throws Exception{
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(tradeOrder.getId());
		stockUpdateDto.setStoreId(tradeOrder.getStoreId());
		stockUpdateDto.setStockOperateEnum(convert(tradeOrder.getStatus()));
		
		// 组合商品数量映射列表
		Map<String,Integer> comboSkuMap = Maps.newHashMap();
		List<String> comboSkuIds = Lists.newArrayList();
		
		// 捆绑商品数量映射列表
		Map<String,Integer> bindSkuMap = Maps.newHashMap();
		List<String> bindSkuIds = Lists.newArrayList();
		
		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		StockUpdateDetailDto updateDetail = null;
		ActivityTypeEnum actType = null;
		for(TradeOrderItem orderItem : tradeOrder.getTradeOrderItem()){
			// 获取订单项商品活动类型
			actType = getActvityType(tradeOrder,orderItem);
			updateDetail = new StockUpdateDetailDto();
			updateDetail.setStoreSkuId(orderItem.getStoreSkuId());
			updateDetail.setSpuType(orderItem.getSpuType());
			updateDetail.setActType(actType);
			updateDetail.setUpdateNum(orderItem.getQuantity());
			if(actType == ActivityTypeEnum.LOW_PRICE){
				// 如果是低价
				updateDetail.setUpdateLockedNum(orderItem.getActivityQuantity());
			}else if (actType == ActivityTypeEnum.SALE_ACTIVITIES 
					|| actType == ActivityTypeEnum.SECKILL_ACTIVITY
					|| actType == ActivityTypeEnum.GROUP_ACTIVITY){
				// 如果是特惠或者是秒杀或者是团购
				updateDetail.setUpdateLockedNum(orderItem.getQuantity());
			}
			if(orderItem.getSpuType() == SpuTypeEnum.assembleSpu){
				comboSkuIds.add(orderItem.getStoreSkuId());
				comboSkuMap.put(orderItem.getStoreSkuId(), orderItem.getQuantity());
			}
			// Begin V2.6.1 added by maojj 2017-08-28
			if(orderItem.getBindType() == SkuBindType.bind){
				// 捆绑商品
				bindSkuIds.add(orderItem.getStoreSkuId());
				bindSkuMap.put(orderItem.getStoreSkuId(), orderItem.getQuantity());
			}
			// End V2.6.1 added by maojj 2017-08-28
			updateDetailList.add(updateDetail);
		}
		// 处理组合商品
		if(CollectionUtils.isNotEmpty(comboSkuIds)){
			List<TradeOrderComboSnapshot> comboSkuList = tradeOrder.getComboDetailList();
			if(CollectionUtils.isEmpty(comboSkuList)){
				// 如果组合商品明细再订单中没有，则去快照表中查询明细
				comboSkuList = tradeOrderComboSnapshotMapper.findByOrderId(tradeOrder.getId());
			}	
			if(CollectionUtils.isEmpty(comboSkuList)){
				// 如果快照表中没有找到明细，则直接从组合成分表中获取明细
				comboSkuList = comboSnapshotAdapter.findByComboSkuIds(comboSkuIds);
			}
			for (TradeOrderComboSnapshot comboSku : comboSkuList) {
				updateDetail = new StockUpdateDetailDto();
				updateDetail.setStoreSkuId(comboSku.getStoreSkuId());
				updateDetail.setSpuType(comboSku.getSkuType());
				updateDetail.setUpdateNum(comboSku.getQuantity() * comboSkuMap.get(comboSku.getComboSkuId()));
				updateDetailList.add(updateDetail);
			}
		}
		
		// Begin V2.6.1 added by maojj 2017-08-28
		// 处理捆绑商品
		if (CollectionUtils.isNotEmpty(bindSkuIds)) {
			StoreSkuComponentParamDto paramDto = new StoreSkuComponentParamDto();
			paramDto.setStoreSkuIds(bindSkuIds);
			List<StoreSkuComponentDto> bindSkuList = storeSkuApi.findComponentByParam(paramDto);
			for(StoreSkuComponentDto bindSku : bindSkuList){
				updateDetail = new StockUpdateDetailDto();
				updateDetail.setStoreSkuId(bindSku.getComponentStoreSkuId());
				updateDetail.setUpdateNum(bindSku.getComponentNum().intValue() * bindSkuMap.get(bindSku.getStoreSkuId()));
				updateDetailList.add(updateDetail);
			}
		}
		// End V2.6.1 added by maojj 2017-08-28
		
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	/**
	 * @Description: 退款时，构建库存更新对象
	 * @param orderRefunds
	 * @param tradeOrder
	 * @param orderItemList
	 * @return
	 * @throws Exception   
	 * @author maojj
	 * @date 2017年3月21日
	 */
	public StockUpdateDto build(TradeOrderRefunds orderRefunds,TradeOrder tradeOrder,List<TradeOrderItem> orderItemList) throws Exception{
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(orderRefunds.getId());
		stockUpdateDto.setStoreId(orderRefunds.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.RETURN_OF_GOODS);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		StockUpdateDetailDto updateDetail = null;
		ActivityTypeEnum actType = null;
		
		// 组合商品数量映射列表
		Map<String,Integer> comboSkuMap = Maps.newHashMap();
		List<String> comboSkuIds = Lists.newArrayList();
		
		for(TradeOrderItem orderItem : orderItemList){
			// 获取订单项商品活动类型
			actType = getActvityType(tradeOrder,orderItem);
			updateDetail = new StockUpdateDetailDto();
			updateDetail.setStoreSkuId(orderItem.getStoreSkuId());
			updateDetail.setSpuType(orderItem.getSpuType());
			updateDetail.setActType(actType);
			updateDetail.setUpdateNum(orderItem.getQuantity());
			processRefundsNum(updateDetail,orderRefunds,orderItem,actType);
			if(orderItem.getSpuType() == SpuTypeEnum.assembleSpu){
				comboSkuIds.add(orderItem.getStoreSkuId());
				comboSkuMap.put(orderItem.getStoreSkuId(), orderItem.getQuantity());
			}
			updateDetailList.add(updateDetail);
		}
		// 处理组合商品
		if(CollectionUtils.isNotEmpty(comboSkuIds)){
			List<TradeOrderComboSnapshot> comboSkuList = tradeOrderComboSnapshotMapper.findByOrderId(tradeOrder.getId());
			if(CollectionUtils.isEmpty(comboSkuList)){
				// 如果快照表中没有找到明细，则直接从组合成分表中获取明细
				comboSkuList = comboSnapshotAdapter.findByComboSkuIds(comboSkuIds);
			}
			for (TradeOrderComboSnapshot comboSku : comboSkuList) {
				if(!comboSkuMap.containsKey(comboSku.getComboSkuId())){
					continue;
				}
				updateDetail = new StockUpdateDetailDto();
				updateDetail.setStoreSkuId(comboSku.getStoreSkuId());
				updateDetail.setSpuType(comboSku.getSkuType());
				updateDetail.setUpdateNum(comboSku.getQuantity() * comboSkuMap.get(comboSku.getComboSkuId()));
				updateDetailList.add(updateDetail);
			}
		}
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	/**
	 * @Description: 构建明细（仅用于秒杀订单）
	 * @param storeSku
	 * @return   
	 * @author maojj
	 * @date 2017年3月15日
	 */
	private StockUpdateDetailDto buildDetail(CurrentStoreSkuBo storeSku) {
		ActivityTypeEnum actType = ActivityTypeEnum.enumValueOf(storeSku.getActivityType());
		SpuTypeEnum spuType = storeSku.getSpuType();
		StockUpdateDetailDto detailDto = new StockUpdateDetailDto();
		detailDto.setStoreSkuId(storeSku.getId());
		detailDto.setSpuType(spuType);
		detailDto.setActType(actType);
		detailDto.setUpdateNum(storeSku.getQuantity());
		detailDto.setUpdateLockedNum(storeSku.getQuantity());
		return detailDto;
	}

	/**
	 * @Description: 根据店铺商品信息构建明细(服务店订单)
	 * @param storeSku
	 * @return   
	 * @author maojj
	 * @date 2017年3月15日
	 */
	public StockUpdateDetailDto buildDetail(GoodsStoreSku storeSku,ActivityTypeEnum actType,int buyNum) {
		StockUpdateDetailDto updateDetail = new StockUpdateDetailDto();
		updateDetail.setStoreSkuId(storeSku.getId());
		updateDetail.setSpuType(storeSku.getSpuTypeEnum());
		updateDetail.setActType(actType);
		updateDetail.setUpdateNum(buyNum);
		if(actType == ActivityTypeEnum.SECKILL_ACTIVITY){
			updateDetail.setUpdateLockedNum(buyNum);
		}
		return updateDetail;
	}
	
	/**
	 * @Description: 获取商品参与的活动类型
	 * @param tradeOrder
	 * @param orderItem
	 * @return
	 * @throws Exception   
	 * @author maojj
	 * @date 2017年3月21日
	 */
	private ActivityTypeEnum getActvityType(TradeOrder tradeOrder,TradeOrderItem orderItem) throws Exception{
		if (ActivityTypeEnum.SECKILL_ACTIVITY == tradeOrder.getActivityType()) {
			ActivitySeckill seckill = activitySeckillService.findSeckillById(tradeOrder.getActivityId());
			SeckillStatusEnum seckillStatus = seckill.getSeckillStatus();
			if (seckillStatus  == SeckillStatusEnum.ing) {
				return ActivityTypeEnum.SECKILL_ACTIVITY;
			}else{
				return ActivityTypeEnum.NO_ACTIVITY;
			}
		}else if(ActivityTypeEnum.GROUP_ACTIVITY == tradeOrder.getActivityType()){
			// 如果是团购
			ActivityDiscount groupAct = activityDiscountMapper.findById(tradeOrder.getActivityId());
			if(groupAct.getStatus() != ActivityDiscountStatus.ing){
				return ActivityTypeEnum.NO_ACTIVITY;
			}
			// 如果团购活动进行中，检查团购商品是否有活动限制
			ActivityDiscountGroup groupSku = activityDiscountGroupMapper
					.findByActivityIdAndSkuId(tradeOrder.getActivityId(), tradeOrder.getActivityItemId());
			if(groupSku.getGoodsCountLimit().compareTo(Integer.valueOf(0)) > 0){
				return ActivityTypeEnum.GROUP_ACTIVITY;
			}else{
				return ActivityTypeEnum.NO_ACTIVITY;
			}
		}
		
		if(tradeOrder.getType() != OrderTypeEnum.PHYSICAL_ORDER){
			return ActivityTypeEnum.NO_ACTIVITY;
		}
		
		Map<String, Object> map = Maps.newHashMap();
		map.put("orderId", tradeOrder.getId());
		map.put("saleGoodsId", orderItem.getStoreSkuId());
		// 查询订单项参与活动的活动ID
		String saleId = activitySaleRecordService.selectOrderGoodsActivity(map);
		if(StringUtils.isEmpty(saleId)){
			// 如果活动记录表中不存在，则表示该商品未参加活动
			return ActivityTypeEnum.NO_ACTIVITY;
		}
		// 如果订单项再活动记录表中存在，则标识用户购买的是活动商品，需要判定当前活动是否在进行中
		ActivitySale actSale = activitySaleMapper.get(saleId);
		if(actSale.getStatus() == 1){
			// 如果活动还在进行中，则返回活动类型
			return actSale.getType();
		}else{
			// 活动已关闭，则商品为普通商品
			return ActivityTypeEnum.NO_ACTIVITY;
		}
		
	}
	
	/**
	 * @Description: 取消订单和拒收共用一个服务层，此处主要是用于区分是取消还是拒收。
	 * @param orderStatus
	 * @return   
	 * @author maojj
	 * @date 2017年3月21日
	 */
	private StockOperateEnum convert(OrderStatusEnum orderStatus){
		StockOperateEnum stockOpt = null;
		switch (orderStatus) {
			case CANCELING:
			case CANCELED:
				stockOpt = StockOperateEnum.CANCEL_ORDER;
				break;
			case REFUSING:
			case REFUSED:
				stockOpt = StockOperateEnum.REFUSED_SIGN;
				break;
			case UNPAID:
			case DROPSHIPPING:
			case WAIT_RECEIVE_ORDER:
				stockOpt = StockOperateEnum.PLACE_ORDER;
				break;
			case HAS_BEEN_SIGNED:
				stockOpt = StockOperateEnum.PLACE_ORDER_COMPLETE;
				break;
			default:
				break;
		}
		return stockOpt;
	}
	
	/**
	 * @Description: 获取退货数量
	 * @param orderRefunds
	 * @param orderItem
	 * @param actType
	 * @return   
	 * @author maojj
	 * @date 2017年3月22日
	 */
	private void processRefundsNum(StockUpdateDetailDto updateDetail, TradeOrderRefunds orderRefunds,
			TradeOrderItem orderItem, ActivityTypeEnum actType) {
		if(orderItem.getSpuType() != SpuTypeEnum.fwdDdxfSpu){
			updateDetail.setUpdateNum(orderItem.getQuantity());
			// 到店消费商品退款，退货数量由退款单项中的数量决定。其他都是与订单项数量等同
			if(actType == ActivityTypeEnum.LOW_PRICE){
				// 低价活动的
				updateDetail.setUpdateLockedNum(orderItem.getActivityQuantity());
			}else if(actType == ActivityTypeEnum.SALE_ACTIVITIES || actType == ActivityTypeEnum.SECKILL_ACTIVITY){
				// 特惠或者秒杀
				updateDetail.setUpdateLockedNum(orderItem.getQuantity());
			}
		}
		// 如果是到店消费商品退货，从退款项中获取商品数量
		for(TradeOrderRefundsItem refundsItem : orderRefunds.getTradeOrderRefundsItem()){
			if(refundsItem.getOrderItemId().equals(orderItem.getId())){
				updateDetail.setUpdateNum(refundsItem.getQuantity());
				if(actType == ActivityTypeEnum.SECKILL_ACTIVITY){
					// 低价活动的
					updateDetail.setUpdateLockedNum(refundsItem.getQuantity());
				}
				break;
			}
		}
	}
	
	/**
	 * @Description: 构建到店消费库存更新对象
	 * @param tradeOrder
	 * @param stockOperateEnum
	 * @param adjustGoodsNum
	 * @return
	 * @throws Exception   
	 * @author maojj
	 * @date 2017年3月21日
	 */
	public StockUpdateDto buildForStoreConsume(TradeOrder tradeOrder, StockOperateEnum stockOperateEnum,
			Integer adjustGoodsNum) throws Exception {
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(tradeOrder.getId());
		stockUpdateDto.setStoreId(tradeOrder.getStoreId());
		stockUpdateDto.setStockOperateEnum(stockOperateEnum);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		StockUpdateDetailDto updateDetail = null;
		// 订单项信息
		for(TradeOrderItem orderItem : tradeOrder.getTradeOrderItem()){
			updateDetail = new StockUpdateDetailDto();
			updateDetail.setStoreSkuId(orderItem.getStoreSkuId());
			updateDetail.setSpuType(orderItem.getSpuType());
			updateDetail.setUpdateNum(adjustGoodsNum == null || adjustGoodsNum < 1 ? orderItem.getQuantity() : adjustGoodsNum);
			updateDetailList.add(updateDetail);
		}
		
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	
	// Begin V2.6.3 added by maojj 2017-10-12
	/**
	 * @Description: 团购订单构建商品更新对象
	 * @param orderGroup
	 * @param isLimitLocked 是否限制活动库存
	 * @return
	 * @throws Exception   
	 * @author maojj
	 * @date 2017年10月27日
	 */
	public StockUpdateDto buildForGroupOrder(TradeOrderGroup orderGroup,boolean isLimitLocked) throws Exception {
		StockUpdateDto stockUpdateDto = new StockUpdateDto();

		stockUpdateDto.setRpcId(UuidUtils.getUuid());
		stockUpdateDto.setMethodName("");
		stockUpdateDto.setOrderId(orderGroup.getId());
		stockUpdateDto.setStoreId(orderGroup.getStoreId());
		stockUpdateDto.setStockOperateEnum(StockOperateEnum.GROUP_ORDER_SUCCESS);

		List<StockUpdateDetailDto> updateDetailList = new ArrayList<StockUpdateDetailDto>();
		StockUpdateDetailDto updateDetail = null;
		updateDetail = new StockUpdateDetailDto();
		updateDetail.setStoreSkuId(orderGroup.getStoreSkuId());
		updateDetail.setUpdateNum(orderGroup.getGroupCount());
		if(isLimitLocked){
			updateDetail.setUpdateLockedNum(orderGroup.getGroupCount());
		}
		updateDetailList.add(updateDetail);
		stockUpdateDto.setUpdateDetailList(updateDetailList);
		return stockUpdateDto;
	}
	// End V2.6.3 added by maojj 2017-10-12
}
