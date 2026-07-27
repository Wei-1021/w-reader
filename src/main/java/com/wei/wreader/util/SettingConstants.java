package com.wei.wreader.util;

/**
 * 设置页面常量
 */
public final class SettingConstants {

    private SettingConstants() {
    }

    // ==================== 风格默认值 ====================
    public static final String STYLE_DEFAULT = "默认";

    // ==================== 自动阅读 ====================
    public static final float AUTO_READ_TIME_DEFAULT = 5f;

    // ==================== 自动滚屏 ====================
    public static final Integer[] AUTO_SCROLL_SPEED_OPTIONS = {0, 5, 10, 20, 30, 50, 70, 100, 150, 200};
    public static final String[] AUTO_SCROLL_SPEED_LABELS = {
        "0 (关闭)", "5%/s (极慢)", "10%/s (极慢)", "20%/s (慢速)", "30%/s (较慢)",
        "50%/s (适中)", "70%/s (较快)", "100%/s (快速)",
        "150%/s (很快)", "200%/s (极快)"
    };
    public static final int AUTO_SCROLL_SPEED_DEFAULT = 20;
    /** 滚屏速率选项默认索引 */
    public static final int AUTO_SCROLL_SPEED_DEFAULT_INDEX = 2;
    public static final int AUTO_SCROLL_SPEED_OFF = 0;

    // ==================== 自动滚屏帧率 ====================
    public static final Integer[] AUTO_SCROLL_FPS_OPTIONS = {15, 24, 30, 60};
    public static final String[] AUTO_SCROLL_FPS_LABELS = {
        "15 fps", "24 fps", "30 fps", "60 fps"
    };
    public static final int AUTO_SCROLL_FPS_DEFAULT = 60;
    /** 滚屏帧率选项默认索引 */
    public static final int AUTO_SCROLL_FPS_DEFAULT_INDEX = 3;

    // ==================== 主图标风格 ====================
    public static final int ICON_STYLE_DEFAULT = 1;
    public static final int ICON_STYLE_LIGHT = 2;
    public static final String[] ICON_STYLE_NAMES = {"默认", "浅色"};
    public static final int[] ICON_STYLE_VALUES = {ICON_STYLE_DEFAULT, ICON_STYLE_LIGHT};

    // ==================== 编辑器提示窗口尺寸 ====================
    public static final Integer[] EDITOR_HINT_WIDTHS = {100, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800};
    public static final Integer[] EDITOR_HINT_HEIGHTS = {100, 150, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800};

