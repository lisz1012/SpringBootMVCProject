package com.lisz.mapper;

import com.lisz.entity.Account;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * AccountMapper继承基类
 */
@Repository
@Mapper //这里注意要加上这个注解, 否则启动Service的时候就会报错
public interface AccountMapper extends MyBatisBaseDao<Account, Integer, AccountExample> {
}