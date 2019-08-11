package com.lisz.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.lisz.mapper.Menu;
import com.lisz.mapper.MenuExample;
import com.lisz.mapper.MenuMapper;

@Service
public class MenuService {

	@Autowired
	private MenuMapper menuMapper;
	
	public List<Menu> findAll() {
		return menuMapper.selectByExample(null);
	}

	public List<Menu> findByName(String name) {
		MenuExample example = new MenuExample();
		example.createCriteria().andNameEqualTo(name);
		return menuMapper.selectByExample(example);
	}

	public void add(Menu menu) {
		menuMapper.insert(menu);
	}

	public Menu findById(Integer id) {
		return menuMapper.selectByPrimaryKey(id);
	}

	public List<Menu> findAllByPage(Integer pageNumber, Integer pageSize) {
		// AOP能改变类的行为或者对方法有增强
		PageHelper.startPage(pageNumber, pageSize); //会先发一条SELECT count(0) FROM menu，看看是不是能取出来，再发select id, `name`, roles, `index` from menu LIMIT ?, ? 
		return findAll();
	}
}
