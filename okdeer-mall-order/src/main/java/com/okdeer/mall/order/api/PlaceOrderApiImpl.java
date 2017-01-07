package com.okdeer.mall.order.api;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.alibaba.dubbo.config.annotation.Service;
import com.okdeer.archive.store.entity.StoreInfo;
import com.okdeer.mall.common.dto.Request;
import com.okdeer.mall.common.dto.Response;
import com.okdeer.mall.order.bo.AppAdapter;
import com.okdeer.mall.order.bo.CurrentStoreSkuBo;
import com.okdeer.mall.order.bo.StoreSkuParserBo;
import com.okdeer.mall.order.dto.AppStoreDto;
import com.okdeer.mall.order.dto.PlaceOrderDto;
import com.okdeer.mall.order.dto.PlaceOrderParamDto;
import com.okdeer.mall.order.enums.OrderOptTypeEnum;
import com.okdeer.mall.order.enums.PayWayEnum;
import com.okdeer.mall.order.enums.PlaceOrderTypeEnum;
import com.okdeer.mall.order.handler.RequestHandlerChain;
import com.okdeer.mall.order.service.PlaceOrderApi;

@Service(version = "1.0.0", interfaceName = "com.okdeer.mall.order.service.PlaceOrderApi")
public class PlaceOrderApiImpl implements PlaceOrderApi {

	@Resource
	private RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> confirmOrderService;

	@Resource
	private RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> confirmServOrderService;

	@Resource
	private RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> submitOrderService;

	@Resource
	private RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> submitServOrderService;

	@Override
	public Response<PlaceOrderDto> confirmOrder(Request<PlaceOrderParamDto> req) throws Exception {
		Response<PlaceOrderDto> resp = new Response<PlaceOrderDto>();
		resp.setData(new PlaceOrderDto());
		req.getData().setOrderOptType(OrderOptTypeEnum.ORDER_SETTLEMENT);
		RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> handlerChain = null;
		if (req.getData().getOrderType() == PlaceOrderTypeEnum.CVS_ORDER) {
			handlerChain = confirmOrderService;
		} else {
			handlerChain = confirmServOrderService;
		}
		handlerChain.process(req, resp);
		resp.getData().setCurrentTime(System.currentTimeMillis());
		if (req.getData().getOrderType() != PlaceOrderTypeEnum.CVS_ORDER) {
			fillResponse(req, resp);
		} else if (!resp.isSuccess()) {
			fillResponse(req, resp);
		} else {
			PlaceOrderParamDto paramDto = req.getData();
			StoreInfo storeInfo = (StoreInfo) paramDto.get("storeInfo");
			StoreSkuParserBo parserBo = (StoreSkuParserBo) paramDto.get("parserBo");
			AppStoreDto appStoreDto = AppAdapter.convert(storeInfo);
			if (parserBo != null) {
				appStoreDto.setFreight(parserBo.getFare());
				if (paramDto.getOrderType() != PlaceOrderTypeEnum.CVS_ORDER) {
					List<CurrentStoreSkuBo> skuList = new ArrayList<CurrentStoreSkuBo>();
					if (CollectionUtils.isNotEmpty(parserBo.getCurrentSkuMap().values())) {
						skuList.addAll(parserBo.getCurrentSkuMap().values());
						if (skuList.size() > 1) {
							resp.getData().setPaymentMode(PayWayEnum.PAY_ONLINE.ordinal());
						} else {
							resp.getData().setPaymentMode(skuList.get(0).getPaymentMode());
						}
					}
				}
			}
			resp.getData().setStoreInfo(appStoreDto);
		}
		return resp;
	}

	private void fillResponse(Request<PlaceOrderParamDto> req, Response<PlaceOrderDto> resp) {
		PlaceOrderParamDto paramDto = req.getData();
		StoreInfo storeInfo = (StoreInfo) paramDto.get("storeInfo");
		StoreSkuParserBo parserBo = (StoreSkuParserBo) paramDto.get("parserBo");
		AppStoreDto appStoreDto = AppAdapter.convert(storeInfo);
		if (parserBo != null) {
			appStoreDto.setFreight(parserBo.getFare());
		}
		resp.getData().setStoreInfo(appStoreDto);
		resp.getData().setSkuList(AppAdapter.convert(parserBo));
		resp.getData().setStoreServExt(AppAdapter.convertAppStoreServiceExtDto(storeInfo));

	}

	@Override
	public Response<PlaceOrderDto> submitOrder(Request<PlaceOrderParamDto> req) throws Exception {
		Response<PlaceOrderDto> resp = new Response<PlaceOrderDto>();
		resp.setData(new PlaceOrderDto());
		req.getData().setOrderOptType(OrderOptTypeEnum.ORDER_SUBMIT);
		RequestHandlerChain<PlaceOrderParamDto, PlaceOrderDto> handlerChain = null;
		if (req.getData().getOrderType() == PlaceOrderTypeEnum.CVS_ORDER) {
			handlerChain = submitOrderService;
		} else {
			handlerChain = submitServOrderService;
		}
		handlerChain.process(req, resp);
		resp.getData().setCurrentTime(System.currentTimeMillis());
		return resp;
	}
}
