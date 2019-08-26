package com.lisz.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageInfo;
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
	private AccountService accountService; //service属于model，和后端做计算存储和整理数据的
	
	@GetMapping("login") //login前面写不写反斜杠都可以
	public String login() {
		
		return "account/login"; //返回template目录下面的login.html页面，此页面并不会被AccountFilter过滤
	}
	
	/**
	 * 异步校验用户登录
	 * @param username
	 * @param password
	 * @return
	 */
	@PostMapping("validateAccount") //validateAccount前面写不写反斜杠都可以
	@ResponseBody // 前端只需要一个数据结果而不需要页面，所以写@ResponseBody
	public String validate(@RequestParam String username, @RequestParam String password, HttpServletRequest request) { //不写@RequestParam也可以的
		Account account = accountService.findByUsernameAndPassword(username, password);
		if (account == null) { // 简单的前端业务逻辑写在Controller中就可以了
			return "error";
		} else {
			// 登录成功就要把用户对象写到session里，在不同的controller或者页面都能使用当前的Account对象
			request.getSession().setAttribute("account", account);
			return "success";
		}
	}
	
	@GetMapping("logout")
	public String logout (HttpServletRequest request) {
		request.getSession().removeAttribute("account");
		//return "/account/login";
		return "index";
	}
	
	@GetMapping("list")
	public String list (@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "1") int pageSize, Model model) {
		PageInfo<Account> page = accountService.findByPage(pageNum, pageSize);
		model.addAttribute("page", page);
		return "/account/list"; // “/” 加不加都可以
	}
	/*@PutMapping("updatePassword") //validateAccount前面写不写反斜杠都可以
	public void updatePassword() { //不写@RequestParam也可以的
		accountService.updatePassword();
	}*/
}
