package com.li.save_order.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.mapstruct.OrdersLogStruct;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ParseLogStructService {

    @Autowired
    OrdersLogStruct ordersLogStruct;


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

    public List<OrdersLog> parseContentSimple(String log) {
        JSONObject jsonObject = JSON.parseObject(log);
        JSONObject rawResponse = jsonObject.getJSONObject("rawResponse");
        JSONObject hits = rawResponse.getJSONObject("hits");
        JSONArray innerHits = hits.getJSONArray("hits");


        ArrayList<OrdersLog> ordersLogs = new ArrayList<>();
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
//                String[] endParts = remaining.split("\",\"resourceType\"");
                String[] endParts = remaining.split("}\",\"");
                if (endParts.length < 1) {
                    throw new IllegalArgumentException("content字段结束位置未找到");
                }

                String singalString = fixWithRegex(endParts[0] + "}");
                JSONObject treasure = JSON.parseObject(singalString);
                OrdersLog ordersLog = JSONObject.parseObject(singalString, OrdersLog.class);

                //设置content之前的内容
                String[] contentBefore = parts[1].split("\\{");
                String beforeString = contentBefore[contentBefore.length - 1];
                if (beforeString != null && beforeString.endsWith("\",")) {
                    beforeString = "{" + beforeString.substring(0, beforeString.length() - 1) + "}";
                }
                OrdersLog beforeTreasure = JSONObject.parseObject(beforeString, OrdersLog.class);
                ordersLogStruct.updateUserFromDto(beforeTreasure, ordersLog);

                //设置content之后的内容
                String[] contentLater = endParts[1].split("}");
                String laterString = contentLater[0];
                laterString = "{\"" + laterString + "}";
                OrdersLog laterTreasure = JSONObject.parseObject(laterString, OrdersLog.class);
                ordersLogStruct.updateUserFromDto(laterTreasure, ordersLog);

                //设置请求时间戳
                String[] timeSplit = endParts[1].split("\"datetime\":\"");
                String datetimeString = timeSplit[1].split("\"")[0];
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
                LocalDateTime dateTime = LocalDateTime.parse(datetimeString, formatter);
                ordersLog.setDateTime(dateTime);


                ordersLog.setRiskLevel(Integer.valueOf(responseInfo.get("riskLevel")));
                ordersLog.setCostTime(Integer.valueOf(responseInfo.get("costTime")));
                ordersLog.setRiskCode(Integer.valueOf(responseInfo.get("riskCode")));
                ordersLog.setRiskMsg(responseInfo.get("riskMsg"));
                ordersLog.setTid(responseInfo.get("tid"));

                ordersLogs.add(ordersLog);
                System.out.println("---------------------------------------------------------change line---------------------------------------------------------------------");
                System.out.println(treasure.toString());
            } catch (Exception e) {
                System.out.println("保存数据库时出现错误" + Arrays.toString(e.getStackTrace()));
            }
        }
        return ordersLogs;
    }
}
