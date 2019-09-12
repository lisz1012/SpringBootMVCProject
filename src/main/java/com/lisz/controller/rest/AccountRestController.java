package com.lisz.controller.rest;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lisz.controller.ResponseStatus;
import com.lisz.service.AccountService;

/**
 * Restful风格的URI的Controller，只跟用户交换JSON数据
 * @author shuzheng
 *
 */
@RestController //Restful Controller, 返回对象的时候，方法的脑袋顶上不用写@ResponseBody注解, 到了前端就能被解析为JSON
@RequestMapping("/api/v1/manager/account") //v1是为了后面出新版本的时候用v2，当前版本不用改.Restful是一种规范：/api/版本号/系统名称/实体/方法/被操作ID。。不一定完全遵循
public class AccountRestController {
	@Autowired
	private AccountService accountService;
	
	@PostMapping("accountRole/add")
	public ResponseStatus roleAdd(@RequestParam int accountId, @RequestParam int[] roleIds) {
		System.out.println("Account ID: " + accountId);
		System.out.println("Role IDs: " + ToStringBuilder.reflectionToString(roleIds));
		
		accountService.addRolesForAccount(roleIds, accountId);
		
		return new ResponseStatus(200, "OK", "Success!"); 
	}
}
