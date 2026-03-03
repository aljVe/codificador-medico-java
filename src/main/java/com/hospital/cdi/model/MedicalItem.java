package com.hospital.cdi.model;

import java.util.List;
import lombok.Data;

@Data
public class MedicalItem {
    private String id;
    private String type;
    private List<String> terms;
    private String alert;
    private String value;
    private String code;
    private TreeNode root;
    private boolean isFromCsv;
}
