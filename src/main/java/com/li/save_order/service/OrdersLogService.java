package com.li.save_order.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.save_order.entity.InvertedDb;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.entity.SceneConfig;
import com.li.save_order.mapper.InvertedDbMapper;
import com.li.save_order.mapper.OrdersLogMapper;
import com.li.save_order.mapper.SceneConfigMapper;
import com.li.save_order.utils.GeneralLongTermCountCacheManager;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static Map<String, String> parseLogSimple(String log) {
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

        HashMap<String, Set<String>> hashMap = new HashMap<>();
        //实际查询时间是 8个小时之后，有时差
        int stepSecond = 30;
        LocalDateTime originalStart = LocalDateTime.of(2025, 10, 21, 6, 25, 0, 0);
        LocalDateTime originalEnd = originalStart.plusSeconds(stepSecond);

        int requestCount = 0;
        for (int k = 0; k < number; k++) {

            HashMap<String, String> stringHashMap = new HashMap<>();
            stringHashMap.put("进入风控V2.0", "");
            stringHashMap.put("结束", "and");
            stringHashMap.put("order", "");
            stringHashMap.put("福州", "");

            String response = null;
            int redo = 0;
            while (response == null || response.startsWith("{\"id\"")) {
                redo++;
                response = callElasticSearchAPI(originalStart.toString(), originalEnd.toString(), stringHashMap);
            }
            System.out.println("当前为第" + k + "次，重试次数为: " + (redo - 1));
            System.out.println("API响应长度: " + response.length());

            parseContentSimple(response);

            originalStart = originalStart.plusSeconds(stepSecond);
            originalEnd = originalEnd.plusSeconds(stepSecond);

            Thread.sleep(30000);
        }

        System.out.println("找到 " + requestCount + " 条匹配记录");
        System.out.println("解析到 " + hashMap.size() + " 个不同的esKey");

        System.out.println("Excel文件已生成: ");
    }

    /**
     * 更简单的方法：使用字符串分割
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

                String sceneCode = "sameReceiverAreaAndSkuCode";
                List<String> skuCodes = ordersLog.getSkuCodes();

                //处理收货小地址倒排索引
                ArrayList<InvertedDb> invertedDbs = new ArrayList<>();
                for (int i = 0; i < ordersLog.getReceiverAddr().length(); i++) {
                    char c = ordersLog.getReceiverAddr().charAt(i);
                    InvertedDb invertedDb = new InvertedDb();
                    invertedDb.setOrderId(ordersLog.getOrderId());
                    invertedDb.setKeyword(String.valueOf(c));
                    invertedDb.setReceiverArea(ordersLog.getReceiverArea());
                    invertedDbs.add(invertedDb);
                }
                invertedDbMapper.insert(invertedDbs);

                //判断同收货大地址同skucode的逻辑
                if (Integer.valueOf(responseInfo.get("riskLevel")) != 2) {
                    for (String skuCode : skuCodes) {
                        if (!skuCode.equals("null")) {
                            String keyName = ordersLog.getReceiverArea() + "_" + skuCode;
                            long currentValue = countCacheManager.saveIndicatorCount(keyName, String.valueOf(ordersLog.getOrderId()), sceneCode);
                            postHandler(sceneCode, currentValue, ordersLog, skuCode);
                        }
                    }
                }

                //判断同收货小地址是否包含手机号的逻辑
                String phoneNumber = findPhoneNumber(ordersLog.getReceiverAddr());
                if (!phoneNumber.isEmpty() && !phoneNumber.equals(ordersLog.getMobile())) {
                    handlerReceiveAddrContainsPhone(ordersLog);
                }

                ordersLog.setRiskLevel(Integer.valueOf(responseInfo.get("riskLevel")));
                ordersLog.setCostTime(Integer.valueOf(responseInfo.get("costTime")));
                ordersLog.setRiskCode(Integer.valueOf(responseInfo.get("riskCode")));
                ordersLog.setRiskMsg(responseInfo.get("riskMsg"));
                ordersLog.setTid(responseInfo.get("tid"));

                int insert = ordersLogMapper.insert(ordersLog);

                System.out.println("---------------------------------------------------------change line---------------------------------------------------------------------");
                System.out.println(treasure.toString());
            } catch (Exception e) {
                System.out.println("保存数据库时出现错误" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    //处置模块
    private void postHandler(String sceneCode, Long currentValue, OrdersLog ordersLog, String skuCode) {
        LambdaQueryWrapper<SceneConfig> queryWrapper = new LambdaQueryWrapper<SceneConfig>().eq(SceneConfig::getSceneCode, sceneCode);
        SceneConfig sceneConfig = sceneConfigMapper.selectOne(queryWrapper);
        if (currentValue >= sceneConfig.getAlertValue()) {
            String alertString = ordersLog.getReceiverArea() + "     区域下的skuCode:"
                    + skuCode + "数量" + currentValue + "已经超出阈值" + sceneConfig.getAlertValue()
                    + "了，订单id为" + ordersLog.getOrderId();
            try {
                sendMessage(alertString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //处理
    private void handlerReceiveAddrContainsPhone(OrdersLog ordersLog) {
        String alertString = "收货大地址:" + ordersLog.getReceiverArea() +
                "收货小地址:" + ordersLog.getReceiverAddr() + "包含手机号!"
                + "且与注册手机号不一致，订单id为" + ordersLog.getOrderId();
        try {
            sendMessage(alertString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String sendMessage(String alertString) throws Exception {
        String json = "{\"msgtype\": \"text\", \"text\": {\"content\": \"" + alertString + "\"}}";

        String webhookUrl = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=a38e7eb1-2131-429a-8de2-99b646816e0f";
        RequestBody bodyContent = RequestBody.create(
                MediaType.parse("application/json"),
                json
        );
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(bodyContent)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public String findPhoneNumber(String paramString) {
        String mobile = "";
        Pattern pattern = Pattern.compile("(?<!\\d)(?:(?:1[3456789]\\d{9})|(?:861[3456789]\\d{9}))(?!\\d)");
        Matcher matcher = pattern.matcher(paramString.replaceAll("\\s*", ""));
        while (matcher.find()) {
            mobile = matcher.group();
            mobile = mobile.startsWith("86") ? mobile.substring(2) : mobile;
        }
        return mobile;
    }


    /**
     * 调用ElasticSearch API的方法
     * 对应新的curl命令的Java实现
     */
    public String callElasticSearchAPI(String startTime, String endTime, HashMap<String, String> inputHashMap) throws IOException {
        // 请求URL
        String url = "https://es-elk-kibana2.yonghuivip.com/s/cp-hcfk/internal/search/ese";

        // 构建请求体JSON
        JSONObject requestBody = new JSONObject();
        JSONObject params = new JSONObject();
        JSONObject body = new JSONObject();

        // 设置基本参数
        params.put("index", "*:logstash*cp-hcfk_risk-center_2*");

        // 设置body内容
        body.put("version", true);
        body.put("size", 500); // 改为500，与curl一致

        // 设置排序
        JSONObject sortObj = new JSONObject();
        JSONObject timestampSort = new JSONObject();
        timestampSort.put("order", "desc");
        timestampSort.put("unmapped_type", "boolean");
        sortObj.put("@timestamp", timestampSort);
        body.put("sort", new Object[]{sortObj});

        // 设置聚合
        JSONObject aggs = new JSONObject();
        JSONObject agg2 = new JSONObject();
        JSONObject dateHistogram = new JSONObject();
        dateHistogram.put("field", "@timestamp");
        dateHistogram.put("fixed_interval", "300m"); // 改为30m，与curl一致
        dateHistogram.put("time_zone", "Asia/Shanghai");
        dateHistogram.put("min_doc_count", 1);
        agg2.put("date_histogram", dateHistogram);
        aggs.put("2", agg2);
        body.put("aggs", aggs);

        // 设置字段
        body.put("stored_fields", new String[]{"*"});
        body.put("script_fields", new JSONObject());

        JSONObject docvalueField = new JSONObject();
        docvalueField.put("field", "@timestamp");
        docvalueField.put("format", "date_time");
        body.put("docvalue_fields", new Object[]{docvalueField});

        JSONObject source = new JSONObject();
        source.put("excludes", new String[]{});
        body.put("_source", source);

        // 设置查询
        JSONObject query = new JSONObject();
        JSONObject boolQuery = new JSONObject();
        boolQuery.put("must", new Object[]{});

        // 构建filter数组
        JSONObject boolFilter = new JSONObject();
        JSONObject virtualhead = boolFilter;


        List<String> collect = inputHashMap.keySet().stream().collect(Collectors.toList());
        for (int i = 0; i < collect.size(); i++) {
            JSONArray boolFilterArray = new JSONArray();
            JSONObject multiMatch = new JSONObject();
            multiMatch.put("type", "phrase");
            multiMatch.put("query", collect.get(i));
            multiMatch.put("lenient", true);

            JSONObject wrapperJson = new JSONObject();
            wrapperJson.put("multi_match", multiMatch);
            boolFilterArray.add(wrapperJson);


            if (i != inputHashMap.size() - 2) {
                JSONObject second = new JSONObject();
                JSONObject inner = new JSONObject();

                second.put("bool", inner);
                boolFilterArray.add(second);
                virtualhead.put("filter", boolFilterArray);
                virtualhead = inner;
            } else {
                JSONObject lastJson = new JSONObject();
                lastJson.put("type", "phrase");
                i++;
                lastJson.put("query", collect.get(i));
                lastJson.put("lenient", true);

                JSONObject wrapper = new JSONObject();
                wrapper.put("multi_match", lastJson);
                boolFilterArray.add(wrapper);

                virtualhead.put("filter", boolFilterArray);
            }
        }


        JSONObject rangeQuery = new JSONObject();
        JSONObject timestampRange = new JSONObject();
        // 修复时间格式，确保包含秒和毫秒
        String gte = startTime.length() == 16 ? startTime + ":00.000Z" : startTime + ".000Z";
        String lte = endTime.length() == 16 ? endTime + ":00.000Z" : endTime + ".000Z";
        timestampRange.put("gte", gte);
        timestampRange.put("lte", lte);
        timestampRange.put("format", "strict_date_optional_time");
        rangeQuery.put("@timestamp", timestampRange);

        JSONObject rangeWrapper = new JSONObject();
        rangeWrapper.put("range", rangeQuery);
        JSONObject firstBool = new JSONObject();
        firstBool.put("bool", boolFilter);
        boolQuery.put("filter", new Object[]{firstBool, rangeWrapper});
        boolQuery.put("should", new Object[]{});
        boolQuery.put("must_not", new Object[]{});
        query.put("bool", boolQuery);
        body.put("query", query);

        // 设置高亮
        JSONObject highlight = new JSONObject();
        highlight.put("pre_tags", new String[]{"@kibana-highlighted-field@"});
        highlight.put("post_tags", new String[]{"@/kibana-highlighted-field@"});
        highlight.put("fields", new JSONObject().put("*", new JSONObject()));
        highlight.put("fragment_size", 2147483647);
        body.put("highlight", highlight);

        params.put("body", body);
        params.put("preference", 1754986734687L); // 使用curl中的preference值
        requestBody.put("params", params);

        // 调试：打印生成的JSON
//        System.out.println("生成的请求JSON:");
//        System.out.println(requestBody.toJSONString());

        // 创建OkHttp客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 创建请求体
        RequestBody bodyContent = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toJSONString()
        );

        // 创建HTTP请求 - 添加curl中的所有重要请求头
        Request request = new Request.Builder()
                .url(url)
                .header("accept", "*/*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("content-type", "applicat  ion/json")
                .header("kbn-version", "7.10.1")
                .header("origin", "https://es-elk-kibana2.yonghuivip.com")
                .header("priority", "u=1, i")
                .header("referer", "https://es-elk-kibana2.yonghuivip.com/s/cp-hcfk/app/discover")
                .header("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .header("Cookie", "sensorsdata2015jssdkchannel=%7B%22prop%22%3A%7B%22_sa_channel_landing_url%22%3A%22%22%7D%7D; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%22197345925498b3-0d9f5bef6cca8d8-19525636-2073600-1973459254a4bf%22%2C%22first_id%22%3A%22%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24latest_referrer%22%3A%22%22%7D%2C%22identities%22%3A%22eyIkaWRlbnRpdHlfY29va2llX2lkIjoiMTk3MzQ1OTI1NDk4YjMtMGQ5ZjViZWY2Y2NhOGQ4LTE5NTI1NjM2LTIwNzM2MDAtMTk3MzQ1OTI1NGE0YmYifQ%3D%3D%22%2C%22history_login_id%22%3A%7B%22name%22%3A%22%22%2C%22value%22%3A%22%22%7D%2C%22%24device_id%22%3A%221904e4e47f9637-0dec8b9fa3daa3-19525637-1764000-1904e4e47fa725%22%7D")
                .post(bodyContent)
                .build();

        // 发送请求并获取响应
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        if (!response.isSuccessful()) {
            System.err.println("HTTP错误: " + response.code() + " " + response.message());
            System.err.println("响应体: " + responseBody);
            throw new IOException("Unexpected response code: " + response.code() + " " + response.message());
        }

        return responseBody;
    }


}
