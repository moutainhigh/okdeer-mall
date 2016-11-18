/** 
 *@Project: okdeer-mall-operate 
 *@Author: xuzq01
 *@Date: 2016年11月14日 
 *@Copyright: ©2014-2020 www.okdeer.com Inc. All rights reserved. 
 */
package com.okdeer.mall.operate.api;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.okdeer.base.common.utils.PageUtils;
import com.okdeer.mall.operate.dto.SkinManagerDto;
import com.okdeer.mall.operate.dto.SkinManagerParamDto;
import com.okdeer.mall.operate.enums.SkinManagerStatus;
import com.okdeer.mall.operate.service.SkinManagerApi;
import com.okdeer.mall.operate.skinmanager.service.SkinManagerService;

/**
 * ClassName: SkinManagerApiImpl 
 * @Description: 
 * @author xuzq01
 * @date 2016年11月14日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *
 */
@Service(version = "1.0.0")
public class SkinManagerApiImpl implements SkinManagerApi {

	@Autowired
	SkinManagerService skinManagerService;

	@Override
	public PageUtils<SkinManagerDto> findSkinList(SkinManagerParamDto paramDto) {
		return skinManagerService.findSkinList(paramDto);
	}

	@Override
	public void add(SkinManagerDto skinManagerDto) throws Exception {
		skinManagerService.add(skinManagerDto);
	}

	@Override
	public void update(SkinManagerDto skinManagerDto) throws Exception {
		skinManagerService.update(skinManagerDto);;

	}

	@Override
	public SkinManagerDto findSkinDetailById(String skinId){
		SkinManagerParamDto paramDto = new SkinManagerParamDto();
		paramDto.setId(skinId);
		return skinManagerService.findSkinDetailByParam(paramDto);
	}
	
	@Override
	public SkinManagerDto findSkinDetailUnderWay() {
		SkinManagerParamDto paramDto = new SkinManagerParamDto();
		paramDto.setStatus(SkinManagerStatus.UNDERWAY);;
		return skinManagerService.findSkinDetailByParam(paramDto);
	}

	@Override
	public void deleteSkinById(String skinId, String userId) {
		skinManagerService.deleteSkinById(skinId, userId);
	}

	@Override
	public void closeSkinById(String skinId, String userId) {
		skinManagerService.closeSkinById(skinId, userId);
	}

	@Override
	public int findSkinCountByName(SkinManagerDto skinManagerDto) {
		return skinManagerService.findSkinCountByName(skinManagerDto);
	}

	@Override
	public int findSkinByTime(SkinManagerDto skinManagerDto) {
		return skinManagerService.findSkinByTime(skinManagerDto);
	}
	
	/**
	 * 执行换肤活动管理 JOB 任务
	 * @Description: TODO   
	 * @return void  
	 * @throws
	 * @author tuzhd
	 * @date 2016年11月16日
	 */
	public void processSkinActivityJob(){
		skinManagerService.processSkinActivityJob();
	}

	/**
	 * (non-Javadoc)
	 * @see com.okdeer.mall.operate.service.ISkinManagerApi#getSkinById(java.lang.String)
	 */
	@Override
	public SkinManagerDto getSkinById(String skinId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * (non-Javadoc)
	 * @see com.okdeer.mall.operate.service.ISkinManagerApi#selectSkinCountByName(com.okdeer.mall.operate.dto.SkinManagerDto)
	 */
	@Override
	public int selectSkinCountByName(SkinManagerDto skinManagerDto) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * (non-Javadoc)
	 * @see com.okdeer.mall.operate.service.ISkinManagerApi#selectSkinByTime(com.okdeer.mall.operate.dto.SkinManagerDto)
	 */
	@Override
	public int selectSkinByTime(SkinManagerDto skinManagerDto) {
		// TODO Auto-generated method stub
		return 0;
	}

}
