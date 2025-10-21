package com.li.save_order.servicebusiness;

import com.li.save_order.entity.InvertedDb;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.mapper.InvertedDbMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class InvertedIndexService {
    @Autowired
    InvertedDbMapper invertedDbMapper;

    public void saveInvertedIndex(OrdersLog ordersLog) {
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
    }
}
