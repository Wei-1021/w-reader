package org.dreamwork.tools.tts;

import java.util.*;
import java.util.stream.Collectors;

public enum VoiceRole {
    ///////////////////////////////  中文    ///////////////////////////////////
    ///////////////////////////////  妹子们  ////////////////////////////////////
    /** 晓北 辽宁 东北大妹子 */
    Xiaobei ("zh-CN-liaoning-XiaobeiNeural", "female", "zh-CN-liaoning", "晓北"),

    /** 晓妮 陕西妹子 */
    Xiaoni ("zh-CN-shaanxi-XiaoniNeural", "female", "zh-CN-shaanxi", "晓妮"),

    /** 晓晓 活泼、温暖的声音，具有多种场景风格和情感 */
    Xiaoxiao ("zh-CN-XiaoxiaoNeural", "female", "zh-CN", "晓晓"),

    /** 晓萱 自信、有能力的声音，具有丰富的角色扮演和情感，适合音频书籍 */
    Xiaoxuan ("zh-CN-XiaoxuanNeural", "female", "zh-CN", "晓萱"),

    /** 晓伊 */ // <-- default
    Xiaoyi ("zh-CN-XiaoyiNeural", "female", "zh-CN", "晓伊"),

    /** 曉佳(HiuGaai),粤语female声 */
    HiuGaai ("zh-HK-HiuGaaiNeural", "female", "zh-HK", "曉佳"),

    /** 曉曼(HiuMaan),粤语female声 */
    HiuMaan ("zh-HK-HiuMaanNeural", "female", "zh-HK", "曉曼"),

    /**
     * 曉臻(HsiaoChen),湾湾female声
     */
    HsiaoChen ("zh-TW-HsiaoChenNeural", "female", "zh-TW", "曉臻"),

    /** 曉雨(HsiaoYu),湾湾female声 */
    HsiaoYu ("zh-TW-HsiaoYuNeural", "female", "zh-TW", "曉雨"),

    ////////////////////////////////// 汉子们 //////////////////////////////////////
    /** 云健 适合影视和体育解说 */
    Yunjian ("zh-CN-YunjianNeural", "male", "zh-CN", "云健"),

    /** 云希 活泼、阳光的声音，具有丰富的情感，可用于许多对话场景 */
    Yunxi ("zh-CN-YunxiNeural", "male", "zh-CN", "云希"),

    /** 云夏 少年年male声 */
    Yunxia ("zh-CN-YunxiaNeural", "male", "zh-CN", "云夏"),

    /** 云扬 专业、流利的声音，具有多种场景风格 */
    Yunyang ("zh-CN-YunyangNeural", "male", "zh-CN", "云扬"),

    /** 雲龍(WanLung),粤语male声 */
    WanLung ("zh-HK-WanLungNeural", "male", "zh-HK", "雲龍"),

    /** 雲哲(YunJhe),湾湾male声 */
    YunJhe ("zh-TW-YunJheNeural", "male", "zh-TW", "雲哲"),

