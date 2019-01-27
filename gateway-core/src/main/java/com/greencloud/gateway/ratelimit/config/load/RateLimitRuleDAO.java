package com.greencloud.gateway.ratelimit.config.load;

import com.google.common.collect.Lists;
import com.greencloud.gateway.common.datasource.DataSourceHolder;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author leejianhao
 */
public class RateLimitRuleDAO implements IRateLimitRuleDAO {

    private String sql;
    private HikariDataSource dataSource;

    public  RateLimitRuleDAO() {
        dataSource = (HikariDataSource) DataSourceHolder.getInstance().getDataSource();
        sql = "select api.path, r.limit, r.limit_client, r.limit_app, r.time_unit from api_flow_rule a left join flow_rule r on a.flow_rule_id = r.id left join api on a.api_id = api.id";
    }

    @Override
    public List<RateLimitRule> getAllRelation() throws Exception {
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = null;
        ResultSet r = null;

        List<RateLimitRule> list = Lists.newArrayList();

        try{

            connection.setAutoCommit(true);
            ps = connection.prepareStatement(sql);

            r = ps.executeQuery();
            while(r.next()){
                list.add(buildApiFlowRuleRelation(r));
            }

        }finally{
            try{
                if( r != null){
                    r.close();
                }

                if(ps != null){
                    ps.close();
                }
            }finally{
                connection.close();
            }
        }
        return list;
    }

    private RateLimitRule buildApiFlowRuleRelation(ResultSet r) throws Exception {
        return new RateLimitRule(r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), getTimeUnit( r.getString(5)));
    }

    private TimeUnit getTimeUnit(String timeUnit) {
        switch (timeUnit) {
            case "SECOND":
                return TimeUnit.SECONDS;
            case "MINUTE":
                return TimeUnit.MINUTES;
            case "HOUR":
                return TimeUnit.HOURS;
            case "DAY":
                return TimeUnit.DAYS;
            default:
                return TimeUnit.SECONDS;
        }

    }
}
