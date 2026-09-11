package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysDictType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型 Mapper（S3）。
 *
 * <p>删除走物理删（dict_type 有 DB 唯一约束 uq_sys_dict_type，逻辑删残留会阻止同名重建；
 * 字典为配置数据，无审计追溯需求）。</p>
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    @Delete("DELETE FROM sys_dict_type WHERE id = #{id}")
    int physicallyDeleteById(Long id);
}
