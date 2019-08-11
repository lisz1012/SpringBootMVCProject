package com.lisz.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lisz.mapper.Account;
import com.lisz.mapper.AccountMapper;

@Service
public class AcountService {
	@Autowired
	private AccountMapper mapper;
	
	public List<Account> findAll() {
		return mapper.findAll();
	}
	
	public void add(Account account) {
	mapper.add(account);
	}
}
