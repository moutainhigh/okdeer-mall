package com.okdeer.mall.operate.operatefields.service;

import java.util.List;

import com.okdeer.base.common.exception.ServiceException;
import com.okdeer.base.service.IBaseService;
import com.okdeer.mall.operate.operatefields.entity.OperateFieldsContent;

/**
 * ClassName: OperateFieldsContentService 
 * @Description: 运营栏位内容service
 * @author zengjizu
 * @date 2017年4月13日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *
 */
public interface OperateFieldsContentService extends IBaseService{
	
	/**
	 * @Description: 根据运营栏位id查询列表
	 * @param fieldId 运营栏位id
	 * @return
	 * @author zengjizu
	 * @date 2017年4月13日
	 */
	List<OperateFieldsContent> findByFieldId(String fieldId);

	/**
	 * 初始化店铺运营栏位内容
	 * @Description: 
	 * @param storeId void
	 * @throws
	 * @author mengsj
	 * @date 2017年4月20日
	 */
	void initOperationFieldContext(String storeId) throws ServiceException;	
}
