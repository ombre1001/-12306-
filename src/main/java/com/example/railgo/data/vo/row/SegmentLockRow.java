package com.example.railgo.data.vo.row;

import lombok.Data;

@Data
public class SegmentLockRow {

    private Long id;

    private Integer segmentSeq;

    private String status;

    private Long orderItemId;
}