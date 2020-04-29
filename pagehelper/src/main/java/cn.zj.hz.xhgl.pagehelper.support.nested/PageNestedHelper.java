package cn.zj.hz.xhgl.pagehelper.support.nested;

import com.github.pagehelper.PageHelper;
import java.util.List;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.springframework.util.CollectionUtils;

/**
 * pageHelper 嵌套映射增强基础方法
 *
 * @since 2020/1/14 10:54
 * @author 金遥力
 */
public class PageNestedHelper {

    public static final ThreadLocal<PageNested> LOCAL_TYPE = new ThreadLocal<>();

    /** 设置 PageNested 参数 */
    public static void setLocalNested(PageNested type) {
        LOCAL_TYPE.set(type);
    }

    /** 获取 PageNested 参数 */
    public static PageNested getLocalNested() {
        return LOCAL_TYPE.get();
    }

    /** 移除本地变量 */
    public static void clearNested() {
        LOCAL_TYPE.remove();
    }

    /** 是否跳过嵌套映射处理， 并设置线程参数 判断 ResultMap 中是否包含 javaType为List的类型，如果有则不跳过，无则跳过 */
    public boolean skip(MappedStatement ms) {
        PageNested pageNested = new PageNested();
        boolean isSkip = true;
        try {
            if (PageHelper.getLocalPage() == null || CollectionUtils.isEmpty(ms.getResultMaps())) {
                return true;
            }
            List<ResultMapping> resultMappings = ms.getResultMaps().get(0).getResultMappings();
            for (ResultMapping resultMapping : resultMappings) {
                if (resultMapping.getFlags().contains(ResultFlag.ID)) {
                    pageNested.addIdColumn(resultMapping.getColumn());
                }
                if (List.class.isAssignableFrom(resultMapping.getJavaType())) {
                    isSkip = false;
                    break;
                }
            }
            return isSkip;
        } finally {
            pageNested.setSkip(isSkip);
            setLocalNested(pageNested);
        }
    }
}
