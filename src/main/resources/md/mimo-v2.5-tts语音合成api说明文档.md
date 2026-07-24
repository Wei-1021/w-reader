# 语音合成（MiMo-V2.5-TTS 系列）

语音合成（文本转语音）支持将输入的文本自动转换为自然流畅的语音输出。您可通过配置发音风格、音色等参数，生成自然生动的语音内容。

**核心能力**

- **预置音色开箱即用**：内置多种精品音色，无需额外配置即可快速使用。
- **音色设计与克隆**：支持通过文本描述设计音色，或基于音频样本复刻任意音色。
- **多样化发音风格**：支持语速、情绪、角色扮演、方言等多种风格控制，语音表达更生动自然。

`mimo-v2.5-tts` 低延迟流式输出功能已上线；当前流式接口恢复正常，可实时返回流式响应。

## 支持的模型列表

当前支持 MiMo-V2.5-TTS 系列的三种模型，模型列表如下：

| Model ID | 功能 | 音色 | 注意事项 |
| --- | --- | --- | --- |
| mimo-v2.5-tts | 使用预置精品音色进行语音合成 | 使用预置音色列表中的精品音色 | 支持唱歌模式，不支持音色设计与音色复刻 |
| mimo-v2.5-tts-voicedesign | 通过文本描述定制音色 | 通过文本描述自动生成音色，无需预置或音频样本 | 不支持唱歌模式、预置音色与音色复刻 |
| mimo-v2.5-tts-voiceclone | 基于音频样本复刻任意音色 | 通过音频样本精准复刻音色，实现任意声音的语音合成 | 不支持唱歌模式、预置音色与音色设计 |

## 准备工作

