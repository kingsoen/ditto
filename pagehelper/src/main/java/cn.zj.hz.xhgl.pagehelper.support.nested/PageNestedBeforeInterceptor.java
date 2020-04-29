package cn.zj.hz.xhgl.pagehelper.support.nested;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * PageHelper 由于不支持 Mybatis
 * 的嵌套映射（https://github.com/pagehelper/Mybatis-PageHelper/blob/master/wikis/en/Important.md）。 因此对
 * pageHelper的通用分页拦截器 {@link com.github.pagehelper.PageInterceptor} 的处理结果进行再处理.
 *
 * <p>拦截器 {@link PageNestedAfterInterceptor}和拦截器 {@link PageNestedBeforeInterceptor} 联合使用作为 {@link
 * com.github.pagehelper.PageInterceptor}的前置处理和后置处理
 *
 * <p>前置处理主要作用为判断查询是否为分页嵌套映射，即开启了pageHelper分页并且返回结果为嵌套映射
 *
 * <p>后置处理主要作用为处理count查询以及分页查询参数
 *
 * <pre>
 * 执行顺序：
 *       &lt;PageNestedBeforeInterceptor do-before&gt;
 *          &lt;pageIntgerceptor do-before&gt;
 *            &lt;PageNestedAfterInterceptor do-before&gt;
 *                &lt;sql execute&gt;
 *            &lt;PageNestedAfterInterceptor do-after&gt;
 *          &lt;pageIntgerceptor do-after&gt;
 *      &lt;PageNestedBeforeInterceptor do-after&gt;
 *  </pre>
 *
 * @since 2020/1/13 14:12
 * @author 金遥力
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class,
                Object.class,
                RowBounds.class,
                ResultHandler.class,
                CacheKey.class,
                BoundSql.class
            }),
})
@Slf4j
public class PageNestedBeforeInterceptor implements Interceptor {

    private volatile PageNestedHelper helper;

    public PageNestedBeforeInterceptor(PageNestedHelper helper) {
        this.helper = helper;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler resultHandler = (ResultHandler) args[3];
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql;
        if (args.length == 4) {
            // 4 个参数时
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            // 6 个参数时
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }

        if (!helper.skip(ms)) {
            log.debug(
                    "进行pageHelper嵌套查询处理，idCloumn=【{}】",
                    PageNestedHelper.getLocalNested().getIdColumn());
        }
        return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }
}
