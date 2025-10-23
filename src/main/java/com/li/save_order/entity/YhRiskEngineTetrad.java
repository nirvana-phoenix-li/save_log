package com.li.save_order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("yh_risk_engine_tetrad")
public class YhRiskEngineTetrad {

    @TableId(value = "model_id", type = IdType.AUTO)
    private Integer modelId;
    @TableField(value = "strategy_id")
    private Integer strategyId;

    @TableField(value = "commercial_id")
    private String commercialId;
    @TableField(value = "scene_code")
    private String sceneCode;
    @TableField(value = "event_code")
    private String eventCode;
    @TableField(value = "business_type")
    private String businessType;
}
