package com.li.save_order.servicebusiness;

import com.li.save_order.entity.OrdersLog;
import com.li.save_order.service.QiweiAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SameSmallAddressContainPhoneService {
    @Autowired
    QiweiAlertService qiweiAlertService;

    //判断同收货小地址是否包含手机号的逻辑
    public void sendSameBigAddressSkuCodeAlert(OrdersLog ordersLog) {
        String phoneNumber = findPhoneNumber(ordersLog.getReceiverAddr());
        if (!phoneNumber.isEmpty() && !phoneNumber.equals(ordersLog.getMobile())) {
            handlerReceiveAddrContainsPhone(ordersLog);
        }
    }

    private String findPhoneNumber(String paramString) {
        String mobile = "";
        Pattern pattern = Pattern.compile("(?<!\\d)(?:(?:1[3456789]\\d{9})|(?:861[3456789]\\d{9}))(?!\\d)");
        Matcher matcher = pattern.matcher(paramString.replaceAll("\\s*", ""));
        while (matcher.find()) {
            mobile = matcher.group();
            mobile = mobile.startsWith("86") ? mobile.substring(2) : mobile;
        }
        return mobile;
    }

    //处理
    private void handlerReceiveAddrContainsPhone(OrdersLog ordersLog) {
        String alertString = "收货大地址:" + ordersLog.getReceiverArea() +
                "收货小地址:" + ordersLog.getReceiverAddr() + "包含手机号!"
                + "且与注册手机号不一致，订单id为" + ordersLog.getOrderId();
        try {
            qiweiAlertService.sendMessage(alertString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
