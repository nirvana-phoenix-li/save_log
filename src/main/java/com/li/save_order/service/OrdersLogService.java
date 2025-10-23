package com.li.save_order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.entity.YhRiskEngineTetrad;
import com.li.save_order.mapper.OrdersLogMapper;
import com.li.save_order.mapper.YhRiskEngineTetradMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
public class OrdersLogService {
    @Autowired
    ParseLogStructService parseLogStructService;
    @Autowired
    NetWorkService netWorkService;
    @Autowired
    DisposalService disposalService;
    @Autowired
    OrdersLogMapper ordersLogMapper;
    @Autowired
    YhRiskEngineTetradMapper yhRiskEngineTetradMapper;


    public void executor(int number) throws IOException, InterruptedException {
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("进入风控V2.0", "");
        stringHashMap.put("结束", "and");
        stringHashMap.put("order", "");
        stringHashMap.put("福州", "");
        //实际查询时间是 8个小时之后，有时差
        int stepSecond = 30;
        LocalDateTime originalStart = LocalDateTime.of(2025, 10, 21, 8, 35, 0, 0);
        LocalDateTime originalEnd = originalStart.plusSeconds(stepSecond);

        for (int k = 0; k < number; k++) {
            String response = null;
            int redo = 0;
            while (response == null || response.startsWith("{\"id\"")) {
                redo++;
                response = netWorkService.callElasticSearchAPI(originalStart.toString(), originalEnd.toString(), stringHashMap);
            }
            System.out.println("当前为第" + k + "次，重试次数为: " + (redo - 1));
            System.out.println("API响应长度: " + response.length());
            List<OrdersLog> ordersLogs = parseLogStructService.parseContentSimple(response);
            for (OrdersLog ordersLog : ordersLogs) {
                disposalService.dealWithAllWork(ordersLog);
                ordersLogMapper.insert(ordersLog);
            }

            originalStart = originalStart.plusSeconds(stepSecond);
            originalEnd = originalEnd.plusSeconds(stepSecond);
            Thread.sleep(stepSecond * 1000);
        }
        System.out.println("循环结束");
    }

    public void getMemberRoute(String memberId, Integer year, Integer month, Integer day, Integer hour, Integer minute) throws IOException {
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("进入风控V2.0", "");
        stringHashMap.put("结束", "and");
        stringHashMap.put(memberId, "");
        stringHashMap.put("order", "");
        //实际查询时间是 8个小时之后，有时差
        int stepSecond = 7200;
        LocalDateTime originalStart = LocalDateTime.of(year, month, day, hour, minute, 0, 0);
        LocalDateTime originalEnd = originalStart.plusSeconds(stepSecond);

        String response = null;
        int redo = 0;
        while (response == null || response.startsWith("{\"id\"")) {
            redo++;
            response = netWorkService.callElasticSearchAPI(originalStart.toString(), originalEnd.toString(), stringHashMap);
        }
        System.out.println("当前重试次数为: " + (redo - 1));
        System.out.println("API响应长度: " + response.length());
        List<OrdersLog> ordersLogs = parseLogStructService.parseContentSimple(response);
        for (OrdersLog ordersLog : ordersLogs) {
            //补全订单场景的数据
            if ("order".equals(ordersLog.getSceneType())) {
                if (ordersLog.getCouponCode() != null) {
                    continue;
                }else if (ordersLog.getIsBalancePay()){
                    ordersLog.setSceneType("sync-disposable-rule");
                }else {
                    ordersLog.setSceneType("sync-part-rule");
                }
            }

            //查询策略表
            LambdaQueryWrapper<YhRiskEngineTetrad> queryWrapper = new LambdaQueryWrapper<YhRiskEngineTetrad>()
                    .eq(YhRiskEngineTetrad::getCommercialId, "default");

            if (ordersLog.getResourceType() != null) {
                queryWrapper.eq(YhRiskEngineTetrad::getEventCode, ordersLog.getResourceType().toLowerCase());
            }

            if (ordersLog.getSceneType() != null) {
                queryWrapper.eq(YhRiskEngineTetrad::getSceneCode, ordersLog.getSceneType().toLowerCase());
            } else {
                queryWrapper.eq(YhRiskEngineTetrad::getSceneCode, "default");
            }

            //第一次的queryWrapper
            LambdaQueryWrapper<YhRiskEngineTetrad> cloneWrapper = queryWrapper.clone();
            if (ordersLog.getBusinessType() != null) {
                queryWrapper.eq(YhRiskEngineTetrad::getBusinessType, ordersLog.getBusinessType().toLowerCase());
            }

            YhRiskEngineTetrad selectOne = yhRiskEngineTetradMapper.selectOne(queryWrapper);
            if (selectOne == null && !StringUtils.equals(ordersLog.getBusinessType(), "default")) {
                cloneWrapper.eq(YhRiskEngineTetrad::getBusinessType, "default");
            }

            //第二次查询
            selectOne = yhRiskEngineTetradMapper.selectOne(cloneWrapper);
            System.out.println(selectOne.getStrategyId());
//            ordersLogMapper.insert(ordersLog);
        }
        originalStart = originalStart.plusSeconds(stepSecond);
        originalEnd = originalEnd.plusSeconds(stepSecond);
        System.out.println("循环结束");
    }


}
