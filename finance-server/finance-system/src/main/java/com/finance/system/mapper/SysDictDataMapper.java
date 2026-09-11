package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysDictData;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典数据 Mapper（S3）。
 *
 * <p>删除走物理删（同 dict_type 下 dict_value 唯一，逻辑删残留会阻止同名重建）。</p>
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    @Delete("DELETE FROM sys_dict_data WHERE id = #{id}")
    int physicallyDeleteById(Long id);

    @Delete("DELETE FROM sys_dict_data WHERE dict_type = #{dictType}")
    int physicallyDeleteByType(String dictType);
}