获取 API Key 等准备工作，请参考 [首次调用 API](https://mimo.mi.com/#/docs/quick-start/first-api-call)。

## 通用注意事项

**调用规则**

- 语音合成的目标文本需填写在 `role` 为 `assistant` 的消息中，不可放在 `user` 角色的消息内。
- `user` 角色的消息为可选参数，可以传入指令来调整语音合成的语气与风格，也可以是对话历史（消息内容不会出现在合成的语音中）。使用 `mimo-v2.5-tts-voicedesign` 模型时，为必填参数。
- 采用流式调用时，输出音频的格式请指定为 `pcm16`，以便拼接成完整音频。拼接示例可参考各章节的 Python 调用方式。

## 风格控制

模型的指令遵循能力足以 cover 以下这些复杂控制（一条自然语言指令即可生效）：

- **多风格切换**：同一角色在同一段语音内完成 *播报 → 低语 → 嘶吼* 的风格转场，过渡自然不突兀。
- **多情绪混合**：支持"压抑的愤怒"、"带着哽咽的笑意"、"温柔但疲惫"、"狂躁中的温柔"等复合情绪，而非只能选单一情绪。
- **多粒度控制**：从*段落级*（整体基调）→ *句子级*（节奏）→ *词级*（重音）→ *字粒度*（某一个字的哽咽、拖音、气音），都可在指令中指定。

我们目前提供两种控制方法：**自然语言控制** 和 **标签控制**。两种方式的内容在 `messages` 中的放置位置不同：

- **自然语言控制** → 放在 `role: user` 的 `content` 中
- **音频标签控制** → 放在 `role: assistant` 的 `content` 中

### 自然语言控制

通过自然语言描述，让模型理解并生成对应风格的语音。**内容放在** `messages` **中** `role: user` **的** `content` **字段。** 可以直接用一句话描述想要的语音风格。

**示例：**

> 用轻快上扬的语调向领导报喜，语速稍快，带着查到成绩后压抑不住的激动与小骄傲，声音明亮有活力。

> 看着刚解决的难题成果忍不住得意忘形地惊呼，声音高亢明亮，语速偏快，语气中带着满满的自信与难以置信。

> 用明亮活泼的青少年嗓音，带着恶作剧得逞后的得意与戏谑，语速偏快且咬字轻巧，在强调赌注时语气微微上扬。

在此基础上，我们还支持一种更复杂、更精细的**导演模式**——像给演员写剧本一样，从**角色、场景、指导**三个维度全方位刻画人物与声线，模型能据此生成更富层次、更具演绎感的语音。

- **【角色】** 写清人物的身份、性格底色、外形气质与说话习惯。
- **【场景】** 交代此刻发生了什么、和谁说话、情绪处在什么位置。越具体越好——时间、地点、事件、对方反应都可以写进来。
- **【指导】** 像导演给演员下达演绎要领：语速、气息、停顿、重音、共鸣位置、音色质感、情绪起伏。可以写得细腻，模型会按这些"舞台提示"来演。

**示例：**

```bash
角色：百年门阀岑家的现任大当家。自出生便被过继给祖庙的守门老人抚养，被塑造成一尊完美无瑕、绝情断欲的家族图腾。常年深居简出，对人有着极强的阶级疏离感。

场景：在祠堂的阴影里，看着那个不顾一切冲破保安防线来找她、企图带她私奔的男人。她要用最冷硬的阶级壁垒，绞杀对方，也绞杀自己刚刚萌芽、却足以燎原的感情。

指导：
冰冷、慵懒却极具威压的低音御姐。发声通道非常松弛，没有任何剑拔弩张，却有着让人骨里生寒的压迫感。

- 语速与顿挫：极慢，每个字都像是在舌尖滚过才吐出来，带着上位者漫不经心的傲慢。句与句之间留下极长的、令人不安的空白。
- 气声与实声：大部分时间，她的声音没有明显的声调起伏，实音重且硬，像是一条平缓却冰冷的暗河。但一定要在某些尾音处（如“真心”），加入极其轻微的气音收束，透出一丝连她自己都没察觉到的疲惫与渴望。
- 咬字肌理：文白杂糅的用词带着旧时代的痕迹，唇齿音发得极轻但极清晰（如“冲撞”“廉价”），显得既清雅又锋利，刀刀见血。
```

导演模式适合对语音表演要求较高的场景，例如角色配音、影视级内容生成等。

### 音频标签控制

通过在文本中嵌入风格标签与音频标签，直接对语音进行精细控制。开头是整体风格标签，中间可以插入细粒度控制标签。**所有标签控制内容放在** `messages` **中** `role: assistant` **的** `content` **字段。**

在目标文本**开头**添加 `(风格)` 标签，即可指定语音的发音风格。支持同时设置多种风格，将多个风格名称置于同一对括号内，分隔符不限。

**支持的括号格式：** 可使用半角 `()`、全角 `（）` 或 `[]`。

**格式示例：** `(风格1 风格2)待合成内容`

以下是一些推荐风格，同时也支持使用未在列表中的自定义风格。

如需体验更佳的唱歌风格，必须在目标文本最开头添加 `(唱歌)` 标签，格式为：`(唱歌)歌词`。`歌词` 建议采用中文，可获得更优合成效果。标签标识支持等效取值：`唱歌`、`sing`、`singing`，填写任意一项效果一致。

| 风格类型 | 风格示例 |
| --- | --- |
| 基础情绪 | 开心/悲伤/愤怒/恐惧/惊讶/兴奋/委屈/平静/冷漠 |
| 复合情绪 | 怅然/欣慰/无奈/愧疚/释然/嫉妒/厌倦/忐忑/动情 |
| 整体语调 | 温柔/高冷/活泼/严肃/慵懒/俏皮/深沉/干练/凌厉 |
| 音色定位 | 磁性/醇厚/清亮/空灵/稚嫩/苍老/甜美/沙哑/醇雅 |
| 人设腔调 | 夹子音/御姐音/正太音/大叔音/台湾腔 |
| 方言 | 东北话/四川话/河南话/粤语 |
| 角色扮演 | 孙悟空/林黛玉 |
| 唱歌 | 唱歌 |

**样例：**

- `(怅然)这么多年过去了，再走过那条街，心里一下子空了一块。`
- `(慵懒)再让我睡五分钟……就五分钟，真的，最后一次。`
- `(磁性)夜已经深了，城市还在呼吸。我是今晚陪你的人，欢迎收听《午夜电台》。`
- `(东北话)哎呀妈呀，这天儿也忒冷了吧！你说这风，嗖嗖的，跟刀子似的，割脸啊！`
- `(粤语)呢个真係好正啊！食过一次就唔会忘记！`
- `(唱歌)原谅我这一生不羁放纵爱自由，也会怕有一天会跌倒，Oh no。背弃了理想，谁人都可以，哪会怕有一天只你共我。`

在此基础上，我们还支持在文本中任意位置插入 `[音频标签]`。通过 [音频标签] ，你可以对声音进行细粒度控制，精准调节语气、情绪和表达风格——无论是低声耳语、放声大笑，还是带点小情绪的小吐槽，也可以灵活插入呼吸声，停顿，咳嗽等，都能轻松实现。语速同样可以灵活调整，让每句话都有它该有的节奏。

| 风格类型 | 风格示例 |
| --- | --- |
| 语速与节奏 | 吸气/深呼吸/叹气/长叹一口气/喘息/屏息 |
| 情绪状态 | 紧张/害怕/激动/疲惫/委屈/撒娇/心虚/震惊/不耐烦 |
| 语音特征 | 颤抖/声音颤抖/变调/破音/鼻音/气声/沙哑 |
| 哭笑表达 | 笑/轻笑/大笑/冷笑/抽泣/呜咽/哽咽/嚎啕大哭 |

**样例：**

- （紧张，深呼吸）呼……冷静，冷静。不就是一个面试吗……（语速加快，碎碎念）自我介绍已经背了五十遍了，应该没问题的。加油，你可以的……（小声）哎呀，领带歪没歪？
- （极其疲惫，有气无力）师傅……到地方了叫我一声……（长叹一口气）我先眯一会儿，这班加得我魂儿都要散了。
- 如果我当时……（沉默片刻）哪怕再坚持一秒钟，结果是不是就不一样了？（苦笑）呵，没如果了。
- （寒冷导致的急促呼吸）呼——呼——这、这大兴安岭的雪……（咳嗽）简直能把人骨头冻透了……别、别停下，走，快走。
- （提高音量喊话）大姐！这鱼新鲜着呢！早上刚捞上来的！哎！那个谁，别乱翻，压坏了你赔啊？！

## 使用预置音色进行语音合成

- 内置多种精品音色，无需额外配置即可直接使用。当前仅支持 `mimo-v2.5-tts` 模型
- 支持通过在 user message 中传入自然语言指令来控制合成语音的风格
- 支持通过音频标签来控制合成语音的风格

### 预置音色列表

使用时，可在 `{"audio": {"voice": "mimo_default"}}` 中设置预置音色。

| 音色名 | Voice ID | 语言 | 性别 |
| --- | --- | --- | --- |
| MiMo-默认 | mimo_default | 因部署集群而异，中国集群默认为 冰糖，其他集群默认为 Mia |
| 冰糖 | 冰糖 | 中文 | 女性 |
| 茉莉 | 茉莉 | 中文 | 女性 |
| 苏打 | 苏打 | 中文 | 男性 |
| 白桦 | 白桦 | 中文 | 男性 |
| Mia | Mia | 英文 | 女性 |
| Chloe | Chloe | 英文 | 女性 |
| Milo | Milo | 英文 | 男性 |
| Dean | Dean | 英文 | 男性 |

### 调用示例

#### 非流式调用

```python
import os
from openai import OpenAI
import base64

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1"
)

completion = client.chat.completions.create(
    model="mimo-v2.5-tts",
    messages=[
        {
            "role": "user",
            "content": "Bright, bouncy, slightly sing-song tone — like you're bursting with good news you can barely hold in. Fast pace, rising pitch at the end."
        },
        {
            "role": "assistant",
            "content": "Hey boss — guess what, guess what? I just got the results back and I actually passed! Not just passed, I got a distinction! I know, I know — you told me I was cutting it close, but hey, here we are. Drinks are on me tonight, okay?"
        }
    ],
    audio={
        "format": "wav",
        "voice": "Chloe"
    }
)

message = completion.choices[0].message
audio_bytes = base64.b64decode(message.audio.data)
with open("audio_file.wav", "wb") as f:
    f.write(audio_bytes)
```

#### 流式调用

`mimo-v2.5-tts` 低延迟流式输出功能已上线；当前流式接口恢复正常，可实时返回流式响应。

```python
import base64
import os
import numpy as np
import soundfile as sf
from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1"
)

completion = client.chat.completions.create(
    model="mimo-v2.5-tts",
    messages=[
        {
            "role": "user",
            "content": "Bright, bouncy, slightly sing-song tone — like you're bursting with good news you can barely hold in. Fast pace, rising pitch at the end."
        },
        {
            "role": "assistant",
            "content": "Hey boss — guess what, guess what? I just got the results back and I actually passed! Not just passed, I got a distinction! I know, I know — you told me I was cutting it close, but hey, here we are. Drinks are on me tonight, okay?"
        }
    ],
    audio={
        "format": "pcm16",
        "voice": "Chloe"
    },
    stream=True
)

# 24kHz PCM16LE mono audio
collected_chunks: np.ndarray = np.array([], dtype=np.float32)

for chunk in completion:
    if not chunk.choices:
        continue
    delta = chunk.choices[0].delta
    audio = getattr(delta, "audio", None)

    if audio is not None:
        assert isinstance(audio, dict), f"Expected audio to be a dict, got {type(audio)}"
        pcm_bytes = base64.b64decode(audio["data"])
        np_pcm = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0
        collected_chunks = np.concatenate((collected_chunks, np_pcm))
        print(f"Received audio chunk of size {len(pcm_bytes)} bytes")

# Save the collected audio to a file
os.makedirs("tmp", exist_ok=True)
sf.write("tmp/output.wav", collected_chunks, samplerate=24000)
print("Audio saved to tmp/output.wav")
```

## 使用文本设计音色进行语音合成

无需提供音频文件，只需在角色为 `user` 的消息中添加音色描述文本，即可生成定制化的语音音色。当前仅支持 `mimo-v2.5-tts-voicedesign` 模型。

### 如何写好音色描述（voice design prompt）

使用 `mimo-v2.5-tts-voicedesign` 模型时，`user` 消息中的文本就是音色设计描述。描述越具体、越生动，生成的音色越贴近预期。

#### 关键维度

一条好的音色描述通常涵盖以下多个维度（不需要面面俱到）：

| 维度 | 示例 |
| --- | --- |
| 性别与年龄 | "young woman in her mid-20s"、"五十多岁的中年男性" |
| 音色/质感 | "deep and gravelly"、"丝滑醇厚、带着磁性" |
| 情绪/语气 | "warm and confident"、"温柔但带着一丝疲惫" |
| 语速/节奏 | "slow and deliberate"、"语速极快，像连珠炮" |

以下维度可选择性加入，增加丰富度：

- **角色/人设**：narrator, podcast host, 评书先生, 深夜电台DJ
- **说话风格**：casual and colloquial, 一本正经地, 压低嗓音像在密谋
- **场景描写**：narrating a nature documentary, 在给投资人路演
- **年代参照**：1940s film noir, 八十年代译制片配音

#### 写法建议

**简洁描述型** -- 用关键词或一句话快速勾勒声音轮廓

```bash
Heavy Russian accent, gruff middle-aged male, blunt and matter-of-fact.
```

**专业描述型** -- 通过场景、人设或多维度细节立体刻画声音

```bash
Young female, extreme close-up with a binaural, ear-to-ear ASMR feel. Audible breathing, subtle swallowing, and soft natural lip sounds. She speaks very slowly, creating a deeply relaxing and immersive experience.
```

```bash
一位年迈的老先生，说带北方口音的普通话，语速缓慢而沉稳，嗓音略带沙哑和沧桑感，仿佛一位饱经风霜的老爷爷在讲故事，充满岁月的智慧。
```

#### 注意事项

- **长度**：1-4 句即可，不需要写长文。核心特征描述清楚比堆砌维度更重要
- **避免冲突**：不要同时要求矛盾的特征（如"稚嫩的童声 + CEO气场"）
- **避免音质效果词**：不要写混响、回声、EQ、压缩等后期处理相关描述
- **避免模糊词**：不要用"普通的""正常的""外国的"等缺乏具体指向的描述
- **中英文均可**：模型同时支持中英文音色描述，选择你最能精确表达的语言
- **合成文本要贴合音色**：`assistant` 消息中的合成文本（text）应与音色描述相匹配，才能获得最佳效果。例如为"温柔治愈系女声"搭配一段晚安独白，而非一段激烈的体育解说。建议使用 LLM 根据你的音色描述自动生成适配的合成文本；在 Studio 页面上，输入音色描述后可直接点击「生成文本」按钮

### 调用示例

`mimo-v2.5-tts-voicedesign` 可通过可选参数 `optimize_text_preview` 控制是否对目标播报文本进行智能润色；设为 `true` 时，可无需传入 `assistant` 消息。

#### 非流式调用

```python
import os
from openai import OpenAI
import base64

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1"
)

completion = client.chat.completions.create(
    model="mimo-v2.5-tts-voicedesign",
    messages=[
        {
            "role": "user",
            "content": "Give me a young male tone."
        },
        {
            "role": "assistant",
            "content": "Yes, I had a sandwich."
        }
    ],
    audio={
        "format": "wav",
        "optimize_text_preview": True
    }
)

message = completion.choices[0].message
audio_bytes = base64.b64decode(message.audio.data)
with open("audio_file.wav", "wb") as f:
    f.write(audio_bytes)
```

#### 流式调用

**注意**

- `mimo-v2.5-tts-voicedesign` 的低延迟流式输出功能暂未上线，如有相关需求，请关注近期的功能更新。
- 流式调用接口目前降级为兼容模式，**仅在所有推理完成后**以流式格式返回**一次**结果。

```python
import base64
import os
import numpy as np
import soundfile as sf
from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1"
)

completion = client.chat.completions.create(
    model="mimo-v2.5-tts-voicedesign",
    messages=[
        {
            "role": "user",
            "content": "Give me a young male tone."
        },
        {
            "role": "assistant",
            "content": "You are UN-BE-LIEVABLE! I am sooooo done with your constant lies. GET. OUT!"
        }
    ],
    audio={
        "format": "pcm16",
        "optimize_text_preview": True
    },
    stream=True
)

# 24kHz PCM16LE mono audio
collected_chunks: np.ndarray = np.array([], dtype=np.float32)

for chunk in completion:
    if not chunk.choices:
        continue
    delta = chunk.choices[0].delta
    audio = getattr(delta, "audio", None)

    if audio is not None:
        assert isinstance(audio, dict), f"Expected audio to be a dict, got {type(audio)}"
        pcm_bytes = base64.b64decode(audio["data"])
        np_pcm = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0
        collected_chunks = np.concatenate((collected_chunks, np_pcm))
        print(f"Received audio chunk of size {len(pcm_bytes)} bytes")

# Save the collected audio to a file
os.makedirs("tmp", exist_ok=True)
sf.write("tmp/output.wav", collected_chunks, samplerate=24000)
print("Audio saved to tmp/output.wav")
```

## 使用音色复刻进行语音合成

- 通过传入音频样本，即可精准复刻目标音色并生成语音。当前仅支持 `mimo-v2.5-tts-voiceclone` 模型
- 支持通过在 user message 中传入自然语言指令来控制合成语音的风格
- 支持通过音频标签来控制合成语音的风格

### 调用示例

将音频文件样本转换为 Base64 编码字符串后传入。转换后的 Base64 编码的字符串大小不能超过 10 MB，目前仅支持传入 `mp3` 和 `wav` 格式的音频样本文件。

请在 Base64 编码前携带前缀：`data:{MIME_TYPE};base64,$BASE64_AUDIO`

- `{MIME_TYPE}`：音频的 MIME 类型（媒体类型），用于标识音频格式，需替换为实际音频对应的 MIME 值。这里的取值可以为：`audio/mpeg`（或 `audio/mp3`），`audio/wav`。
- `$BASE64_AUDIO`：音频文件的纯 Base64 编码字符串（不含任何前缀）。

> 下述示例中的 `{MIME_TYPE}` 与 `$BASE64_AUDIO` 均为占位符，运行前请替换为实际有效数据。

#### 非流式调用

```python
import base64
import os

from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1",
)

with open("voice.mp3", "rb") as f:
    voice_bytes = f.read()
voice_base64 = base64.b64encode(voice_bytes).decode("utf-8")

completion = client.chat.completions.create(
    model="mimo-v2.5-tts-voiceclone",
    messages=[
        {
            "role": "user",
            "content": ""
        },
        {
            "role": "assistant",
            "content": "Yes, I had a sandwich."
        }
    ],
    audio={
        "format": "wav",
        "voice": f"data:audio/mpeg;base64,{voice_base64}"
    }
)

message = completion.choices[0].message
audio_bytes = base64.b64decode(message.audio.data)
with open("audio_file.wav", "wb") as f:
    f.write(audio_bytes)
```

#### 流式调用

**注意**

- `mimo-v2.5-tts-voiceclone` 的低延迟流式输出功能暂未上线，如有相关需求，请关注近期的功能更新。
- 流式调用接口目前降级为兼容模式，**仅在所有推理完成后**以流式格式返回**一次**结果。

```python
import base64
import os

import numpy as np
import soundfile as sf
from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get("MIMO_API_KEY"),
    base_url="https://api.xiaomimimo.com/v1",
)

with open("voice.mp3", "rb") as f:
    voice_bytes = f.read()
voice_base64 = base64.b64encode(voice_bytes).decode("utf-8")

completion = client.chat.completions.create(
    model="mimo-v2.5-tts-voiceclone",
    messages=[
        {
            "role": "user",
            "content": ""
        },
        {
            "role": "assistant",
            "content": "Yes, I had a sandwich."
        }
    ],
    audio={
        "format": "wav",
        "voice": f"data:audio/mpeg;base64,{voice_base64}",
    },
    stream=True
)

# 24kHz PCM16LE mono audio
collected_chunks: np.ndarray = np.array([], dtype=np.float32)

for chunk in completion:
    if not chunk.choices:
        continue
    delta = chunk.choices[0].delta
    audio = getattr(delta, "audio", None)

    if audio is not None:
        assert isinstance(audio, dict), (
            f"Expected audio to be a dict, got {type(audio)}"
        )
        pcm_bytes = base64.b64decode(audio["data"])
        np_pcm = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0
        collected_chunks = np.concatenate((collected_chunks, np_pcm))
        print(f"Received audio chunk of size {len(pcm_bytes)} bytes")

# Save the collected audio to a file
os.makedirs("tmp", exist_ok=True)
sf.write("tmp/output.wav", collected_chunks, samplerate=24000)
print("Audio saved to tmp/output.wav")
```

## 计费说明

- 计费：限时免费。
- 查看账单：您可以在控制台的 [账单明细](https://platform.xiaomimimo.com/#/console/usage) 页面查看用量。

更新时间 2026 年 07 月 15 日

[语音识别（MiMo-V2.5-ASR）](/docs/zh-CN/quick-start/usage-guide/audio/Speech-Recognition)

[邀请有礼](/docs/zh-CN/quick-start/promotions/refer)