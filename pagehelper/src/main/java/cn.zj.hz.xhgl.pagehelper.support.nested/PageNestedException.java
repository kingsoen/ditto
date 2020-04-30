package cn.zj.hz.xhgl.pagehelper.support.nested;

/**
 * 嵌套映射分页查询异常
 *
 * @since 2020/1/14 16:30
 * @author jyl
 */
public class PageNestedException extends RuntimeException {

    public PageNestedException() {
        super();
    }

    public PageNestedException(String message) {
        super(message);
    }

    public PageNestedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PageNestedException(Throwable cause) {
        super(cause);
    }
}
