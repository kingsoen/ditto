package cn.zj.hz.xhgl.mybatis.easyconfig.loader;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.DES;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * des加密密码解密
 *
 * @since 2020/3/11 18:33
 * @author jyl
 */
public class DesPasswordDecryptor extends AbstractPasswordDecryptor {

    private static final String passwordCipherKey = "passwordCipherKey";

    @Override
    public String doDecrypt(String password, Map<String, Object> decryptPropertiesMap) {
        String cipherKey = (String) decryptPropertiesMap.get(passwordCipherKey);
        Assert.notNull(password, "DataSource password property must not be null");
        Assert.notNull(cipherKey, "DataSource passwordCipherKey property must not be null");
        DES des = SecureUtil.des(cipherKey.getBytes());
        return des.decryptStr(password, CharsetUtil.CHARSET_UTF_8);
    }

    @Override
    public List<String> propertiesKey() {
        return Collections.singletonList(passwordCipherKey);
    }
}
