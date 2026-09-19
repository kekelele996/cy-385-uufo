package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.Baby;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BabyMapper extends BaseMapper<Baby> {

    /** 按主键加行锁，串行化同一宝宝的反应登记/撤销，保证并发提交结果一致 */
    @Select("SELECT id FROM baby WHERE id = #{id} FOR UPDATE")
    Long selectIdForUpdate(@Param("id") Long id);
}
