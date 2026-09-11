package com.finance.system;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysDictData;
import com.finance.system.domain.SysDictType;
import com.finance.system.dto.DictDataDTO;
import com.finance.system.dto.DictTypeDTO;
import com.finance.system.service.SysDictService;
import com.finance.system.service.SysDictService.DictCacheItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 字典管理测试（S3）：两级 CRUD + 唯一校验（类型 dict_type / 数据 dict_value）+ Redis 缓存读写与失效。
 *
 * <p>使用测试专用前缀 sys_test_* 避免与 V2 种子（sys_user_gender/sys_normal_disable）冲突；
 * AfterEach 清理本测试写入的类型/数据与缓存 key。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SysDictServiceTest {

    private static final String TYPE = "sys_test_gender";
    private static final String CACHE_KEY = "dict:data:" + TYPE;

    @Autowired
    private SysDictService dictService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long typeId;

    @BeforeEach
    void clean() {
        // 物理清残留（防上次失败残留 + 逻辑删与 uq_sys_dict_type 冲突的历史行）
        jdbcTemplate.execute("DELETE FROM sys_dict_data WHERE dict_type = '" + TYPE + "'");
        jdbcTemplate.execute("DELETE FROM sys_dict_type WHERE dict_type = '" + TYPE + "'");
        redisTemplate.delete(CACHE_KEY);
    }

    @AfterEach
    void cleanAfter() {
        if (typeId != null) {
            try {
                dictService.deleteType(typeId);
            } catch (Exception ignored) {
                // 类型可能已被删除
            }
        }
        redisTemplate.delete(CACHE_KEY);
    }

    private Long createType(String dictType) {
        DictTypeDTO dto = new DictTypeDTO();
        dto.setDictName("测试字典");
        dto.setDictType(dictType);
        dto.setStatus(SysDictType.STATUS_ACTIVE);
        dto.setRemark("S3 测试");
        return dictService.createType(dto);
    }

    private Long createData(String dictType, String label, String value, int sort) {
        DictDataDTO dto = new DictDataDTO();
        dto.setDictType(dictType);
        dto.setDictLabel(label);
        dto.setDictValue(value);
        dto.setDictSort(sort);
        dto.setStatus(SysDictData.STATUS_ACTIVE);
        return dictService.createData(dto);
    }

    @Test
    void typeCreate_uniqueDictTypeRejected() {
        typeId = createType(TYPE);
        DictTypeDTO dup = new DictTypeDTO();
        dup.setDictName("重复类型");
        dup.setDictType(TYPE);
        dup.setStatus(SysDictType.STATUS_ACTIVE);
        assertThatThrownBy(() -> dictService.createType(dup))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void typePage_andList() {
        typeId = createType(TYPE);
        PageResult<SysDictType> page = dictService.pageTypes(1, 10, "测试字典", TYPE, null);
        assertThat(page.getRecords()).anySatisfy(t -> {
            assertThat(t.getDictType()).isEqualTo(TYPE);
        });
        assertThat(page.getTotal()).isGreaterThan(0);
        // 全量列表（ACTIVE）含新类型
        assertThat(dictService.listAll()).anySatisfy(t ->
                assertThat(t.getDictType()).isEqualTo(TYPE));
    }

    @Test
    void dataCreate_sameValueUnderTypeRejected() {
        typeId = createType(TYPE);
        createData(TYPE, "男", "M", 1);
        assertThatThrownBy(() -> createData(TYPE, "男2", "M", 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void dataCrud_listSortedBySort() {
        typeId = createType(TYPE);
        Long second = createData(TYPE, "女", "F", 2);
        Long first = createData(TYPE, "男", "M", 1);

        List<SysDictData> data = dictService.listData(TYPE);
        assertThat(data).hasSize(2);
        assertThat(data.get(0).getId()).isEqualTo(first);
        assertThat(data.get(1).getId()).isEqualTo(second);
        assertThat(data.get(0).getDictValue()).isEqualTo("M");

        // 更新
        DictDataDTO upd = new DictDataDTO();
        upd.setId(second);
        upd.setDictType(TYPE);
        upd.setDictLabel("女2");
        upd.setDictValue("F");
        upd.setDictSort(9);
        upd.setStatus(SysDictData.STATUS_ACTIVE);
        dictService.updateData(upd);
        assertThat(dictService.listData(TYPE))
                .filteredOn(d -> d.getId().equals(second))
                .anySatisfy(d -> assertThat(d.getDictLabel()).isEqualTo("女2"));

        // 删除
        dictService.deleteData(first);
        assertThat(dictService.listData(TYPE)).hasSize(1);
    }

    @Test
    void redisCache_readHit_evictOnChange() {
        typeId = createType(TYPE);
        createData(TYPE, "男", "M", 1);
        createData(TYPE, "女", "F", 2);

        // 首次读 → 回源 DB 并写缓存
        List<DictCacheItem> items = dictService.getDataByType(TYPE);
        assertThat(items).hasSize(2);
        assertThat(redisTemplate.opsForValue().get(CACHE_KEY)).isNotNull();

        // 缓存命中（直接读缓存）
        List<DictCacheItem> cached = dictService.getDataByType(TYPE);
        assertThat(cached).hasSize(2);
        assertThat(cached.get(0).getValue()).isEqualTo("M");

        // 变更（新增数据）→ 缓存失效并回源重建
        createData(TYPE, "未知", "X", 3);
        List<DictCacheItem> after = dictService.getDataByType(TYPE);
        assertThat(after).hasSize(3);
        assertThat(after.get(2).getValue()).isEqualTo("X");
    }

    @Test
    void deleteType_cascadeDeletesData() {
        typeId = createType(TYPE);
        createData(TYPE, "男", "M", 1);
        createData(TYPE, "女", "F", 2);

        Long id = typeId;
        typeId = null; // 交给 deleteType 后不再重复清理
        dictService.deleteType(id);
        assertThat(dictService.listData(TYPE)).isEmpty();
        assertThat(redisTemplate.opsForValue().get(CACHE_KEY)).isNull();
        // 类型不可再查（逻辑删后列表不含）
        PageResult<SysDictType> page = dictService.pageTypes(1, 10, null, TYPE, null);
        assertThat(page.getTotal()).isZero();
    }
}
