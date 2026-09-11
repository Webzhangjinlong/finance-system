package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysDictData;
import com.finance.system.domain.SysDictType;
import com.finance.system.dto.DictDataDTO;
import com.finance.system.dto.DictTypeDTO;
import com.finance.system.mapper.SysDictDataMapper;
import com.finance.system.mapper.SysDictTypeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 字典管理服务（S3）：两级字典（类型 + 数据）+ Redis 缓存失效。
 *
 * <p>约束：dict_type 全局唯一（逻辑删行查重）；同 dict_type 下 dict_value 唯一；
 * 删除类型级联逻辑删其数据；类型/数据变更后失效对应 Redis 缓存（key=dict:data:{dictType}），
 * 缓存读仅返回 ACTIVE 数据（label/value/sort），管理端列表走 DB。</p>
 */
@Service
public class SysDictService {

    private static final String CACHE_PREFIX = "dict:data:";

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SysDictService(SysDictTypeMapper typeMapper, SysDictDataMapper dataMapper,
                          StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.typeMapper = typeMapper;
        this.dataMapper = dataMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== 字典类型 ====================

    public PageResult<SysDictType> pageTypes(long page, long size, String dictName, String dictType, String status) {
        LambdaQueryWrapper<SysDictType> qw = new LambdaQueryWrapper<SysDictType>()
                .like(StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
                .like(StringUtils.hasText(dictType), SysDictType::getDictType, dictType)
                .eq(StringUtils.hasText(status), SysDictType::getStatus, status)
                .orderByAsc(SysDictType::getId);
        Page<SysDictType> p = typeMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public List<SysDictType> listAll() {
        return typeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, SysDictType.STATUS_ACTIVE)
                .orderByAsc(SysDictType::getId));
    }

    @Transactional
    public Long createType(DictTypeDTO dto) {
        checkTypeUnique(dto.getDictType(), null);
        SysDictType t = new SysDictType();
        t.setDictName(dto.getDictName());
        t.setDictType(dto.getDictType());
        t.setStatus(dto.getStatus());
        t.setRemark(dto.getRemark());
        typeMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void updateType(DictTypeDTO dto) {
        SysDictType exist = typeMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("字典类型不存在");
        }
        checkTypeUnique(dto.getDictType(), dto.getId());
        SysDictType t = new SysDictType();
        t.setId(dto.getId());
        t.setDictName(dto.getDictName());
        t.setDictType(dto.getDictType());
        t.setStatus(dto.getStatus());
        t.setRemark(dto.getRemark());
        typeMapper.updateById(t);
        evictCache(dto.getDictType());
        evictCache(exist.getDictType());
    }

    @Transactional
    public void deleteType(Long id) {
        SysDictType exist = typeMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("字典类型不存在");
        }
        // 物理删（dict_type 唯一约束 + 配置数据语义，可复用同名重建）
        typeMapper.physicallyDeleteById(id);
        dataMapper.physicallyDeleteByType(exist.getDictType());
        evictCache(exist.getDictType());
    }

    private void checkTypeUnique(String dictType, Long excludeId) {
        Long c = typeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId));
        if (c != null && c > 0) {
            throw new BusinessException("字典类型 " + dictType + " 已存在");
        }
    }

    // ==================== 字典数据 ====================

    public List<SysDictData> listData(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return Collections.emptyList();
        }
        return dataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictSort)
                .orderByAsc(SysDictData::getId));
    }

    @Transactional
    public Long createData(DictDataDTO dto) {
        checkTypeExists(dto.getDictType());
        checkDataUnique(dto.getDictType(), dto.getDictValue(), null);
        SysDictData d = new SysDictData();
        d.setDictType(dto.getDictType());
        d.setDictLabel(dto.getDictLabel());
        d.setDictValue(dto.getDictValue());
        d.setDictSort(dto.getDictSort());
        d.setStatus(dto.getStatus());
        dataMapper.insert(d);
        evictCache(dto.getDictType());
        return d.getId();
    }

    @Transactional
    public void updateData(DictDataDTO dto) {
        SysDictData exist = dataMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("字典数据不存在");
        }
        checkDataUnique(dto.getDictType(), dto.getDictValue(), dto.getId());
        SysDictData d = new SysDictData();
        d.setId(dto.getId());
        d.setDictType(dto.getDictType());
        d.setDictLabel(dto.getDictLabel());
        d.setDictValue(dto.getDictValue());
        d.setDictSort(dto.getDictSort());
        d.setStatus(dto.getStatus());
        dataMapper.updateById(d);
        evictCache(dto.getDictType());
        evictCache(exist.getDictType());
    }

    @Transactional
    public void deleteData(Long id) {
        SysDictData exist = dataMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("字典数据不存在");
        }
        dataMapper.physicallyDeleteById(id);
        evictCache(exist.getDictType());
    }

    private void checkTypeExists(String dictType) {
        Long c = typeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType));
        if (c == null || c == 0) {
            throw new BusinessException("字典类型 " + dictType + " 不存在");
        }
    }

    private void checkDataUnique(String dictType, String dictValue, Long excludeId) {
        Long c = dataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue)
                .ne(excludeId != null, SysDictData::getId, excludeId));
        if (c != null && c > 0) {
            throw new BusinessException("字典 " + dictType + " 下键值 " + dictValue + " 已存在");
        }
    }

    // ==================== Redis 缓存 ====================

    /**
     * 按字典类型读取启用数据（label/value/sort），Redis 缓存优先（key=dict:data:{dictType}）。
     * 供前端下拉/其他模块使用；管理端列表走 DB 不走缓存。
     */
    public List<DictCacheItem> getDataByType(String dictType) {
        String key = CACHE_PREFIX + dictType;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<DictCacheItem>>() {
                });
            }
        } catch (Exception e) {
            // 缓存反序列化失败则回源 DB（缓存可重建，不阻断）
        }
        List<DictCacheItem> items = dataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, dictType)
                        .eq(SysDictData::getStatus, SysDictData.STATUS_ACTIVE)
                        .orderByAsc(SysDictData::getDictSort)
                        .orderByAsc(SysDictData::getId))
                .stream().map(d -> {
                    DictCacheItem it = new DictCacheItem();
                    it.setLabel(d.getDictLabel());
                    it.setValue(d.getDictValue());
                    it.setSort(d.getDictSort());
                    return it;
                }).toList();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(items));
        } catch (Exception e) {
            // 缓存写失败不影响功能
        }
        return items;
    }

    private void evictCache(String dictType) {
        if (StringUtils.hasText(dictType)) {
            try {
                redisTemplate.delete(CACHE_PREFIX + dictType);
            } catch (Exception e) {
                // 缓存删除失败不影响功能（下次读会回源重建）
            }
        }
    }

    /** 缓存条目（仅 label/value/sort，规避雪花 ID Long 序列化精度问题）。 */
    public static class DictCacheItem {
        private String label;
        private String value;
        private Integer sort;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }
}
