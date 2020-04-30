package cn.zj.hz.xhgl.mybatis.easyconfig;

import cn.zj.hz.xhgl.mybatis.easyconfig.loader.PasswordDecryptor;
import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScannerRegistrar;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * datasource + mybatis 配置注册
 *
 * @since 2020/1/6 15:22
 * @author jyl
 */
public class MybatisDataSourceRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private AbstractEnvironment environment;

    private static final String DEFAULT_PASSWORD_DECRYPTOR_CLASS_NAME =
            "cn.zj.hz.xhgl.mybatis.easyconfig.loader.PlaintPasswordDecryptor";

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (AbstractEnvironment) environment;
    }

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annoAttrs =
                AnnotationAttributes.fromMap(
                        importingClassMetadata.getAnnotationAttributes(
                                MybatisConfigurationScan.class.getName()));
        if (annoAttrs != null) {
            registerBeanDefinitions(annoAttrs, importingClassMetadata, registry, 0);
        }
    }

    void registerBeanDefinitions(
            AnnotationAttributes annoAttrs,
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry,
            int index) {
        String dataSourceRef =
                registerDataSourceDefinitions(annoAttrs, registry, importingClassMetadata, index);
        registerMybatisSessionBeanDefinitions(
                annoAttrs, registry, importingClassMetadata, dataSourceRef, index);
        registerMybatisBeanDefinitions(
                annoAttrs.getAnnotation("value"),
                registry,
                generateBaseBeanName(importingClassMetadata, MapperScannerRegistrar.class, index));
    }

    private String registerDataSourceDefinitions(
            AnnotationAttributes annoAttrs,
            BeanDefinitionRegistry registry,
            AnnotationMetadata importingClassMetadata,
            int index) {
        // 数据源
        String dataSourcePrefix = annoAttrs.getString("dataSourcePrefix") + ".";
        MutablePropertySources propertySources = environment.getPropertySources();
        BeanDefinitionBuilder dataSourcebuilder =
                BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
        propertySources.forEach(
                propertySource -> {
                    if (propertySource instanceof MapPropertySource) {
                        MapPropertySource mps = (MapPropertySource) propertySource;
                        Set<String> keys = mps.getSource().keySet();
                        for (String key : keys) {
                            // 以第一次取到的KV为准，参考
                            // org.springframework.boot.context.properties.bind.Binder#findProperty
                            String kebabKey;
                            if (key.startsWith(dataSourcePrefix)
                                    && !dataSourcebuilder
                                            .getBeanDefinition()
                                            .getPropertyValues()
                                            .contains(
                                                    kebabKey =
                                                            kebabToCamelCase(
                                                                    key.replaceFirst(
                                                                            dataSourcePrefix,
                                                                            "")))) {
                                dataSourcebuilder.addPropertyValue(kebabKey, mps.getProperty(key));
                            }
                        }
                    }
                });
        dataSourceProperiesProcess(dataSourcebuilder);
        String dataSourceRef = annoAttrs.getString("dataSourceRef");
        if (!StringUtils.hasText(dataSourceRef)) {
            dataSourceRef =
                    generateBaseBeanName(importingClassMetadata, DruidDataSource.class, index);
        }
        registry.registerBeanDefinition(
                dataSourceRef, validatePrimary(registry, dataSourcebuilder, DataSource.class));
        // 事务管理
        BeanDefinitionBuilder dataSourceTransactionManagerBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(DataSourceTransactionManager.class);
        dataSourceTransactionManagerBuilder.addConstructorArgReference(dataSourceRef);
        String transactionManagerRef = annoAttrs.getString("transactionManagerRef");
        if (!StringUtils.hasText(transactionManagerRef)) {
            transactionManagerRef =
                    generateBaseBeanName(
                            importingClassMetadata, DataSourceTransactionManager.class, index);
        }
        registry.registerBeanDefinition(
                transactionManagerRef,
                validatePrimary(
                        registry,
                        dataSourceTransactionManagerBuilder,
                        DataSourceTransactionManager.class));
        return dataSourceRef;
    }

    private void dataSourceProperiesProcess(BeanDefinitionBuilder dataSourcebuilder) {
        MutablePropertyValues propertyValues =
                dataSourcebuilder.getBeanDefinition().getPropertyValues();
        String passwordDecryptorClassName = DEFAULT_PASSWORD_DECRYPTOR_CLASS_NAME;
        if (propertyValues.contains("passwordDecryptorClassName")) {
            passwordDecryptorClassName = (String) propertyValues.get("passwordDecryptorClassName");
        }
        try {
            Class<?> passwordDecryptorClass = Class.forName(passwordDecryptorClassName);
            Object passwordDecryptor = BeanUtils.instantiateClass(passwordDecryptorClass);
            if (passwordDecryptor instanceof PasswordDecryptor) {
                ((PasswordDecryptor) passwordDecryptor).decrypt(propertyValues);
            } else {
                throw new BeanInstantiationException(
                        passwordDecryptorClass,
                        "No implementation for " + PasswordDecryptor.class.getCanonicalName());
            }
        } catch (Exception e) {
            throw new NoSuchBeanDefinitionException(passwordDecryptorClassName + "is not a class");
        }
    }

    private void registerMybatisSessionBeanDefinitions(
            AnnotationAttributes annoAttrs,
            BeanDefinitionRegistry registry,
            AnnotationMetadata importingClassMetadata,
            String dataSourceRef,
            int index) {
        // sqlSessionFactory
        String sqlSessionFactoryRef =
                registersqlSessionFactoryDefinitions(
                        annoAttrs, registry, importingClassMetadata, dataSourceRef, index);
        // sqlSessionTemplate
        registerSqlSessionTemplateDefinitions(
                annoAttrs, registry, importingClassMetadata, sqlSessionFactoryRef, index);
    }

    private String registersqlSessionFactoryDefinitions(
            AnnotationAttributes annoAttrs,
            BeanDefinitionRegistry registry,
            AnnotationMetadata importingClassMetadata,
            String dataSourceRef,
            int index) {
        BeanDefinitionBuilder sqlSessionFactoryBeanBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(SqlSessionFactoryBean.class);
        sqlSessionFactoryBeanBuilder.addPropertyReference("dataSource", dataSourceRef);

        sqlSessionFactoryBeanBuilder.addPropertyValue(
                "configLocation", annoAttrs.getString("configLocation"));

        String typeAliasesPageage = annoAttrs.getString("typeAliasesPageage");
        if (StringUtils.hasText(typeAliasesPageage)) {
            sqlSessionFactoryBeanBuilder.addPropertyValue("typeAliasesPackage", typeAliasesPageage);
        }

        sqlSessionFactoryBeanBuilder.addPropertyValue(
                "mapperLocations", annoAttrs.getStringArray("mapperLocations"));

        AnnotationAttributes mapperScanAttrs = annoAttrs.getAnnotation("value");
        String sqlSessionFactoryRef = mapperScanAttrs.getString("sqlSessionFactoryRef");
        if (!StringUtils.hasText(sqlSessionFactoryRef)) {
            sqlSessionFactoryRef =
                    generateBaseBeanName(importingClassMetadata, SqlSessionFactory.class, index);
            mapperScanAttrs.put("sqlSessionFactoryRef", sqlSessionFactoryRef);
        }
        registry.registerBeanDefinition(
                sqlSessionFactoryRef,
                validatePrimary(
                        registry, sqlSessionFactoryBeanBuilder, SqlSessionFactoryBean.class));
        return sqlSessionFactoryRef;
    }

    private void registerSqlSessionTemplateDefinitions(
            AnnotationAttributes annoAttrs,
            BeanDefinitionRegistry registry,
            AnnotationMetadata importingClassMetadata,
            String sqlSessionFactoryRef,
            int index) {
        BeanDefinitionBuilder sqlSessionTemplateBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(SqlSessionTemplate.class);
        sqlSessionTemplateBuilder.addConstructorArgReference(sqlSessionFactoryRef);

        AnnotationAttributes mapperScanAttrs = annoAttrs.getAnnotation("value");
        String sqlSessionTemplateRef = mapperScanAttrs.getString("sqlSessionTemplateRef");
        if (!StringUtils.hasText(sqlSessionTemplateRef)) {
            sqlSessionTemplateRef =
                    generateBaseBeanName(importingClassMetadata, SqlSessionTemplate.class, index);
            mapperScanAttrs.put("sqlSessionTemplateRef", sqlSessionTemplateRef);
        }
        registry.registerBeanDefinition(
                sqlSessionTemplateRef,
                validatePrimary(registry, sqlSessionTemplateBuilder, SqlSessionTemplate.class));
    }

    /**
     * 由于 {@link MapperScannerRegistrar} 的 registerBeanDefinitions(AnnotationMetadata annoMeta,
     * AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry, String beanName)
     * 方法的作用域为default，因此代码复制到此处 代码 copy from:
     * https://github.com/mybatis/spring/blob/master/src/main/java/org/mybatis/spring/annotation/MapperScannerRegistrar.java
     */
    private void registerMybatisBeanDefinitions(
            AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry, String beanName) {

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
        builder.addPropertyValue("processPropertyPlaceHolders", true);

        Class<? extends Annotation> annotationClass = annoAttrs.getClass("annotationClass");
        if (!Annotation.class.equals(annotationClass)) {
            builder.addPropertyValue("annotationClass", annotationClass);
        }

        Class<?> markerInterface = annoAttrs.getClass("markerInterface");
        if (!Class.class.equals(markerInterface)) {
            builder.addPropertyValue("markerInterface", markerInterface);
        }

        Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
        if (!BeanNameGenerator.class.equals(generatorClass)) {
            builder.addPropertyValue("nameGenerator", BeanUtils.instantiateClass(generatorClass));
        }

        Class<? extends MapperFactoryBean> mapperFactoryBeanClass =
                annoAttrs.getClass("factoryBean");
        if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
            builder.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
        }

        String sqlSessionTemplateRef = annoAttrs.getString("sqlSessionTemplateRef");
        if (StringUtils.hasText(sqlSessionTemplateRef)) {
            builder.addPropertyValue(
                    "sqlSessionTemplateBeanName", annoAttrs.getString("sqlSessionTemplateRef"));
        }

        String sqlSessionFactoryRef = annoAttrs.getString("sqlSessionFactoryRef");
        if (StringUtils.hasText(sqlSessionFactoryRef)
                && !StringUtils.hasText(sqlSessionTemplateRef)) {
            builder.addPropertyValue(
                    "sqlSessionFactoryBeanName", annoAttrs.getString("sqlSessionFactoryRef"));
        }

        List<String> basePackages = new ArrayList<>();
        basePackages.addAll(
                Arrays.stream(annoAttrs.getStringArray("value"))
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));

        basePackages.addAll(
                Arrays.stream(annoAttrs.getStringArray("basePackages"))
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));

        basePackages.addAll(
                Arrays.stream(annoAttrs.getClassArray("basePackageClasses"))
                        .map(ClassUtils::getPackageName)
                        .collect(Collectors.toList()));

        String lazyInitialization = annoAttrs.getString("lazyInitialization");
        if (StringUtils.hasText(lazyInitialization)) {
            builder.addPropertyValue("lazyInitialization", lazyInitialization);
        }

        builder.addPropertyValue(
                "basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));

        registry.registerBeanDefinition(
                beanName, validatePrimary(registry, builder, MapperScannerConfigurer.class));
    }

    private static String generateBaseBeanName(
            AnnotationMetadata importingClassMetadata, Class beanClass, int index) {
        return importingClassMetadata.getClassName()
                + "#"
                + beanClass.getSimpleName()
                + "#"
                + index;
    }

    /** 校验bean是否需要被设置为primary，需要则设置BeanDefinition的primary=true */
    private AbstractBeanDefinition validatePrimary(
            BeanDefinitionRegistry registry,
            BeanDefinitionBuilder beanDefinitionBuilder,
            Class<?> beanClass) {
        AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        try {
            ((DefaultListableBeanFactory) registry).getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            // spring容器中不存在该bean，则设置为primary
            beanDefinition.setPrimary(true);
        }
        return beanDefinition;
    }

    private String kebabToCamelCase(String propertyName) {
        StringBuilder buf = new StringBuilder();
        boolean preIsKebab = false;
        for (int i = 0; i < propertyName.length(); ++i) {
            char ch = propertyName.charAt(i);
            if (ch == '-') {
                preIsKebab = true;
            } else if (preIsKebab) {
                preIsKebab = false;
                if (ch >= 'a' && ch <= 'z') {
                    buf.append((char) (ch - 32));
                } else {
                    buf.append(ch);
                }
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }
}
