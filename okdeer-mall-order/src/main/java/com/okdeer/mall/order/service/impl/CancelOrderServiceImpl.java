
package com.okdeer.mall.order.service.impl;

import static com.okdeer.common.consts.DescriptConstants.ORDER_STATUS_CHANGE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.message.Message;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.okdeer.archive.goods.store.service.GoodsStoreSkuServiceServiceApi;
import com.okdeer.archive.store.service.StoreInfoServiceApi;
import com.okdeer.base.common.enums.Disabled;
import com.okdeer.base.common.enums.WhetherEnum;
import com.okdeer.base.common.exception.ServiceException;
import com.okdeer.base.common.utils.DateUtils;
import com.okdeer.base.common.utils.StringUtils;
import com.okdeer.base.common.utils.UuidUtils;
import com.okdeer.base.framework.mq.RocketMQProducer;
import com.okdeer.mall.activity.coupons.enums.ActivityTypeEnum;
import com.okdeer.mall.activity.coupons.service.ActivityCouponsRecordService;
import com.okdeer.mall.activity.coupons.service.ActivitySaleRecordService;
import com.okdeer.mall.activity.discount.service.ActivityDiscountRecordService;
import com.okdeer.mall.activity.discount.service.ActivityJoinRecordService;
import com.okdeer.mall.activity.seckill.service.ActivitySeckillRecordService;
import com.okdeer.mall.ele.service.ExpressService;
import com.okdeer.mall.order.bo.TradeOrderContext;
import com.okdeer.mall.order.constant.mq.PayMessageConstant;
import com.okdeer.mall.order.entity.ActivityJoinRecord;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderItem;
import com.okdeer.mall.order.entity.TradeOrderLog;
import com.okdeer.mall.order.entity.TradeOrderPay;
import com.okdeer.mall.order.enums.OrderCancelType;
import com.okdeer.mall.order.enums.OrderStatusEnum;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.enums.PayWayEnum;
import com.okdeer.mall.order.enums.SendMsgType;
import com.okdeer.mall.order.mapper.TradeOrderItemMapper;
import com.okdeer.mall.order.mapper.TradeOrderLogMapper;
import com.okdeer.mall.order.mapper.TradeOrderMapper;
import com.okdeer.mall.order.service.CancelOrderService;
import com.okdeer.mall.order.service.StockOperateService;
import com.okdeer.mall.order.service.TradeMessageService;
import com.okdeer.mall.order.service.TradeOrderPayService;
import com.okdeer.mall.order.service.TradeOrderTraceService;
import com.okdeer.mall.order.service.TradePinMoneyUseService;
import com.okdeer.mall.order.service.TradeorderProcessLister;
import com.okdeer.mall.order.vo.SendMsgParamVo;
import com.okdeer.mall.system.mq.RollbackMQProducer;

/**
 * ClassName: CancelOrderServiceImpl 
 * @Description: 取消订单service实现类
 * @author zengjizu
 * @date 2016年11月10日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *     v1.2.0            2016-11-11          zengjz           重写取消订单代码
 *     V2.3 			 2017-04-22			 maojj			  取消时释放用户满减记录
 */
@Service
public class CancelOrderServiceImpl implements CancelOrderService {

	private static final Logger logger = LoggerFactory.getLogger(CancelOrderServiceImpl.class);

	@Reference(version = "1.0.0", check = false)
	private StoreInfoServiceApi storeInfoService;

	@Autowired
	private TradeOrderItemMapper tradeOrderItemMapper;

	@Autowired
	private ActivityCouponsRecordService activityCouponsRecordService;

	@Autowired
	private TradePinMoneyUseService tradePinMoneyUseService;

	/**
	 * 支付service
	 */
	@Autowired
	private TradeOrderPayService tradeOrderPayService;

	@Reference(version = "1.0.0", check = false)
	private GoodsStoreSkuServiceServiceApi goodsStoreSkuServiceService;

	@Autowired
	private ActivitySeckillRecordService activitySeckillRecordService;

	/**
	 * 特惠活动记录信息mapper
	 */
	@Autowired
	private ActivitySaleRecordService activitySaleRecordService;

	/**
	 * 消息发送
	 */
	@Autowired
	private TradeMessageService tradeMessageService;

	/**
	 * 回滚消息生产者
	 */
	@Resource
	private RollbackMQProducer rollbackMQProducer;

