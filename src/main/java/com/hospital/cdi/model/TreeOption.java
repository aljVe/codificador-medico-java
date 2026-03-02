package com.hospital.cdi.model;

import lombok.Data;

@Data
public class TreeOption {
    private String label;
    private String value;
    private TreeNode next;
    
    public TreeOption() {}

    public TreeOption(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public TreeOption(String label, TreeNode next) {
        this.label = label;
        this.next = next;
    }
}
