package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.Baby;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BabyMapper extends BaseMapper<Baby> {

    /** 行级锁定宝宝档案，串行化同一宝宝的反应登记/撤销，避免并发丢失食材状态更新 */
    @Select("SELECT * FROM baby WHERE id = #{id} FOR UPDATE")
    Baby selectByIdForUpdate(@Param("id") Long id);
}
