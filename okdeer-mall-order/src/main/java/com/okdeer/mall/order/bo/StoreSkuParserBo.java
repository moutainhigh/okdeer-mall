package com.okdeer.mall.order.bo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import com.okdeer.archive.goods.assemble.dto.GoodsStoreAssembleDto;
import com.okdeer.archive.goods.assemble.dto.GoodsStoreSkuAssembleDto;
import com.okdeer.archive.goods.spu.enums.SpuTypeEnum;
import com.okdeer.archive.goods.store.entity.GoodsStoreSku;
import com.okdeer.archive.goods.store.entity.GoodsStoreSkuService;
import com.okdeer.archive.goods.store.entity.GoodsStoreSkuStock;
import com.okdeer.archive.goods.store.enums.IsShopNum;
import com.okdeer.base.common.utils.DateUtils;
import com.okdeer.mall.activity.coupons.entity.ActivitySale;
import com.okdeer.mall.activity.coupons.entity.ActivitySaleGoods;
import com.okdeer.mall.activity.coupons.entity.ActivitySaleRecord;
import com.okdeer.mall.activity.coupons.enums.ActivityTypeEnum;
import com.okdeer.mall.order.dto.PlaceOrderItemDto;

/**
 * ClassName: StoreSkuPaserBo 
 * @Description: 店铺商品解析器
 * @author maojj
 * @date 2017年1月3日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿1.2.5		2017年1月3日				maojj		      店铺商品解析器
 */
public class StoreSkuParserBo {

	/**
	 * 正在进行中的活动商品映射关系
	 */
	private Map<String, List<String>> activitySkuMap;

	/**
	 * 正在进行中的活动信息列表
	 */
	private List<ActivitySale> activityList;

	/**
	 * 正在进行中的活动信息映射 key:活动Id，value：ActivitySale
	 */
	private Map<String, ActivitySale> activityMap = new HashMap<String, ActivitySale>();

	/**
	 * 活动商品信息。key：店铺商品ID，Value：商品活动信息
	 */
	private Map<String, ActivitySaleGoods> currentActivitySkuMap = new HashMap<String, ActivitySaleGoods>();

	/**
	 * 用户已购买活动信息。Key：活动Id，Value：购买活动商品的总款项
	 */
	private Map<String, Integer> buyKindCount = new HashMap<String, Integer>();

	/**
	 * 用户已购买活动商品。Ke：店铺商品ID，Value：购买总数
	 */
	private Map<String, Integer> buyGoodsCount = new HashMap<String, Integer>();

	/**
	 * 当前商品映射信息
	 */
	private Map<String, CurrentStoreSkuBo> currentSkuMap = new HashMap<String, CurrentStoreSkuBo>();

	/**
	 * 当前商品列表
	 */
	private List<GoodsStoreSku> currentSkuList;

	/**
	 * 组合商品Id列表
	 */
	private List<String> comboSkuIdList = new ArrayList<String>();
	
	/**
	 * 组合商品Id成分映射　Key:组合商品ID Value：组合商品明细列表
	 */
	private Map<String,List<GoodsStoreSkuAssembleDto>> comboSkuMap = new HashMap<String,List<GoodsStoreSkuAssembleDto>>();

	/**
	 * 购买商品ID清单
	 */
	private List<String> skuIdList;

	/**
	 * 订单项总金额
	 */
	private BigDecimal totalItemAmount = BigDecimal.valueOf(0.0);
	
	/**
	 * 缓存低价商品的活动数量
	 */
	private Map<String,Integer> skuActNumMap = new HashMap<String,Integer>();
	
	/**
	 * 订单项总数量
	 */
	private int totalQuantity = 0;

	/**
	 * 商品类目Id
	 */
	private Set<String> categoryIdSet = new HashSet<String>();
	
	/**
	 * 订单运费
	 */
	private BigDecimal fare = BigDecimal.valueOf(0.0);
	
	/**
	 * 是否是低价活动订单
	 */
	private boolean isLowFavour;
	
	/**
	 * 低价优惠总金额
	 */
	private BigDecimal totalLowFavour = BigDecimal.valueOf(0.0);
	
	/**
	 * 低价活动Id 
	 */
	private String lowActivityId;
	
