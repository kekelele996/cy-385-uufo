package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.IngredientStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface IngredientStatusMapper extends BaseMapper<IngredientStatus> {

    /**
     * 原子写入状态：并发首次登记同一食材时依赖唯一键收敛为一行，等待方转走 UPDATE 分支。
     * 状态/原因完全由有效反应事实在事务内重新计算后覆盖，保证撤销后状态一致。
     */
    @Insert("INSERT INTO ingredient_status (baby_id, ingredient_name, status, reason) "
            + "VALUES (#{babyId}, #{ingredientName}, #{status}, #{reason}) "
            + "ON DUPLICATE KEY UPDATE status = VALUES(status), reason = VALUES(reason)")
    int upsertStatus(@Param("babyId") Long babyId,
                     @Param("ingredientName") String ingredientName,
                     @Param("status") String status,
                     @Param("reason") String reason);
}
