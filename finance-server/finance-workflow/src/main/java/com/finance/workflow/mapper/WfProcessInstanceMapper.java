package com.finance.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.workflow.domain.WfProcessInstance;
import org.apache.ibatis.annotations.Mapper;

/** 流程实例关联表 Mapper（W1）。 */
@Mapper
public interface WfProcessInstanceMapper extends BaseMapper<WfProcessInstance> {
}
