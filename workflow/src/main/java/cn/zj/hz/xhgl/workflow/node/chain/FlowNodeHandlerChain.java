package cn.zj.hz.xhgl.workflow.node.chain;

import cn.zj.hz.xhgl.workflow.entity.RunningTask;
import cn.zj.hz.xhgl.workflow.node.FlowNodeHandler;
import cn.zj.hz.xhgl.workflow.node.Node;

/**
 * 链式工作流节点处理
 * @author jyl
 * @since 2020/5/11 10:39
 */
public abstract class FlowNodeHandlerChain implements FlowNodeHandler {

    public abstract FlowNodeHandlerChain nextNodeChain();

    public abstract FlowNodeHandlerChain backNodeChain();

    public void back(){
        backNodeChain().handle();
    }

    public void next(){

    }

    @Override
    public void handle(RunningTask task) {
        // todo 更新工作流表的状态
    }
}
