package com.lisz.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lisz.entity.Account;
import com.lisz.mapper.AccountExample;
import com.lisz.mapper.AccountMapper;

@Service
public class AccountService {
	
	@Autowired
	private AccountMapper mapper;

	public List<Account> findByUsernameAndPassword(String username, String password) {
		AccountExample example = new AccountExample();
		example.createCriteria().andUsernameEqualTo(username).andPasswordEqualTo(password);
		return mapper.selectByExample(example);
	}

}
