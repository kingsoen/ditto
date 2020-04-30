package cn.zj.hz.xhgl.pagehelper.support.nested;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 嵌套映射分页对象
 *
 * @since 2020/1/14 15:47
 * @author jyl
 */
@Data
public class PageNested {

    /** 是否跳过 */
    private boolean skip;
    /** 主键字段 */
    private List<String> idColumn;
    /** 分组count */
    private List<Long> groupCount;

    public PageNested() {}

    public void addIdColumn(String idColumn) {
        if (this.idColumn == null) {
            this.idColumn = new ArrayList<>();
        }
        this.idColumn.add(idColumn);
    }
}
