package com.li.save_order.service;

import com.li.save_order.entity.OrdersLog;
import com.li.save_order.mapper.OrdersLogMapper;
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

        originalStart = originalStart.plusSeconds(stepSecond);
        originalEnd = originalEnd.plusSeconds(stepSecond);

        System.out.println("循环结束");
    }


}
