package com.github.osodevops.akka.openapi.core.fixtures;

import java.util.List;

/**
 * Self-referential class for testing circular reference detection.
 */
public class TreeNode {
    private String name;
    private TreeNode parent;
    private List<TreeNode> children;

    public TreeNode() {}

    public TreeNode(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TreeNode getParent() { return parent; }
    public void setParent(TreeNode parent) { this.parent = parent; }

    public List<TreeNode> getChildren() { return children; }
    public void setChildren(List<TreeNode> children) { this.children = children; }
}
