package cn.zj.hz.xhgl.workflow.node;

import cn.zj.hz.xhgl.workflow.entity.RunningTask;

/**
 * 工作流节点处理
 * @author jyl
 * @since 2020/5/11 10:29
 */
public interface FlowNodeHandler {

    Node getNode();

    void handle(RunningTask task);
}