    /////////////////////////////////////////// 外语 ///////////////////////////////////////
    ///////////////////////////////  妹子们  ////////////////////////////////////
    Adri ("af-ZA-AdriNeural", "female", "af-ZA", "Adri"),
    Mekdes ("am-ET-MekdesNeural", "female", "am-ET", "Mekdes"),
    Fatima ("ar-AE-FatimaNeural", "female", "ar-AE", "Fatima"),
    Laila ("ar-BH-LailaNeural", "female", "ar-BH", "Laila"),
    Amina ("ar-DZ-AminaNeural", "female", "ar-DZ", "Amina"),
    Salma ("ar-EG-SalmaNeural", "female", "ar-EG", "Salma"),
    Rana ("ar-IQ-RanaNeural", "female", "ar-IQ", "Rana"),
    Sana ("ar-JO-SanaNeural", "female", "ar-JO", "Sana"),
    Noura ("ar-KW-NouraNeural", "female", "ar-KW", "Noura"),
    Layla ("ar-LB-LaylaNeural", "female", "ar-LB", "Layla"),
    Iman ("ar-LY-ImanNeural", "female", "ar-LY", "Iman"),
    Mouna ("ar-MA-MounaNeural", "female", "ar-MA", "Mouna"),
    Aysha ("ar-OM-AyshaNeural", "female", "ar-OM", "Aysha"),
    Amal ("ar-QA-AmalNeural", "female", "ar-QA", "Amal"),
    Zariyah ("ar-SA-ZariyahNeural", "female", "ar-SA", "Zariyah"),
    Amany ("ar-SY-AmanyNeural", "female", "ar-SY", "Amany"),
    Reem ("ar-TN-ReemNeural", "female", "ar-TN", "Reem"),
    Maryam ("ar-YE-MaryamNeural", "female", "ar-YE", "Maryam"),
    Banu ("az-AZ-BanuNeural", "female", "az-AZ", "Banu"),
    Kalina ("bg-BG-KalinaNeural", "female", "bg-BG", "Kalina"),
    Nabanita ("bn-BD-NabanitaNeural", "female", "bn-BD", "Nabanita"),
    Tanishaa ("bn-IN-TanishaaNeural", "female", "bn-IN", "Tanishaa"),
    Vesna ("bs-BA-VesnaNeural", "female", "bs-BA", "Vesna"),
    Joana ("ca-ES-JoanaNeural", "female", "ca-ES", "Joana"),
    Vlasta ("cs-CZ-VlastaNeural", "female", "cs-CZ", "Vlasta"),
    Nia ("cy-GB-NiaNeural", "female", "cy-GB", "Nia"),
    Christel ("da-DK-ChristelNeural", "female", "da-DK", "Christel"),
    Ingrid ("de-AT-IngridNeural", "female", "de-AT", "Ingrid"),
    Leni ("de-CH-LeniNeural", "female", "de-CH", "Leni"),
    Amala ("de-DE-AmalaNeural", "female", "de-DE", "Amala"),
    Katja ("de-DE-KatjaNeural", "female", "de-DE", "Katja"),
    SeraphinaMultilingual ("de-DE-SeraphinaMultilingualNeural", "female", "de-DE", "SeraphinaMultilingual"),
    Athina ("el-GR-AthinaNeural", "female", "el-GR", "Athina"),
    Natasha ("en-AU-NatashaNeural", "female", "en-AU", "Natasha"),
    Clara ("en-CA-ClaraNeural", "female", "en-CA", "Clara"),
    Libby ("en-GB-LibbyNeural", "female", "en-GB", "Libby"),
    Maisie ("en-GB-MaisieNeural", "female", "en-GB", "Maisie"),
    Sonia ("en-GB-SoniaNeural", "female", "en-GB", "Sonia"),
    Yan ("en-HK-YanNeural", "female", "en-HK", "Yan"),
    Emily ("en-IE-EmilyNeural", "female", "en-IE", "Emily"),
    NeerjaExpressive ("en-IN-NeerjaExpressiveNeural", "female", "en-IN", "NeerjaExpressive"),
    Neerja ("en-IN-NeerjaNeural", "female", "en-IN", "Neerja"),
    Asilia ("en-KE-AsiliaNeural", "female", "en-KE", "Asilia"),
    Ezinne ("en-NG-EzinneNeural", "female", "en-NG", "Ezinne"),
    Molly ("en-NZ-MollyNeural", "female", "en-NZ", "Molly"),
    Rosa ("en-PH-RosaNeural", "female", "en-PH", "Rosa"),
    Luna ("en-SG-LunaNeural", "female", "en-SG", "Luna"),
    Imani ("en-TZ-ImaniNeural", "female", "en-TZ", "Imani"),
    Ana ("en-US-AnaNeural", "female", "en-US", "Ana"),
    Aria ("en-US-AriaNeural", "female", "en-US", "Aria"),
    AvaMultilingual ("en-US-AvaMultilingualNeural", "female", "en-US", "AvaMultilingual"),
    Ava ("en-US-AvaNeural", "female", "en-US", "Ava"),
    EmmaMultilingual ("en-US-EmmaMultilingualNeural", "female", "en-US", "EmmaMultilingual"),
    Emma ("en-US-EmmaNeural", "female", "en-US", "Emma"),
    Jenny ("en-US-JennyNeural", "female", "en-US", "Jenny"),
    Michelle ("en-US-MichelleNeural", "female", "en-US", "Michelle"),
    Leah ("en-ZA-LeahNeural", "female", "en-ZA", "Leah"),
    Elena ("es-AR-ElenaNeural", "female", "es-AR", "Elena"),
    Sofia ("es-BO-SofiaNeural", "female", "es-BO", "Sofia"),
    Catalina ("es-CL-CatalinaNeural", "female", "es-CL", "Catalina"),
    Salome ("es-CO-SalomeNeural", "female", "es-CO", "Salome"),
    Maria ("es-CR-MariaNeural", "female", "es-CR", "Maria"),
    Belkys ("es-CU-BelkysNeural", "female", "es-CU", "Belkys"),
    Ramona ("es-DO-RamonaNeural", "female", "es-DO", "Ramona"),
    Andrea ("es-EC-AndreaNeural", "female", "es-EC", "Andrea"),
    Elvira ("es-ES-ElviraNeural", "female", "es-ES", "Elvira"),
    Ximena ("es-ES-XimenaNeural", "female", "es-ES", "Ximena"),
    Teresa ("es-GQ-TeresaNeural", "female", "es-GQ", "Teresa"),
    Marta ("es-GT-MartaNeural", "female", "es-GT", "Marta"),
    Karla ("es-HN-KarlaNeural", "female", "es-HN", "Karla"),
    Dalia ("es-MX-DaliaNeural", "female", "es-MX", "Dalia"),
    Yolanda ("es-NI-YolandaNeural", "female", "es-NI", "Yolanda"),
    Margarita ("es-PA-MargaritaNeural", "female", "es-PA", "Margarita"),
    Camila ("es-PE-CamilaNeural", "female", "es-PE", "Camila"),
    Karina ("es-PR-KarinaNeural", "female", "es-PR", "Karina"),
    Tania ("es-PY-TaniaNeural", "female", "es-PY", "Tania"),
    Lorena ("es-SV-LorenaNeural", "female", "es-SV", "Lorena"),
    Paloma ("es-US-PalomaNeural", "female", "es-US", "Paloma"),
    Valentina ("es-UY-ValentinaNeural", "female", "es-UY", "Valentina"),
    Paola ("es-VE-PaolaNeural", "female", "es-VE", "Paola"),
    Anu ("et-EE-AnuNeural", "female", "et-EE", "Anu"),
    Dilara ("fa-IR-DilaraNeural", "female", "fa-IR", "Dilara"),
    Noora ("fi-FI-NooraNeural", "female", "fi-FI", "Noora"),
    Charline ("fr-BE-CharlineNeural", "female", "fr-BE", "Charline"),
    Sylvie ("fr-CA-SylvieNeural", "female", "fr-CA", "Sylvie"),
    Ariane ("fr-CH-ArianeNeural", "female", "fr-CH", "Ariane"),
    Denise ("fr-FR-DeniseNeural", "female", "fr-FR", "Denise"),
    Eloise ("fr-FR-EloiseNeural", "female", "fr-FR", "Eloise"),
    VivienneMultilingual ("fr-FR-VivienneMultilingualNeural", "female", "fr-FR", "VivienneMultilingual"),
    Orla ("ga-IE-OrlaNeural", "female", "ga-IE", "Orla"),
    Sabela ("gl-ES-SabelaNeural", "female", "gl-ES", "Sabela"),
    Dhwani ("gu-IN-DhwaniNeural", "female", "gu-IN", "Dhwani"),
    Hila ("he-IL-HilaNeural", "female", "he-IL", "Hila"),
    Swara ("hi-IN-SwaraNeural", "female", "hi-IN", "Swara"),
    Gabrijela ("hr-HR-GabrijelaNeural", "female", "hr-HR", "Gabrijela"),
    Noemi ("hu-HU-NoemiNeural", "female", "hu-HU", "Noemi"),
    Gadis ("id-ID-GadisNeural", "female", "id-ID", "Gadis"),
    Gudrun ("is-IS-GudrunNeural", "female", "is-IS", "Gudrun"),
    Elsa ("it-IT-ElsaNeural", "female", "it-IT", "Elsa"),
    Isabella ("it-IT-IsabellaNeural", "female", "it-IT", "Isabella"),
    Nanami ("ja-JP-NanamiNeural", "female", "ja-JP", "Nanami"),
    Siti ("jv-ID-SitiNeural", "female", "jv-ID", "Siti"),
    Eka ("ka-GE-EkaNeural", "female", "ka-GE", "Eka"),
    Aigul ("kk-KZ-AigulNeural", "female", "kk-KZ", "Aigul"),
    Sreymom ("km-KH-SreymomNeural", "female", "km-KH", "Sreymom"),
    Sapna ("kn-IN-SapnaNeural", "female", "kn-IN", "Sapna"),
    SunHi ("ko-KR-SunHiNeural", "female", "ko-KR", "SunHi"),
    Keomany ("lo-LA-KeomanyNeural", "female", "lo-LA", "Keomany"),
    Ona ("lt-LT-OnaNeural", "female", "lt-LT", "Ona"),
    Everita ("lv-LV-EveritaNeural", "female", "lv-LV", "Everita"),
    Marija ("mk-MK-MarijaNeural", "female", "mk-MK", "Marija"),
    Sobhana ("ml-IN-SobhanaNeural", "female", "ml-IN", "Sobhana"),
    Yesui ("mn-MN-YesuiNeural", "female", "mn-MN", "Yesui"),
    Aarohi ("mr-IN-AarohiNeural", "female", "mr-IN", "Aarohi"),
    Yasmin ("ms-MY-YasminNeural", "female", "ms-MY", "Yasmin"),
    Grace ("mt-MT-GraceNeural", "female", "mt-MT", "Grace"),
    Nilar ("my-MM-NilarNeural", "female", "my-MM", "Nilar"),
    Pernille ("nb-NO-PernilleNeural", "female", "nb-NO", "Pernille"),
    Hemkala ("ne-NP-HemkalaNeural", "female", "ne-NP", "Hemkala"),
    Dena ("nl-BE-DenaNeural", "female", "nl-BE", "Dena"),
    Colette ("nl-NL-ColetteNeural", "female", "nl-NL", "Colette"),
    Fenna ("nl-NL-FennaNeural", "female", "nl-NL", "Fenna"),
    Zofia ("pl-PL-ZofiaNeural", "female", "pl-PL", "Zofia"),
    Latifa ("ps-AF-LatifaNeural", "female", "ps-AF", "Latifa"),
    Francisca ("pt-BR-FranciscaNeural", "female", "pt-BR", "Francisca"),
    Thalita ("pt-BR-ThalitaNeural", "female", "pt-BR", "Thalita"),
    Raquel ("pt-PT-RaquelNeural", "female", "pt-PT", "Raquel"),
    Alina ("ro-RO-AlinaNeural", "female", "ro-RO", "Alina"),
    Svetlana ("ru-RU-SvetlanaNeural", "female", "ru-RU", "Svetlana"),
    Thilini ("si-LK-ThiliniNeural", "female", "si-LK", "Thilini"),
    Viktoria ("sk-SK-ViktoriaNeural", "female", "sk-SK", "Viktoria"),
    Petra ("sl-SI-PetraNeural", "female", "sl-SI", "Petra"),
    Ubax ("so-SO-UbaxNeural", "female", "so-SO", "Ubax"),
    Anila ("sq-AL-AnilaNeural", "female", "sq-AL", "Anila"),
    Sophie ("sr-RS-SophieNeural", "female", "sr-RS", "Sophie"),
    Tuti ("su-ID-TutiNeural", "female", "su-ID", "Tuti"),
    Sofie ("sv-SE-SofieNeural", "female", "sv-SE", "Sofie"),
    Zuri ("sw-KE-ZuriNeural", "female", "sw-KE", "Zuri"),
    Rehema ("sw-TZ-RehemaNeural", "female", "sw-TZ", "Rehema"),
    Pallavi ("ta-IN-PallaviNeural", "female", "ta-IN", "Pallavi"),
    Saranya ("ta-LK-SaranyaNeural", "female", "ta-LK", "Saranya"),
    Kani ("ta-MY-KaniNeural", "female", "ta-MY", "Kani"),
    Venba ("ta-SG-VenbaNeural", "female", "ta-SG", "Venba"),
    Shruti ("te-IN-ShrutiNeural", "female", "te-IN", "Shruti"),
    Premwadee ("th-TH-PremwadeeNeural", "female", "th-TH", "Premwadee"),
    Emel ("tr-TR-EmelNeural", "female", "tr-TR", "Emel"),
    Polina ("uk-UA-PolinaNeural", "female", "uk-UA", "Polina"),
    Gul ("ur-IN-GulNeural", "female", "ur-IN", "Gul"),
    Uzma ("ur-PK-UzmaNeural", "female", "ur-PK", "Uzma"),
    Madina ("uz-UZ-MadinaNeural", "female", "uz-UZ", "Madina"),
    HoaiMy ("vi-VN-HoaiMyNeural", "female", "vi-VN", "HoaiMy"),
    Thando ("zu-ZA-ThandoNeural", "female", "zu-ZA", "Thando"),
    ////////////////////////////////// 汉子们 //////////////////////////////////////
    Willem ("af-ZA-WillemNeural", "male", "af-ZA", "Willem"),
    Ameha ("am-ET-AmehaNeural", "male", "am-ET", "Ameha"),
    Hamdan ("ar-AE-HamdanNeural", "male", "ar-AE", "Hamdan"),
    Ali ("ar-BH-AliNeural", "male", "ar-BH", "Ali"),
    Ismael ("ar-DZ-IsmaelNeural", "male", "ar-DZ", "Ismael"),
    Shakir ("ar-EG-ShakirNeural", "male", "ar-EG", "Shakir"),
    Bassel ("ar-IQ-BasselNeural", "male", "ar-IQ", "Bassel"),
    Taim ("ar-JO-TaimNeural", "male", "ar-JO", "Taim"),
    Fahed ("ar-KW-FahedNeural", "male", "ar-KW", "Fahed"),
    Rami ("ar-LB-RamiNeural", "male", "ar-LB", "Rami"),
    Omar ("ar-LY-OmarNeural", "male", "ar-LY", "Omar"),
    Jamal ("ar-MA-JamalNeural", "male", "ar-MA", "Jamal"),
    Abdullah ("ar-OM-AbdullahNeural", "male", "ar-OM", "Abdullah"),
    Moaz ("ar-QA-MoazNeural", "male", "ar-QA", "Moaz"),
    Hamed ("ar-SA-HamedNeural", "male", "ar-SA", "Hamed"),
    Laith ("ar-SY-LaithNeural", "male", "ar-SY", "Laith"),
    Hedi ("ar-TN-HediNeural", "male", "ar-TN", "Hedi"),
    Saleh ("ar-YE-SalehNeural", "male", "ar-YE", "Saleh"),
    Babek ("az-AZ-BabekNeural", "male", "az-AZ", "Babek"),
    Borislav ("bg-BG-BorislavNeural", "male", "bg-BG", "Borislav"),
    Pradeep ("bn-BD-PradeepNeural", "male", "bn-BD", "Pradeep"),
    Bashkar ("bn-IN-BashkarNeural", "male", "bn-IN", "Bashkar"),
    Goran ("bs-BA-GoranNeural", "male", "bs-BA", "Goran"),
    Enric ("ca-ES-EnricNeural", "male", "ca-ES", "Enric"),
    Antonin ("cs-CZ-AntoninNeural", "male", "cs-CZ", "Antonin"),
    Aled ("cy-GB-AledNeural", "male", "cy-GB", "Aled"),
    Jeppe ("da-DK-JeppeNeural", "male", "da-DK", "Jeppe"),
    Jonas ("de-AT-JonasNeural", "male", "de-AT", "Jonas"),
    Jan ("de-CH-JanNeural", "male", "de-CH", "Jan"),
    Conrad ("de-DE-ConradNeural", "male", "de-DE", "Conrad"),
    FlorianMultilingual ("de-DE-FlorianMultilingualNeural", "male", "de-DE", "FlorianMultilingual"),
    Killian ("de-DE-KillianNeural", "male", "de-DE", "Killian"),
    Nestoras ("el-GR-NestorasNeural", "male", "el-GR", "Nestoras"),
    William ("en-AU-WilliamNeural", "male", "en-AU", "William"),
    Liam ("en-CA-LiamNeural", "male", "en-CA", "Liam"),
    Ryan ("en-GB-RyanNeural", "male", "en-GB", "Ryan"),
    Thomas ("en-GB-ThomasNeural", "male", "en-GB", "Thomas"),
    Sam ("en-HK-SamNeural", "male", "en-HK", "Sam"),
    Connor ("en-IE-ConnorNeural", "male", "en-IE", "Connor"),
    Prabhat ("en-IN-PrabhatNeural", "male", "en-IN", "Prabhat"),
    Chilemba ("en-KE-ChilembaNeural", "male", "en-KE", "Chilemba"),
    Abeo ("en-NG-AbeoNeural", "male", "en-NG", "Abeo"),
    Mitchell ("en-NZ-MitchellNeural", "male", "en-NZ", "Mitchell"),
    James ("en-PH-JamesNeural", "male", "en-PH", "James"),
    Wayne ("en-SG-WayneNeural", "male", "en-SG", "Wayne"),
    Elimu ("en-TZ-ElimuNeural", "male", "en-TZ", "Elimu"),
    AndrewMultilingual ("en-US-AndrewMultilingualNeural", "male", "en-US", "AndrewMultilingual"),
    Andrew ("en-US-AndrewNeural", "male", "en-US", "Andrew"),
    BrianMultilingual ("en-US-BrianMultilingualNeural", "male", "en-US", "BrianMultilingual"),
    Brian ("en-US-BrianNeural", "male", "en-US", "Brian"),
    Christopher ("en-US-ChristopherNeural", "male", "en-US", "Christopher"),
    Eric ("en-US-EricNeural", "male", "en-US", "Eric"),
    Guy ("en-US-GuyNeural", "male", "en-US", "Guy"),
    Roger ("en-US-RogerNeural", "male", "en-US", "Roger"),
    Steffan ("en-US-SteffanNeural", "male", "en-US", "Steffan"),
    Luke ("en-ZA-LukeNeural", "male", "en-ZA", "Luke"),
    Tomas ("es-AR-TomasNeural", "male", "es-AR", "Tomas"),
    Marcelo ("es-BO-MarceloNeural", "male", "es-BO", "Marcelo"),
    Lorenzo ("es-CL-LorenzoNeural", "male", "es-CL", "Lorenzo"),
    Gonzalo ("es-CO-GonzaloNeural", "male", "es-CO", "Gonzalo"),
    Juan ("es-CR-JuanNeural", "male", "es-CR", "Juan"),
    Manuel ("es-CU-ManuelNeural", "male", "es-CU", "Manuel"),
    Emilio ("es-DO-EmilioNeural", "male", "es-DO", "Emilio"),
    Luis ("es-EC-LuisNeural", "male", "es-EC", "Luis"),
    Alvaro ("es-ES-AlvaroNeural", "male", "es-ES", "Alvaro"),
    Javier ("es-GQ-JavierNeural", "male", "es-GQ", "Javier"),
    Andres ("es-GT-AndresNeural", "male", "es-GT", "Andres"),
    Carlos ("es-HN-CarlosNeural", "male", "es-HN", "Carlos"),
    Jorge ("es-MX-JorgeNeural", "male", "es-MX", "Jorge"),
    Federico ("es-NI-FedericoNeural", "male", "es-NI", "Federico"),
    Roberto ("es-PA-RobertoNeural", "male", "es-PA", "Roberto"),
    Alex ("es-PE-AlexNeural", "male", "es-PE", "Alex"),
    Victor ("es-PR-VictorNeural", "male", "es-PR", "Victor"),
    Mario ("es-PY-MarioNeural", "male", "es-PY", "Mario"),
    Rodrigo ("es-SV-RodrigoNeural", "male", "es-SV", "Rodrigo"),
    Alonso ("es-US-AlonsoNeural", "male", "es-US", "Alonso"),
    Mateo ("es-UY-MateoNeural", "male", "es-UY", "Mateo"),
    Sebastian ("es-VE-SebastianNeural", "male", "es-VE", "Sebastian"),
    Kert ("et-EE-KertNeural", "male", "et-EE", "Kert"),
    Farid ("fa-IR-FaridNeural", "male", "fa-IR", "Farid"),
    Harri ("fi-FI-HarriNeural", "male", "fi-FI", "Harri"),
    Gerard ("fr-BE-GerardNeural", "male", "fr-BE", "Gerard"),
    Antoine ("fr-CA-AntoineNeural", "male", "fr-CA", "Antoine"),
    Jean ("fr-CA-JeanNeural", "male", "fr-CA", "Jean"),
    Thierry ("fr-CA-ThierryNeural", "male", "fr-CA", "Thierry"),
    Fabrice ("fr-CH-FabriceNeural", "male", "fr-CH", "Fabrice"),
    Henri ("fr-FR-HenriNeural", "male", "fr-FR", "Henri"),
    RemyMultilingual ("fr-FR-RemyMultilingualNeural", "male", "fr-FR", "RemyMultilingual"),
    Colm ("ga-IE-ColmNeural", "male", "ga-IE", "Colm"),
    Roi ("gl-ES-RoiNeural", "male", "gl-ES", "Roi"),
    Niranjan ("gu-IN-NiranjanNeural", "male", "gu-IN", "Niranjan"),
    Avri ("he-IL-AvriNeural", "male", "he-IL", "Avri"),
    Madhur ("hi-IN-MadhurNeural", "male", "hi-IN", "Madhur"),
    Srecko ("hr-HR-SreckoNeural", "male", "hr-HR", "Srecko"),
    Tamas ("hu-HU-TamasNeural", "male", "hu-HU", "Tamas"),
    Ardi ("id-ID-ArdiNeural", "male", "id-ID", "Ardi"),
    Gunnar ("is-IS-GunnarNeural", "male", "is-IS", "Gunnar"),
    Diego ("it-IT-DiegoNeural", "male", "it-IT", "Diego"),
    Giuseppe ("it-IT-GiuseppeNeural", "male", "it-IT", "Giuseppe"),
    Keita ("ja-JP-KeitaNeural", "male", "ja-JP", "Keita"),
    Dimas ("jv-ID-DimasNeural", "male", "jv-ID", "Dimas"),
    Giorgi ("ka-GE-GiorgiNeural", "male", "ka-GE", "Giorgi"),
    Daulet ("kk-KZ-DauletNeural", "male", "kk-KZ", "Daulet"),
    Piseth ("km-KH-PisethNeural", "male", "km-KH", "Piseth"),
    Gagan ("kn-IN-GaganNeural", "male", "kn-IN", "Gagan"),
    Hyunsu ("ko-KR-HyunsuNeural", "male", "ko-KR", "Hyunsu"),
    InJoon ("ko-KR-InJoonNeural", "male", "ko-KR", "InJoon"),
    Chanthavong ("lo-LA-ChanthavongNeural", "male", "lo-LA", "Chanthavong"),
    Leonas ("lt-LT-LeonasNeural", "male", "lt-LT", "Leonas"),
    Nils ("lv-LV-NilsNeural", "male", "lv-LV", "Nils"),
    Aleksandar ("mk-MK-AleksandarNeural", "male", "mk-MK", "Aleksandar"),
    Midhun ("ml-IN-MidhunNeural", "male", "ml-IN", "Midhun"),
    Bataa ("mn-MN-BataaNeural", "male", "mn-MN", "Bataa"),
    Manohar ("mr-IN-ManoharNeural", "male", "mr-IN", "Manohar"),
    Osman ("ms-MY-OsmanNeural", "male", "ms-MY", "Osman"),
    Joseph ("mt-MT-JosephNeural", "male", "mt-MT", "Joseph"),
    Thiha ("my-MM-ThihaNeural", "male", "my-MM", "Thiha"),
    Finn ("nb-NO-FinnNeural", "male", "nb-NO", "Finn"),
    Sagar ("ne-NP-SagarNeural", "male", "ne-NP", "Sagar"),
    Arnaud ("nl-BE-ArnaudNeural", "male", "nl-BE", "Arnaud"),
    Maarten ("nl-NL-MaartenNeural", "male", "nl-NL", "Maarten"),
    Marek ("pl-PL-MarekNeural", "male", "pl-PL", "Marek"),
    GulNawaz ("ps-AF-GulNawazNeural", "male", "ps-AF", "GulNawaz"),
    Antonio ("pt-BR-AntonioNeural", "male", "pt-BR", "Antonio"),
    Duarte ("pt-PT-DuarteNeural", "male", "pt-PT", "Duarte"),
    Emil ("ro-RO-EmilNeural", "male", "ro-RO", "Emil"),
    Dmitry ("ru-RU-DmitryNeural", "male", "ru-RU", "Dmitry"),
    Sameera ("si-LK-SameeraNeural", "male", "si-LK", "Sameera"),
    Lukas ("sk-SK-LukasNeural", "male", "sk-SK", "Lukas"),
    Rok ("sl-SI-RokNeural", "male", "sl-SI", "Rok"),
    Muuse ("so-SO-MuuseNeural", "male", "so-SO", "Muuse"),
    Ilir ("sq-AL-IlirNeural", "male", "sq-AL", "Ilir"),
    Nicholas ("sr-RS-NicholasNeural", "male", "sr-RS", "Nicholas"),
    Jajang ("su-ID-JajangNeural", "male", "su-ID", "Jajang"),
    Mattias ("sv-SE-MattiasNeural", "male", "sv-SE", "Mattias"),
    Rafiki ("sw-KE-RafikiNeural", "male", "sw-KE", "Rafiki"),
    Daudi ("sw-TZ-DaudiNeural", "male", "sw-TZ", "Daudi"),
    Valluvar ("ta-IN-ValluvarNeural", "male", "ta-IN", "Valluvar"),
    Kumar ("ta-LK-KumarNeural", "male", "ta-LK", "Kumar"),
    Surya ("ta-MY-SuryaNeural", "male", "ta-MY", "Surya"),
    Anbu ("ta-SG-AnbuNeural", "male", "ta-SG", "Anbu"),
    Mohan ("te-IN-MohanNeural", "male", "te-IN", "Mohan"),
    Niwat ("th-TH-NiwatNeural", "male", "th-TH", "Niwat"),
    Ahmet ("tr-TR-AhmetNeural", "male", "tr-TR", "Ahmet"),
    Ostap ("uk-UA-OstapNeural", "male", "uk-UA", "Ostap"),
    Salman ("ur-IN-SalmanNeural", "male", "ur-IN", "Salman"),
    Asad ("ur-PK-AsadNeural", "male", "ur-PK", "Asad"),
    Sardor ("uz-UZ-SardorNeural", "male", "uz-UZ", "Sardor"),
    NamMinh ("vi-VN-NamMinhNeural", "male", "vi-VN", "NamMinh"),
    Themba ("zu-ZA-ThembaNeural", "male", "zu-ZA", "Themba"),
    ;
    public final String shortName;
    public final String nickname;
    public final String gender;
    public final String locale;

