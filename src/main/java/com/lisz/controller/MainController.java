package com.lisz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MainController {
	
	@GetMapping("/")
	public String index() { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		return "index";
	}
	
	@GetMapping("/index")
	public String index1() { //这里由于用了@ResponseBody，返回的accounts会被转化成一个JSON数组在前端显示，这里返回Object即可,List<Account>也行
		return "index";  // 这里写"account/login"（带目录的），会跳转到login页面
	}
	
	@GetMapping("/errorPage/{code}")
	public String errorPage(Model model, @PathVariable int code) {
		String msg = "未知错误";
		switch (code) {
		case 401:
			msg = "您无权访问当前页面";
			break;
		case 500:
			msg = "服务器内部错误";
		default:
			break;
		}
		model.addAttribute("msg", msg);
		return "errorPage";
	}
}
