package com.greencloud.gateway.scriptManage;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.google.common.collect.Maps;
import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GatewayFilterPoller {

	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayFilterPoller.class);

	private Map<String, FilterInfo> runningFilters = Maps.newHashMap();

	private DynamicBooleanProperty pollerEnabled = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(GatewayConstants.GATEWAY_FILTER_POLLER_ENABLED, true);

	private DynamicLongProperty pollerInterval = DynamicPropertyFactory.getInstance()
			.getLongProperty(GatewayConstants.GATEWAY_FILTER_POLLER_INTERVAL, 30000);

	private DynamicBooleanProperty active = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(GatewayConstants.GATEWAY_USE_ACTIVE_FILTERS, true);
	private DynamicBooleanProperty canary = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(GatewayConstants.GATEWAY_USE_CANARY_FILTERS, false);

	private DynamicStringProperty preFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_PRE_PATH, null);
	private DynamicStringProperty routeFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_ROUTE_PATH, null);
	private DynamicStringProperty postFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_POST_PATH, null);
	private DynamicStringProperty errorFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_ERROR_PATH, null);
	private DynamicStringProperty customFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_CUSTOM_PATH, null);

	private static GatewayFilterPoller instance = null;

	private volatile boolean running = true;

	private Thread checherThread = new Thread("GatewayFilterPoller") {

		@Override
		public void run() {
			while (running) {
				try {
					if (!pollerEnabled.get()) {
						continue;
					}
					if (canary.get()) {
						Transaction tran = Cat.getProducer().newTransaction("FilterPoller", "canary-"+ GatewayFilterDAOFactory.getCurrentType());
						
						try{
							Map<String, FilterInfo> filterSet = Maps.newHashMap();
	
							List<FilterInfo> activeScripts = GatewayFilterDAOFactory.getGatewayFilterDAO().getAllActiveFilters();
	
							if (!activeScripts.isEmpty()) {
								for (FilterInfo filterInfo : activeScripts) {
									filterSet.put(filterInfo.getFilterId(), filterInfo);
								}
							}
	
							List<FilterInfo> canaryScripts = GatewayFilterDAOFactory.getGatewayFilterDAO().getAllCanaryFilters();
							if (!canaryScripts.isEmpty()) {
								for (FilterInfo filterInfo : canaryScripts) {
									filterSet.put(filterInfo.getFilterId(), filterInfo);
								}
							}
	
							for (FilterInfo filterInfo : filterSet.values()) {
								doFilterCheck(filterInfo);
							}

							List<FilterInfo> inActiveScripts = GatewayFilterDAOFactory.getGatewayFilterDAO().getAllInActiveFilters();
							for (FilterInfo filterInfo : inActiveScripts) {
								deleteInActiveFilterFromDisk(filterInfo);
							}

							tran.setStatus(Transaction.SUCCESS);
						}catch(Throwable t){
							tran.setStatus(t);
							Cat.logError(t);
						}finally{
							tran.complete();
						}
					} else if (active.get()) {
						Transaction tran = Cat.newTransaction("FilterPoller", "active-"+ GatewayFilterDAOFactory.getCurrentType());
						
						try{
							List<FilterInfo> newFilters = GatewayFilterDAOFactory.getGatewayFilterDAO().getAllActiveFilters();
							
							tran.setStatus(Transaction.SUCCESS);
							if (newFilters.isEmpty()) {
								continue;
							}
							for (FilterInfo newFilter : newFilters) {
								doFilterCheck(newFilter);
							}
						}catch(Throwable t){
							tran.setStatus(t);
							Cat.logError(t);
						}finally{
							tran.complete();
						}
					}
				} catch (Throwable t) {
					LOGGER.error("GatewayFilterPoller run error!", t);
				} finally {
					try {
						sleep(pollerInterval.get());
					} catch (InterruptedException e) {
						LOGGER.error("GatewayFilterPoller sleep error!", e);
					}
				}
			}
		}
	};
	
	private GatewayFilterPoller(){

		this.checherThread.start();
	}
	
	
	public static void start(){
		if(instance == null){
			synchronized(GatewayFilterPoller.class){
				if(instance == null){
					instance = new GatewayFilterPoller() ;
				}
			}
		}
	}
	
	public static GatewayFilterPoller getInstance(){
		return instance;
	}

	public void stop(){
		this.running = false;
	}

	private void deleteInActiveFilterFromDisk(FilterInfo deleteFilter) {
		String filterType = deleteFilter.getFilterType();

		String path = getFilterPath(filterType);

		File f = new File(path, deleteFilter.getFilterName() + ".groovy");
		if (f.exists()) {
			f.delete();
		}
	}

	private String getFilterPath(String filterType) {
		String path = preFiltersPath.get();
		if ("post".equals(filterType)) {
			path = postFiltersPath.get();
		} else if ("route".equals(filterType)) {
			path = routeFiltersPath.get();
		} else if ("error".equals(filterType)) {
			path = errorFiltersPath.get();
		} else if (!"pre".equals(filterType) && customFiltersPath.get() != null) {
			path = customFiltersPath.get();
		}
		return path;
	}

	private void doFilterCheck(FilterInfo newFilter) throws IOException {
		FilterInfo existFilter = runningFilters.get(newFilter.getFilterId());
		if (existFilter == null || !existFilter.equals(newFilter)) {
			LOGGER.info("adding filter to disk" + newFilter.toString());
			writeFilterToDisk(newFilter);
			runningFilters.put(newFilter.getFilterId(), newFilter);
		}
	}

	private void writeFilterToDisk(FilterInfo newFilter) throws IOException {
		String filterType = newFilter.getFilterType();

		String path = getFilterPath(filterType);

		File f = new File(path, newFilter.getFilterName() + ".groovy");
		FileWriter file = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(file);
		out.write(newFilter.getFilterCode());
		out.close();
		file.close();
		LOGGER.info("filter written " + f.getPath());
	}
}