	/**
	 * 订单操作记录mapper
	 */
	@Autowired
	private TradeOrderLogMapper tradeOrderLogMapper;

	/**
	 * 订单轨迹服务
	 */
	@Resource
	private TradeOrderTraceService tradeOrderTraceService;

	@Autowired
	private TradeOrderMapper tradeOrderMapper;

	@Autowired
	private StockOperateService stockOperateService;

	/**
	 * 消息发送对象
	 */
	@Resource
	private RocketMQProducer rocketMQProducer;

	@Resource
	private ActivityDiscountRecordService activityDiscountRecordService;

	/**
	 * 第三方支付订单取消短信文案
	 */
	@Value("${third.pay.cancel.order}")
	private String thirdPayCancelOrder;

	/**
	 * 余额支付订单取消短信文案
	 */
	@Value("${balance.pay.cancel.order}")
	private String balancePayCancelOrder;

	@Autowired
	@Qualifier(value = "jxcSynTradeorderProcessLister")
	private TradeorderProcessLister tradeorderProcessLister;

	@Resource
	private ActivityJoinRecordService activityJoinRecordService;

	/**
	 * 注入配送-service
	 */
	@Autowired
	private ExpressService expressService;

	/**
	 * @Description: 取消订单
	 * @param order 订单
	 * @author zengjizu
	 * @throws ServiceException 
	 * @date 2016年11月10日
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean cancelOrder(TradeOrder tradeOrder, boolean isBuyerOperate) throws Exception {
		tradeOrder.setTradeOrderItem(tradeOrderItemMapper.selectTradeOrderItem(tradeOrder.getId()));
		String operator = null;
		if (isBuyerOperate) {
			operator = tradeOrder.getUserId();
		} else {
			operator = tradeOrder.getUpdateUserId();
		}
		updateCancelOrder(tradeOrder, operator);
		return true;
	}

	protected String joinMsgConten(String template, String... param) {
		int idx = 0;
		for (int i = 0; i < param.length; i++) {
			logger.info("param[i]:{}", param[i]);
			idx = template.indexOf("#");
			template = template.replaceFirst(String.valueOf(template.charAt(idx)), param[i]);
		}
		return template;
	}

	/**
	 * @Description: 订单取消（拒收）
	 * @param tradeOrder
	 * @param operator
	 * @throws Exception   
	 * @author guocp
	 * @date 2017年8月14日
	 */
	@Transactional(rollbackFor = Exception.class)
	private void updateCancelOrder(TradeOrder tradeOrder, String operator) throws Exception {
		List<String> rpcIdList = new ArrayList<String>();
		try {
			// 判断是否付款，如果付款需要先退款--->取消订单完成
			TradeOrder oldOrder = new TradeOrder();
			BeanUtils.copyProperties(tradeOrder, oldOrder);
			tradeOrder.setCurrentStatus(oldOrder.getStatus());

			// 用户取消时判断是否需要收取违约金
			boolean isBreach = isBreach(tradeOrder.getCancelType(), oldOrder);
			if (isBreach) {
				// 如果需要收取违约金
				tradeOrder.setIsBreach(WhetherEnum.whether);
			}
			// 设置订单状态
			setOrderStatus(tradeOrder, oldOrder);
			// 设置退款金额
			setReturnAmount(tradeOrder, oldOrder);
			// 释放活动锁定
			releaseActivity(tradeOrder, oldOrder);
			// 保存订单轨迹
			saveOrderLog(tradeOrder, operator);
			// 保存订单轨迹
			tradeOrderTraceService.saveOrderTrace(tradeOrder);

			// 更新订单状态
			Integer updateRows = tradeOrderMapper.updateOrderStatus(tradeOrder);
			logger.info("更新后的订单状态:{}", tradeOrder.getStatus().getName());
			if (updateRows == null || updateRows.intValue() == 0) {
				throw new Exception(ORDER_STATUS_CHANGE);
			}
			// 回收库存
			stockOperateService.recycleStockByOrder(tradeOrder, rpcIdList);
			// 发送短信
			if (OrderStatusEnum.DROPSHIPPING == oldOrder.getStatus()
					|| OrderStatusEnum.TO_BE_SIGNED == oldOrder.getStatus()
					|| OrderStatusEnum.WAIT_RECEIVE_ORDER == oldOrder.getStatus()) {
				// 查询支付信息
				TradeOrderPay tradeOrderPay = tradeOrderPayService.selectByOrderId(oldOrder.getId());
				tradeOrder.setTradeOrderPay(tradeOrderPay);
				tradeMessageService.sendSmsByCancel(tradeOrder, oldOrder.getStatus());
			}
			// 发消息到云钱包，关闭订单
			if (OrderStatusEnum.UNPAID == oldOrder.getStatus()) {
				// 只有待支付订单状态需要关闭
				sendCancelMsg(tradeOrder.getTradeNum());
			}
			// 如果是货到付款的,走拒收的流程
			if (oldOrder.getPayWay() != PayWayEnum.PAY_ONLINE) {
				// add by zhangkeneng 和左文明对接丢消息
				TradeOrderContext tradeOrderContext = new TradeOrderContext();
				tradeOrderContext.setTradeOrder(tradeOrder);
				tradeOrderContext.setTradeOrderPay(tradeOrder.getTradeOrderPay());
				tradeOrderContext.setItemList(tradeOrder.getTradeOrderItem());
				tradeOrderContext.setTradeOrderLogistics(tradeOrder.getTradeOrderLogistics());
				tradeorderProcessLister.tradeOrderStatusChange(tradeOrderContext);
			}
			
			if(tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER){
				// begin V2.5.0 add by wangf01 20170629
				expressService.cancelExpressOrder(tradeOrder.getOrderNo());
				// end V2.5.0 add by wangf01 20170629
			}
			
			// 最后一步退款，避免出现发送了消息，后续操作失败了，无法回滚资金
			if (OrderStatusEnum.UNPAID != oldOrder.getStatus()) {
				// 如果不是支付中的状态是需要退款给用户的
				this.tradeOrderPayService.cancelOrderPay(tradeOrder);
			}
			// begin V2.7.0 xuzq 20171225 订单状态改变 推送商家版app消息
			SendMsgParamVo sendMsgParamVo = new SendMsgParamVo(tradeOrder);
			tradeMessageService.sendSellerAppMessage(sendMsgParamVo, SendMsgType.orderStatusUpdate);
			// end V2.7.0 xuzq 20171225 订单状态改变 推送商家版app消息
		} catch (Exception e) {
			rollbackMQProducer.sendStockRollbackMsg(rpcIdList);
			throw e;
		}
	}

