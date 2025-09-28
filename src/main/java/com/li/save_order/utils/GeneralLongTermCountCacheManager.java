package com.li.save_order.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


/**
 * 通用的中长期时间范围数量统计插件，适用于数据量比较小而且时间长度在半年或者一年之内的指标统计与查询
 * 实现方式为redis的zset滑动窗口实现。
 */
@Slf4j
@Component
public class GeneralLongTermCountCacheManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存长期小数据量的指标数量，只能统计count
     *
     * @param keyName   redis 聚合的标识，例如memberId或者shopid等
     * @param remark    放在value中，可用作范围聚合或者标识区分，无使用需求可直接给任意非重复的值，比如当前时间戳
     * @param sceneCode 场景编码
     */
    public long saveIndicatorCount(String keyName, String remark, String sceneCode) {
        try {
            long currentMS = System.currentTimeMillis();
            long startTime = currentMS - 3600 * 1000;
            String fullName = "localTest:" + sceneCode + ":" + keyName;
            this.redisTemplate.opsForZSet().removeRangeByScore(fullName, 0, startTime);
            //避免redis的大key，判断如果达到窗口最大值限额则直接返回不走后续逻辑
            long totalCount = this.redisTemplate.opsForZSet().zCard(fullName);

            log.info("通用的中长期时间范围数量统计插件fullName:{}的窗口数量为,totalCount={}", fullName, totalCount);

            this.redisTemplate.opsForZSet().add(fullName, remark, currentMS);
            this.redisTemplate.expire(fullName, 3600, TimeUnit.SECONDS);

            return totalCount;
        } catch (Exception e) {
            log.error("保存长期小数据量的指标数量出现错误,入参为keyName:{},remark:{},keyName:{},sceneCode:{} ", keyName, remark, sceneCode, e.getStackTrace());
        }
        return 0;
    }


    /**
     * 查询长期小数据量的指标数量
     */
    public long getIndicatorCount(String keyName, String sceneCode, long beforeSecond) {
        try {
            long currentMS = System.currentTimeMillis();
            String fullName = "localTest:" + sceneCode + ":" + keyName;
            long startTime = currentMS - beforeSecond * 1000;
            Long count = this.redisTemplate.opsForZSet().count(fullName, startTime, currentMS);
//            log.info("通用的中长期时间范围数量统计插件fullName:{},beforeSecond:{},查询出的数量为={}", fullName, beforeSecond, count);
            return count;
        } catch (Exception e) {
            log.error("查询长期小数据量的指标数量出现错误,入参为keyName:{},sceneCode:{},sceneCode:{},beforeSecond:{} "
                    , keyName, sceneCode, beforeSecond, e.getStackTrace());
        }
        return 0L;
    }


}

