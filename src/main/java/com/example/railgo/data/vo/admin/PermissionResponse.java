package com.example.railgo.data.vo.admin;

import lombok.Data;

@Data
public class PermissionResponse {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String module;
    private String description;
    private String status;
}