    // ==================== 语速选项 ====================
    public static final Float[] RATE_OPTIONS = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};

    // ==================== 音量选项 ====================
    public static final Integer[] VOLUME_OPTIONS = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    // ==================== 音色描述预设 ====================
    public static final String[][] VOICE_DESC_PRESETS = {
        {"温柔女声 - 适合言情/治愈", "年轻女性，声音温柔甜美，语速适中偏慢，语调柔和亲切，像在耳边轻声细语，适合阅读温馨治愈的故事"},
        {"磁性男声 - 适合悬疑/历史", "中年男性，声音低沉磁性，语速沉稳有力，语调沉着冷静，像深夜电台主播，适合阅读悬疑推理或历史故事"},
        {"活泼少女 - 适合校园/奇幻", "年轻女性，声音清脆活泼，语速稍快，语调明快上扬，充满青春活力，适合阅读校园青春或奇幻冒险故事"},
        {"沉稳大叔 - 适合武侠/军事", "中年男性，声音浑厚沉稳，语速平稳，语调刚毅有力，像说书先生，适合阅读武侠小说或军事题材"},
        {"知性女声 - 适合文学/散文", "青年女性，声音清亮知性，语速适中，语调优雅从容，像专业播音员，适合阅读文学名著或散文诗歌"},
        {"少年音 - 适合玄幻/修仙", "青年男性，声音清朗少年感，语速可快可慢，语调富有变化，充满朝气，适合阅读玄幻修仙小说"},
        {"老年叙事 - 适合传记/历史", "老年男性，声音沧桑醇厚，语速缓慢，语调平和深远，像老爷爷讲故事，适合阅读传记或历史小说"},
        {"俏皮女声 - 适合喜剧/轻小说", "年轻女性，声音灵动俏皮，语速轻快，语调活泼有趣，带点小幽默，适合阅读轻松搞笑的故事"}
    };

    // ==================== 风格指令预设 ====================
    public static final String[][] VOICE_STYLE_PRESETS = {
        {"温柔大姐姐", "声音温暖明亮，像午后窗边晒着太阳织毛衣的大姐姐，语调柔和，带着包容和耐心，每句话都让人觉得被照顾着。"},
        {"神秘占卜师", "声音低缓而富有穿透力，像深夜摇曳烛光后慢慢翻开塔罗牌的人，语速不疾不徐，每个字都像藏着天机，耐人寻味。"},
        {"热血解说员", "声音高亢激昂，像世界杯决赛最后十秒的现场解说，节奏极快，气息饱满，越说越兴奋，听着就让人肾上腺素飙升。"},
        {"沧桑老前辈", "声音低沉沙哑一点，像个历经沧桑的老前辈在讲述传奇人物。语气里带点由衷的敬佩，娓娓道来。"},
        {"江南说书人", "声音清亮圆润，像苏州茶馆里拍响醒木的老先生，字正腔圆中带着几分悠然闲适，抑扬顿挫间把故事讲得活灵活现。"},
        {"冷面导师", "声音清冷克制，像深夜实验室里对着论文逐行批注的教授，语气平静而精准，不带多余情绪，但每个断句都透着不容置疑的权威感。"},
        {"邻家少年", "声音青涩透亮，像放学路上踩着落叶跟你聊天的同桌，语气随意自然，偶尔冒出一点俏皮和小得意，满是青春的气息。"},
        {"电台深夜DJ", "声音慵懒而富有磁性，像凌晨两点城市电台里缓缓开口的人，语调平缓舒展，带着一点若有若无的倦意，让人不自觉放松下来。"},
        {"江湖女侠", "声音干脆利落，像荒野客栈里拔剑饮酒的女子，语速果断不拖沓，字句间透着洒脱和豪气，又藏着几分不羁的柔情。"},
    };


    // ==================== 预设对话框 ====================
    public static final String PRESET_DIALOG_TITLE = "预设";
    public static final String PRESET_DIALOG_HINT = "选择预设";
    public static final String PRESET_DIALOG_TIP = "提示：选择后点击确定将覆盖当前";
    public static final String PRESET_DIALOG_DIMENSION_KEY = "WReader.VoiceDescPreset";
    public static final float PRESET_SPLITTER_PROPORTION = 0.4f;
    public static final int PRESET_DIALOG_WIDTH = 550;
    public static final int PRESET_DIALOG_HEIGHT = 350;
    public static final int PRESET_PREVIEW_ROWS = 4;
    public static final int PRESET_PANEL_PADDING = 10;
    public static final int PRESET_HINT_BOTTOM_PADDING = 8;
    public static final int PRESET_TIP_TOP_PADDING = 8;
    public static final int PRESET_PREVIEW_PADDING = 8;

    // ==================== 音色描述提示 ====================
    public static final String VOICE_DESC_HINT = "提示：描述你想要的语音音色，如「温柔女声，语速适中」。也可点击右侧「预设」按钮选择预设描述。";

    // ==================== 分割器 ====================
    public static final String TTS_SPLITTER_KEY = "w-reader.tts.splitter";

    // ==================== 边框标题 ====================
    public static final String BORDER_TITLE_GENERAL = "general";
    public static final String BORDER_TITLE_STATUS_BAR_FONT = "Status Bar Font";
    public static final String BORDER_TITLE_AUDIO_MANAGE = "Audio Manage";

    // ==================== 联系方式 ====================
    public static final String CONTACT_TITLE = "联系方式";
    public static final String CONTACT_HTML = """
            <h3>如果您有任何问题或好的建议，请通过以下方式联系：</h3>
            <span style="color: #589DF6;">Email: 1075542448@qq.com</span><br>
            <span style="color: #589DF6;">QQ: 1075542448</span><br>
            <span style="color: #589DF6;">QQ群: 1060150904</span><br>
            """;
}