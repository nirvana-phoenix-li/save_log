package com.li.save_order.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_trace")
public class MemberTrace {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "mobile")
    private String mobile;

    @TableField(value = "memberId")
    private String memberId;

    @TableField(value = "requestTime")
    private LocalDateTime requestTime;

    @TableField(value = "strategyId")
    private String strategyId;
}