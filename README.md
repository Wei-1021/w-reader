
谁还在为写代码间隙想放松，却怕切换软件被抓包而纠结？谁还在为找小说资源、适配阅读格式而头疼？这款专为 JetBrains IDEs 量身打造的 W-Reader 沉浸式小说阅读器插件，直接把 “私人图书馆” 搬进你的 JetBrains 全家桶（IDEA、PyCharm、WebStorm 等），让编码与阅读无缝衔接，摸鱼、充电两不误！🚀
# 介绍
W-Reader 是一款专为 JetBrains 系列 IDE（如 IntelliJ IDEA、PyCharm、WebStorm、RustRover 等）打造的轻量级小说阅读插件。它将阅读体验无缝融入开发环境，在不离开编码上下文的前提下，让你轻松享受小说阅读的乐趣。
### 核心功能
- 支持在线搜索海量小说资源，精准定位心仪作品。
- 整合多个优质书源，覆盖热门网文与经典名著。 
- 支持自定义书源规则，可自行添加书源。
- 支持 TXT/EPUB 格式本地小说加载。 
- 支持侧边栏/状态栏模式切换，适配不同屏幕布局。
- 支持编辑器提示消息、代码注释块模式阅读。
- 开启自动阅读功能，内容自动切换播放。 
- 支持多音色听书模式，享受听觉盛宴。 
- 预设翻页/章节切换/听书等快捷键，提升阅读效率。
# 安装
1. 打开插件市场
在你的 JetBrains IDE 中，打开 File → Settings → Plugins 
2. 搜索并安装
在插件市场搜索 W-Reader，点击安装按钮
3. 重启并使用
安装完成后重启 IDE，在侧边栏找到 W-Reader 图标开始使用
![插件市场搜索W-Reader](https://i-blog.csdnimg.cn/direct/de50f087155b4f1b97565c5e2720f53c.png)

# 功能介绍
### 在线搜索小说
在线搜索海量小说，热门网文、经典名著一搜即得，精准定位心仪作品，不用再跨平台到处找资源；更整合多个优质书源，随时随心切换，告别书荒困扰；
![搜索小说](https://i-blog.csdnimg.cn/direct/7dc55ac0fadf45be88bb4b5d1fc9e540.png)
![小说搜索结果](https://i-blog.csdnimg.cn/direct/c5d2ae6228cf4ddd8c72638e3d15f2c1.png)

### 自定义书源规则
W-Reader不仅内置了默认的书源，而且还支持通过编写自定义书源规则，添加更多属于你自己的书源列表，未来还计划添加书源广场，整合编写好的自定义书源规则，减少大家编写那复杂的书源规则；
![自定义书源规则](https://i-blog.csdnimg.cn/direct/34aa63a9bf0a487d9ff89a8126f6c6c9.png)

### 加载本地小说文件
除了在线阅读，W-Reader 完美支持本地小说文件：
- **TXT 格式**：自动分章、智能识别标题与段落，支持自定义解析文件的正则表达式（有默认的正则）；
- **EPUB 格式**：完整解析目录、元数据与排版结构，还支持显示EPUB 文件内嵌的图片，可点击图片放大预览；
只需选择小说文件，即可立即开始阅读。所有内容仅在本地处理，无任何上传或隐私风险。
![选择文件字符集、填写TXT文件的解析正则](https://i-blog.csdnimg.cn/direct/330d8142e233470f8b470ec30359ff7a.png)
![选择本地文件](https://i-blog.csdnimg.cn/direct/9238969e8c814663b43ae0c7ed5976da.png)

### 多模式切换
为适配不同屏幕尺寸与工作习惯，W-Reader 提供四种显示模式：
- **侧边栏模式**：占据 IDE 右侧区域，适合大屏用户，提供类电子书阅读体验。
- **状态栏迷你模式**：在 IDE 底部状态栏嵌入精简阅读面板，适合小屏或专注编码时快速浏览。
- **编辑器提示消息模式**：在光标悬停或弹出提示时显示小说内容（适合碎片化阅读）
- **代码注释块阅读模式**：将小说文本以注释形式插入任意代码文件中，IDE 自动高亮
四种模式可随时切换，且阅读进度自动同步。
![侧边栏模式](https://i-blog.csdnimg.cn/direct/caf50462339e494cbc4f2b54149a32ec.png)![请添加图片描述](https://i-blog.csdnimg.cn/direct/d7c034a0a49a451193d410e5eb65e757.png)
![状态栏模式](https://i-blog.csdnimg.cn/direct/4ce3dbb5aea34b19a26b940746bdbe21.png)
![编辑器提示消息模式](https://i-blog.csdnimg.cn/direct/5ef7a0a294654cc39cf79c0d3910b360.png)
![代码注释块](https://i-blog.csdnimg.cn/direct/6cbead0e19504a2397056962541c91ff.png)
### 听书
W-Reader 内置语音播报功能，支持 多音色听书：
- 可选择不同语速、音调、语音角色、语音风格等
- 听书时仍可继续编码，实现“耳读手写”并行
![设置](https://i-blog.csdnimg.cn/direct/452b34372fb54eb8ba84169af412d6ec.png)

### 快捷键操控
为提升操作效率，W-Reader 预设了一套完整的键盘快捷键：
| 功能 | 快捷键 |
|--|--|
|搜索  | Alt + NumPad-7(小键盘7) |
| 目录  | Alt + NumPad-9(小键盘9)  |
| 自定义书源规则  | Alt + NumPad DECIMAL(小键盘.)  |
| 加载本地文件  |  Alt + NumPad ADD(小键盘+号) |
| 隐藏/显示文字  | Alt + NumPad SUBTRACT(小键盘-号)  |
| 代码注释块  | Alt + NumPad DIVIDE(小键盘/号)  |
| 编辑器提示消息  | Alt + NumPad-0(小键盘0)  |
| 听书  | Alt + NumPad-3(小键盘3)  |
| 上一章  | Alt + NumPad-4(小键盘4)  |
| 下一章  | Alt + NumPad-6(小键盘6)  |
| 上一行 | Alt + NumPad-8(小键盘8)  |
| 下一行  |  Alt + NumPad-2(小键盘2) |

部分功能没有设置快捷键，若没有小键盘或需要自定义快捷键，可通过`File → Settings → Keymap → Plugins → W-Reader`进行修改。
![快捷键](https://i-blog.csdnimg.cn/direct/c5e45ce99de547d8b308ae71fef22adf.png)
## 联系方式
如果您有任何问题或建议，请通过以下方式联系： <br>
Email: 1075542448@qq.com <br>
QQ群：1060150904
