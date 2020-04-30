package cn.zj.hz.xhgl.mybatis.easyconfig;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 单个mybatis配置注解类
 *
 * @since 2020/1/6 14:59
 * @author jyl
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MybatisDataSourceRegistrar.class)
@Repeatable(MybatisConfigurationScans.class)
@Component
public @interface MybatisConfigurationScan {

    /** 数据源properties配置的前缀 */
    String dataSourcePrefix();

    /** 数据源映射名 */
    String dataSourceRef() default "";

    /** 事务管理映射名 */
    String transactionManagerRef() default "";

    /** mybatis mapperScan */
    MapperScan value();

    /** mapper需要的xml文件路径 */
    String[] mapperLocations();

    /** entity包路径 */
    String typeAliasesPageage() default "";

    /** mybatis配置文件路径 */
    String configLocation();
}
