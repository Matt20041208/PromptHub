package com.prompt.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("prompt_tag_rel")
public class PromptTagRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private Long tagId;
}
