package cn.zj.hz.xhgl.mybatis.easyconfig.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.MutablePropertyValues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码解密统一抽象类
 *
 * @since 2020/3/11 18:39
 * @author 金遥力
 */
@Slf4j
public abstract class AbstractPasswordDecryptor implements PasswordDecryptor {

    @Override
    public String decrypt(MutablePropertyValues propertyValues) {
        // 移除多余的属性
        propertyValues.removePropertyValue("passwordDecryptorClassName");
        Map<String, Object> decryptPropertiesMap = new HashMap<>();
        for (String propertyKey : propertiesKey()) {
            decryptPropertiesMap.put(propertyKey, propertyValues.get(propertyKey));
            propertyValues.removePropertyValue(propertyKey);
        }
        // 解密
        String password =
                doDecrypt(String.valueOf(propertyValues.get("password")), decryptPropertiesMap);
        if (log.isDebugEnabled()) {
            log.debug("通过 {} 解密后的密码为 {}", this.getClass().getName(), password);
        }
        propertyValues.add("password", password);
        return password;
    }

    /**
     * 进行密码解密
     *
     * @param password 密码
     * @param decryptPropertiesMap 解密属性map
     * @return 解密后的密码
     */
    public abstract String doDecrypt(String password, Map<String, Object> decryptPropertiesMap);

    /**
     * 自定义的解密属性Key集合
     *
     * @return List<String>
     */
    public abstract List<String> propertiesKey();
}
