package cn.zj.hz.xhgl.workflow.node;

import lombok.Data;

/**
 * 工作流节点
 * @author jyl
 * @since 2020/5/11 10:30
 */
@Data
public class Node {
    /** 状态 */
    private String status;

    public Node(String status) {
        this.status = status;
    }
}
