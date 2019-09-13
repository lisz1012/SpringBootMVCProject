package com.lisz.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lisz.controller.ResponseStatus;
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
		/*AccountExample example = new AccountExample();
		example.createCriteria().andUsernameEqualTo(username).andPasswordEqualTo(password);
		List<Account> list = mapper.selectByExample(example);
		return list.isEmpty() ? null : list.get(0);*/
		return mapper.findByUsernameAndPassword(username, password);
	}

	public List<Account> findAll() {
		return mapper.selectByExample(null);
	}

	public PageInfo<Account> findByPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize); // PageHelper和PageInfo是分页主要用到的工具类。
		return new PageInfo<Account>(findAll(), 10);//包装成一个PageInfo对象，解决分页的问题.可以通过第二个参数动态调整最多显示的页码数,default = 8
	}

	public ResponseStatus deleteById(int id) {
		// 1. 提示用户将要删除，不能直接删
		// 2. 回收站等通过删除标记来完成删除操作，以达到数据永远删不掉的效果 / update有的也是只增不改，多余的数据存储到别的机器或者数据库表
		int rows = mapper.deleteByPrimaryKey(id);
		if (rows == 1) {
			return new ResponseStatus(200, "OK", "Delete successfully");
		}
		return new ResponseStatus(500, "Internal error", "Delete failed.");
	}

	public ResponseStatus updatePasswordById(Integer id, String newPassword) {
		try {
			newPassword = AES256Utils.Encrypt(newPassword);
			Account account = mapper.selectByPrimaryKey(id);
			account.setPassword(newPassword);
			mapper.updateByPrimaryKey(account);
			return new ResponseStatus(200, "OK", "Password update successful");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseStatus(500, "Internal Error", "Password update failed");
		}
	}

	public Account findById(Integer id) {
		//return mapper.selectByPrimaryKey(id);
		return mapper.findById(id);
	}

	public ResponseStatus updateProfileUrlById(String profileUrl, Integer id) {
		try {
			Account account = mapper.selectByPrimaryKey(id);
			account.setProfileUrl(profileUrl);
			mapper.updateByPrimaryKey(account); //updateSelective是有哪些字段就更新哪些，没有的不去管,这里都行
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseStatus(500, "Internal Error", "Profile URL update failed");
		}
		return null;
	}

	public ResponseStatus update(Account account) {
		try {
			mapper.updateByPrimaryKeySelective(account); //updateSelective是有哪些字段就更新哪些，没有的不去管,这里都行
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseStatus(500, "Internal Error", "Profile URL update failed");
		}
		return null;
	}

	public void addRolesForAccount(int[] roleIds, int accountId) {
		mapper.addRolesForAccount(roleIds, accountId);
	}

	/*public void updatePassword() {
		List<Account> list = mapper.selectByExample(null);
		list.forEach(a -> {
			a.setPassword(AES256Utils.Encrypt(a.getPassword()));
			mapper.updateByPrimaryKey(a);
		});
		
	}*/
}
