package com.lisz.mapper;

import com.lisz.entity.Role;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * RoleMapper继承基类
 */
@Repository
@Mapper
public interface RoleMapper extends MyBatisBaseDao<Role, Integer, RoleExample> {

	void addPermissionForRole(@Param("roleId") int roleId, @Param("permissionId") int permissionId);

	void addPermissionsForRole(@Param("roleId") int roleId, @Param("permissionIds") int[] permissionIds); //变量名与mapper中的#{}内的名字一样的话，@Param可以省略

	Role findById(Integer id);
}