package com.lisz.controller;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lisz.entity.Permission;
import com.lisz.service.PermissionService;

/**
 * Restful风格的URI的Controller，只跟用户交换JSON数据
 * @author shuzheng
 *
 */
@RestController //Restful Controller, 返回对象的时候，方法的脑袋顶上不用写@ResponseBody注解
@RequestMapping("/api/v1/manager/permission") //v1是为了后面出新版本的时候用v2，当前版本不用改.Restful是一种规范：/api/版本号/系统名称/实体/方法/被操作ID。。不一定完全遵循
public class PermissionRestController {
	@Autowired
	private PermissionService permissionService;
	
	@PostMapping("add")
	public ResponseStatus add(@RequestBody Permission permission) {
		System.out.println(ToStringBuilder.reflectionToString(permission, ToStringStyle.MULTI_LINE_STYLE));
		return permissionService.add(permission);
	}
	
	@PostMapping("update")
	public ResponseStatus update(@RequestBody Permission permission) {
		System.out.println(ToStringBuilder.reflectionToString(permission, ToStringStyle.MULTI_LINE_STYLE));
		return permissionService.update(permission);
	}
	
	@DeleteMapping("delete")
	public ResponseStatus deleteById(@RequestParam int id) {
		System.out.println("Deleting permission id = " + id);
		return permissionService.deleteById(id);
	}
}
