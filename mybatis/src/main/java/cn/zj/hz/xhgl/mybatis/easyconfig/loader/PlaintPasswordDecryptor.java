package cn.zj.hz.xhgl.mybatis.easyconfig.loader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 明文密码
 *
 * @since 2020/3/11 18:05
 * @author jyl
 */
public class PlaintPasswordDecryptor extends AbstractPasswordDecryptor {

    @Override
    public String doDecrypt(String password, Map<String, Object> decryptPropertiesMap) {
        return password;
    }

    @Override
    public List<String> propertiesKey() {
        return Collections.emptyList();
    }
}
