package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderPageResponse<T> {

    private long page;

    private long size;

    private long total;

    private long pages;

    private List<T> records;
}
