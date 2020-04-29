package cn.zj.hz.xhgl.mybatis.easyconfig.loader;

import org.springframework.beans.MutablePropertyValues;

/**
 * 密码解密
 *
 * @since 2020/3/11 17:55
 * @author 金遥力
 */
public interface PasswordDecryptor {
    /**
     * 进行属性参数中密码的解密，并重新赋值
     *
     * @param propertyValues 属性
     * @return String 解密后的密码
     */
    public String decrypt(MutablePropertyValues propertyValues);
}
