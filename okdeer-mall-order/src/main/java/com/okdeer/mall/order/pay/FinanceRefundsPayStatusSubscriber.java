/** 
 * @Copyright: Copyright ©2005-2020 yschome.com Inc. All rights reserved
 * @项目名称: yschome-mall 
 * @文件名称: RefundsPayStatusSubscriberServiceImpl.java 
 * @Date: 2016年3月23日 
 * 注意：本内容仅限于友门鹿公司内部传阅，禁止外泄以及用于其他的商业目的 
 */

package com.okdeer.mall.order.pay;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import com.okdeer.mall.order.constant.mq.PayMessageConstant;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderRefunds;
import com.okdeer.mall.order.entity.TradeOrderRefundsLog;
import com.okdeer.mall.order.enums.OrderStatusEnum;
import com.okdeer.mall.order.enums.RefundsStatusEnum;
import com.okdeer.mall.order.mapper.TradeOrderRefundsLogMapper;
import com.okdeer.mall.order.pay.entity.FinanceResponseResult;
import com.okdeer.mall.order.pay.entity.RefuseFinanceResponseResult;
import com.okdeer.mall.order.service.TradeOrderRefundsService;
import com.okdeer.mall.order.service.TradeOrderService;
import com.okdeer.base.common.utils.mapper.JsonMapper;
import com.okdeer.base.common.utils.StringUtils;
import com.okdeer.base.framework.mq.AbstractRocketMQSubscriber;

/**
 * 退款支付状态同步
 * 
 * @pr yschome-mall
 * @author guocp
 * @date 2016年3月23日 下午7:25:11
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *    1.0.Z			2016-09-05			zengj					增加订单操作记录   
 */
@Service
public class FinanceRefundsPayStatusSubscriber extends AbstractRocketMQSubscriber implements PayMessageConstant {

	private static final Logger logger = LoggerFactory.getLogger(FinanceRefundsPayStatusSubscriber.class);

	/** 退款单service */
	@Autowired
	public TradeOrderRefundsService tradeOrderRefundsService;

	/** 订单service */
	@Autowired
	public TradeOrderService tradeOrderService;

	// Begin 1.0.Z 增加订单操作记录Service add by zengj
	/**
	 * 订单操作记录Service
	 */
	@Resource
	private TradeOrderRefundsLogMapper tradeOrderRefundsLogMapper;
	// End 1.0.Z 增加订单操作记录Service add by zengj

	@Override
	public String getTopic() {
		return TOPIC_REFUND_RESULT;
	}

	@Override
	public String getTags() {
		return TAG_REFUND_RESULT + JOINT + TAG_REFUSE_REFUND_RESULT;
	}

	@Override
	public ConsumeConcurrentlyStatus subscribeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

		MessageExt message = msgs.get(0);
		String msg = new String(msgs.get(0).getBody(), Charsets.UTF_8);

		ConsumeConcurrentlyStatus status = ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		if (TAG_REFUSE_REFUND_RESULT.equals(message.getTags())) {
			// 取消订单财务支付状态同步
			logger.info("取消订单财务支付状态同步:" + msg);
			status = processCancel(msg);
		} else if (TAG_REFUND_RESULT.equals(message.getTags())) {
			// 退款单财务支付状态同步
			logger.info("退款单财务支付状态同步:" + msg);
			status = processRefund(msg);
		}
		return status;
	}

	/**
	 * 退款单财务支付结果消息处理
	 */
	private ConsumeConcurrentlyStatus processRefund(String msg) {
		try {
			FinanceResponseResult result = JsonMapper.nonEmptyMapper().fromJson(msg, FinanceResponseResult.class);
			logger.info("************退款单号*********：" + result.getRefundNo());
			TradeOrderRefunds orderRefunds = tradeOrderRefundsService.getByRefundNo(result.getRefundNo());
			if (orderRefunds == null) {
				logger.warn("退款单支付状态同步未找到退款单，交易流水号：", result.getRefundNo());
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}

			// 更新订单状态
			if (orderRefunds.getRefundsStatus() == RefundsStatusEnum.SELLER_REFUNDING) {
				logger.info("=============卖家退款中，修改退款的订单的状态=============");
				orderRefunds.setRefundsStatus(RefundsStatusEnum.REFUND_SUCCESS);
			} else if (orderRefunds.getRefundsStatus() == RefundsStatusEnum.YSC_REFUND) {
				orderRefunds.setRefundsStatus(RefundsStatusEnum.YSC_REFUND_SUCCESS);
			} else if (orderRefunds.getRefundsStatus() == RefundsStatusEnum.FORCE_SELLER_REFUND) {
				orderRefunds.setRefundsStatus(RefundsStatusEnum.FORCE_SELLER_REFUND_SUCCESS);
			}
			orderRefunds.setRefundMoneyTime(new Date());
			tradeOrderRefundsService.updateRefunds(orderRefunds);
			// Begin 1.0.Z 增加订单操作记录 add by zengj
			tradeOrderRefundsLogMapper
					.insertSelective(new TradeOrderRefundsLog(orderRefunds.getId(), orderRefunds.getOperator(),
							orderRefunds.getRefundsStatus().getName(), orderRefunds.getRefundsStatus().getValue()));
			// End 1.0.Z 增加订单操作记录 add by zengj
			logger.info("=============修改退款的订单的状态成功=============");
		} catch (Exception e) {
			logger.error("退款单支付状态同步消息处理失败", e);
			return ConsumeConcurrentlyStatus.RECONSUME_LATER;
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

	/**
	 * 取消订单财务支付结果消息处理
	 */
	private ConsumeConcurrentlyStatus processCancel(String msg) {
		try {
			RefuseFinanceResponseResult result = JsonMapper.nonEmptyMapper().fromJson(msg,
					RefuseFinanceResponseResult.class);
			if (StringUtils.isEmpty(result.getOrderId())) {
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			TradeOrder selectOrder = tradeOrderService.selectById(result.getOrderId());
			if (selectOrder.getStatus() == OrderStatusEnum.CANCELING) {
				TradeOrder tradeOrder = new TradeOrder();
				tradeOrder.setId(result.getOrderId());
				tradeOrder.setStatus(OrderStatusEnum.CANCELED);
				tradeOrder.setUpdateTime(new Date());
				tradeOrderService.updateOrderStatus(tradeOrder);
			} else if (selectOrder.getStatus() == OrderStatusEnum.REFUSING) {
				TradeOrder tradeOrder = new TradeOrder();
				tradeOrder.setId(result.getOrderId());
				tradeOrder.setStatus(OrderStatusEnum.REFUSED);
				tradeOrder.setUpdateTime(new Date());
				tradeOrderService.updateOrderStatus(tradeOrder);
			} else {
				logger.error("该订单不属于取消中状态，故无法取消退款，订单ID为" + msg);
			}
		} catch (Exception e) {
			logger.error("取消订单支付状态同步消息处理失败", e);
			return ConsumeConcurrentlyStatus.RECONSUME_LATER;
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
