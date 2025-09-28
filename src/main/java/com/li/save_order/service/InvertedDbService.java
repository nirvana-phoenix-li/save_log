package com.li.save_order.service;

import com.li.save_order.entity.InvertedDb;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.mapper.InvertedDbMapper;
import com.li.save_order.utils.GeneralLongTermCountCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InvertedDbService {
    @Autowired
    InvertedDbMapper invertedDbMapper;

    @Autowired
    GeneralLongTermCountCacheManager countCacheManager;

    public void executor() {



        List<OrdersLog> ordersLogs = new ArrayList<>();


       


        for (OrdersLog ordersLog : ordersLogs) {
            String receiverArea = ordersLog.getReceiverArea();
            String receiverAddr = ordersLog.getReceiverAddr();
            for (int i = 0; i < receiverAddr.length(); i++) {
                char c = receiverAddr.charAt(i);
                InvertedDb invertedDb = new InvertedDb();
                invertedDb.setOrderId(ordersLog.getOrderId());
                invertedDb.setKeyword(String.valueOf(c));
                invertedDb.setReceiverArea(receiverArea);
                invertedDbMapper.insert(invertedDb);
            }
        }
    }

}
