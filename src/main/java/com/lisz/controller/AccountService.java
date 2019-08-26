package com.lisz.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lisz.entity.Account;
import com.lisz.mapper.AccountExample;
import com.lisz.mapper.AccountMapper;
import com.lisz.utils.AES256Utils;

@Service
public class AccountService {
	
	@Autowired
	private AccountMapper mapper;

	public Account findByUsernameAndPassword(String username, String password) {
		password = AES256Utils.Encrypt(password);
		AccountExample example = new AccountExample();
		example.createCriteria().andUsernameEqualTo(username).andPasswordEqualTo(password);
		List<Account> list = mapper.selectByExample(example);
		return list.isEmpty() ? null : list.get(0);
	}

	public List<Account> findAll() {
		return mapper.selectByExample(null);
	}

	public PageInfo<Account> findByPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		return new PageInfo<Account>(findAll(), 10);//包装成一个PageInfo对象，解决分页的问题.可以通过第二个参数动态调整最多显示的页码数,default = 8.或者调用PageInfo.setNavigatePages设置
	}

	/*public void updatePassword() {
		List<Account> list = mapper.selectByExample(null);
		list.forEach(a -> {
			a.setPassword(AES256Utils.Encrypt(a.getPassword()));
			mapper.updateByPrimaryKey(a);
		});
		
	}*/
}
