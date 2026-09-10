package com.finance.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.hr.domain.HrAttendance;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 考勤 Mapper。upsert 需物理清旧（L41：逻辑删残留仍占唯一约束）。
 */
@Mapper
public interface HrAttendanceMapper extends BaseMapper<HrAttendance> {

    /** 物理删除员工+日期记录（防 uq_hr_attendance 残留占用）。 */
    @Delete("DELETE FROM hr_attendance WHERE company_code = #{companyCode}"
            + " AND employee_id = #{employeeId} AND work_date = #{workDate}")
    int physicalDeleteByEmployeeAndDate(@Param("companyCode") String companyCode,
                                        @Param("employeeId") Long employeeId,
                                        @Param("workDate") LocalDate workDate);
}
