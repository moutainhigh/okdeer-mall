package com.okdeer.mall.order.mq;

import java.math.BigDecimal;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.okdeer.base.common.utils.mapper.JsonMapper;
import com.okdeer.base.framework.mq.annotation.RocketMQListener;
import com.okdeer.base.framework.mq.message.MQMessage;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.mq.constants.TradeOrderTopic;


/**
 * ClassName: 订单消息接收处理新类 
 * @author tuzhd
 * @date 2016年12月12日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		V1.2.3			2016-12-12			tuzhd			 完成订单消息处理 
 */
@Service
public class TradeOrderSubScriber {
	private static final Logger logger = LoggerFactory.getLogger(TradeOrderSubScriber.class);
	
	@Resource
	private TradeOrderSubScriberHandler tradeOrderSubScriberHandler;
	
	/**
	 * @Description: 接收完成订单消息，处理相关业务
	 * @param enMessage   
	 * @throws
	 * @author tuzhd
	 * @date 2016年12月12日
	 */
	@RocketMQListener(topic = TradeOrderTopic.ORDER_COMPLETE_TOCPIC, tag = "*")
	public ConsumeConcurrentlyStatus trigger(MQMessage enMessage) {

		TradeOrder tradeOrder = (TradeOrder) enMessage.getContent();
		logger.debug("订单完成后处理开始：{}", JsonMapper.nonEmptyMapper().toJson(tradeOrder));
		try {
			//九月活动需求 改为已完成订单（原来为消费即可获取抽奖次数）
			tradeOrderSubScriberHandler.activityAddPrizeCcount(tradeOrder);
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		} catch (Exception e) {
			logger.error("订单完成后处理业务异常：{}",JsonMapper.nonEmptyMapper().toJson(tradeOrder), e);
			return ConsumeConcurrentlyStatus.RECONSUME_LATER;
		}
	}
	
}
