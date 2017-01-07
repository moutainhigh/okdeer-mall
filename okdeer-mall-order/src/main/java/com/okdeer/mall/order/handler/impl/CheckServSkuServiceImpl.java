package com.okdeer.mall.order.handler.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.okdeer.archive.goods.store.entity.GoodsStoreSku;
import com.okdeer.archive.goods.store.enums.BSSC;
import com.okdeer.archive.goods.store.service.GoodsStoreSkuServiceApi;
import com.okdeer.archive.store.entity.StoreInfo;
import com.okdeer.archive.store.entity.StoreInfoServiceExt;
import com.okdeer.archive.store.enums.ResultCodeEnum;
import com.okdeer.mall.common.dto.Request;
import com.okdeer.mall.common.dto.Response;
import com.okdeer.mall.order.bo.CurrentStoreSkuBo;
import com.okdeer.mall.order.bo.StoreSkuParserBo;
import com.okdeer.mall.order.dto.PlaceOrderDto;
import com.okdeer.mall.order.dto.PlaceOrderItemDto;
import com.okdeer.mall.order.dto.PlaceOrderParamDto;
import com.okdeer.mall.order.enums.OrderOptTypeEnum;
import com.okdeer.mall.order.enums.OrderTypeEnum;
import com.okdeer.mall.order.handler.RequestHandler;

/**
 * ClassName: CheckServSkuServiceImpl 
 * @Description: 检查服务商品信息
 * @author maojj
 * @date 2017年1月5日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿2.0 			2017年1月5日				maojj
 */
@Service("checkServSkuService")
public class CheckServSkuServiceImpl implements RequestHandler<PlaceOrderParamDto, PlaceOrderDto> {

	/**
	 * 商品信息Service
	 */
	@Reference(version = "1.0.0", check = false)
	private GoodsStoreSkuServiceApi goodsStoreSkuServiceApi;

	@Override
	public void process(Request<PlaceOrderParamDto> req, Response<PlaceOrderDto> resp) throws Exception {
		PlaceOrderParamDto paramDto = req.getData();
		// 提取下单商品ID清单
		List<String> skuIdList = extractSkuId(paramDto.getSkuList());
		// 查询当前商品信息
		List<GoodsStoreSku> currentSkuList = findCurrentSkuList(skuIdList);
		// 判断商品列表与请求清单是否一致
		if (currentSkuList.size() != skuIdList.size()) {
			resp.setResult(ResultCodeEnum.GOODS_NOT_MATCH);
			return;
		}
		StoreSkuParserBo parserBo = new StoreSkuParserBo(currentSkuList);
		parserBo.setSkuIdList(skuIdList);
		// 缓存商品解析结果
		paramDto.put("parserBo", parserBo);
		parserBo.parseCurrentSku();
		parserBo.loadBuySkuList(paramDto.getSkuList());
		// 检查商品信息是否发生变化
		ResultCodeEnum checkResult = isChange(paramDto, parserBo);
		// 检查商品信息是否发生变化
		if (checkResult != ResultCodeEnum.SUCCESS) {
			resp.setResult(checkResult);
			return;
		}
		// 检查店铺配送费信息
		checkResult = checkFare(paramDto, parserBo);
		if (checkResult != ResultCodeEnum.SUCCESS) {
			resp.setResult(checkResult);
			return;
		}
	}

	private List<String> extractSkuId(List<PlaceOrderItemDto> itemList) {
		List<String> skuIdList = new ArrayList<String>();
		for (PlaceOrderItemDto item : itemList) {
			skuIdList.add(item.getStoreSkuId());
		}
		return skuIdList;
	}

	public List<GoodsStoreSku> findCurrentSkuList(List<String> skuIdList) throws Exception {
		return goodsStoreSkuServiceApi.selectSkuByIds(skuIdList);
	}

	public ResultCodeEnum isChange(PlaceOrderParamDto paramDto, StoreSkuParserBo parserBo) {
		ResultCodeEnum checkResult = ResultCodeEnum.SUCCESS;
		// 检查商品信息是否发生变化
		for (PlaceOrderItemDto item : paramDto.getSkuList()) {
			CurrentStoreSkuBo currentSku = parserBo.getCurrentStoreSkuBo(item.getStoreSkuId());
			// 检查是否下架
			if (currentSku.getOnline() == BSSC.UNSHELVE) {
				// 商品下架
				if (paramDto.getSkuType() == OrderTypeEnum.SERVICE_STORE_ORDER) {
					checkResult = ResultCodeEnum.SERV_GOODS_NOT_BUY;
				} else {
					checkResult = ResultCodeEnum.SERV_GOODS_NOT_EXSITS;
				}
			} else if (paramDto.getSkuType() == OrderTypeEnum.STORE_CONSUME_ORDER) {
				// 判断到店消费商品是否已过有效期
				Date endTime = currentSku.getEndTime();
				if (endTime == null || new Date().after(endTime)) {
					// 服务商品已过期，不能预约
					checkResult = ResultCodeEnum.SERV_GOODS_EXP;
				}
			}

			if (checkResult != ResultCodeEnum.SUCCESS) {
				break;
			}
		}
		return checkResult;
	}

	public ResultCodeEnum checkFare(PlaceOrderParamDto paramDto, StoreSkuParserBo parserBo) {
		ResultCodeEnum checkResult = ResultCodeEnum.SUCCESS;
		if (paramDto.getSkuType() == OrderTypeEnum.SERVICE_STORE_ORDER) {
			// 服务店扩展信息
			StoreInfoServiceExt serviceExt = ((StoreInfo) paramDto.get("storeInfo")).getStoreInfoServiceExt();
			if (serviceExt != null && serviceExt.getIsShoppingCart() == 1 && serviceExt.getIsStartingPrice() == 1
					&& serviceExt.getIsSupportPurchase() == 0) {
				BigDecimal startingPrice = serviceExt.getStartingPrice();
				// 商品总金额
				BigDecimal totalAmount = parserBo.getTotalItemAmount();
				if (totalAmount.compareTo(startingPrice) == -1) {
					// 订单总价小与起送价
					if (paramDto.getOrderOptType() == OrderOptTypeEnum.ORDER_SUBMIT) {
						// 提交订单
						checkResult = ResultCodeEnum.SERV_ORDER_AMOUT_NOT_ENOUGH;
					} else {
						checkResult = ResultCodeEnum.SERV_ORDER_AMOUT_NOT_ENOUGH_SUBMIT;
					}
				}

				if (serviceExt != null && serviceExt.getIsShoppingCart() == 1
						&& serviceExt.getIsDistributionFee() == 1) {
					// 支持购物车并且有配送费
					if (serviceExt.getIsStartingPrice() == 1) {
						// 有起送价
						if (serviceExt.getIsCollect() == 1) {
							// 已满起送价收取配送费
							parserBo.setFare(BigDecimal.valueOf(serviceExt.getDistributionFee()));
						} else {
							if (totalAmount.compareTo(startingPrice) == -1) {
								// 设置运费
								parserBo.setFare(BigDecimal.valueOf(serviceExt.getDistributionFee()));
							}
						}

					} else {
						// 没有起送价
						if (serviceExt.getIsCollect() == 1) {
							// 已满起送价收取配送费，0：否，1：是
							// 设置运费
							parserBo.setFare(BigDecimal.valueOf(serviceExt.getDistributionFee()));
						}

					}
				}
			}
		}
		return checkResult;
	}
}