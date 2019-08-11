package com.lisz.controller;


import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lisz.mapper.Account;
import com.lisz.mapper.Menu;
import com.lisz.service.AcountService;
import com.lisz.service.MenuService;

@Controller
public class MainController {
	@Autowired
	private AcountService accountService;
	
	@Autowired
	private MenuService menuService;
	
	@GetMapping("/list")
	//@ResponseBody //节省点时间，不搞前端页面了，用@ResponseBody即可
	public String list(Model model) { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		List<Account> accounts = accountService.findAll();
		model.addAttribute("accounts", accounts);
		return "list";
	}
	
	@RequestMapping("/add")
	@ResponseBody
	public String add() { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		Account account = new Account();
		account.setLocation("Jinan");
		account.setNickName("w8");
		account.setUsername("zhangjc7");
		account.setPassword("000");
		account.setAge(12);
		accountService.add(account);
		return "OK";
	}
	
	@GetMapping("/menus")
	@ResponseBody //节省点时间，不搞前端页面了，用@ResponseBody即可
	public Object menus(Model model) { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		return menuService.findAll();
	}
	
	@GetMapping("/menus/{name}")
	@ResponseBody //节省点时间，不搞前端页面了，用@ResponseBody即可
	public Object getMenusByName(@PathVariable String name, Model model) { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		return menuService.findByName(name);
	}
}
