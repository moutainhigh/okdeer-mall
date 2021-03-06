
package com.okdeer.mall.order.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Reference;
import com.okdeer.archive.stock.dto.StockUpdateDto;
import com.okdeer.archive.stock.service.GoodsStoreSkuStockApi;
import com.okdeer.jxc.stock.service.StockUpdateServiceApi;
import com.okdeer.mall.order.builder.MallStockUpdateBuilder;
import com.okdeer.mall.order.entity.TradeOrder;
import com.okdeer.mall.order.entity.TradeOrderItem;
import com.okdeer.mall.order.entity.TradeOrderRefunds;
import com.okdeer.mall.order.entity.TradeOrderRefundsItem;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.mapper.TradeOrderItemMapper;
import com.okdeer.mall.order.service.StockOperateService;

/**
 * ClassName: StockOperateServiceImpl 
 * @Description: 库存操作service实现类
 * @author zengjizu
 * @date 2016年11月11日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *
 */
@Service
public class StockOperateServiceImpl implements StockOperateService {

	@Reference(version = "1.0.0", check = false)
	private GoodsStoreSkuStockApi goodsStoreSkuStockApi;

	/**
	 * 库存管理Service
	 */
	@Reference(version = "1.0.0", check = false)
	private StockUpdateServiceApi stockUpdateServiceApi;

	@Resource
	private MallStockUpdateBuilder mallStockUpdateBuilder;

	
	@Autowired
	private TradeOrderItemMapper tradeOrderItemMapper;

	/**
	 * @Description: 根据订单回收库存
	 * @param tradeOrder
	 * @param rpcIdList
	 * @return
	 * @author zengjizu
	 * @date 2016年11月11日
	 */
	@Override
	public void recycleStockByOrder(TradeOrder tradeOrder, List<String> rpcIdList) throws Exception {
		if(tradeOrder.getType() == OrderTypeEnum.GROUP_ORDER){
			// 团购订单没有发生库存扣减。只有当团购订单转为寄送服务订单时，才存在库存的扣减
			return;
		}
		StockUpdateDto mallStockUpdate = mallStockUpdateBuilder.build(tradeOrder);
		rpcIdList.add(mallStockUpdate.getRpcId());
		goodsStoreSkuStockApi.updateStock(mallStockUpdate);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public void recycleStockByRefund(TradeOrder tradeOrder, TradeOrderRefunds orderRefunds,
			List<String> rpcIdList) throws Exception {
		List<String> orderItemIdList = extraOrderItemIdList(orderRefunds.getTradeOrderRefundsItem());
		List<TradeOrderItem> orderItemList = tradeOrderItemMapper.findOrderItemByIdList(orderItemIdList);
		
		StockUpdateDto mallStockUpdate = mallStockUpdateBuilder.build(orderRefunds, tradeOrder, orderItemList);
		rpcIdList.add(mallStockUpdate.getRpcId());
		if(mallStockUpdate != null){
			goodsStoreSkuStockApi.updateStock(mallStockUpdate);
		}
	}
	
	public List<String> extraOrderItemIdList(List<TradeOrderRefundsItem> refundsItemList){
		List<String> orderItemIdList = new ArrayList<String>();
		for(TradeOrderRefundsItem refundsItem : refundsItemList){
			orderItemIdList.add(refundsItem.getOrderItemId());
		}
		return orderItemIdList;
	}

}
