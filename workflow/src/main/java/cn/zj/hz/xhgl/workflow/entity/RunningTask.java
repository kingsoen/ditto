package cn.zj.hz.xhgl.workflow.entity;

import lombok.Data;

import java.util.Date;

/**
 * 执行的工作任务
 * @author jyl
 * @since 2020/5/11 10:33
 */
@Data
public class RunningTask {
    /** 任务id */
    private String taskId;
    /** 任务类型 */
    private String taskType;
    /** 任务状态 */
    private String status;
    /** 状态得分 */
    private String verified;
    /** 内容id */
    private String contentId;
    /** 创建时间 */
    private Date createTime;
    /** 创建者 */
    private String createBy;
    /** 修改时间 */
    private Date modifiedTime;
    /** 修改人 */
    private String modifiedBy;
}
