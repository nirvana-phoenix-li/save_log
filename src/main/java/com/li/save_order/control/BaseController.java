package com.li.save_order.control;

import com.li.save_order.service.InvertedDbService;
import com.li.save_order.service.OrdersLogService;
import com.li.save_order.utils.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;


@Controller
public class BaseController {

    @Autowired
    OrdersLogService ordersLogService;

    @Autowired
    RedisService redisService;

    @Autowired
    InvertedDbService invertedDbService;


    @RequestMapping("/hello")
    @ResponseBody
    public String hello() {
        System.out.println("hello springboot!");
        try {
            redisService.set("test", "mytest");
            String test = (String) redisService.get("test");
            System.out.println(test);
            ordersLogService.executor(10000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "hello springboot!";
    }

    //将特征转为倒排索引
    @RequestMapping("/featureInvert")
    @ResponseBody
    public String featureInvert() {


        invertedDbService.executor();
        return "hello springboot!";
    }
}



