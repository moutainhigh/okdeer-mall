package com.okdeer.mall.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okdeer.archive.store.enums.ResultCodeEnum;
import com.okdeer.base.common.enums.WhetherEnum;
import com.okdeer.mall.common.dto.Response;
import com.okdeer.mall.common.utils.DateUtils;
import com.okdeer.mall.order.constant.OrderTraceConstant;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderTrace;
import com.okdeer.mall.order.enums.OrderCancelType;
import com.okdeer.mall.order.enums.OrderStatusEnum;
import com.okdeer.mall.order.enums.OrderTraceEnum;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.enums.PayWayEnum;
import com.okdeer.mall.order.mapper.TradeOrderMapper;
import com.okdeer.mall.order.mapper.TradeOrderTraceMapper;
import com.okdeer.mall.order.service.TradeOrderSendMessageService;
import com.okdeer.mall.order.service.TradeOrderTraceService;
import com.okdeer.mall.order.vo.RefundsTraceResp;
import com.okdeer.mall.order.vo.RefundsTraceVo;
import com.okdeer.mall.system.utils.ConvertUtil;

/**
 * ClassName: TradeOrderTraceServiceImpl 
 * @Description: 订单轨迹服务实现类
 * @author maojj
 * @date 2016年11月4日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿1.2			2016年11月4日				maojj		      订单轨迹服务实现类
 *      友门鹿2.1         2017年2月18日                                   zhaoqc        便利店订单状态发生改变时发送通知
 */
@Service
public class TradeOrderTraceServiceImpl implements TradeOrderTraceService {