	/**
	 * 低价活动已关闭
	 */
	private boolean isCloseLow;
	
	public StoreSkuParserBo(List<GoodsStoreSku> currentSkuList) {
		this.currentSkuList = currentSkuList;
	}

	/**
	 * @Description: 解析当前商品信息   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public void parseCurrentSku() {
		for (GoodsStoreSku storeSku : this.currentSkuList) {
			categoryIdSet.add(storeSku.getSpuCategoryId());

			CurrentStoreSkuBo currentSku = new CurrentStoreSkuBo();
			currentSku.setId(storeSku.getId());
			currentSku.setStoreSpuId(storeSku.getStoreSpuId());
			currentSku.setName(storeSku.getName());
			currentSku.setBarCode(storeSku.getBarCode());
			currentSku.setStyleCode(storeSku.getStyleCode());
			currentSku.setSkuId(storeSku.getSkuId());
			currentSku.setMultipleSkuId(storeSku.getMultipleSkuId());
			currentSku.setPropertiesIndb(storeSku.getPropertiesIndb());
			currentSku.setSpuType(storeSku.getSpuTypeEnum());
			currentSku.setGuaranteed(storeSku.getGuaranteed());
			currentSku.setTradeMax(storeSku.getTradeMax());
			currentSku.setOnline(storeSku.getOnline());
			currentSku.setSaleNum(storeSku.getSaleNum());
			currentSku.setUnit(storeSku.getUnit());
			if(storeSku.getPayType() != null){
				currentSku.setPaymentMode(storeSku.getPayType().ordinal());
			}
			
			GoodsStoreSkuService skuServiceEntity = storeSku.getGoodsStoreSkuService();
			if (skuServiceEntity != null) {
				// 是否有起购量 0：无 1：有
				IsShopNum isShopNum = skuServiceEntity.getIsShopNum();
				if (isShopNum== IsShopNum.YES) {
					// 有起购量
					currentSku.setShopNum(skuServiceEntity.getShopNum());
				} else {
					// 无起购量
					currentSku.setShopNum(1);
				}
				currentSku.setEndTime(skuServiceEntity.getEndTime());
				if(storeSku.getSpuTypeEnum() == SpuTypeEnum.fwdDdxfSpu){
					// 到店消费，取goods_store_sku_service表的is_unsubscribe是否支持退订，0：不支持，1：支持
					if (skuServiceEntity.getIsUnsubscribe() != null) {
						currentSku.setGuaranteed(String.valueOf(skuServiceEntity.getIsUnsubscribe().ordinal()));
					} else {
						currentSku.setGuaranteed("0");
					}
				}
			}
			if(storeSku.getGoodsStoreSkuPicture() != null){
				currentSku.setMainPicUrl(storeSku.getGoodsStoreSkuPicture().getUrl());
			}
			
			if (this.currentActivitySkuMap.containsKey(storeSku.getId())) {
				ActivitySaleGoods actGoods = this.currentActivitySkuMap.get(storeSku.getId());
				ActivitySale actInfo = this.activityMap.get(actGoods.getSaleId());
				currentSku.setActivityType(actInfo.getType().ordinal());
				currentSku.setActivityId(actInfo.getId());
				currentSku.setLimitBuyNum(actGoods.getTradeMax());

				if (actInfo.getType() == ActivityTypeEnum.LOW_PRICE) {
					// 如果是低价，保存商品的线上价格为线上价格，活动价格为活动价格
					currentSku.setOnlinePrice(storeSku.getOnlinePrice());
					currentSku.setActPrice(actGoods.getSalePrice());
					currentSku.setLimitKind(actInfo.getLimit());
				} else {
					// 如果是特惠，特惠商品的线上价格为活动价格
					currentSku.setLimitKind(actInfo.getLimit());
					currentSku.setOnlinePrice(actGoods.getSalePrice());
				} 
			} else {
				currentSku.setActivityType(ActivityTypeEnum.NO_ACTIVITY.ordinal());
				currentSku.setOnlinePrice(storeSku.getOnlinePrice());
			}
			currentSku.setUpdateTime(DateUtils.formatDateTime(storeSku.getUpdateTime()));
			this.currentSkuMap.put(storeSku.getId(), currentSku);
			if (storeSku.getSpuTypeEnum() == SpuTypeEnum.assembleSpu) {
				this.comboSkuIdList.add(storeSku.getId());
			}
		}
	}
	
	/**
	 * @Description: 处理秒杀商品   
	 * @author maojj
	 * @date 2017年1月17日
	 */
	public void processSeckill(){
		for(CurrentStoreSkuBo skuBo : this.currentSkuMap.values()){
			skuBo.setActivityType(ActivityTypeEnum.SECKILL_ACTIVITY.ordinal());
		}
	}

