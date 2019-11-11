package com.lisz.entity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 系统配置相关
 * @author shuzheng
 *
 */
@Component //纳入spring 管理
public class SysConfig {
	@Value("${system.name}") //SpEL表达式，当前指定的properties文件，如：spring.profiles.active=dev 或者没指定时默认的application.properties文件中没有system.name的话springboot会启动失败
	private String name;     //直接从命令行参数中取也能拿得到：java -jar SpringBootMVCProjectApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --system.name=Google

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
