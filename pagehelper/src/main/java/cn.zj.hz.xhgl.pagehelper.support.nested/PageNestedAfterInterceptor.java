package cn.zj.hz.xhgl.pagehelper.support.nested;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.util.ExecutorUtil;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.CollectionUtils;

/**
 * PageHelper 由于不支持 Mybatis
 * 的嵌套映射（https://github.com/pagehelper/Mybatis-PageHelper/blob/master/wikis/en/Important.md）。 因此对
 * pageHelper的通用分页拦截器 {@link com.github.pagehelper.PageInterceptor} 的处理结果进行再处理.
 *
 * <p>拦截器 {@link PageNestedAfterInterceptor}和拦截器 {@link PageNestedBeforeInterceptor} 联合使用作为 {@link
 * com.github.pagehelper.PageInterceptor}的前置处理和后置处理
 *
 * <p>前置处理主要作用为判断查询是否为分页嵌套映射，即是否开启了pageHelper分页并且返回结果为嵌套映射
 *
 * <p>后置处理主要作用为处理count查询以及分页查询参数。将pageHelper的count查询处理为分组统计。 再根据统计的每组总数对page参数的调整
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
 * @author jyl
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
public class PageNestedAfterInterceptor implements Interceptor {

    private final String countSuffix = "_COUNT";
    private final String paramFirstPageHelper = "First_PageHelper";
    private final String paramSecondPageNum = "Second_PageHelper";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        boolean clear = true;
        try {
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
            //
            PageNested localNested = PageNestedHelper.getLocalNested();
            if (localNested.isSkip()) {
                // 如果跳过嵌套处理
                if (ms.getId().endsWith(countSuffix)) {
                    clear = false;
                }
                return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            } else if (CollectionUtils.isEmpty(localNested.getIdColumn())) {
                throw new PageNestedException("id列不存在，嵌套分页查询必须设置id列");
            } else {
                if (ms.getId().endsWith(countSuffix)) {
                    // msId为_COUNT结尾则表明为pageHelper处理的count查询，需要加上group by
                    clear = false;
                    return countGroup(
                            executor, ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                } else {
                    //  page query
                    return pageQuery(
                            executor, ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                }
            }
        } finally {
            if (clear) {
                PageNested localNested = PageNestedHelper.getLocalNested();
                if (!localNested.isSkip()) {
                    // 嵌套查询重新设置pageHelper total参数，再清理线程参数
                    PageHelper.getLocalPage().setTotal(localNested.getGroupCount().size());
                }
                PageNestedHelper.clearNested();
            }
        }
    }

    private List<Long> countGroup(
            Executor executor,
            MappedStatement ms,
            Object parameter,
            RowBounds rowBounds,
            ResultHandler resultHandler,
            CacheKey cacheKey,
            BoundSql boundSql)
            throws SQLException {
        // 是否为手写的 count 查询
        MappedStatement countMs =
                ExecutorUtil.getExistedMappedStatement(ms.getConfiguration(), ms.getId());
        List<Long> groupCount;
        if (countMs != null) {
            groupCount =
                    executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        } else {
            // 先简单处理在sql尾部加 group by {idColumn}，目前该方法分组统计结果很不准确，推荐使用手写 count 查询
            // 后续可改成用net.sf.jsqlparser.parser.CCJSqlParserUtil解析sql处理
            String sql =
                    getSimpleGroupCountSql(
                            boundSql.getSql(), PageNestedHelper.getLocalNested().getIdColumn());
            BoundSql groupCountBoundSql =
                    new BoundSql(
                            ms.getConfiguration(), sql, boundSql.getParameterMappings(), parameter);
            Map<String, Object> additionalParameters =
                    ExecutorUtil.getAdditionalParameter(boundSql);
            // 当使用动态 SQL 时，可能会产生临时的参数，这些参数需要手动设置到新的 BoundSql 中
            for (String key : additionalParameters.keySet()) {
                groupCountBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
            }
            groupCount =
                    executor.query(
                            ms, parameter, rowBounds, resultHandler, cacheKey, groupCountBoundSql);
        }
        // 设置线程 PageNested 参数
        PageNestedHelper.getLocalNested().setGroupCount(groupCount);

        // 由于 pageHelper 在查询完总数后会判断分页是否超过总数，因此这里先将不准确的散列总条数返回给pageHelper，在最后再对total重新赋值嵌套总条数
        return Collections.singletonList(groupCount.stream().reduce(Long::sum).orElse(0L));
    }

    private <E> List<E> pageQuery(
            Executor executor,
            MappedStatement ms,
            Object parameter,
            RowBounds rowBounds,
            ResultHandler resultHandler,
            CacheKey cacheKey,
            BoundSql boundSql)
            throws SQLException {
        HashMap param = (HashMap) parameter;
        // 仅为mysql方言
        Integer origPageNum = (Integer) param.get(paramFirstPageHelper);
        Integer origPageSize = (Integer) param.get(paramSecondPageNum);
        int startPage = 0;
        int pageSize = 0;
        List<Long> groupCount = PageNestedHelper.getLocalNested().getGroupCount();
        for (int i = 0; i < groupCount.size(); i++) {
            if (i < origPageNum) {
                startPage += groupCount.get(i);
            } else if (i < origPageNum + origPageSize && i >= origPageNum) {
                pageSize += groupCount.get(i);
            }
        }
        param.put(paramFirstPageHelper, startPage);
        param.put(paramSecondPageNum, pageSize);
        return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }

    /**
     * 获取普通的 group count sql
     *
     * @param sql pagehelper count sql
     * @return 返回聚合count查询sql
     */
    private String getSimpleGroupCountSql(final String sql, List<String> columes) {
        return sql + " group by " + StringUtils.join(columes, ",");
    }
}
