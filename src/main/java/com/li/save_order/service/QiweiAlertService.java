package com.li.save_order.service;

import okhttp3.*;
import org.springframework.stereotype.Service;

@Service
public class QiweiAlertService {

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
}