	/**
	 * @Description: 加载活动商品列表
	 * @param activityList   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public void loadActivityList(List<ActivitySale> activityList) {
		this.activityList = activityList;
		if (CollectionUtils.isEmpty(this.activityList)) {
			return;
		}
		for (ActivitySale activity : activityList) {
			this.activityMap.put(activity.getId(), activity);
		}
	}

	public void loadSaleGoodsList(List<ActivitySaleGoods> saleGoodsList) {
		if (CollectionUtils.isEmpty(saleGoodsList)) {
			return;
		}
		for (ActivitySaleGoods saleGoods : saleGoodsList) {
			this.currentActivitySkuMap.put(saleGoods.getStoreSkuId(), saleGoods);
		}
	}

	/**
	 * @Description: 加载商品库存列表
	 * @param stockList   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public void loadStockList(List<GoodsStoreSkuStock> stockList) {
		CurrentStoreSkuBo storeSkuBo = null;
		for (GoodsStoreSkuStock stock : stockList) {
			storeSkuBo = this.getCurrentStoreSkuBo(stock.getStoreSkuId());
			ActivityTypeEnum activityType = ActivityTypeEnum.enumValueOf(storeSkuBo.getActivityType());
			switch (activityType) {
				case NO_ACTIVITY:
					// 如果没参加活动
					storeSkuBo.setSellable(stock.getSellable());
					break;
				case SALE_ACTIVITIES:
				case LOW_PRICE:
					// 如果是低价或者特惠
					storeSkuBo.setSellable(stock.getSellable());
					storeSkuBo.setLocked(stock.getLocked());
					break;
				case SECKILL_ACTIVITY:
					// 如果是秒杀。将锁定库存设置为可售库存。为了统一服务商品库存的判断，所以此处作此处理。
					storeSkuBo.setSellable(stock.getLocked());
					break;
				default:
					break;
			}
		}
	}

	/**
	 * @Description: 加载购买商品列表
	 * @param buySkuList   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public void loadBuySkuList(List<PlaceOrderItemDto> buySkuList) {
		CurrentStoreSkuBo skuBo = null;
		for (PlaceOrderItemDto item : buySkuList) {
			skuBo = this.currentSkuMap.get(item.getStoreSkuId());
			skuBo.setQuantity(item.getQuantity());
			skuBo.setSkuActQuantity(item.getSkuActQuantity());

			this.totalItemAmount = totalItemAmount
					.add(skuBo.getOnlinePrice().multiply(BigDecimal.valueOf(skuBo.getQuantity())));
			this.totalQuantity += skuBo.getQuantity();
			
			this.skuActNumMap.put(item.getStoreSkuId(), Integer.valueOf(item.getSkuActQuantity()));
			if(item.getSkuActType() == ActivityTypeEnum.LOW_PRICE.ordinal() && item.getSkuActQuantity()>0 && skuBo.getActivityType()==ActivityTypeEnum.NO_ACTIVITY.ordinal()){
				// 如果购买请求中，存在商品为低价商品类型，但是经过后台解析之后，该商品活动类型为未参加活动商品，则标识低价活动当前已经结束。
				this.isCloseLow = true;
			}
		}
	}

	/**
	 * @Description: 加载用户购买活动商品记录列表
	 * @param buyRecordList   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public void loadBuyRecordList(List<ActivitySaleRecord> buyRecordList) {
		if (CollectionUtils.isEmpty(buyRecordList)) {
			return;
		}
		for (ActivitySaleRecord record : buyRecordList) {
			Integer totalKindBuy = this.buyKindCount.get(record.getSaleId());
			if (totalKindBuy == null) {
				this.buyKindCount.put(record.getSaleId(), Integer.valueOf(1));
			} else {
				this.buyKindCount.put(record.getSaleId(), totalKindBuy + 1);
			}
			this.buyGoodsCount.put(record.getSaleGoodsId(), record.getSaleGoodsNum());
		}
	}
	
	/**
	 * @Description: 
	 * @param comboSkuList   
	 * @author maojj
	 * @date 2017年1月5日
	 */
	public void loadComboSkuList(List<GoodsStoreAssembleDto> comboSkuList){
		if(CollectionUtils.isEmpty(comboSkuList)){
			return;
		}
		for(GoodsStoreAssembleDto dto : comboSkuList){
			for(GoodsStoreSkuAssembleDto detail : dto.getGoodsStoreSkuAssembleDtos()){
				if(!this.comboSkuMap.containsKey(detail.getAssembleSkuId())){
					this.comboSkuMap.put(detail.getAssembleSkuId(), new ArrayList<GoodsStoreSkuAssembleDto>());
				}
				this.comboSkuMap.get(detail.getAssembleSkuId()).add(detail);
			}
		}
	}
	
