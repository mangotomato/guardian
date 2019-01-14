package com.greencloud.gateway.servlet;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.google.common.collect.Lists;
import com.greencloud.gateway.common.CatContext;
import com.greencloud.gateway.common.ant.AntPathMatcher;
import com.greencloud.gateway.common.ant.PathMatcher;
import com.greencloud.gateway.constants.Constants;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CatServletFilter implements Filter {

	private String[] urlPatterns = new String[0];
	private List<String> ignoreUrls = Collections.emptyList();

	private PathMatcher matcher = new AntPathMatcher();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String patterns = filterConfig.getInitParameter
				("CatHttpModuleUrlPatterns");
		if (patterns != null) {
			patterns = patterns.trim();
			urlPatterns = patterns.split(",");
			for (int i = 0; i < urlPatterns.length; i++) {
				urlPatterns[i] = urlPatterns[i].trim();
			}
		}

		String ignoreUrlString = filterConfig.getInitParameter("ignoreUrls");
		if (ignoreUrlString != null) {
			String[] ignoreUrlArr = ignoreUrlString.trim().split(",");
			for (int i = 0; i < ignoreUrlArr.length; i++) {
				ignoreUrlArr[i] = ignoreUrlArr[i].trim();
			}
			ignoreUrls = Arrays.asList(ignoreUrlArr);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse
            servletResponse, FilterChain filterChain) throws IOException,
            ServletException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;

		String contextPath = request.getContextPath();
		String path = request.getRequestURI();
		if (contextPath != null && contextPath.trim().length() > 0) {
			path = request.getRequestURI().replace(contextPath, "");
		}

		for (String ignoreUrl : ignoreUrls) {
			if (matcher.match(ignoreUrl, path)) {
				return;
			}
		}

		String url = request.getRequestURL().toString();
		for (String urlPattern : urlPatterns) {
			if (url.startsWith(urlPattern)) {
				url = urlPattern;
			}
		}

		Transaction t = Cat.newTransaction("Service", url);

		try {

			// 衔接上游cat上下文，串起整个链路
			CatContext propertyContext = new CatContext();
			propertyContext.addProperty(Cat.Context.ROOT, request.getHeader(Constants.CAT_ROOT_MESSAGE_ID));
			propertyContext.addProperty(Cat.Context.PARENT, request.getHeader(Constants.CAT_PARENT_MESSAGE_ID));
			propertyContext.addProperty(Cat.Context.CHILD, request.getHeader(Constants.CAT_CHILD_MESSAGE_ID));
			Cat.logRemoteCallServer(propertyContext);

			Cat.logEvent("Service.method", request.getMethod(), Message.SUCCESS, request.getRequestURL().toString());
			Cat.logEvent("Service.client", request.getRemoteHost());

			filterChain.doFilter(servletRequest, servletResponse);

			t.setStatus(Transaction.SUCCESS);
		} catch (Exception ex) {
			t.setStatus(ex);
			Cat.logError(ex);
			throw ex;
		} finally {
			t.complete();
		}
	}

	@Override
	public void destroy() {

	}
}