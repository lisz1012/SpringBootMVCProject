package com.lisz.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
