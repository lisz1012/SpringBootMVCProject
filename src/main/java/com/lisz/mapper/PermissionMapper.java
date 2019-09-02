package com.lisz.mapper;

import com.lisz.entity.Permission;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * PermissionMapper继承基类
 */
@Repository
@Mapper
public interface PermissionMapper extends MyBatisBaseDao<Permission, Integer, PermissionExample> {
}