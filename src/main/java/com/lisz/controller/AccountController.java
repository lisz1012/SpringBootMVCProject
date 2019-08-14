package com.lisz.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lisz.entity.Account;

/**
 * 用户账号相关
 * @author shuzheng
 *
 */
@Controller // 写@RestController里面就没法跳转页面了
@RequestMapping("/account")
public class AccountController {
	@Autowired
	private AccountService accountService;
	
	@GetMapping("login") //login前面写不写反斜杠都可以
	public String login() {
		
		return "account/login";
	}
	
	@PostMapping("validateAccount") //validateAccount前面写不写反斜杠都可以
	@ResponseBody // 前端只需要一个数据结果而不需要页面，所以写@ResponseBody
	public String validate(@RequestParam String username, @RequestParam String password) { //不写@RequestParam也可以的
		List<Account> accounts = accountService.findByUsernameAndPassword(username, password);
		System.out.println("aaa");
		if (accounts.isEmpty()) {
			return "error";
		} else {
			return "success";
		}
	}
	
}
