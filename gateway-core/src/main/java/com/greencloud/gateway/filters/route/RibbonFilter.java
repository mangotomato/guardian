package com.greencloud.gateway.filters.route;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.common.CatContext;
import com.greencloud.gateway.constants.Constants;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.microservice.ribbon.ClientFactory;
import com.greencloud.gateway.util.HTTPRequestUtil;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.niws.client.http.RestClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leejianhao
 */
public class RibbonFilter extends GatewayFilter {

	private static final DynamicBooleanProperty EUREKA_ENABLE = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(GatewayConstants.EUREKA_ENABLE, false);
	private static final DynamicStringProperty ROUTE_REGISTRY_CENTER = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.ROUTE_REGISTER_CENTER, "");

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 100;
	}

	@Override
	public boolean shouldFilter() {
		return "eureka".equalsIgnoreCase(ROUTE_REGISTRY_CENTER.get()) && EUREKA_ENABLE.get();
	}

	@Override
	public Object run() throws GatewayException {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		String url = ctx.getRouteUrl();

		String serviceName = (String) ctx.get(GatewayConstants.ROUTE_REGISTER_CENTER_SERVICE_NAME);

		Transaction tran = Cat.newTransaction("RibbonFilter",
				StringUtils.substringBefore(request.getRequestURL().toString(), "?") + "=>" + StringUtils
						.substringBefore(url, "?"));
		try {
			String verb = request.getMethod().toUpperCase();
			Collection<Header> headers = buildRequestHeaders(request);
			InputStream requestEntity = getRequestBody(request);

			IClientConfig clientConfig = buildRequestConfig(serviceName);
			RestClient client = (RestClient) ClientFactory.getNamedClient(serviceName, clientConfig);

			HttpResponse response = forward(client, clientConfig, verb, url, serviceName, headers, requestEntity);
			setResponse(response);
			tran.setStatus(Transaction.SUCCESS);
		} catch (Exception ex) {
			tran.setStatus(ex);
			String errorMsg = String.format("Origin URL: %s => Target URL: %s error, cause: %s",
					request.getRequestURL(), url, ex.getMessage());
			Cat.logError(errorMsg, ex);
			throw new GatewayException("Route Error", 500, ex.getMessage());
		} finally {
			tran.complete();
		}
		return null;
	}

	public HttpResponse forward(AbstractLoadBalancerAwareClient client, IClientConfig clientConfig, String verb,
								String uri, String serviceName,
								Collection<Header> headers, InputStream entity)
			throws IOException, URISyntaxException, ClientException {
		HttpRequest request;
		HttpRequest.Builder builder = HttpRequest.newBuilder();

		for (Header header : headers) {
			if (Constants.CAT_PARENT_MESSAGE_ID.equalsIgnoreCase(header.getName())
					|| Constants.CAT_CHILD_MESSAGE_ID.equalsIgnoreCase(header.getName())
					|| Constants.CAT_ROOT_MESSAGE_ID.equalsIgnoreCase(header.getName())) {
				continue;
			}
			builder.header(header.getName(), header.getValue());
		}

		Cat.Context ctx = new CatContext();
		Cat.logRemoteCallClient(ctx);
		builder.header(Constants.CAT_ROOT_MESSAGE_ID, ctx.getProperty(Cat.Context.ROOT));
		builder.header(Constants.CAT_PARENT_MESSAGE_ID, ctx.getProperty(Cat.Context.PARENT));
		builder.header(Constants.CAT_CHILD_MESSAGE_ID, ctx.getProperty(Cat.Context.CHILD));

		switch (verb) {
			case "POST":
				builder.verb(HttpRequest.Verb.POST);
				request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
				break;
			case "PUT":
				builder.verb(HttpRequest.Verb.PUT);
				request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
				break;
			default:
				builder.verb(getVerb(verb));
				request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
		}

		HttpResponse response = (HttpResponse) client.executeWithLoadBalancer(request);
		return response;
	}

	private HttpRequest.Verb getVerb(String verb) {
		switch (verb) {
			case "POST":
				return HttpRequest.Verb.POST;
			case "PUT":
				return HttpRequest.Verb.PUT;
			case "DELETE":
				return HttpRequest.Verb.DELETE;
			case "HEAD":
				return HttpRequest.Verb.HEAD;
			case "OPTIONS":
				return HttpRequest.Verb.OPTIONS;
			case "GET":
				return HttpRequest.Verb.GET;
			default:
				return HttpRequest.Verb.GET;
		}
	}

	void setResponse(HttpResponse response) throws ClientException, IOException {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.setResponseStatusCode(response.getStatus());

		boolean isOriginResponseGZipped = false;
		String headerValue;
		Map<String, Collection<String>> map = response.getHeaders();
		for (String headerName : map.keySet()) {
			headerValue = (map.get(headerName).toArray()[0]).toString();
			ctx.addOriginResponseHeader(headerName, headerValue);
			if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
				ctx.setOriginContentLength(headerValue);
			}
			if (isValidResponseHeader(headerName)) {
				ctx.addGatewayResponseHeader(headerName, headerValue);
			}
			if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_ENCODING)) {
				if (HTTPRequestUtil.getInstance().isGzipped(headerValue)) {
					isOriginResponseGZipped = true;
				}
			}
		}
		ctx.setResponseGZipped(isOriginResponseGZipped);
		InputStream inputStream = response.getInputStream();
		ctx.setResponseDataStream(inputStream);
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		} catch (IOException e) {
			// no requestBody is ok.
		}
		return requestEntity;
	}

	private Collection<Header> buildRequestHeaders(HttpServletRequest request) {
		Map<String, Header> headersMap = new HashMap<>();

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement().toLowerCase();
			String value = request.getHeader(name);
			if (isValidRequestHeader(name)) {
				headersMap.put(name, new BasicHeader(name, value));
			}
		}

		Map<String, String> requestHeaders = RequestContext.getCurrentContext().getGatewayRequestHeaders();
		for (String key : requestHeaders.keySet()) {
			String name = key.toLowerCase();
			String value = requestHeaders.get(key);
			headersMap.put(name, new BasicHeader(name, value));
		}

		if (RequestContext.getCurrentContext().getResponseGZipped()) {
			String name = "accept-encoding";
			String value = "gzip";
			headersMap.put(name, new BasicHeader(name, value));
		}
		return headersMap.values();
	}

	private boolean isValidRequestHeader(String name) {
		if (name.toLowerCase().contains("content-length")) {
			return false;
		}
		if (!RequestContext.getCurrentContext().getResponseGZipped()) {
			if (name.toLowerCase().contains("accept-encoding")) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidResponseHeader(String name) {
		switch (name.toLowerCase()) {
			case "connection":
			case "content-length":
			case "content-encoding":
			case "server":
			case "transfer-encoding":
			case "access-control-allow-origin":
			case "access-control-allow-headers":
				return false;
			default:
				return true;
		}
	}

	private IClientConfig buildRequestConfig(String serviceName) {
		IClientConfig clientConfig = ClientFactory.getNamedConfig(serviceName);
		// 通过eureka client获取server list
		clientConfig.setProperty(CommonClientConfigKey.NIWSServerListClassName,
				"com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList");
		// 配置DiscoveryEnabledNIWSServerList，必须配置vipAddress
		clientConfig.setProperty(CommonClientConfigKey.DeploymentContextBasedVipAddresses,
				serviceName);
		// 通过roundrobin挑选server
		clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerRuleClassName,
				"com.netflix.loadbalancer.RoundRobinRule");
		clientConfig.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, RibbonPropertyHelper
				.getRibbonMaxHttpConnectionsPerHost(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxTotalHttpConnections, RibbonPropertyHelper
				.getRibbonMaxTotalHttpConnections(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxAutoRetries, RibbonPropertyHelper
				.getRibbonMaxAutoRetries(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxAutoRetriesNextServer, RibbonPropertyHelper
				.getRibbonMaxAutoRetriesNextServer(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.RequestSpecificRetryOn, true);
		return clientConfig;
	}

	/**
	 * @program: gateway-parent
	 * @description:
	 * @author: WuYiping
	 * @create: 2019-03-08 15:53
	 **/
	private static class RibbonPropertyHelper {

		public static int getRibbonMaxHttpConnectionsPerHost(String serviceName) {
			int i = DynamicPropertyFactory.getInstance()
					.getIntProperty("ribbon." + serviceName + ".hystri.maxconnections.perhost", 0)
					.get();
			if (i == 0) {
				i = DynamicPropertyFactory.getInstance()
						.getIntProperty(GatewayConstants.GATEWAY_CLIENT_ROUTE_MAX_CONNECTIONS, 500)
						.get();
			}
			return i;
		}

		public static int getRibbonMaxTotalHttpConnections(String serviceName) {
			int i = DynamicPropertyFactory.getInstance()
					.getIntProperty("ribbon." + serviceName + ".hystrix.maxconnections", 0)
					.get();
			if (i == 0) {
				i = DynamicPropertyFactory.getInstance()
						.getIntProperty(GatewayConstants.GATEWAY_CLIENT_MAX_CONNECTIONS, 2000)
						.get();
			}
			return i;
		}

		public static int getRibbonMaxAutoRetries(String serviceName) {
			int i = DynamicPropertyFactory.getInstance()
					.getIntProperty("ribbon." + serviceName + ".hystrix.maxautoretries", 0)
					.get();
			if (i == 0) {
				i = DynamicPropertyFactory.getInstance()
						.getIntProperty("ribbon.global.hystrix.maxautoretries.global", 1)
						.get();
			}
			return i;
		}

		public static int getRibbonMaxAutoRetriesNextServer(String serviceName) {
			int i = DynamicPropertyFactory.getInstance()
					.getIntProperty(serviceName + ".ribbon.hystrix.maxautoretries.nextserver", 0)
					.get();
			if (i == 0) {
				i = DynamicPropertyFactory.getInstance()
						.getIntProperty("ribbon.hystrix.maxautoretries.nextserver.global", 1)
						.get();
			}
			return i;
		}
	}
}
