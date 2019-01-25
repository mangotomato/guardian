package com.greencloud.gateway.ratelimit.config.load;

import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.greencloud.gateway.ratelimit.config.RateLimitRuleManager;
import com.greencloud.gateway.scriptManage.GatewayFilterPoller;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author leejianhao
 */
public class RateLimitRuleLoaderPoller {
    private static final Logger logger = LoggerFactory.getLogger(GatewayFilterPoller.class);

    private DynamicBooleanProperty pollerEnabled = DynamicPropertyFactory.getInstance()
            .getBooleanProperty(GatewayConstants.GATEWAY_FLOW_RULE_POLLER_ENABLED, true);
    private DynamicLongProperty pollerInterval = DynamicPropertyFactory.getInstance()
            .getLongProperty(GatewayConstants.GATEWAY_FLOW_RULE_INTERVAL, 30000);

    private static RateLimitRuleLoaderPoller instance = null;

    private volatile boolean running = true;

    private Thread loaderThread = new Thread("GatewayFilterPoller") {

        @Override
        public void run() {
            while (running) {
                try {
                    if (!pollerEnabled.get()) {
                        continue;
                    }

                    loadRateLimitRule();

                } catch (Throwable t) {
                    logger.error("RateLimitRuleLoaderPoller run error!", t);
                } finally {
                    try {
                        sleep(pollerInterval.get());
                    } catch (InterruptedException e) {
                        logger.error("RateLimitRuleLoaderPoller sleep error!", e);
                    }
                }
            }
        }
    };

    private void loadRateLimitRule() {
        try {
            IRateLimitRuleDAO dao = new RateLimitRuleDAO();
            List<RateLimitRule> rules = dao.getAllRelation();
            RateLimitRuleManager.loadRules(rules);
            logger.info("Rate limit rule loaded");
        } catch (Exception e) {
            logger.info("Rate limit rule loaded fail", e);
        }
    }

    private RateLimitRuleLoaderPoller() {
        loaderThread.start();
    }

    public static void start() {
        if (instance == null) {
            synchronized (RateLimitRuleLoaderPoller.class) {
                if (instance == null) {
                    instance = new RateLimitRuleLoaderPoller();
                }
            }
        }
    }

    public static RateLimitRuleLoaderPoller getInstance() {
        return instance;
    }

    public void stop() {
        this.running = false;
    }
}
