package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissions;
}