	/**
	 * @Description: 解析是否存在低价优惠
	 * @return   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public boolean parseLowFavour() {
		for (CurrentStoreSkuBo skuBo : this.currentSkuMap.values()) {
			if (skuBo.getActivityType() == ActivityTypeEnum.LOW_PRICE.ordinal() && skuBo.getSkuActQuantity() > 0) {
				this.isLowFavour = true;
				this.lowActivityId = skuBo.getActivityId();
				this.totalLowFavour = this.totalLowFavour.add(skuBo.getOnlinePrice().subtract(skuBo.getActPrice())
						.multiply(BigDecimal.valueOf(skuBo.getSkuActQuantity())));
			}
		}
		return false;
	}

	/**
	 * @Description: 获取用户已购买活动的总款项
	 * @param activityId
	 * @return   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public int getBuyKind(String activityId) {
		return this.buyKindCount.containsKey(activityId) ? this.buyKindCount.get(activityId) : 0;
	}

	/**
	 * @Description: 获取用户已购活动商品的总数量
	 * @param storeSkuId
	 * @return   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public int getBoughtSkuNum(String storeSkuId) {
		return this.buyGoodsCount.containsKey(storeSkuId) ? this.buyGoodsCount.get(storeSkuId) : 0;
	}

	/**
	 * @Description: 获取当前活动商品购买数量
	 * @param storeSkuId
	 * @return   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public int getBuyNum(String storeSkuId) {
		CurrentStoreSkuBo storeSkuBo = this.getCurrentStoreSkuBo(storeSkuId);
		if (storeSkuBo.getActivityType() == ActivityTypeEnum.LOW_PRICE.ordinal()) {
			return storeSkuBo.getSkuActQuantity();
		} else {
			return storeSkuBo.getQuantity();
		}
	}

	/**
	 * @Description: 统计活动新增款项
	 * @param activityId
	 * @return   
	 * @author maojj
	 * @date 2017年1月4日
	 */
	public int countAddKindNum(String activityId) {
		int totalAdd = 0;
		// 用户当前需要购买的活动商品信息
		List<String> skuIds = this.activitySkuMap.get(activityId);
		for (String skuId : skuIds) {
			CurrentStoreSkuBo currentSku = this.currentSkuMap.get(skuId);
			if (currentSku.getActivityType() == ActivityTypeEnum.LOW_PRICE.ordinal()
					&& currentSku.getSkuActQuantity() == 0) {
				// 如果低价商品购买数量为0，则跳过
				continue;
			}
			if (!this.buyGoodsCount.containsKey(skuId)) {
				totalAdd++;
			}
		}
		return totalAdd;
	}
	
	/**
	 * @Description: 提取非组合商品的id列表
	 * @return   
	 * @author maojj
	 * @date 2017年2月23日
	 */
	public List<String> extraSkuListExcludeCombo(){
		if(CollectionUtils.isEmpty(this.comboSkuIdList)){
			return this.skuIdList;
		}
		List<String> excludeComboList = new ArrayList<String>();
		for(String skuId : this.skuIdList){
			if(!this.comboSkuIdList.contains(skuId)){
				excludeComboList.add(skuId);
			}
		}
		return excludeComboList;
	}
	
