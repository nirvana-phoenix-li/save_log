package com.li.save_order.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@TableName("orders_log")
public class OrdersLog {


    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "orderId")
    private Long orderId;

    @TableField(value = "latitude")
    private String latitude;

    @TableField(value = "orderRemark")
    private String orderRemark;

    @TableField(value = "salesChannel")
    private Integer salesChannel;

    @TableField(value = "deviceId")
    private String deviceId;

    @TableField(value = "receiverCity")
    private String receiverCity;

    @TableField(value = "platform")
    private String platform;

    @TableField(value = "receiverPhone")
    private String receiverPhone;

    @TableField(value = "osVersion")
    private String osVersion;

    @TableField(value = "deliveryMode")
    private Integer deliveryMode;

    @TableField(value = "timeSlotDate")
    private Long timeSlotDate;

    @TableField(value = "shopId")
    private String shopId;

    @TableField(value = "longitude")
    private String longitude;

    @TableField(value = "memberId")
    private String memberId;

    @TableField(value = "os")
    private String os;

    @TableField(value = "ip")
    private String ip;

    @TableField(value = "mobile")
    private String mobile;

    @TableField(value = "promotionCode")
    private String promotionCode;

    @TableField(value = "mobileType")
    private String mobileType;

    @TableField(value = "receiverArea")
    private String receiverArea;

    @TableField(value = "sessionId")
    private String sessionId;

    @TableField(value = "orderAt")
    private Date orderAt;

    @TableField(value = "version")
    private String version;

    @TableField(value = "salesBusinessType")
    private Integer salesBusinessType;

    @TableField(value = "receiverAddr")
    private String receiverAddr;

    @TableField(value = "isBalancePay")
    private Boolean isBalancePay;

    @TableField(value = "receiverName")
    private String receiverName;

    @TableField(value = "riskLevel")
    private Integer riskLevel;

    @TableField(value = "costTime")
    private Integer costTime;

    @TableField(value = "riskCode")
    private Integer riskCode;

    @TableField(value = "riskMsg")
    private String riskMsg;

    @TableField(value = "tid")
    private String tid;

    @TableField(value = "idCardInfo", typeHandler = FastjsonTypeHandler.class)
    private List<Map<String, Object>> idCardInfo;

    @TableField(value = "promotionInfoList", typeHandler = FastjsonTypeHandler.class)
    private List<Map<String, Object>> promotionInfoList;

    @TableField(value = "skuCodes", typeHandler = FastjsonTypeHandler.class)
    private List<String> skuCodes;

    @TableField(value = "skuDetailInfoList", typeHandler = FastjsonTypeHandler.class)
    private List<Map<String, Object>> skuDetailInfoList;

    @TableField(value = "orderTags", typeHandler = FastjsonTypeHandler.class)
    private List<String> orderTags;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime created_at;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at;

}


