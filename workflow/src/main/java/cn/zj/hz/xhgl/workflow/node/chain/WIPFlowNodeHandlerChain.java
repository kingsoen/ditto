package cn.zj.hz.xhgl.workflow.node.chain;

import cn.zj.hz.xhgl.workflow.node.Node;

/**
 * @author jyl
 * @since 2020/5/11 10:52
 */
public class WIPFlowNodeHandlerChain extends BeginFlowNodeHandlerChain {

    @Override
    public FlowNodeHandlerChain nextNodeChain() {
        return null;
    }

    @Override
    public Node getNode() {
        return new Node("WIP");
    }
}
