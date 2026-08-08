package com.example.railgo.data.vo.admin;

import lombok.Data;

@Data
public class AdminUserStatisticsResponse {
    private Long totalUsers;
    private Long enabledUsers;
    private Long disabledUsers;
    private Long newUsers;
    private Long activePurchasingUsers;
}