  //日志管理器
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeOrderTraceServiceImpl.class);
    
	private static final BigDecimal ZERO = BigDecimal.valueOf(0.0);

	@Resource
	private TradeOrderTraceMapper tradeOrderTraceMapper;

	@Resource
	private TradeOrderMapper tradeOrderMapper;

	@Resource
	private TradeOrderSendMessageService sendMessageService;
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrderTrace(TradeOrder tradeOrder) {
	    //便利店实物订单状态改变时，发送通知
//        if(tradeOrder.getType() == OrderTypeEnum.PHYSICAL_ORDER) {
//            //便利店订单状态发生改变发送消息
//            LOGGER.info("便利店订单状态发生改变向用户发送通知消息");
//            //向用户推送消息时，输出订单信息的日志
//            LOGGER.info("订单Id:{},订单号：{}", tradeOrder.getId(), tradeOrder.getOrderNo());
//            this.sendMessageService.tradeSendMessage(tradeOrder, null);
//        }
	        
		// 只有上门服务的订单需要保存订单轨迹。
		if (tradeOrder.getType() != OrderTypeEnum.SERVICE_STORE_ORDER) {
			return;
		}
		// 如果订单状态为待付款且订单实付金额为0，不记录轨迹
		if (tradeOrder.getStatus() == OrderStatusEnum.UNPAID && ZERO.compareTo(tradeOrder.getActualAmount()) == 0) {
			return;
		}
		// 创建订单轨迹
		List<TradeOrderTrace> traceList = buildTraceList(tradeOrder);
		// 保存轨迹
		if (CollectionUtils.isEmpty(traceList)) {
			return;
		}
		for (TradeOrderTrace trace : traceList) {
			tradeOrderTraceMapper.add(trace);
		}
	}

	/**
	 * @Description: 构建订单轨迹列表
	 * @param tradeOrder
	 * @return   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private List<TradeOrderTrace> buildTraceList(TradeOrder tradeOrder) {
		List<TradeOrderTrace> traceList = new ArrayList<TradeOrderTrace>();
		Date currentDate = new Date();
		// 构建操作轨迹
		TradeOrderTrace optTrace = buildOptTrace(tradeOrder);
		if (optTrace != null && optTrace.getTraceStatus() != null) {
			traceList.add(optTrace);
		}
		if (tradeOrder.getStatus() == OrderStatusEnum.UNPAID) {
			// 订单状态为待付款时，需要多记录一条轨迹
			TradeOrderTrace trace = new TradeOrderTrace(tradeOrder.getId());
			trace.setTraceStatus(OrderTraceEnum.WAIT_PAID);
			trace.setRemark(OrderTraceConstant.WAIT_PAID_REMARK);
			traceList.add(trace);
		}
		if (tradeOrder.getStatus() == OrderStatusEnum.WAIT_RECEIVE_ORDER && tradeOrder.getPayWay() == PayWayEnum.OFFLINE_CONFIRM_AND_PAY){
			// 如果是线下确认并当面支付的订单，需要记录提交订单和待付款的轨迹。
			TradeOrderTrace placeOrderTrace = new TradeOrderTrace(tradeOrder.getId());
			placeOrderTrace.setOptTime(currentDate);
			placeOrderTrace.setTraceStatus(OrderTraceEnum.SUBMIT_ORDER);
			placeOrderTrace.setRemark(String.format(OrderTraceConstant.SUBMIT_ORDER_REMARK, tradeOrder.getOrderNo()));
			traceList.add(placeOrderTrace);
			
			TradeOrderTrace waitPaidTrace = new TradeOrderTrace(tradeOrder.getId());
			waitPaidTrace.setOptTime(currentDate);
			waitPaidTrace.setTraceStatus(OrderTraceEnum.WAIT_PAID);
			waitPaidTrace.setRemark(OrderTraceConstant.WAIT_PAID_OFFLINE_CONFIRM_REMARK);
			traceList.add(waitPaidTrace);
		}
		return traceList;
	}

	/**
	 * @Description: 构建操作轨迹
	 * @param tradeOrder 交易订单
	 * @return   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private TradeOrderTrace buildOptTrace(TradeOrder tradeOrder) {
		TradeOrderTrace trace = new TradeOrderTrace(tradeOrder.getId());
		// 是否需要记录轨迹
		boolean isNeedTrace = true;
		switch (tradeOrder.getStatus()) {
			case UNPAID:
				trace.setTraceStatus(OrderTraceEnum.SUBMIT_ORDER);
				trace.setRemark(String.format(OrderTraceConstant.SUBMIT_ORDER_REMARK, tradeOrder.getOrderNo()));
				break;
			case WAIT_RECEIVE_ORDER:
				trace.setTraceStatus(OrderTraceEnum.WAIT_RECEIVE);
				if (tradeOrder.getPayWay() == PayWayEnum.OFFLINE_CONFIRM_AND_PAY) {
					trace.setRemark(OrderTraceConstant.WAIT_RECEIVE_OFFLINE_CONFIRM_REMARK);
				} else {
					trace.setRemark(OrderTraceConstant.WAIT_RECEIVE_REMARK);
				}
				break;
			case DROPSHIPPING:
				trace.setTraceStatus(OrderTraceEnum.WAIT_DISPATCHED);
				trace.setRemark(OrderTraceConstant.WAIT_DISPATCHED_REMARK);
				break;
			case TO_BE_SIGNED:
				trace.setTraceStatus(OrderTraceEnum.SET_OUT);
				trace.setRemark(OrderTraceConstant.SET_OUT_REMARK);
				break;
			case CANCELED:
			case CANCELING:
			case REFUSING:
			case REFUSED:
				processCancelTrace(trace,tradeOrder);
				break;
			case HAS_BEEN_SIGNED:
				trace.setTraceStatus(OrderTraceEnum.COMPLETED);
				trace.setRemark(OrderTraceConstant.COMPLETED_REMARK);
				break;
			default:
				// 如果订单状态不匹配上述任意状态，则不需要记录轨迹
				isNeedTrace = false;
				break;
		}
		if (!isNeedTrace) {
			return null;
		}
		return trace;
	}

	/**
	 * @Description: 获取订单取消的备注
	 * @param tradeOrder 交易订单
	 * @return   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private void processCancelTrace(TradeOrderTrace trace,TradeOrder tradeOrder) {
		String remark = "";
		// 订单取消原因
		String reason = tradeOrder.getReason();
		// 判断订单取消类型：0：系统取消，1：用户取消，2：商家取消
		OrderCancelType cancelType = tradeOrder.getCancelType();
		// 是否有违约金 0:否，1:是
		WhetherEnum isBreach = tradeOrder.getIsBreach();
		// 查询当前订单
		TradeOrder currentOrder = tradeOrderMapper.selectByPrimaryKey(tradeOrder.getId());
		if (currentOrder.getPayWay() == PayWayEnum.OFFLINE_CONFIRM_AND_PAY) {
			if (cancelType == OrderCancelType.CANCEL_BY_BUYER) {
				remark = String.format(OrderTraceConstant.CANCEL_ON_UNPAID, reason);
			} else if (cancelType == OrderCancelType.CANCEL_BY_SELLER) {
				remark = String.format(OrderTraceConstant.CANCEL_BY_SELLER_OFFLINE_CONFIRM, reason);
			} else if (cancelType == OrderCancelType.CANCEL_BY_SYSTEM) {
				remark = OrderTraceConstant.CANCEL_BY_SELLER_TIMEOUT_OFFLINE_CONFIRM;
			}
		} else {
			switch (currentOrder.getStatus()) {
				case UNPAID:
					if (cancelType == OrderCancelType.CANCEL_BY_BUYER) {
						remark = String.format(OrderTraceConstant.CANCEL_ON_UNPAID, reason);
					} else {
						remark = OrderTraceConstant.CANCEL_ON_TIMEOUT;
					}
					break;
				case WAIT_RECEIVE_ORDER:
					if (cancelType == OrderCancelType.CANCEL_BY_BUYER) {
						remark = String.format(OrderTraceConstant.CANCEL_BY_USER, reason);
					} else if (cancelType == OrderCancelType.CANCEL_BY_SELLER) {
						remark = String.format(OrderTraceConstant.CANCEL_BY_SELLER, reason);
					} else if (cancelType == OrderCancelType.CANCEL_BY_SYSTEM) {
						remark = OrderTraceConstant.CANCEL_BY_SELLER_TIMEOUT;
					}
					break;
				case DROPSHIPPING:
					if (cancelType == OrderCancelType.CANCEL_BY_BUYER) {
						if (isBreach == WhetherEnum.whether) {
							// 第二个参数时违约金的百分比
							remark = String.format(OrderTraceConstant.CANCEL_BY_USER_BREAK_CONTRACT, reason, tradeOrder.getBreachPercent(),ConvertUtil.format(tradeOrder.getBreachMoney()));
						} else {
							remark = String.format(OrderTraceConstant.CANCEL_BY_USER_DROPSHIPPING, reason);
						}
					} else if (cancelType == OrderCancelType.CANCEL_BY_SELLER) {
						remark = String.format(OrderTraceConstant.CANCEL_BY_SELLER, reason);
					} else if (cancelType == OrderCancelType.CANCEL_BY_SYSTEM) {
						remark = OrderTraceConstant.CANCEL_BY_SELLER_TIMEOUT;
					}
					break;
				default:
					break;
			}
		}
		if(StringUtils.isNotEmpty(remark)){
			trace.setTraceStatus(OrderTraceEnum.CANCELED);
			trace.setRemark(remark);
		}
	}
	
	@Override
	public Response<RefundsTraceResp> findOrderTrace(String orderId) {
		Response<RefundsTraceResp> resp = new Response<RefundsTraceResp>();
		RefundsTraceResp respData = new RefundsTraceResp();
		// 根据订单Id查询订单
		TradeOrder tradeOrder = tradeOrderMapper.selectByPrimaryKey(orderId);
		// 根据订单ID查询轨迹列表
		List<TradeOrderTrace> traceList = tradeOrderTraceMapper.findTraceList(orderId);
		// 定义返回给App的退款轨迹列表
		List<RefundsTraceVo> traceVoList = new ArrayList<RefundsTraceVo>();
		RefundsTraceVo traceVo = null;
		// 判断是否为历史退款单。如果是，则直接返回
		if (isHistory(traceList)) {
			// 如果是历史定单，直接响应。
			resp.setCode(ResultCodeEnum.SUCCESS.getCode());
			// 是否为历史订单，0：否，1：是
			respData.setIsHistory(WhetherEnum.whether.ordinal());
			resp.setData(respData);
			return resp;
		}
		for (TradeOrderTrace trace : traceList) {
			traceVo = new RefundsTraceVo();
			traceVo.setTitle(trace.getTraceStatus().getDesc());
			traceVo.setTraceStatus(trace.getTraceStatus());
			traceVo.setContent(trace.getRemark());
			traceVo.setTime(DateUtils.formatDate(trace.getOptTime(), "MM-dd HH:mm"));
			// 是否已完成 0：否，1：是
			traceVo.setIsDone(WhetherEnum.whether.ordinal());
			traceVoList.add(traceVo);
		}
		// 填充未完成的轨迹节点
		fillUncompletedTrace(traceVoList, tradeOrder.getStatus());
		respData.setTraceList(traceVoList);
		resp.setData(respData);
		resp.setCode(ResultCodeEnum.SUCCESS.getCode());
		return resp;
	}

	/**
	 * @Description: 判断是否为历史记录
	 * @param traceList
	 * @return   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private boolean isHistory(List<TradeOrderTrace> traceList) {
		if (CollectionUtils.isEmpty(traceList)) {
			// 没有任何轨迹，则认为是历史退款单
			return true;
		}
		TradeOrderTrace firstNode = traceList.get(0);
		// 如果第一个轨迹节点不是提交订单或者是待接单，也认为是历史订单
		return firstNode.getTraceStatus() != OrderTraceEnum.SUBMIT_ORDER
				&& firstNode.getTraceStatus() != OrderTraceEnum.WAIT_RECEIVE;
	}

	/**
	 * @Description: 填充未完成的轨迹节点
	 * @param traceVoList
	 * @param orderStatus   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private void fillUncompletedTrace(List<RefundsTraceVo> traceVoList, OrderStatusEnum orderStatus) {
		switch (orderStatus) {
			case UNPAID:
			case BUYER_PAYING:
				traceVoList.add(buildUncompletedTrace(OrderTraceEnum.WAIT_RECEIVE.getDesc(),
						OrderTraceConstant.WAIT_RECEIVE_REMARK));
				traceVoList.add(buildUncompletedTrace(OrderTraceEnum.WAIT_DISPATCHED.getDesc(),
						OrderTraceConstant.WAIT_DISPATCHED_REMARK));
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.SET_OUT.getDesc(), OrderTraceConstant.SET_OUT_REMARK));
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.COMPLETED.getDesc(), OrderTraceConstant.COMPLETED_REMARK));
				break;
			case WAIT_RECEIVE_ORDER:
				traceVoList.add(buildUncompletedTrace(OrderTraceEnum.WAIT_DISPATCHED.getDesc(),
						OrderTraceConstant.WAIT_DISPATCHED_REMARK));
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.SET_OUT.getDesc(), OrderTraceConstant.SET_OUT_REMARK));
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.COMPLETED.getDesc(), OrderTraceConstant.COMPLETED_REMARK));
				break;
			case DROPSHIPPING:
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.SET_OUT.getDesc(), OrderTraceConstant.SET_OUT_REMARK));
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.COMPLETED.getDesc(), OrderTraceConstant.COMPLETED_REMARK));
				break;
			case TO_BE_SIGNED:
				traceVoList.add(
						buildUncompletedTrace(OrderTraceEnum.COMPLETED.getDesc(), OrderTraceConstant.COMPLETED_REMARK));
				break;
			default:
				break;
		}
	}

	/**
	 * @Description: 构建未完成的轨迹节点
	 * @param title
	 * @param content
	 * @return   
	 * @author maojj
	 * @date 2016年11月7日
	 */
	private RefundsTraceVo buildUncompletedTrace(String title, String content) {
		RefundsTraceVo traceVo = new RefundsTraceVo();
		traceVo.setTitle(title);
		traceVo.setContent(content);
		traceVo.setIsDone(WhetherEnum.not.ordinal());
		return traceVo;
	}

	@Override
	public void updateRemarkAfterAppraise(TradeOrderTrace trace) {
		tradeOrderTraceMapper.updateRemarkAfterAppraise(trace);
	}
}
