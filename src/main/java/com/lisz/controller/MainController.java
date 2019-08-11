package com.lisz.controller;


import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lisz.mapper.Account;
import com.lisz.service.AcountService;

@Controller
public class MainController {
	@Autowired
	private AcountService accountService;
	
	@GetMapping("/list")
	@ResponseBody //节省点时间，不搞前端页面了，用@ResponseBody即可
	public Object list(Model model) { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		List<Account> accounts = accountService.findAll();
		//model.addAttribute("accounts", accounts);
		return accounts;
	}
}
