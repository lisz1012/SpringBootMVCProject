package com.lisz.controller;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageInfo;
import com.lisz.entity.Account;
import com.lisz.entity.Permission;
import com.lisz.entity.Role;
import com.lisz.service.AccountService;
import com.lisz.service.PermissionService;
import com.lisz.service.RoleService;

/**
 * 用户账号相关
 * @author shuzheng
 *
 */
@Controller // 写@RestController里面就没法跳转页面了
@RequestMapping("/manager")
public class ManagerController {
	
	@Autowired
	private AccountService accountService; //service属于model，和后端做计算存储和整理数据的
	
	@Autowired
	private PermissionService permissionService; //service属于model，和后端做计算存储和整理数据的
	
	@Autowired
	private RoleService roleService; //service属于model，和后端做计算存储和整理数据的
	
	@GetMapping("accountList")
	public String accountList(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "5") int pageSize, Model model) {
		PageInfo<Account> page = accountService.findByPage(pageNum, pageSize);
		model.addAttribute("page", page);
		return "/manager/accountList";
	}
	
	@GetMapping("roleList")
	public String roleList(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "5") int pageSize, Model model) {
		PageInfo<Role> page = roleService.findByPage(pageNum, pageSize);
		model.addAttribute("page", page);
		return "/manager/roleList";
	}
	
	@GetMapping("permissionList")
	public String permissionList(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "5") int pageSize, Model model) {
		PageInfo<Permission> page = permissionService.findByPage(pageNum, pageSize);
		model.addAttribute("page", page);
		return "/manager/permissionList";
	}
	
}
