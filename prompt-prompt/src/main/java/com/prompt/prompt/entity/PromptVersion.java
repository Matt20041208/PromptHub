package com.prompt.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.prompt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prompt_version")
public class PromptVersion extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private String versionNo;
    private String content;
    private String changelog;
}
