package com.wei.wreader.utils.comm;

import org.codehaus.janino.SimpleCompiler;

import java.io.StringReader;
import java.lang.reflect.Method;

/**
 * <strong>动态代码执行器</strong>
 *
 * <pre>
 * 格式:
 * {@code
 *     <java>
 *         <package_import>导入的包</package_import>
 *         <code>要执行的代码</code>
 *     </java>
 * }
 * </pre>
 *
 * @author weizhanjie
 */
public class DynamicCodeExecutor {

    /**
     * 执行动态代码
     *
     * @param code           动态代码
     * @param methodName     要执行的方法名
     * @param parameterTypes 方法参数类型数组
     * @param parameters     方法参数数组
     * @return 方法执行结果
     * @throws Exception 执行异常
     */
    public static Object executeMethod(String code, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Exception {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be blank.");
        }

        if (!code.contains("<java>") && !code.contains("</java>")) {
            throw new IllegalArgumentException("Code must contain <java> and </java>.");
        }

        // 从给定的配置字符串中提取出包导入语句
        String packageImport = extractPackageImport(code);
        // 从给定的配置字符串中提取出实际代码内容
        String codeCall = extractCodeCall(code);

        String classTemplate = packageImport +
                "public class DynamicCode {\n" +
                "    public DynamicCode() {}\n" +
                "    " + codeCall + "\n" +
                "}";

        // 加载指定的类加载器
        ClassLoader classLoader = DynamicCodeExecutor.class.getClassLoader();
        // 创建一个SimpleCompiler实例，并设置类加载器
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setParentClassLoader(classLoader);
        compiler.cook(new StringReader(classTemplate));

        // 加载并运行生成的类
        Class<?> clazz = compiler.getClassLoader().loadClass("DynamicCode");
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 调用指定的方法
        Method method = clazz.getMethod(methodName, parameterTypes);
        return method.invoke(instance, parameters);
    }

    /**
     * 从给定的配置字符串中提取出 {@code <code>} 和 {@code </code>} 标签之间的实际内容
     *
     * @param configStr 包含代码配置的完整字符串
     * @return {@code <code>} 和 {@code </code>} 标签之间的字符串
     */
    private static String extractCodeCall(String configStr) {
        return configStr.substring(configStr.indexOf("<code>") + "<code>".length(), configStr.indexOf("</code>"));
    }

    /**
     * 从给定的配置字符串中提取出 {@code <package_import>} 和 {@code </package_import>} 标签的内容
     *
     * @param configStr
     */
    private static String extractPackageImport(String configStr) {
        if (!configStr.contains("<package_import>") || !configStr.contains("</package_import>")) {
            return "";
        }
        return configStr.substring(configStr.indexOf("<package_import>") + "<package_import>".length(), configStr.indexOf("</package_import>"));
    }

