package com.wei.wreader.utils.tts;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音风格
 *
 * @author weizhanjie
 */
public enum VoiceStyle {
    /**
     * 默认
     */
    style_default("default", "默认"),
    /**
     * 用兴奋和精力充沛的语气推广产品或服务
     */
    advertisement_upbeat("advertisement_upbeat", "广告"),
    /**
     * 以较高的音调和音量表达温暖而亲切的语气。 说话者处于吸引听众注意力的状态。 说话者的个性往往是讨喜的
     */
    affectionate("affectionate", "深情"),
    /**
     * 表达生气和厌恶的语气
     */
    angry("angry", "愤怒"),
    /**
     * 数字助理用的是热情而轻松的语气
     */
    assistant("assistant", "助理"),
    /**
     * 以沉着冷静的态度说话。 语气、音调和韵律与其他语音类型相比要统一得多
     */
    calm("calm", "平静"),
    /**
     * 表达轻松随意的语气
     */
    chat("chat", "聊天 - 休闲"),
    /**
     * 表达积极愉快的语气
     */
    cheerful("cheerful", "愉快"),
    /**
     * 以友好热情的语气为客户提供支持
     */
    customerservice("customerservice", "客户服务"),
    /**
     * 调低音调和音量来表达忧郁、沮丧的语气
     */
    depressed("depressed", "压抑"),
    /**
     * 表达轻蔑和抱怨的语气。 这种情绪的语音表现出不悦和蔑视
     */
    disgruntled("disgruntled", "不悦"),
    /**
     * 用一种轻松、感兴趣和信息丰富的风格讲述纪录片，适合配音纪录片、专家评论和类似内容
     */
    documentary_narration("documentary-narration", "纪录片"),
    /**
     * 在说话者感到不舒适时表达不确定、犹豫的语气
     */
    embarrassed("embarrassed", "尴尬"),
    /**
     * 表达关心和理解
     */
    empathetic("empathetic", "同情"),
    /**
     * 当你渴望别人拥有的东西时，表达一种钦佩的语气
     */
    envious("envious", "羡慕"),
    /**
     * 表达乐观和充满希望的语气。 似乎发生了一些美好的事情，说话人对此非常满意
     */
    excited("excited", "兴奋"),
    /**
     * 以较高的音调、较高的音量和较快的语速来表达恐惧、紧张的语气。 说话人处于紧张和不安的状态
     */
    fearful("fearful", "恐惧"),
    /**
     * 表达一种愉快、怡人且温暖的语气。 听起来很真诚且满怀关切
     */
    friendly("friendly", "友好"),
    /**
     * 以较低的音调和音量表达温和、礼貌和愉快的语气
     */
    gentle("gentle", "温和"),
    /**
     * 表达一种温暖且渴望的语气。 听起来像是会有好事发生在说话人身上
     */
    hopeful("hopeful", "希望"),
    /**
     * 以优美又带感伤的方式表达情感
     */
    lyrical("lyrical", "抒情"),
    /**
     * 以专业、客观的语气朗读内容
     */
    narration_professional("narration-professional", "专业"),
    /**
     * 为内容阅读表达一种舒缓而悦耳的语气
     */
    narration_relaxed("narration-relaxed", "旁白 - 放松"),
    /**
     * 以正式专业的语气叙述新闻
     */
    newscast("newscast", "新闻"),
    /**
     * 以通用、随意的语气发布一般新闻
     */
    newscast_casual("newscast-casual", "新闻 - 休闲"),
    /**
     * 以正式、自信和权威的语气发布新闻
     */
    newscast_formal("newscast-formal", "新闻 - 正式"),
    /**
     * 在读诗时表达出带情感和节奏的语气
     */
    poetry_reading("poetry-reading", "诗歌朗诵"),
    /**
     * 表达悲伤语气
     */
    sad("sad", "悲伤"),
    /**
     * 表达严肃和命令的语气。 说话者的声音通常比较僵硬，节奏也不那么轻松
     */
    serious("serious", "严肃"),
    /**
     * 表达严肃和命令的语气。 说话者的声音通常比较僵硬，节奏也不那么轻松
     */
    shouting("shouting", "喊话"),
    /**
     * 用轻松有趣的语气播报体育赛事
     */
    sports_commentary("sports_commentary", "体育解说"),
    /**
     * 用快速且充满活力的语气播报体育赛事精彩瞬间
     */
    sports_commentary_excited("sports_commentary_excited", "体育解说 - 兴奋"),
    /**
     * 说话非常柔和，发出的声音小且温柔
     */
    whispering("whispering", "语调小"),
    /**
     * 表达一种非常害怕的语气，语速快且声音颤抖。 听起来说话人处于不稳定的疯狂状态
     */
    terrified("terrified", "害怕"),
    /**
     * 表达一种冷淡无情的语气
     */
    unfriendly("unfriendly", "冷淡"),
    ;

    public final String value;
    public final String name;

    VoiceStyle(String value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static VoiceStyle getByValue(String value) {
        for (VoiceStyle voiceStyle : VoiceStyle.values()) {
            if (voiceStyle.value.equals(value)) {
                return voiceStyle;
            }
        }
        return style_default;
    }

    /**
     * 根据name获取枚举
     * @param name
     * @return
     */
    public static VoiceStyle getByName(String name) {
        for (VoiceStyle voiceStyle : VoiceStyle.values()) {
            if (voiceStyle.name.equals(name)) {
                return voiceStyle;
            }
        }
        return style_default;
    }

    /**
     * 获取所有value
     */
    public static List<String> getValues() {
        List<String> values = new ArrayList<>();
        for (VoiceStyle voiceStyle : VoiceStyle.values()) {
            values.add(voiceStyle.value);
        }
        return values;
    }

    /**
     * 获取所有name
     * @return
     */
    public static List<String> getNames() {
        List<String> values = new ArrayList<>();
        for (VoiceStyle voiceStyle : VoiceStyle.values()) {
            values.add(voiceStyle.name);
        }
        return values;
    }

}
