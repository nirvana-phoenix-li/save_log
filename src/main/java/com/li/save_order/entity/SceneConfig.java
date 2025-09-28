package com.li.save_order.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("scene_config")
public class SceneConfig {


    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "sceneCode")
    private String sceneCode;

    @TableField(value = "alertValue")
    private Integer alertValue;

}


