package com.lisz.controller.rest;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lisz.controller.ResponseStatus;
import com.lisz.entity.Role;
import com.lisz.service.RoleService;

/**
 * Restful风格的URI的Controller，只跟用户交换JSON数据
 * @author shuzheng
 *
 */
@RestController //Restful Controller, 返回对象的时候，方法的脑袋顶上不用写@ResponseBody注解, 到了前端就能被解析为JSON
@RequestMapping("/api/v1/manager/role") //v1是为了后面出新版本的时候用v2，当前版本不用改.Restful是一种规范：/api/版本号/系统名称/实体/方法/被操作ID。。不一定完全遵循
public class RoleRestController {
	@Autowired
	private RoleService roleService;
	
	@PostMapping("add")
	public ResponseStatus add(@RequestBody Role role) {
		System.out.println(ToStringBuilder.reflectionToString(role, ToStringStyle.MULTI_LINE_STYLE));
		return roleService.add(role);
	}
	
	@PostMapping("update")
	public ResponseStatus update(@RequestBody Role role) {
		System.out.println(ToStringBuilder.reflectionToString(role, ToStringStyle.MULTI_LINE_STYLE));
		return roleService.update(role);
	}
	
	@DeleteMapping("delete")
	public ResponseStatus deleteById(@RequestParam int id) {
		System.out.println("Deleting permission id = " + id);
		return roleService.deleteById(id);
	}
	
	@PostMapping("rolePermission/add")
	public ResponseStatus permissionAdd(@RequestParam int roleId, @RequestParam int[] permissionIds) {
		System.out.println("Role ID: " + roleId);
		System.out.println("Permission IDs: " + ToStringBuilder.reflectionToString(permissionIds));
		
		roleService.addPermissionsForRole(permissionIds, roleId);
		
		return new ResponseStatus(200, "OK", "Success!"); 
	}
}
