
package com.okdeer.mall.activity.coupons.job;

import com.alibaba.dubbo.config.annotation.Reference;
import com.dangdang.ddframe.job.api.JobExecutionMultipleShardingContext;
import com.dangdang.ddframe.job.plugin.job.type.simple.AbstractSimpleElasticJob;
import com.okdeer.archive.goods.dto.StoreMenuParamDto;
import com.okdeer.archive.goods.store.service.ELGoodsServiceApi;
import com.okdeer.common.utils.ELOperateEnum;
import com.okdeer.mall.activity.coupons.entity.ActivitySale;
import com.okdeer.mall.activity.coupons.enums.ActivitySaleStatus;
import com.okdeer.mall.activity.coupons.service.ActivitySaleService;
import com.okdeer.mall.activity.service.ArchiveSendMsgService;
import com.okdeer.mall.activity.service.ELSkuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.okdeer.common.consts.StoreMenuTopicTagConstants.TAG_STORE_MENU_UPDATE;

/**
 * @pr mall
 * @desc 修改特惠活动状态job
 * @author zhangkn
 * @date 2016年1月28日 下午1:59:59
 * @copyright ©2005-2020 yschome.com Inc. All rights reserved
 */
@Service
public class ActivitySaleJob extends AbstractSimpleElasticJob {

	private static final Logger logger = LoggerFactory.getLogger(ActivitySaleJob.class);

	/**
	 * 搜索引擎消费注入
     */
	@Autowired
	private ELSkuService elSkuService;

	@Autowired
	private ActivitySaleService activitySaleService;

//	@Autowired
//	private ActivitySaleGoodsService activitySaleGoodsService;

	@Reference(version = "1.0.0")
	private ELGoodsServiceApi elGoodsServiceApi;

	@Autowired
	private ArchiveSendMsgService archiveSendMsgService;

	@Override
	public void process(JobExecutionMultipleShardingContext arg0) {
		try {
			logger.info("特惠活动定时器开始");
			List<ActivitySale> list = activitySaleService.listByTask();

			if (list != null && list.size() > 0) {
				for (ActivitySale a : list) {
					try {
						if (ActivitySaleStatus.noStart.getValue().equals(a.getStatus())) {
							// 未开始改为进行中
							List<String> idList = new ArrayList<String>();
							idList.add(a.getId());
							elSkuService.syncSaleToEL(idList, ActivitySaleStatus.ing.getValue(),
									a.getStoreId(), "0", ELOperateEnum.UPDATE.ordinal());
							logger.info("开启特惠活动信息（同步搜索引擎-开启）：activityIds{}",idList);
							//activitySaleService.updateBatchStatus(idList, ActivitySaleStatus.ing.getValue(), a.getStoreId(), "0");
						} else if (ActivitySaleStatus.ing.getValue().equals(a.getStatus())) {
							// 进行中改为已关闭
							List<String> idList = new ArrayList<String>();
							idList.add(a.getId());
							elSkuService.syncSaleToEL(idList, ActivitySaleStatus.end.getValue(),
									a.getStoreId(), "0", ELOperateEnum.UPDATE.ordinal());
							logger.info("关闭特惠活动信息（同步搜索引擎-关闭）：activityIds{}",idList);
							//activitySaleService.updateBatchStatus(idList, ActivitySaleStatus.end.getValue(),
							//		a.getStoreId(), "0");
						}
						// 发送消息，更新店铺菜单
						StoreMenuParamDto paramDto = new StoreMenuParamDto();
						paramDto.setStoreId(a.getStoreId());
						archiveSendMsgService.structureProducerStoreMenu(paramDto, TAG_STORE_MENU_UPDATE);
					} catch (Exception e) {
						logger.error("特惠活动定时器异常" + a.getId(), e);
					}

				}
			}
			logger.info("特惠活动定时器结束");
		} catch (Exception e) {
			logger.error("特惠活动定时器异常", e);
		}
	}
}