    VoiceRole (String shortName, String gender, String locale, String nickname) {
        this.shortName = shortName;
        this.gender = gender;
        this.locale = locale;
        this.nickname = nickname;
    }

    /**
     * 根据指定的语言过滤
     * @param locale 指定的语言
     * @return 指定语言的语音列表
     */
    public static List<VoiceRole> byLocale (String locale) {
        final String lang = locale.toLowerCase ();
        return Arrays.stream (values ())
                .filter (v -> v.shortName.toLowerCase().startsWith (lang))
                .collect(Collectors.toList());
    }

    /**
     * 根据语音性别过滤
     * @param gender 指定的性别
     * @return 指定性别的语音列表
     */
    public static List<VoiceRole> byGender (String gender) {
        String g = gender.toLowerCase ();
        return Arrays.stream (values ()).filter (v -> v.gender.equals (g)).collect(Collectors.toList());
    }

    /**
     * 第一级按照语言分组，第二级按性别分组
     * @return 分组好的语音
     */
    public static Map<String, Map<String, List<VoiceRole>>> group () {
        Map<String, Map<String, List<VoiceRole>>> group = new HashMap<> ();
        Arrays.stream (values ()).forEach (v -> {
            Map<String, List<VoiceRole>> map = group.computeIfAbsent (v.locale, k -> new HashMap<> ());
            map.computeIfAbsent (v.gender, k -> new ArrayList<> ()).add (v);
        });
        return group;
    }
}