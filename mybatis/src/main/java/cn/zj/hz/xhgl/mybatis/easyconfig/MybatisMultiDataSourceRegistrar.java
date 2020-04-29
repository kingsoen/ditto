package cn.zj.hz.xhgl.mybatis.easyconfig;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 多datasource + mybatis 配置注册
 *
 * @since 2020/1/6 15:22
 * @author 金遥力
 */
public class MybatisMultiDataSourceRegistrar extends MybatisDataSourceRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes mapperScansAttrs =
                AnnotationAttributes.fromMap(
                        importingClassMetadata.getAnnotationAttributes(
                                MybatisConfigurationScans.class.getName()));
        if (mapperScansAttrs != null) {
            AnnotationAttributes[] annotations = mapperScansAttrs.getAnnotationArray("value");
            for (int i = 0; i < annotations.length; i++) {
                registerBeanDefinitions(annotations[i], importingClassMetadata, registry, i);
            }
        }
    }
}
