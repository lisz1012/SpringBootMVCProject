package com.lisz.mapper;

import com.lisz.entity.Permission;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * PermissionMapper继承基类
 */
@Repository
@Mapper
public interface PermissionMapper extends MyBatisBaseDao<Permission, Integer, PermissionExample> {

	List<Permission> getPermissionsForRoleId(@Param("id") int id); // @Param("id")指定的是在PermissionMapper.xml中#{id}中的id是多少
}