	/**
	 * @Description: 提取特惠商品信息用于发送库存提醒消息
	 * @return ， key:storeSkuId  value:saleId 特惠活动id
	 * @author maojj
	 * @date 2017年2月23日
	 */
	public Map<String,String> extraPreferenceMap(){
		if(this.currentActivitySkuMap.isEmpty()){
			return null;
		}
		Map<String,String> resultMap = new HashMap<String,String>();
		ActivitySaleGoods saleGoods = null;
		for(Entry<String, ActivitySaleGoods> entry : this.currentActivitySkuMap.entrySet()){
			saleGoods = entry.getValue();
			resultMap.put(saleGoods.getStoreSkuId(), saleGoods.getSaleId());
		}
		return resultMap;
	}

	public Map<String, List<String>> getActivitySkuMap() {
		return activitySkuMap;
	}

	public void setActivitySkuMap(Map<String, List<String>> activitySkuMap) {
		this.activitySkuMap = activitySkuMap;
	}

	public Map<String, ActivitySaleGoods> getCurrentActivitySkuMap() {
		return currentActivitySkuMap;
	}

	public void setCurrentActivitySkuMap(Map<String, ActivitySaleGoods> currentActivitySkuMap) {
		this.currentActivitySkuMap = currentActivitySkuMap;
	}

	public Map<String, CurrentStoreSkuBo> getCurrentSkuMap() {
		return currentSkuMap;
	}

	public void setCurrentSkuMap(Map<String, CurrentStoreSkuBo> currentSkuMap) {
		this.currentSkuMap = currentSkuMap;
	}

	public List<GoodsStoreSku> getCurrentSkuList() {
		return currentSkuList;
	}

	public CurrentStoreSkuBo getCurrentStoreSkuBo(String storeSkuId) {
		return this.currentSkuMap.get(storeSkuId);
	}

	public void setCurrentSkuList(List<GoodsStoreSku> currentSkuList) {
		this.currentSkuList = currentSkuList;
	}

	public List<ActivitySale> getActivityList() {
		return activityList;
	}

	public void setCategoryIdSet(Set<String> categoryIdSet) {
		this.categoryIdSet = categoryIdSet;
	}

	public Map<String, Integer> getBuyKindCount() {
		return buyKindCount;
	}

	public void setBuyKindCount(Map<String, Integer> buyKindCount) {
		this.buyKindCount = buyKindCount;
	}

	public Map<String, Integer> getBuyGoodsCount() {
		return buyGoodsCount;
	}

	public void setBuyGoodsCount(Map<String, Integer> buyGoodsCount) {
		this.buyGoodsCount = buyGoodsCount;
	}

	public Set<String> getCategoryIdSet() {
		return categoryIdSet;
	}

	public List<String> getSkuIdList() {
		return skuIdList;
	}

	public void setSkuIdList(List<String> skuIdList) {
		this.skuIdList = skuIdList;
	}

	public Integer getCurrentStock(String storeSkuId) {
		return this.currentSkuMap.get(storeSkuId).getSellable();
	}

	public List<String> getComboSkuIdList() {
		return comboSkuIdList;
	}

	public BigDecimal getTotalItemAmount() {
		return totalItemAmount;
	}

	public void setTotalItemAmount(BigDecimal totalItemAmount) {
		this.totalItemAmount = totalItemAmount;
	}

	
	public Map<String, List<GoodsStoreSkuAssembleDto>> getComboSkuMap() {
		return comboSkuMap;
	}
	
	public int getTotalQuantity() {
		return totalQuantity;
	}

	public void setFare(BigDecimal fare){
		this.fare = fare;
	}
	
	public BigDecimal getFare() {
		return fare;
	}

	public boolean isLowFavour() {
		return isLowFavour;
	}
	
	public String getLowActivityId() {
		return lowActivityId;
	}

	public BigDecimal getTotalLowFavour() {
		return totalLowFavour;
	}

	public boolean isCloseLow() {
		return isCloseLow;
	}

	public Map<String, Integer> getSkuActNumMap() {
		return skuActNumMap;
	}
}