	/**
	 * @Description: 设置退款金额
	 * @param tradeOrder
	 * @param oldOrder
	 * @author zengjizu
	 * @date 2017年8月23日
	 */
	private void setReturnAmount(TradeOrder tradeOrder, TradeOrder oldOrder) {
		if (OrderStatusEnum.UNPAID == oldOrder.getStatus()) {
			// 待支付中，退款金额为0
			tradeOrder.setActualReturnAmount(new BigDecimal(0));
			return;
		}
		// 默认是等于实际支付金额
		BigDecimal returnAmount = tradeOrder.getActualAmount();
		tradeOrder.setActualReturnAmount(tradeOrder.getActualAmount());
		if (tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER && (tradeOrder.getStatus() == OrderStatusEnum.REFUSED
				|| tradeOrder.getStatus() == OrderStatusEnum.REFUSING)) {
			// 实物订单
			if (tradeOrder.getFare() == null) {
				tradeOrder.setFare(new BigDecimal(0));
			}
			if (tradeOrder.getRealFarePreferential() == null) {
				tradeOrder.setRealFarePreferential(new BigDecimal(0));
			}
			// 减去运费
			returnAmount = returnAmount.subtract(tradeOrder.getFare().subtract(tradeOrder.getRealFarePreferential()));
		} else if (tradeOrder.getType() == OrderTypeEnum.SERVICE_STORE_ORDER
				&& tradeOrder.getIsBreach() == WhetherEnum.whether) {
			// 上门服务订单
			returnAmount = returnAmount.subtract(tradeOrder.getBreachMoney());
		}
		tradeOrder.setActualReturnAmount(returnAmount);
	}

