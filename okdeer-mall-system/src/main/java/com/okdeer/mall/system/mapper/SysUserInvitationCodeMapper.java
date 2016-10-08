package com.okdeer.mall.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.okdeer.mall.system.entity.SysUserInvitationCode;
import com.okdeer.mall.system.entity.SysUserInvitationCodeVo;
import com.okdeer.base.dal.IBaseCrudMapper;

/**
 * ClassName: SysUserInvitationCodeMapper 
 * @Description: 用户邀请码mapper
 * @author zhulq
 * @date 2016年9月19日
 *
 * =================================================================================================
 *     Task ID			  Date			     Author		      Description
 * ----------------+----------------+-------------------+-------------------------------------------
 *		重构4.1			 2016年9月19日 			zhulq
 *      V1.1.0           2016年9月28日                                zhaoqc        添加根据用户Id查询邀请码信息 
 */
public interface SysUserInvitationCodeMapper extends IBaseCrudMapper {

	
	/**
	 * 
	 * @Description: 根据用户ID查询用户邀请码信息 </p>
	 * @param sysBuyerUserId
	 * @return
	 * @author yangq
	 * @date 2016年9月28日
	 */
	SysUserInvitationCode selectInvitationById(String sysBuyerUserId);
	
	/**
	 * 
	 * @Description: 根据邀请码code查询用户邀请码信息 </p>
	 * @param code
	 * @return
	 * @author yangq
	 * @date 2016年9月28日
	 */
	SysUserInvitationCode selectInvitationByCode(String InvitationCode);
	
	/**
	 * @Description: 获取邀请码列表
	 * @param sysUserInvitationCodeVo 邀请码vo
	 * @return 结果集
	 * @author zhulq
	 * @date 2016年9月20日
	 */
	List<SysUserInvitationCodeVo> findByQueryVo(SysUserInvitationCodeVo sysUserInvitationCodeVo);
	
	/**
	 * @Description: 新增
	 * @param sysUserInvitationCode sysUserInvitationCode
	 * @author zhulq
	 * @date 2016年9月26日
	 */
	void saveCode(SysUserInvitationCode sysUserInvitationCode);
	
	/**
	 * @Description: 跟新邀请码表
	 * @param sysUserInvitationCode  sysUserInvitationCode 
	 * @author zhulq
	 * @date 2016年9月26日
	 */
	void updateCode(SysUserInvitationCode sysUserInvitationCode);

	/**
     * 根据用户Id查询邀请码信息
     * @param userId 用户Id
     * @param userType 用户类型
     * @return 邀请码实体
     */
    SysUserInvitationCode findInvitationCodeByUserId(@Param("userId") String userId, @Param("userType")int userType);
    
    /**
     * 根据用户Id及邀请码查询邀请码信息
     * @param userId 用户Id
     * @param invitationCode 邀请码
     * @return 邀请码实体
     * @tuzhiding start 2016-10-4
     */
    List<SysUserInvitationCode> findInvitationByIdCode(@Param("userId")String userId, @Param("invitationCode")String invitationCode);
    //end 2016-10-4
    
    /**
     * 根据邀请码查找邀请码实体
     * @param invitationCode 邀请码
     * @return 邀请码实体
     */
    SysUserInvitationCode findInvitationCodeByCode(@Param("invitationCode") String invitationCode);
}
