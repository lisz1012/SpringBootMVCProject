package com.lisz.mapper;

import com.lisz.entity.Account;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * AccountMapper继承基类
 */
@Repository
@Mapper
public interface AccountMapper extends MyBatisBaseDao<Account, Integer, AccountExample> {
	List<Account> findPermissions();
}