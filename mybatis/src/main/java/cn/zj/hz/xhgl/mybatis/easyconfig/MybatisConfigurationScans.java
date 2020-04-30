package cn.zj.hz.xhgl.mybatis.easyconfig;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 多个mybatis配置注解类
 *
 * @since 2020/1/6 14:59
 * @author jyl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MybatisMultiDataSourceRegistrar.class)
@Component
public @interface MybatisConfigurationScans {

    MybatisConfigurationScan[] value();
}
