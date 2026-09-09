package com.itech.learnspace.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentBatchRequest {
    private List<Row> rows = new ArrayList<Row>();

    @Data
    public static class Row {
        private String className;
        private String studentNo;
        private String realName;
    }
}