	/**
	 * @Description: 保存订单操作日志
	 * @param tradeOrder
	 * @param operator   
	 * @author guocp
	 * @date 2017年8月14日
	 */
	private void saveOrderLog(TradeOrder tradeOrder, String operator) {
		// 保存订单操作日志信息
		TradeOrderLog tradeOrderLog = new TradeOrderLog();
		tradeOrderLog.setId(UuidUtils.getUuid());
		tradeOrderLog.setOperate(tradeOrder.getStatus().getValue() + "---" + tradeOrder.getStatus().getName());
		tradeOrderLog.setOperateUser(operator);
		tradeOrderLog.setRecordTime(new Date());
		tradeOrderLog.setOrderId(tradeOrder.getId());
		tradeOrderLogMapper.insertSelective(tradeOrderLog);
	}

	/**
	 * @Description: 设置订单状态
	 * @param tradeOrder
	 * @param oldOrder   
	 * @author guocp
	 * @throws Exception 
	 * @date 2017年8月14日
	 */
	private void setOrderStatus(TradeOrder tradeOrder, TradeOrder oldOrder) throws Exception {
		// 订单状态为已发货或者待发货，全部变为取消中
		if (OrderStatusEnum.DROPSHIPPING == oldOrder.getStatus()
				|| OrderStatusEnum.WAIT_RECEIVE_ORDER == oldOrder.getStatus()) {
			if (oldOrder.getPayWay() == PayWayEnum.PAY_ONLINE) {
				// begin modify by zengjz 违约金逻辑判断
				if (WhetherEnum.whether == tradeOrder.getIsBreach()
						&& oldOrder.getBreachMoney().compareTo(oldOrder.getActualAmount()) <= 0) {
					// 如果违约金是百分白的话，直接把订单状态改为取消完成
					tradeOrder.setStatus(OrderStatusEnum.CANCELED);
				} else {
					tradeOrder.setStatus(OrderStatusEnum.CANCELING);
				}
				// end modify by zengjz 违约金逻辑判断
			} else {
				tradeOrder.setStatus(OrderStatusEnum.CANCELED);
			}
		} else if (OrderStatusEnum.TO_BE_SIGNED == oldOrder.getStatus()) {
			if (oldOrder.getPayWay() == PayWayEnum.PAY_ONLINE) {
				tradeOrder.setStatus(OrderStatusEnum.REFUSING);
			} else {
				tradeOrder.setStatus(OrderStatusEnum.REFUSED);
			}
		} else if (OrderStatusEnum.UNPAID == oldOrder.getStatus()) {
			// 未支付订单变成已取消
			tradeOrder.setStatus(OrderStatusEnum.CANCELED);
		} else {
			throw new Exception(ORDER_STATUS_CHANGE);
		}
	}

