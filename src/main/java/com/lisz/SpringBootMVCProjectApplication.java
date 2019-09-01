package com.lisz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@MapperScan("com.lisz.mapper")//Spring启动的时候要注册一下，否则通过配置文件从容器中拿的时候会找不到，启动的时候就报错。或者在实体类脑袋上面写@Mapper
public class SpringBootMVCProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootMVCProjectApplication.class, args);
	}

}
