package com.prompt.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.trade.entity.TransactionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionLogMapper extends BaseMapper<TransactionLog> {
}
