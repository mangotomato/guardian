package com.greencloud.gateway.upstreamCheck;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.greencloud.gateway.constants.HttpMethod;
import com.greencloud.gateway.upstreamCheck.config.Status;
import com.greencloud.gateway.upstreamCheck.config.UpstreamCheckConfig;
import com.greencloud.gateway.upstreamCheck.http.HttpCheckInvoker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author leejianhao
 */
public class UpstreamCheck {
    private static volatile UpstreamCheck upstreamCheck;

    private ConcurrentMap<String/* service group */, UpstreamCheckConfig> configs = new ConcurrentHashMap<>();
    private ConcurrentMap<String/* service group */, Status> status = new ConcurrentHashMap<>();
    ThreadPoolExecutor poolExecutor;

    private Runnable check = new Runnable() {
        @Override
        public void run() {

        }
    };

    private UpstreamCheck() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("check-pool-%d").build();
        poolExecutor = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(), namedThreadFactory);
        startCheck();
    }

    public static UpstreamCheck getInstance() {
        if (upstreamCheck == null) {
            synchronized (UpstreamCheck.class) {
                if (upstreamCheck == null) {
                    upstreamCheck = new UpstreamCheck();
                }
            }
        }
        return upstreamCheck;
    }

    public void setUpstreamCheckConfig(String serviceGroup, String configStr) {

    }

    public void startCheck() {
        for (Map.Entry<String, UpstreamCheckConfig> entry : configs.entrySet()) {
            poolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String serviceGroup = entry.getKey();
                    UpstreamCheckConfig config = entry.getValue();

                    if (HttpMethod.GET.equals(config.getMethod())) {
                        List<String> servers = config.getServers();
                        for (String server : servers) {
                            String url = server + config.getPath();
                            Status status = HttpCheckInvoker.get(url, config.getTimeout(), config.getTimeout(), config.getExpectResponse(), config.getExpectStatus());
                            if (status == Status.UP) {
                                int count = incrAndGet(serviceGroup, server, status);
                                if (count == 1) {
                                    tryEraseDownStatus(serviceGroup, serviceGroup);
                                }
                                if (count >= config.getRiseCount()) {
                                    updateServiceInstanceStatus(serviceGroup, server, status);
                                }
                            } else if (status == Status.DOWN) {
                                int count = incrAndGet(serviceGroup, server, status);
                                if (count == 1) {
                                    tryEraseUpStatus(serviceGroup, serviceGroup);
                                }
                                if (count >= config.getFallCount()) {
                                    updateServiceInstanceStatus(serviceGroup, server, status);
                                }
                            }
                        }
                    }
                }
            });

        }
    }

    private void tryEraseStatus(String serviceGroup, String serviceInstance, Status status) {
        // todo
    }

    private void tryEraseDownStatus(String serviceGroup, String serviceInstance) {
        Status status = Status.DOWN;
        tryEraseStatus(serviceGroup, serviceInstance, status);
    }

    private void tryEraseUpStatus(String serviceGroup, String serviceInstance) {
        Status status = Status.UP;
        tryEraseStatus(serviceGroup, serviceInstance, status);
    }

    private int incrAndGet(String serviceGroup, String serviceInstance, Status status) {
        // todo update to redis
        return 0;
    }

    private void updateServiceInstanceStatus(String serviceGroup, String serverInstance, Status status) {
        // todo update to redis
    }
}
