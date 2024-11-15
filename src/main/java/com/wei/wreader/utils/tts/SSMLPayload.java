package com.wei.wreader.utils.tts;

import com.wei.wreader.utils.tts.VoiceRole;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SSMLPayload implements Serializable {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String SSML_PATTERN =
            "X-RequestId:%s\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "X-Timestamp:%sZ\r\n" +
                    "Path:ssml\r\n" +
                    "\r\n" +
                    "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='%s'>\r\n" +
                    "<voice name='%s'><prosody pitch='+0Hz' rate='%s' volume='%s'>%s</prosody></voice></speak>";

    /**
     * 语音角色
     */
    public VoiceRole role;

    /**
     * 相对语速
     * <p>以相对数字表示：以充当默认值倍率的数字表示。 例如，如果值为 1，则原始速率不会变化。 如果值为 0.5，则速率为原始速率的一半。 如果值为 2，则速率为原始速率的 2 倍。
     * 以百分比表示：以“+”（可选）或“-”开头且后跟“%”的数字表示，指示相对变化。
     * 例如 <pre>
     * &lt;prosody rate="50%"&gt;some text&lt;/prosody&gt;</pre> 或 <pre>
     * &lt;prosody rate="-50%"&gt;some text&lt;/prosody&gt;</pre>
     */
    public String rate = "1";
    /**
     * 音量
     * <ul>
     *  <li>绝对值：以从 0.0 到 100.0（从最安静到最大声）的数字表示。 例如 75。 默认值为 100.0。</li>
     *  <li>相对值：
     * 以相对数字表示：以前面带有“+”或“-”的数字表示，指定音量的变化量。 例如 +10 或 -5.5。
     * 以百分比表示：以“+”（可选）或“-”开头且后跟“%”的数字表示，指示相对变化。例如<pre>
     * &lt;prosody volume="50%"&gt;some text&lt;/prosody&gt;</pre> 或 <pre>
     * &lt;prosody volume="+3%"&gt;some text&lt;/prosody&gt;</pre>
     * </li>
     * </ul>
     */
    public String volume = "100.0";

    public String content;

    SSMLPayload(VoiceRole role) {
        this.role = role;
    }

    SSMLPayload(VoiceRole role, String rate, String volume) {
        this.role = role;
        this.rate = rate;
        this.volume = volume;
    }

    @Override
    public String toString() {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format(
                SSML_PATTERN,
                uuid,
                dtf.format(ZonedDateTime.now()),
                role.locale,
                role.shortName,
                rate,
                volume,
                content
        );
    }
}