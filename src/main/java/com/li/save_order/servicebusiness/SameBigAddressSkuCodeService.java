package com.li.save_order.servicebusiness;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.save_order.entity.OrdersLog;
import com.li.save_order.entity.SceneConfig;
import com.li.save_order.mapper.SceneConfigMapper;
import com.li.save_order.service.QiweiAlertService;
import com.li.save_order.utils.GeneralLongTermCountCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SameBigAddressSkuCodeService {
    private static String sceneCode = "sameReceiverAreaAndSkuCode";
    @Autowired
    GeneralLongTermCountCacheManager countCacheManager;
    @Autowired
    SceneConfigMapper sceneConfigMapper;
    @Autowired
    QiweiAlertService qiweiAlertService;

    //判断同收货大地址同skucode的逻辑
    public void sendSameBigAddressSkuCodeAlert(OrdersLog ordersLog) {
        for (String skuCode : ordersLog.getSkuCodes()) {
            if (!skuCode.equals("null")) {
                String keyName = ordersLog.getReceiverArea() + "_" + skuCode;
                long currentValue = countCacheManager.saveIndicatorCount(keyName, String.valueOf(ordersLog.getOrderId()), sceneCode);
                postHandler(sceneCode, currentValue, ordersLog, skuCode);
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
                qiweiAlertService.sendMessage(alertString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
