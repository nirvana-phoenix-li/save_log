package com.li.save_order.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//send https curl and set param data
@Service
public class NetWorkService {

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
