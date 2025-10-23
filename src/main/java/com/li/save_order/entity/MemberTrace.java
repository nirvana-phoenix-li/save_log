package com.li.save_order.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_trace")
public class MemberTrace {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String mobile;

    private String memberId;

    private LocalDateTime requestTime;

    private String strategyId;
}