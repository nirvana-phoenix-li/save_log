import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.li.save_order.entity.OrdersLog;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class T {
    public static void main(String[] args) {
        String originalJsonString = "{\"deliveryMode\":8,\"deviceId\":\"3775fc21-aa0a-44c6-8c95-f71943264418\",\"idCardInfo\":\"{\"cardNo\":\"513722198810013846\",\"name\":\"罗敏\"}\",\"ip\":\"106.89.31.135\",\"isBalancePay\":false,\"latitude\":\"29.80807703505751\",\"longitude\":\"106.41277661189838\",\"memberId\":\"995912973736398302\",\"mobile\":\"18983185863\",\"mobileType\":\"BLK-AL80\",\"orderAt\":\"2025-09-10 09:24:04\",\"orderId\":1208761500030009,\"orderRemark\":\"\",\"orderTags\":[],\"os\":\"android\",\"osVersion\":\"android31\",\"platform\":\"android\",\"promotionCode\":\"\",\"promotionInfoList\":[],\"receiverAddr\":\"一单元12-4\",\"receiverArea\":\"红雨花园\",\"receiverCity\":\"重庆\",\"receiverName\":\"罗|F\",\"receiverPhone\":\"15826165115\",\"salesBusinessType\":1,\"salesChannel\":201,\"sessionId\":\"ded55484-2808-4038-b080-292bb5933c01\",\"shopId\":\"WTT003\",\"skuCodes\":[null,null,null],\"skuDetailInfoList\":[{\"bomType\":2,\"categoryCode\":\"13270306\",\"exclusiveForNewcomers\":false,\"expiration\":1,\"goodsCount\":1,\"goodsFlag\":\"normal\",\"skuCodeWithPrefix\":\"T-CB217039381-G\"},{\"bomType\":2,\"categoryCode\":\"13270301\",\"exclusiveForNewcomers\":false,\"expiration\":1,\"goodsCount\":1,\"goodsFlag\":\"normal\",\"skuCodeWithPrefix\":\"T-CB208062958-G\"},{\"bomType\":2,\"categoryCode\":\"13270304\",\"exclusiveForNewcomers\":false,\"expiration\":1,\"goodsCount\":1,\"goodsFlag\":\"normal\",\"skuCodeWithPrefix\":\"T-CB208062010-G\"}],\"version\":\"11.9.1.0\"}";
        String s = fixWithRegex(originalJsonString);

        JSONObject treasure = JSON.parseObject(s);
        OrdersLog ordersLog = JSONObject.parseObject(s, OrdersLog.class);
        System.out.println();
    }

    private static String fixWithRegex(String jsonString) {
        int firstIndex = jsonString.indexOf("idCardInfo");
        if (firstIndex == -1) {return jsonString;}
        String firstString = jsonString.substring(0, firstIndex+12);
        String substring = jsonString.substring(firstIndex+13);

        int index = substring.indexOf("}");
        String secondString = substring.substring(0, index+1);
        String thirdString = substring.substring(index+2);
        return firstString + secondString + thirdString;
    }
}
