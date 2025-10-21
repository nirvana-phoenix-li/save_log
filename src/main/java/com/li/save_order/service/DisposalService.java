package com.li.save_order.service;

import com.li.save_order.entity.OrdersLog;
import com.li.save_order.servicebusiness.InvertedIndexService;
import com.li.save_order.servicebusiness.SameBigAddressSkuCodeService;
import com.li.save_order.servicebusiness.SameSmallAddressContainPhoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//attain the original data
//all work will manage in this service
@Service
public class DisposalService {
    @Autowired
    InvertedIndexService invertedIndexService;
    @Autowired
    SameBigAddressSkuCodeService sameBigAddressSkuCodeService;
    @Autowired
    SameSmallAddressContainPhoneService sameSmallAddressContainPhoneService;


    public void dealWithAllWork(OrdersLog ordersLog) {
        invertedIndexService.saveInvertedIndex(ordersLog);
        if (ordersLog.getRiskLevel() != 2) {
            sameBigAddressSkuCodeService.sendSameBigAddressSkuCodeAlert(ordersLog);
            sameSmallAddressContainPhoneService.sendSameBigAddressSkuCodeAlert(ordersLog);
        }
    }
}
