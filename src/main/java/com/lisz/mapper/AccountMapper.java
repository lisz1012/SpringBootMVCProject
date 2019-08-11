package com.lisz.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

// MyBatis-Plus中ActiveRecord的思想是：new User().setName().setLocation().save()直接进数据库
// 数据就绑定在对象上，一个对象就对应的一个数据。每个Entity类都得到增强，每个实体类增加了CRUD方法
@Mapper //Spring启动的时候要注册一下，否则通过配置文件从容器中拿的时候会找不到，启动的时候就报错。或者在启动类SpringBootMyBatisApplication的脑袋上面写@MapperScan("包名")
public interface AccountMapper {

	// 在application.properties.xml中去找sql的文件的位置，然后找到sql，然后通过反射或者动态代理，拿sql并实现类
	@Select("select * from account")
	List<Account> findAll();

}
