package com.li.save_order.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.mapper.InvertedDbMapper;
import com.li.save_order.mapper.OrdersLogMapper;
import com.li.save_order.mapper.SceneConfigMapper;
import com.li.save_order.utils.GeneralLongTermCountCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrdersLogService {
    @Autowired
    OrdersLogMapper ordersLogMapper;
    @Autowired
    SceneConfigMapper sceneConfigMapper;
    @Autowired
    InvertedDbMapper invertedDbMapper;
    @Autowired
    GeneralLongTermCountCacheManager countCacheManager;
    @Autowired
    NetWorkService netWorkService;
    @Autowired
    DisposalService disposalService;


    private static String fixWithRegex(String jsonString) {
        int firstIndex = jsonString.indexOf("idCardInfo");
        if (firstIndex == -1) {
            return jsonString;
        }
        String firstString = jsonString.substring(0, firstIndex + 12);
        String substring = jsonString.substring(firstIndex + 13);

        int index = substring.indexOf("}");
        String secondString = substring.substring(0, index + 1);
        String thirdString = substring.substring(index + 2);
        return firstString + secondString + thirdString;
    }

    private static Map<String, String> parseLogSimple(String log) {
        Map<String, String> result = new HashMap<>();

        if (log == null || log.isEmpty()) {
            return result;
        }

        // 提取TID
        String tid = extractBetween(log, "[TID: ", "]");
        if (tid != null) result.put("tid", tid);

        // 提取消耗时间
        String costTime = extractBetween(log, "消耗[", "ms]");
        if (costTime != null) result.put("costTime", costTime);

        // 提取整个JSON返回值
        String jsonPart = extractBetween(log, "返回值:[", "}]");
        if (jsonPart != null) {
            jsonPart += "}";

            // 从JSON中提取具体字段
            String riskCode = extractBetween(jsonPart, "\"riskCode\":", ",");
            if (riskCode != null) result.put("riskCode", riskCode.trim());

            String riskLevel = extractBetween(jsonPart, "\"riskLevel\":", ",");
            if (riskLevel != null) result.put("riskLevel", riskLevel.trim());

            String riskMsg = extractBetween(jsonPart, "\"riskMsg\":\"", "\"");
            if (riskMsg != null) result.put("riskMsg", riskMsg);
        }

        return result;
    }

    private static String extractBetween(String source, String start, String end) {
        try {
            int startIndex = source.indexOf(start);
            if (startIndex == -1) return null;

            startIndex += start.length();
            int endIndex = source.indexOf(end, startIndex);
            if (endIndex == -1) return null;

            return source.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

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
            parseContentSimple(response);
            originalStart = originalStart.plusSeconds(stepSecond);
            originalEnd = originalEnd.plusSeconds(stepSecond);
            Thread.sleep(stepSecond * 1000);
        }

        System.out.println("循环结束");
    }

    /**
     * 解析日志，并且存入数据库中
     */
    private void parseContentSimple(String log) {
        JSONObject jsonObject = JSON.parseObject(log);
        JSONObject rawResponse = jsonObject.getJSONObject("rawResponse");
        JSONObject hits = rawResponse.getJSONObject("hits");
        JSONArray innerHits = hits.getJSONArray("hits");

        for (Object innerHit : innerHits) {
            try {

                JSONObject findIt = (JSONObject) innerHit;
                String findItString = findIt.toString();
                // 移除转义字符
                String cleanJson = findItString.replace("\\", "");
                String regx = "\"content\":\"";

                String[] parts = cleanJson.split(regx);
                if (parts.length < 2) {
                    throw new IllegalArgumentException("content字段未找到");
                }

                //response信息
                Map<String, String> responseInfo = parseLogSimple(parts[1]);


                String remaining = parts[2];
                String[] endParts = remaining.split("\",\"resourceType\"");
                if (endParts.length < 1) {
                    throw new IllegalArgumentException("content字段结束位置未找到");
                }

                String singalString = fixWithRegex(endParts[0]);
                JSONObject treasure = JSON.parseObject(singalString);
                OrdersLog ordersLog = JSONObject.parseObject(singalString, OrdersLog.class);
                ordersLog.setRiskLevel(Integer.valueOf(responseInfo.get("riskLevel")));
                ordersLog.setCostTime(Integer.valueOf(responseInfo.get("costTime")));
                ordersLog.setRiskCode(Integer.valueOf(responseInfo.get("riskCode")));
                ordersLog.setRiskMsg(responseInfo.get("riskMsg"));
                ordersLog.setTid(responseInfo.get("tid"));
                disposalService.dealWithAllWork(ordersLog);
                ordersLogMapper.insert(ordersLog);
                System.out.println("---------------------------------------------------------change line---------------------------------------------------------------------");
                System.out.println(treasure.toString());
            } catch (Exception e) {
                System.out.println("保存数据库时出现错误" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

}
