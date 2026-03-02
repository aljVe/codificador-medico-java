package com.hospital.cdi.model;

import java.util.List;
import lombok.Data;

@Data
public class TreeNode {
    private String question;
    private List<TreeOption> options;

    public TreeNode() {}
    
    public TreeNode(String question, List<TreeOption> options) {
        this.question = question;
        this.options = options;
    }
}
