package com.okdeer.mall.order.pay.callback;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import com.okdeer.api.pay.enums.TradeErrorEnum;
import com.okdeer.api.pay.pay.dto.PayResponseDto;
import com.okdeer.base.common.utils.StringUtils;
import com.okdeer.base.common.utils.UuidUtils;
import com.okdeer.base.common.utils.mapper.JsonMapper;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderPay;
import com.okdeer.mall.order.enums.OrderStatusEnum;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.enums.PayTypeEnum;
import com.okdeer.mall.order.enums.SendMsgType;
import com.okdeer.mall.order.mapper.TradeOrderItemMapper;
import com.okdeer.mall.order.mapper.TradeOrderMapper;
import com.okdeer.mall.order.mapper.TradeOrderPayMapper;
import com.okdeer.mall.order.pay.ThirdStatusSubscriber;
import com.okdeer.mall.order.pay.entity.ResponseResult;
import com.okdeer.mall.order.service.TradeMessageService;
import com.okdeer.mall.order.service.TradeOrderService;
import com.okdeer.mall.order.timer.TradeOrderTimer;
import com.okdeer.mall.order.vo.SendMsgParamVo;

/**
 * ClassName: PayResultHandler 
 * @Description: 支付结果的处理者
 * @author maojj
 * @date 2016年11月14日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿1.2			2016年11月14日				maojj			支付结果的处理者
 */
public abstract class AbstractPayResultHandler {
	
	protected static final Logger logger = LoggerFactory.getLogger(ThirdStatusSubscriber.class);
	
	@Resource
	protected TradeOrderService tradeOrderService;
	
	@Autowired
	protected TradeMessageService tradeMessageService;
	
	@Autowired
	protected TradeOrderTimer tradeOrderTimer;

	@Resource
	protected TradeOrderMapper tradeOrderMapper;
	
	@Resource
	protected TradeOrderPayMapper tradeOrderPayMapper;
	
	@Resource
	protected TradeOrderItemMapper tradeOrderItemMapper;

	/**
	 * @Description: 第三方支付结果处理
	 * @param tradeOrder
	 * @param respDto
	 * @return
	 * @throws Exception   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	@Transactional
	public ConsumeConcurrentlyStatus handler(TradeOrder tradeOrder,PayResponseDto respDto) throws Exception{
		// 第一步 幂等性校验，防止重复消费
		if(isConsumed(tradeOrder)){
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}
		// 第二步 设置订单支付记录
		TradeOrderPay tradeOrderPay = buildTradeOrderPay(tradeOrder.getId(),respDto);
		tradeOrder.setTradeOrderPay(tradeOrderPay);
		// 第三步 设置订单状态
		setOrderStatus(tradeOrder);
		// 订单前置处理
		preProcessOrder(tradeOrder);
		// 处理订单项
		processOrderItem(tradeOrder,respDto);
		// 第四步 更新订单状态
		updateOrderStatus(tradeOrder);
		// 第五步 保存订单支付记录
		tradeOrderPayMapper.insertSelective(tradeOrderPay);
		// 订单后置处理
		postProcessOrder(tradeOrder);
		// 第六步 发送通知消息
		sendNotifyMessage(tradeOrder);
		// 第七步 发送超时消息
		sendTimerMessage(tradeOrder);
		
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	


	/**
	 * @Description: 余额支付
	 * @param tradeOrder
	 * @param respResult   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	public void handler(TradeOrder tradeOrder,ResponseResult respResult) throws Exception{
		// 第一步 幂等性校验，防止重复消费
		if(isConsumed(tradeOrder)){
			return;
		}
		// 判断支付结果
		if (respResult.getCode().equals(TradeErrorEnum.SUCCESS.getName())) {
			// 第二步 设置订单支付记录
			TradeOrderPay tradeOrderPay = buildTradeOrderPay(tradeOrder,respResult);
			// 第三步 设置订单状态
			setOrderStatus(tradeOrder);
			// 订单前置处理
			preProcessOrder(tradeOrder);
			// 处理订单项
			processOrderItem(tradeOrder);
			// 第四步 更新订单状态
			updateOrderStatus(tradeOrder);
			// 第五步 保存订单支付记录
			tradeOrderPayMapper.insertSelective(tradeOrderPay);
			// 订单后置处理
			postProcessOrder(tradeOrder);
			// 第六步 发送通知消息
			sendNotifyMessage(tradeOrder);
			// 第七步 发送超时消息
			sendTimerMessage(tradeOrder);
		}else{
			// 如果支付失败，只需更改订单状态
			logger.error("订单余额支付失败,订单编号为：" + tradeOrder.getOrderNo());
			tradeOrder.setStatus(OrderStatusEnum.UNPAID);
			updateOrderStatus(tradeOrder);
		}
	}
	
	/**
	 * @Description: 解析Mq消息
	 * @param msgs 消息
	 * @return 支付结果Dto 
	 * @author maojj
	 * @date 2016年11月14日
	 */
	protected PayResponseDto parseMessage(List<MessageExt> msgs){
		String msg = new String(msgs.get(0).getBody(), Charsets.UTF_8);
		logger.info("订单支付状态消息:" + msg);
		return JsonMapper.nonEmptyMapper().fromJson(msg, PayResponseDto.class);
	}
	
