
package com.okdeer.mall.operate.advert.api;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.okdeer.archive.goods.store.dto.GoodsStoreActivitySkuDto;
import com.okdeer.base.common.utils.PageUtils;
import com.okdeer.mall.activity.advert.service.ColumnAdvertGoodsApi;
import com.okdeer.mall.base.BaseServiceTest;

/**
 * ClassName: IColumnAdvertServiceApiTest 
 * @Description: 广告服务测试类
 * @author zengjizu
 * @date 2016年11月8日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *   v1.2.0             2016-11-08           zengjz           增加testUpdateSort测试方法
 */
public class IColumnAdvertServiceApiTest extends BaseServiceTest {

	@Autowired
	private ColumnAdvertGoodsApi columnAdvertGoodsApi;

	
	//@Test
	public void testUpdateSort() {
		String id = "8a2863a556c28acc0156c2a7efcb00d7";
		int sort = 10;
		try {
			Assert.assertTrue(true);
		} catch (Exception e) {
			Assert.fail("设置广告排序出错");
		}

	}
	@Test
	public void findAdvertGoodsByAdvertIdTest() {
		String advertId = "100005";
		String storeId = "8a9868f655c5821701561fd6281e33b3";
		Integer pageNumber = 1;
		Integer pageSize = 20;
		PageUtils<GoodsStoreActivitySkuDto> voList = columnAdvertGoodsApi.findAdvertGoodsByAdvertId(advertId,storeId,pageNumber,pageSize);
		Assert.assertTrue(voList.getList().size()>0);

	}
}
