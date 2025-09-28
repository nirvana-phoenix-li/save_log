package com.li.save_order.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("inverted_db")
public class InvertedDb {


    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "orderId")
    private Long orderId;

    @TableField(value = "keyword")
    private String keyword;


    @TableField(value = "receiverArea")
    private String receiverArea;

}


