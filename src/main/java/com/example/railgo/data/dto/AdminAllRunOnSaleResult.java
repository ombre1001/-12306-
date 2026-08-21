package com.example.railgo.data.dto;

public record AdminAllRunOnSaleResult(

        /**
         * 运行计划总数。
         */
        long totalRunCount,

        /**
         * 本次实际修改为ON_SALE的数量。
         */
        int updatedCount,

        /**
         * 调用前已经是ON_SALE的数量。
         */
        long alreadyOnSaleCount,

        /**
         * 因未初始化库存而跳过的数量。
         */
        long inventoryNotInitializedCount,

        /**
         * 已取消的运行计划数量。
         */
        long cancelledCount
) {
}