    public static void main(String[] args) {
        String code = """
                <java>
                	<package_import>
                	import java.util.*;import com.wei.wreader.utils.*;import com.jayway.jsonpath.*;
                	</package_import>
                <code>
                    public String execute(Map<String, Object> paramMap, Integer index, String bookId, String itemId) {
                        String dataJsonStr = (String) paramMap.get("dataJsonStr");
                        String menuListJsonStr = (String) paramMap.get("menuListJsonStr");
                        String contUrlSuffix = (String) StringUtil.jsonPathRead(menuListJsonStr, "$[" + index + "].contUrlSuffix");
                        if (!contUrlSuffix.contains("reqEncryptParam")) {
                           String freeContUrlPrefix = (String) StringUtil.jsonPathRead(dataJsonStr, "$.data.freeContUrlPrefix");
                           return freeContUrlPrefix + contUrlSuffix;
                        } else {
                           String shortContUrlPrefix = (String) StringUtil.jsonPathRead(dataJsonStr, "$.data.shortContUrlPrefix");
                           String shortContUrlSuffix = (String) StringUtil.jsonPathRead(menuListJsonStr, "$[" + index + "].shortContUrlSuffix");
                           return shortContUrlPrefix + shortContUrlSuffix;
                        }
                    }
                </code>
                </java>
                """;
        String code2 = """
                <java><package_import>import java.util.*;import com.wei.wreader.utils.*;</package_import><code>public String execute(String bookId){String encryptKey = "37e81a9d8f02596e1b895d07c171d5c9";String user_id="8000000";long timestamp=System.currentTimeMillis()/1000;String param=bookId+timestamp+user_id+encryptKey;String md5 = EncryptUtils.md5(param);String url = "https://ocean.shuqireader.com/api/bcspub/qswebapi/book/chapterlist?_=&bookId=" + bookId + "&user_id=8000000&sign=" + md5 + "&timestamp=" + timestamp;return url;} </code></java>
                """;

        String json = """
                {"state":"200","message":"success","data":{"bookName":"混沌剑帝","authorName":"叶擎苍","chapterNum":"806","imgUrl":"http://img-tailor.11222.cn/bcv/big/202408161158204635_sa.jpg","hide":false,"coverIsOpen":true,"readIsOpen":true,"buyIsOpen":false,"anyUpTime":"1732275576","chapterDelTime":0,"wordCount":"186.01","updateType":2,"state":"1","lastInsTime":"1723799633","payMode":"3","contentUrl":"http://content.shuqireader.com/sapi/chapter/content/?8f4e32073308a73c159557f6d27cc3cc_8000000_16","freeContUrlPrefix":"http://c13.shuqireader.com/qswebapi/chapter/contentfree/","chargeContUrlPrefix":"https://ocean.shuqireader.com/api/bcspub/qswebapi/chapter/contentcharge/","shortContUrlPrefix":"http://c13.shuqireader.com/sapi/chapter/contentshort/","authorWordsUrlPrefix":"http://c13.shuqireader.com/sapi/chapter/authorwords/","intro":"","lastBuyTime":"0","bookId":"9013482","anyBuy":false,"readFeatureOpt":"0","iosReadFeatureOpt":"0","ttsSpeaker":{},"chapterList":[{"volumeId":"1","volumeName":"正文","volumeOrder":"1","volumeList":[{"chapterId":"2504139","chapterName":"第1章 宁无缺","payStatus":"0","chapterPrice":"0","wordCount":"3440","chapterUpdateTime":"1723799499","shortContUrlSuffix":"?bookId=9013482&chapterId=2504139&ut=1723799499&ver=1&aut=1732275576&sign=24743a0f5ac90d82482530abf58cc8a2","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504139&ut=1723799499&num=1&ver=1&aut=1732275576&sign=b6ed449fe4abcadbdc1bb9cc3223d2f8","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"1","isBuy":false,"isFreeRead":true},{"chapterId":"2504140","chapterName":"第2章 混沌剑体","payStatus":"0","chapterPrice":"0","wordCount":"2687","chapterUpdateTime":"1723799499","shortContUrlSuffix":"?bookId=9013482&chapterId=2504140&ut=1723799499&ver=1&aut=1732275576&sign=34b4958eef89ce7d85028347e52bf110","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504140&ut=1723799499&num=1&ver=1&aut=1732275576&sign=fc2175eeca9a8ca31427a0a1542db658","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"2","isBuy":false,"isFreeRead":true},{"chapterId":"2504141","chapterName":"第3章 你不配！","payStatus":"0","chapterPrice":"0","wordCount":"2341","chapterUpdateTime":"1729392196","shortContUrlSuffix":"?bookId=9013482&chapterId=2504141&ut=1729392196&ver=1&aut=1732275576&sign=58c3facccd2bcaadde2f45fcf8f494d0","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504141&ut=1729392196&num=1&ver=1&aut=1732275576&sign=d536c0a1dc48bcbd67691f6dd60a0d49","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"3","isBuy":false,"isFreeRead":true},{"chapterId":"2504142","chapterName":"第4章 震怒","payStatus":"0","chapterPrice":"0","wordCount":"2820","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504142&ut=1723799500&ver=1&aut=1732275576&sign=3565399702f85c74c019c5549fe0ae3d","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504142&ut=1723799500&num=1&ver=1&aut=1732275576&sign=be1408dada64469bed2472699725cb30","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"4","isBuy":false,"isFreeRead":true},{"chapterId":"2504143","chapterName":"第5章 边境阻劫","payStatus":"0","chapterPrice":"0","wordCount":"2947","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504143&ut=1723799500&ver=1&aut=1732275576&sign=e571b5e309471ea2659dac7da8581170","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504143&ut=1723799500&num=1&ver=1&aut=1732275576&sign=7574561e1e817ec78ba3fd297e313590","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"5","isBuy":false,"isFreeRead":true},{"chapterId":"2504144","chapterName":"第6章 凭你们也配？","payStatus":"0","chapterPrice":"0","wordCount":"2998","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504144&ut=1723799500&ver=1&aut=1732275576&sign=b7a3dbb8511ce5d80ecd69779b2db863","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504144&ut=1723799500&num=1&ver=1&aut=1732275576&sign=5fe2a54017fc4e3ea7fe6321c0ffc96d","shelf":1,"dateOpen":"2024-08-16 17:11:40","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"6","isBuy":false,"isFreeRead":true},{"chapterId":"2504145","chapterName":"第7章 武道科举","payStatus":"0","chapterPrice":"0","wordCount":"3655","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504145&ut=1723799500&ver=1&aut=1732275576&sign=0d5888dcaaae61bf174974c04688e05a","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504145&ut=1723799500&num=1&ver=1&aut=1732275576&sign=0b151ce48bee36199950e85ff007b17d","shelf":1,"dateOpen":"2024-08-16 17:11:41","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"7","isBuy":false,"isFreeRead":true},{"chapterId":"2504146","chapterName":"第8章 你还有何话可说？","payStatus":"0","chapterPrice":"0","wordCount":"2733","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504146&ut=1723799500&ver=1&aut=1732275576&sign=57c783b13503515e2e331f4fedaf53a0","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504146&ut=1723799500&num=1&ver=1&aut=1732275576&sign=52dce8f775a4aeb201388b266c13c9af","shelf":1,"dateOpen":"2024-08-16 17:11:41","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"8","isBuy":false,"isFreeRead":true},{"chapterId":"2504147","chapterName":"第9章 命不久矣","payStatus":"0","chapterPrice":"0","wordCount":"2434","chapterUpdateTime":"1723799500","shortContUrlSuffix":"?bookId=9013482&chapterId=2504147&ut=1723799500&ver=1&aut=1732275576&sign=53ec4bb58ca186e475cff5e113371f52","oriPrice":0.0,"contUrlSuffix":"?bookId=9013482&chapterId=2504147&ut=1723799500&num=1&ver=1&aut=1732275576&sign=a472f113481ef94ca8f778baebf88444","shelf":1,"dateOpen":"2024-08-16 17:11:41","chapterLockDesc":null,"vipPriorityRead":false,"chapterOrdid":"9","isBuy":false,"isFreeRead":true}]}],"isFreeLimit":false,"isBuy":false}}
                """;

        try {
            Object result = executeMethod(code, "execute",
                    new Class[]{String.class, Integer.class, String.class, String.class},
                    new Object[]{json, 0, "53258", "2504146"});
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}



