package com.lisz.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 用户权限处理
 * @author shuzheng
 *
 */
@Component
@WebFilter(urlPatterns = "/*") //写filter必须有这两个注解，第二个标识了所有的URL都要经过Filter来一次, 而且必须实现Filter
public class AccountFilter implements Filter {
	
	private static final Set<String> IGNORED_URI = new HashSet<>();
	static {
		IGNORED_URI.add("/index");
		IGNORED_URI.add("/css/");
		IGNORED_URI.add("/js/");
		IGNORED_URI.add("/images/");
		IGNORED_URI.add("/account/login");
		IGNORED_URI.add("/account/validateAccount"); //登录验证不能拦截啊
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = ((HttpServletRequest)req);
		HttpServletResponse response = ((HttpServletResponse)resp);
		Object obj = request.getSession().getAttribute("account");
		String uri = request.getRequestURI();  //"/index" 是个uri
		if (obj == null && !canIgnore(uri)) {
			// 跳转登录页面
			response.sendRedirect("/account/login");
			return;
		}
		chain.doFilter(request, response); //只有这一句的话直接通过，相当于没有filter
	}

	private boolean canIgnore(String uri) {
		for (String str : IGNORED_URI) {
			if (uri.startsWith(str)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// 这里可以加载Filter启动之前所需要的资源，这里只打印一下
		System.out.println("Filter is being started...");
		Filter.super.init(filterConfig);
	}
	
	@GetMapping("/account/logout")
	public String logout (HttpServletRequest request) {
		request.getSession().removeAttribute("account");
		return "/account/login";
	}
}
