package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.Feeding;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FeedingMapper extends BaseMapper<Feeding> {

    /** 锁定餐次行，保证同一餐的重复/并发反应提交只生成一条有效反应 */
    @Select("SELECT * FROM feeding WHERE id = #{id} FOR UPDATE")
    Feeding selectByIdForUpdate(@Param("id") Long id);
}
