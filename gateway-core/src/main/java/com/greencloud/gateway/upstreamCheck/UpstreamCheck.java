package com.greencloud.gateway.upstreamCheck;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.constants.HttpMethod;
import com.greencloud.gateway.upstreamCheck.config.CheckTypeEnum;
import com.greencloud.gateway.upstreamCheck.config.Status;
import com.greencloud.gateway.upstreamCheck.config.UpstreamCheckConfig;
import com.greencloud.gateway.upstreamCheck.http.HttpCheckInvoker;
import com.greencloud.gateway.util.RedisUtil;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leejianhao
 */
public class UpstreamCheck {
    private static final Logger logger = LoggerFactory.getLogger(UpstreamCheck.class);

    private static final DynamicStringProperty UPSTREAM_CHECK_CONFIG = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.GATEWAY_UPSTREAM_CHECK_CONFIG, null);

    private ConcurrentMap<String/* service group */, List<String>> servers = new ConcurrentHashMap<>();
    private ConcurrentMap<String/* service group */, Status> status = new ConcurrentHashMap<>();
    private AtomicReference<UpstreamCheckConfig> configRef = new AtomicReference<>();

    private ThreadPoolExecutor poolExecutor;
    private List<UpStreamCheckListener> listeners;

    private volatile boolean running = true;

    private volatile static UpstreamCheck instance;

    private Thread checkThread = new Thread("UpstreamCheck") {
        @Override
        public void run() {
            while (running) {
                UpstreamCheckConfig config = configRef.get();
                if (config == null) {
                    return;
                }

                for (Map.Entry<String, List<String>> entry : servers.entrySet()) {

                    poolExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            String serviceGroup = entry.getKey();
                            List<String> servers = entry.getValue();

                            if (HttpMethod.GET.equals(config.getMethod())) {
                                for (String server : servers) {

                                    String url = server + config.getPath();

                                    Status status = HttpCheckInvoker.get(url, config.getTimeout(), config.getTimeout(),
                                            config.getExpectResponse(), config.getExpectStatus());

                                    long count = incrAndGet(serviceGroup, server, status);

                                    if (status == Status.UP) {
                                        // edge trigger
                                        if (count >= config.getRiseCount()) {
                                            tryEraseDownStatus(serviceGroup, serviceGroup);
                                            updateServerInstanceStatus(serviceGroup, server, status);
                                            notifyListener(serviceGroup, server, Status.UP);
                                        }
                                    } else if (status == Status.DOWN) {
                                        // edge trigger
                                        if (count >= config.getFallCount()) {
                                            tryEraseUpStatus(serviceGroup, server);
                                            updateServerInstanceStatus(serviceGroup, server, status);
                                            notifyListener(serviceGroup, server, Status.DOWN);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }

                try {
                    Thread.sleep(config.getInterval());
                } catch (InterruptedException e) {
                    logger.error("Upstream check sleep error", e);
                }
            }
        }
    };

    public UpstreamCheck() {
        listeners = new LinkedList<>();

        loadConfig();
        UPSTREAM_CHECK_CONFIG.addCallback(new Runnable() {
            @Override
            public void run() {
                loadConfig();
            }
        });

        initThreadPool();
        startCheck();
    }

    public static void start() {
        if (instance == null) {
            synchronized (UpstreamCheck.class) {
                if (instance == null) {
                    instance = new UpstreamCheck();
                }
            }
        }
    }

    public static UpstreamCheck getInstance() {
        if (instance == null) {
            start();
        }
        return instance;
    }

    public void stop() {
        this.running = false;
        poolExecutor.shutdownNow();
    }

    private void loadConfig() {
        String configStr = UPSTREAM_CHECK_CONFIG.get();
        if (!Strings.isNullOrEmpty(configStr)) {
            UpstreamCheckConfig config = parseConfig(configStr);
            this.configRef.set(config);
        }
    }

    /**
     * check interval=2000 rise=2 fall=2 timeout=1000 type=http;
     * check_http_send GET /healthcheck;
     * check_http_expect_alive 200;
     * @param configStr
     * @return
     */
    private UpstreamCheckConfig parseConfig(String configStr) {
        UpstreamCheckConfig upstreamCheckConfig = new UpstreamCheckConfig();

        try {
            String[] configLine = configStr.split(";");

            // line 1
            String check = configLine[0].trim();
            String[] checkConfigItem = check.split(" ");
            for (int i = 1; i < checkConfigItem.length; i++) {
                String aItem = checkConfigItem[i];
                String[] kvalue = aItem.split("=");
                switch (kvalue[0]) {
                    case "interval":
                        upstreamCheckConfig.setInterval(Integer.parseInt(kvalue[1]));
                        break;
                    case "rise":
                        upstreamCheckConfig.setRiseCount(Integer.parseInt(kvalue[1]));
                        break;
                    case "fall":
                        upstreamCheckConfig.setFallCount(Integer.parseInt(kvalue[1]));
                        break;
                    case "timeout":
                        upstreamCheckConfig.setTimeout(Integer.parseInt(kvalue[1]));
                        break;
                    case "type":
                        // 当前版本只支持http
                        upstreamCheckConfig.setType(CheckTypeEnum.HTTP);
                }
            }

            // line 2
            String checkHttpSend = configLine[1].trim();
            String[] parts = checkHttpSend.split(" ");
            upstreamCheckConfig.setMethod(parts[1].toUpperCase());
            upstreamCheckConfig.setPath(parts[2]);

            // line 3
            String checkHttpExpectAlive = configLine[2].trim();
            String[] expectAliveStatus = checkHttpExpectAlive.split(" ");
            List<Integer> status = Lists.newArrayList();
            for (int i = 1; i < expectAliveStatus.length; i++) {
                status.add(Integer.parseInt(expectAliveStatus[i]));
            }

            upstreamCheckConfig.setExpectStatus(status);

        } catch (Exception e) {
            logger.error("error to parse upstream_check configRef", e);
            return null;
        }
        return upstreamCheckConfig;
    }

    private void initThreadPool() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("check-pool-%d").build();
        poolExecutor = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(), namedThreadFactory);
    }

    public void addListener(UpStreamCheckListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(UpStreamCheckListener listener) {
        this.listeners.remove(listener);
    }

    public void setUpStreamCheckServers(String serviceGroup, List<String> servers) {
       this.servers.put(serviceGroup, servers);
    }

    private void startCheck() {
        checkThread.start();
    }

    private void notifyListener(String serviceGroup, String server, Status status) {
        for (UpStreamCheckListener listener : listeners) {
            listener.onChange(serviceGroup, server, status);
        }
    }

    private void tryEraseStatus(String serviceGroup, String serverInstance, Status status) {
        String key = serviceGroup + ":" + serverInstance + ":" + status.name();
        RedisUtil.getInstance().del(key);
    }

    private void tryEraseDownStatus(String serviceGroup, String serverInstance) {
        Status status = Status.DOWN;
        tryEraseStatus(serviceGroup, serverInstance, status);
    }

    private void tryEraseUpStatus(String serviceGroup, String serverInstance) {
        Status status = Status.UP;
        tryEraseStatus(serviceGroup, serverInstance, status);
    }

    private long incrAndGet(String serviceGroup, String serverInstance, Status status) {
        String key = serviceGroup + ":" + serverInstance + ":" + status.name();
        return RedisUtil.getInstance().incr(key);
    }

    private void updateServerInstanceStatus(String serviceGroup, String serverInstance, Status status) {
        String key = serviceGroup + ":" + serverInstance;
        RedisUtil.getInstance().set(key, status.name());
    }
}
