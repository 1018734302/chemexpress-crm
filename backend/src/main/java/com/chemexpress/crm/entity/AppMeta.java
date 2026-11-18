package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("app_meta")
public class AppMeta {
    @TableId
    private String metaKey;
    private String metaValue;
}
