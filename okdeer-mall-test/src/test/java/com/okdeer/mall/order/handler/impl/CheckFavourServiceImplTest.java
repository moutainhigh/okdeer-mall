package com.okdeer.mall.order.handler.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import com.okdeer.mall.activity.coupons.entity.ActivityCoupons;


/**
 * ClassName: CheckFavourServiceImplTest 
 * @Description: 检查优惠是否可用
 * (1)使用的是代金券
 * 	  	1、用户Id和代金券领取记录不匹配。****这个校验应该抽移到请求参数的校验过程中。后期需要进行优化改造
 * 	  	2、检查代金券状态是否正常。
 * 	  	3、检查金额是否达到使用下限
 * 		4、检查代金券使用范围
 * 		5、检查请求参数活动Id和代金券Id一致性。***该校验应该抽移到请求参数的校验过程中。后期需要进行优化改造
 * 		6、检查代金券是否仅限新用户
 * 		7、检查代金券客户端限制类型
 * 		8、便利店代金券检查分类限制
 * 		9、服务店代金券检查导航限制
 * 		10、检查代金券设备限制、账号限制
 * 		11、检查代金券活动设备限制、账号限制
 * (2)使用的是满减	
 * @author maojj
 * @date 2017年9月26日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		友门鹿2.3 		2017年9月26日				maojj
 */
public class CheckFavourServiceImplTest {

	@Test
	public void testCheckCoupons() {
		
	}
	
	@Test
	public void testCalucateDiscountAmount() throws Exception{
		CheckFavourServiceImpl checkFavourService = new CheckFavourServiceImpl();
		Method method = Arrays.asList(CheckFavourServiceImpl.class.getDeclaredMethods()).stream()
				.filter(e -> "calucateDiscountAmount".equals(e.getName())).findFirst().get();
		method.setAccessible(true);
		ActivityCoupons couponsInfo = new ActivityCoupons();
		couponsInfo.setType(8);
		couponsInfo.setOrderDiscountMax(10);
		couponsInfo.setFaceValue(90);
		BigDecimal discount = (BigDecimal) method.invoke(checkFavourService, couponsInfo,BigDecimal.valueOf(10));
		assertEquals(BigDecimal.valueOf(1), discount);
	}

}