	/**
	 * @Description: 释放活动锁定
	 * @param tradeOrder   
	 * @author guocp
	 * @throws ServiceException 
	 * @date 2017年8月14日
	 */
	private void releaseActivity(TradeOrder tradeOrder, TradeOrder oldOrder) throws ServiceException {
		if (tradeOrder.getActivityType() == ActivityTypeEnum.VONCHER
				|| StringUtils.isNotEmpty(tradeOrder.getFareActivityId())) {
			if (tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER
					|| OrderStatusEnum.DROPSHIPPING != oldOrder.getStatus()
					|| (OrderCancelType.CANCEL_BY_BUYER != tradeOrder.getCancelType())) {
				// 如果实物订单或者不是待服务状态或者不是用户取消的就需要释放代金卷
				// 释放所有代金卷
				activityCouponsRecordService.releaseConpons(tradeOrder);
			}
		}
		
		// 团购活动参与记录 或满赠 或加价购或N件X元活动
		activityJoinRecordService.updateByOrderId(new ActivityJoinRecord(tradeOrder.getId(), Disabled.invalid));
		
		if (tradeOrder.getActivityType() == ActivityTypeEnum.SECKILL_ACTIVITY) {
			// 秒杀活动释放购买记录
			activitySeckillRecordService.updateStatusBySeckillId(tradeOrder.getId());
		}
		if (tradeOrder.getActivityType() == ActivityTypeEnum.FULL_REDUCTION_ACTIVITIES) {
			// Begin V2.3 added by maojj 2017-04-22
			// 释放用户参与满减活动的频次。规则与代金券保持一致。
			if (tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER
					|| OrderStatusEnum.DROPSHIPPING != oldOrder.getStatus()
					|| (OrderCancelType.CANCEL_BY_BUYER != tradeOrder.getCancelType())) {
				// 如果实物订单或者不是待服务状态或者不是用户取消的就需要释放
				activityDiscountRecordService.deleteByOrderId(tradeOrder.getId());
			}
			// End V2.3 added by maojj 2017-04-22
		}

		// 特惠活动释放限购数量
		for (TradeOrderItem item : tradeOrder.getTradeOrderItem()) {
			Map<String, Object> params = Maps.newHashMap();
			params.put("orderId", item.getOrderId());
			params.put("storeSkuId", item.getStoreSkuId());
			activitySaleRecordService.updateDisabledByOrderId(params);
		}

		// 释放零花钱
		if (tradeOrder.getPinMoney().compareTo(BigDecimal.ZERO) > 0) {
			if (tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER
					|| OrderStatusEnum.DROPSHIPPING != oldOrder.getStatus()
					|| (OrderCancelType.CANCEL_BY_BUYER != tradeOrder.getCancelType())) {
				// 释放订单占用零花钱
				tradePinMoneyUseService.releaseOrderOccupy(tradeOrder.getId());
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateWithUserRefuse(TradeOrder tradeOrder) throws Exception {
		tradeOrder.setTradeOrderItem(tradeOrderItemMapper.selectTradeOrderItem(tradeOrder.getId()));
		updateCancelOrder(tradeOrder, tradeOrder.getUpdateUserId());
	}

	/**
	 * @Description: 是否收取违约金
	 * @param cancelType 取消类型
	 * @param tradeOrder 
	 * @return   
	 * @author maojj
	 * @date 2016年11月9日
	 */
	private boolean isBreach(OrderCancelType cancelType, TradeOrder tradeOrder) {
		if (tradeOrder.getType() != OrderTypeEnum.SERVICE_STORE_ORDER
				|| BigDecimal.valueOf(0.0).compareTo(tradeOrder.getActualAmount()) == 0) {
			// 不是服务订单或者实付金额为0的，不收取违约金
			return false;
		}
		if (cancelType != OrderCancelType.CANCEL_BY_BUYER || tradeOrder.getIsBreachMoney() == null
				|| tradeOrder.getIsBreachMoney() == WhetherEnum.not) {
			// 不是用户取消的，均不收取违约金。店铺未设置违约金的不收取。
			return false;
		}
		if (tradeOrder.getStatus() == OrderStatusEnum.UNPAID || tradeOrder.getStatus() == OrderStatusEnum.BUYER_PAYING
				|| tradeOrder.getStatus() == OrderStatusEnum.WAIT_RECEIVE_ORDER) {
			// 未支付或者待接单状态下的，不收取违约金
			return false;
		}
		// 获取订单的预约服务时间
		String servTime = tradeOrder.getPickUpTime();
		if (StringUtils.isEmpty(servTime) || servTime.length() < 16) {
			return false;
		}
		Date servDate = DateUtils.parseDate(servTime.substring(0, 16));
		// 当前时间和服务时间的时间差
		long diffTime = servDate.getTime() - System.currentTimeMillis();
		if (diffTime < tradeOrder.getBreachTime().intValue() * 60 * 60 * 1000) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isBreach(String orderId) throws Exception {
		TradeOrder tradeOrder = tradeOrderMapper.selectByPrimaryKey(orderId);
		if (tradeOrder == null) {
			throw new Exception("订单不存在");
		}
		return isBreach(OrderCancelType.CANCEL_BY_BUYER, tradeOrder);
	}

	/**
	 * @Description: 订单取消发送消息到云钱包
	 * @param tradeNum 商户订单号
	 * @throws Exception
	 * @author zengjizu
	 * @date 2016年11月22日
	 */
	private void sendCancelMsg(String tradeNum) throws Exception {
		Map<String, String> msgMap = Maps.newHashMap();
		msgMap.put("tradeNum", tradeNum);

		String sendStr = JSONObject.toJSONString(msgMap);
		logger.debug("发送消息到云钱包{}", sendStr);
		Message msg = new Message(PayMessageConstant.TOPIC_ORDER_STATUS_CHANGE, PayMessageConstant.TAG_ORDER_CANCELED,
				sendStr.getBytes(Charsets.UTF_8));
		SendResult sendResult = rocketMQProducer.send(msg);
		if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
			throw new Exception("发送消息到云钱包失败");
		}
	}
}