	/**
	 * @Description: 根据交易流水查询订单记录
	 * @param tradeNum
	 * @return   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	protected TradeOrder findByTradeNum(String tradeNum){
		if (StringUtils.isEmpty(tradeNum)) {
			return null;
		}
		//tradeOrderMapper.findByTradeNum(tradeNum);
		return null;
	}
	
	/**
	 * @Description: 幂等性校验，为防止重复消费，根据订单状态和交易记录判断消息是否已经被消费
	 * @param tradeOrder
	 * @return   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	protected boolean isConsumed(TradeOrder tradeOrder){
		if (tradeOrder == null || (tradeOrder.getStatus() != OrderStatusEnum.UNPAID
				&& tradeOrder.getStatus() != OrderStatusEnum.BUYER_PAYING)) {
			return true;
		}
		// 订单Id是否已生成支付记录
		int count = tradeOrderPayMapper.selectTradeOrderPayByOrderId(tradeOrder.getId());
		if(count > 0){
			// 如果订单Id已生成支付记录，则标识该消息已被消费
			return true;
		}
		return false;
	}
	
	protected TradeOrderPay buildTradeOrderPay(String orderId,PayResponseDto respDto){
		TradeOrderPay tradeOrderPay = new TradeOrderPay();
		tradeOrderPay.setId(UuidUtils.getUuid());
		tradeOrderPay.setOrderId(orderId);
		// 将云钱包响应的支付结果转换为商城的支付结果
		tradeOrderPay.setPayType(PayTypeEnum.enumValueOf(respDto.getPayType().ordinal()));
		tradeOrderPay.setPayAmount(respDto.getTradeAmount());
		tradeOrderPay.setPayTime(new Date());
		tradeOrderPay.setCreateTime(new Date());
		tradeOrderPay.setReturns(respDto.getFlowNo());
		return tradeOrderPay;
	}
	
	protected TradeOrderPay buildTradeOrderPay(TradeOrder tradeOrder,ResponseResult respResult){
		TradeOrderPay tradeOrderPay = new TradeOrderPay();
		tradeOrderPay.setId(UuidUtils.getUuid());
		tradeOrderPay.setOrderId(tradeOrder.getId());
		// 将云钱包响应的支付结果转换为商城的支付结果
		tradeOrderPay.setPayType(PayTypeEnum.WALLET);
		tradeOrderPay.setPayAmount(tradeOrder.getActualAmount());
		tradeOrderPay.setPayTime(new Date());
		tradeOrderPay.setCreateTime(new Date());
		return tradeOrderPay;
	}
	
	protected void setOrderStatus(TradeOrder tradeOrder){
		switch (tradeOrder.getType()) {
			case PHYSICAL_ORDER:
			case PHONE_PAY_ORDER:
			case TRAFFIC_PAY_ORDER:	
				tradeOrder.setStatus(OrderStatusEnum.DROPSHIPPING);
				break;
			case SERVICE_ORDER:
			case STORE_CONSUME_ORDER:
				// 团购
				tradeOrder.setStatus(OrderStatusEnum.HAS_BEEN_SIGNED);
				break;
			case SERVICE_STORE_ORDER:
				// 上门服务
				tradeOrder.setStatus(OrderStatusEnum.WAIT_RECEIVE_ORDER);
				break;
			default:
				break;
		}
	}
	
	protected void sendNotifyMessage(TradeOrder tradeOrder) throws Exception{
		tradeMessageService.saveSysMsg(tradeOrder, SendMsgType.createOrder);
		// 发送消息
		tradeMessageService.sendSmsByCreateOrder(tradeOrder);
		// 发送POS消息
		SendMsgParamVo sendMsgParamVo = new SendMsgParamVo(tradeOrder);
		tradeMessageService.sendPosMessage(sendMsgParamVo, SendMsgType.createOrder);
		// 到店消费订单、服务订单商家版不发送消息
		if (tradeOrder.getType() != OrderTypeEnum.SERVICE_STORE_ORDER
				&& tradeOrder.getType() != OrderTypeEnum.STORE_CONSUME_ORDER) {
			tradeMessageService.sendSellerAppMessage(sendMsgParamVo, SendMsgType.createOrder);
		}
	}
	
	/**
	 * @Description: 订单前置处理
	 * @param tradeOrder
	 * @throws Exception   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	public void preProcessOrder(TradeOrder tradeOrder) throws Exception{
		// 模板方法，留给具体的实现类处理
	}
	
	/**
	 * 重载方法
	 * @param tradeOrder
	 * @param respDto   
	 * @author guocp
	 * @date 2016年11月22日
	 */
	protected void processOrderItem(TradeOrder tradeOrder, PayResponseDto respDto) throws Exception {
		processOrderItem(tradeOrder,null);
	}
	
	/**
	 * @Description: 处理订单项
	 * @param tradeOrder
	 * @throws Exception   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	protected void processOrderItem(TradeOrder tradeOrder) throws Exception{
		// 模板方法，留给具体的实现类处理
	}
	
	protected void updateOrderStatus(TradeOrder tradeOrder) throws Exception{
		tradeOrderService.updateOrderStatus(tradeOrder);
	}
	
	public void sendTimerMessage(TradeOrder tradeOrder) throws Exception{
		// 模板方法，留给具体的实现类处理
	}
	
	/**
	 * @Description: 订单后置处理
	 * @param tradeOrder
	 * @throws Exception   
	 * @author maojj
	 * @date 2016年11月14日
	 */
	public void postProcessOrder(TradeOrder tradeOrder) throws Exception{
		// 模板方法，留给具体的实现类处理
	}
	
}
