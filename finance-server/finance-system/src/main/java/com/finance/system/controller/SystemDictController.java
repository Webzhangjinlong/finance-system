package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.common.core.domain.PageResult;
import com.finance.system.domain.SysDictData;
import com.finance.system.domain.SysDictType;
import com.finance.system.dto.DictDataDTO;
import com.finance.system.dto.DictTypeDTO;
import com.finance.system.service.SysDictService;
import com.finance.system.service.SysDictService.DictCacheItem;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理接口（S3）：/system/dict，两级字典（类型 + 数据）+ Redis 缓存失效。
 *
 * <p>权限码 system:dict:list/add/edit/del + system:dict:data:*（V19 已授权 admin）；
 * 字典缓存读取 GET /system/dict/data/type/{dictType} 供下拉/其他模块使用（需 system:dict:list）。</p>
 */
@RestController
@RequestMapping("/system/dict")
public class SystemDictController {

    private final SysDictService dictService;

    public SystemDictController(SysDictService dictService) {
        this.dictService = dictService;
    }

    // ==================== 字典类型 ====================

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<SysDictType>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String dictName,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String status) {
        return Result.ok(dictService.pageTypes(page, size, dictName, dictType, status));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<List<SysDictType>> list() {
        return Result.ok(dictService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:add')")
    @OperLog(title = "新增字典类型", operType = OperType.INSERT)
    public Result<Long> createType(@Valid @RequestBody DictTypeDTO dto) {
        return Result.ok(dictService.createType(dto));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @OperLog(title = "修改字典类型", operType = OperType.UPDATE)
    public Result<Void> updateType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.updateType(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:del')")
    @OperLog(title = "删除字典类型（级联删数据）", operType = OperType.DELETE)
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    // ==================== 字典数据 ====================

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<List<SysDictData>> listData(@RequestParam String dictType) {
        return Result.ok(dictService.listData(dictType));
    }

    /** 缓存读取（label/value/sort，仅 ACTIVE），供下拉/其他模块使用。 */
    @GetMapping("/data/type/{dictType}")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<List<DictCacheItem>> dataByType(@PathVariable String dictType) {
        return Result.ok(dictService.getDataByType(dictType));
    }

    @PostMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:data:add')")
    @OperLog(title = "新增字典数据", operType = OperType.INSERT)
    public Result<Long> createData(@Valid @RequestBody DictDataDTO dto) {
        return Result.ok(dictService.createData(dto));
    }

    @PutMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:data:edit')")
    @OperLog(title = "修改字典数据", operType = OperType.UPDATE)
    public Result<Void> updateData(@Valid @RequestBody DictDataDTO dto) {
        dictService.updateData(dto);
        return Result.ok();
    }

    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict:data:del')")
    @OperLog(title = "删除字典数据", operType = OperType.DELETE)
    public Result<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.ok();
    }
}
