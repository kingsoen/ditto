package cn.zj.hz.xhgl.workflow.node.chain;

import cn.zj.hz.xhgl.workflow.entity.RunningTask;

/**
 * 起始节点的处理
 * @author jyl
 * @since 2020/5/11 10:44
 */
public abstract class BeginFlowNodeHandlerChain extends FlowNodeHandlerChain {

    @Override
    public FlowNodeHandlerChain backNodeChain() {
        return null;
    }

    @Override
    public void back() {
        throw new FlowNodeHandlerChainException("起始节点无法执行回退操作。");
    }

    @Override
    public void handle(RunningTask task) {
        // todo 写入工作流信息
    }
}
