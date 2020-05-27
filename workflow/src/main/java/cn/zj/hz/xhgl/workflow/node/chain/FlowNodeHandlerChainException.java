package cn.zj.hz.xhgl.workflow.node.chain;

/**
 * 工作流链式处理异常
 * @author jyl
 * @since 2020/5/11 10:48
 */
public class FlowNodeHandlerChainException extends RuntimeException {

    public FlowNodeHandlerChainException(String msg) {
        super(msg);
    }
}
