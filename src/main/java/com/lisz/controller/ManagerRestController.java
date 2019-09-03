package com.lisz.controller;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lisz.entity.Permission;

/**
 * Restful风格的URI的Controller，只跟用户交换JSON数据
 * @author shuzheng
 *
 */
@RestController //Restful Controller, 返回对象的时候，方法的脑袋顶上不用写@ResponseBody注解
@RequestMapping("/api/v1/manager/permission") //v1是为了后面出新版本的时候用v2，当前版本不用改
public class ManagerRestController {
	
	@PostMapping("add")
	public ResponseStatus add(@RequestBody Permission permission) {
		System.out.println(ToStringBuilder.reflectionToString(permission, ToStringStyle.MULTI_LINE_STYLE));
		return new ResponseStatus(200, "", "");
	}
}
