# 自定义书源规则说明

**如有任何问题或者建议，请联系我们进行反馈：**
- QQ群：1060150904

### JSON模板格式如下:
```json
[{
  "enabled": true,
  "id": "",
  "name": "",
  "baseUrl": "",
  "header": "",
  "hasHtml": true,
  "searchRules": {
    "url": "",
    "dataBookListRule": "",
    "bookListElementName": "",
    "bookListUrlElement": "",
    "bookListTitleElement": ""
  },
  "listMainRules": {
    "url": "",
    "urlDataRule": "",
    "itemIdField": "",
    "itemTitleField": "",
    "itemUrlField": "",
    "urlDataHandleRule": "",
    "listMainElementName": "",
    "urlElement": "",
    "titleElement": "",
    "nextListMainUrl": "",
    "useNextListMainApi": false,
    "nextListMainApiDataRule": ""
  },
  "chapterRules": {
    "url": "",
    "urlDataRule": "",
    "contentHandleRule": "",
    "useContentOriginalStyle": false,
    "replaceContentOriginalRegex": "",
    "contentElementName": "",
    "nextContentUrl": "",
    "useNextContentApi": false,
    "nextContentApiDataRule": "",
    "contentRegexList": []
  },
  "bookInfoRules": {
    "bookIdField": "",
    "bookNameField": "",
    "bookUrlField": "",
    "bookAuthorField": "",
    "bookDescField": "",
    "bookImgUrlField": ""
  }
}]
```
**❗ 提示：填写`JSON`格式数据时，请务必对填写的内容进行转义，否则会出现解析错误。**

### 配置项说明:
____________________________

#### <font color="#249aff">enabled</font>:
是否启用
____________________________ 
#### <font color="#249aff">id</font>：
书源规则的ID，用于标识书源唯一性（一般为网站域名）。
____________________________ 
#### <font color="#249aff">name</font>：
书源名称，即网站名称。
____________________________
#### <font color="#249aff">baseUrl</font>：
书源的根URL，用于拼接其他URL（**注意要带上`http://`或`https://`**），例如：https://www.testsite.com。
____________________________
#### <font color="#249aff">header</font>：
请求头，用于模拟请求（暂时没用）。
____________________________
#### <font color="#249aff">hasHtml</font>：
是否启用HTML解析，如果为`false`则表示返回结果为JSON数据。
> 以下两种情况会用到`hasHtml`：
> * 1.搜索小说列表：用于判断当前请求的结果是否为HTML页面，如果搜索小说时返回的是一个html页面，那么此字段必须设置为true;
> * 2.获取小说目录：用于判断获取小说目录时返回的结果是否为HTML页面，只有当listMainRules.url不为空，且hasHtml=false时，系统才会判定请求结果为json数据，否则统一认定为html页面;
____________________________
#### <font color="#249aff">searchRules</font>：
搜索规则。
> ##### <font color="grey">searchRules</font>.<font color="249aff">url</font>：
> 用于获取搜索小说的结果列表，可以是url地址，也可以是一段java代码。
>> 1. 若url为`GET`请求，则直接填写url地址，请求参数用`${关键字/运算表达式}`占位符替代，这里有两个参数：`key`和`page`，分别表示搜索的关键字和页码， 但是因为目前没有翻页功能，所以页码参数实际上会固定设置为1，例如：`https://www.testsite.com/search?q=${key}&page=${page}`。
>>
>> 2. 若url为`POST`请求，则使用以下JSON格式填写（当然，`GET`请求也可以使用这个规则，但此规则只适合`GET`和`POST`请求，其它类型暂不支持）：
>>   ```json
>>   {
>>       "url": "URL地址",
>>       "method": "请求类型",
>>       "queryParams": {
>>           "参数1": "值1",
>>           "参数2": "值2"
>>       },     
>>       "bodyParams": {
>>           "参数1": "值1",
>>           "参数2": "值2"
>>       },
>>       "header":{
>>           "请求头参数1": "值1"
>>       }
>>   }
>>   ```
>>   其中:
>>   - `url`：URL地址，
>>   - `method`：请求类型： `GET/POST`，默认为`GET`，
>>   - `queryParams`：查询参数（即拼接在url后面的参数），
>>   - `bodyParams`：请求体参数，
>>   - `header`：请求头
>>
>>   请求参数用`${关键字/运算表达式}`占位符替代，这里有两个参数：`key`和`page`，分别表示搜索的关键字和页码，例如：
>>   ```json
>>   {
>>       "url":"http://www.testsite.com/search",
>>       "method": "POST",
>>       "queryParams": {
>>           "q": "${key}"
>>       },
>>       "bodyParams": {
>>           "keyword":"${key}",
>>           "page":"${page}"
>>       },
>>       "header":{
>>           "Content-Type": "text/html; charset=utf-8"
>>       }
>>   }
>>   ```
>>
>> 3. 若为`java`代码，则需要使用以下格式填写：
>>   ```xml
>>   <java>
>>       <package_import>导入的包</package_import>
>>       <code>要执行的代码</code>
>>   </java>
>>   ```
>>   例如:
>>   ```xml
>>   <java>
>>       <package_import>import java.util.*;</package_import>
>>       <code>
>>           public String execute(String key, String page) {
>>               String uuid = UUID.randomUUID().toString().replaceAll("-", "");
>>               return "http://www.wreaderbook.com/search?q=" + key + "&amp;page=" + page + "&amp;t=" + uuid;";
>>           }
>>       </code>
>>   </java>
>>   ```
>>   其中：入口方法为`execute`，参数为：`String key`和`String page`，
>>   两个参数分别代表：
>>   - `key`：搜索的关键字；
>>   - `page`：页码，当前版本固定为1；
>>
>>   `execute`方法的**返回值**为请求小说搜索结果列表的`url`链接。
>>
>> 4. 若为`JavaScript`代码，则有以下两种方式：
>>   - 在代码字符串开头添加`@js:`标记（**注意字母大小写**）；
>>   - 使用`<js></js>`标签包住代码字符串；
>>
>>   其中可用的全局参数为：
>>   - `key`：搜索的关键字；
>>   - `page`：页码，当前版本固定为1；
>>
>>   示例：(示例代码将代码换行是为了方便阅读，实际过程中请将代码压缩成一行)
>>   ```js
>>   @js: let tt = new Date().getTime(); 
>>   let encodeKey = encodeURIComponent(key); 
>>   let url = `https://www.ttest.info/e/search/index.php?keyboard=${encodeKey}&show=title%2Cwriter%2Cbyr&searchget=1&tt=${tt}`; 
>>   (url)
>>   ```
>
> ##### <font color="grey">searchRules</font>.<font color="249aff">dataBookListRule</font>：
> 获取小说列表的JSONPath语法规则，用于处理搜索结果数据，只有当搜索结果返回`JSON`格式的数据时才需要填写，填写规则请参考`JSONPath`，例如接口返回的数据如下：
>> ```json
>> {
>>    "code": 200,
>>    "data": [
>>        {
>>            "_id": "6378fbf07b0ea2c487432117",
>>            "book_id": "12035313404304503",
>>            "all_words": 2113921,
>>            "author_name": "夜十三",
>>            "category_id": 20019,
>>            "cover_url": "https://bookcover.yuewen.com/qdbimg/349573/c_12035313404304503/180?h=2j01ji3g45iq9",
>>            "intro": "intro",
>>            "keyword": "",
>>            "status": 30,
>>            "sub_category_id": 20020,
>>            "title": "完美重生",
>>            "category_name": "都市",
>>            "sub_category_name": "都市生活"
>>        },
>>        {
>>            "_id": "637a1ce67b0ea2c487711015",
>>            "book_id": "7036856404321803",
>>            "all_words": 2198317,
>>            "author_name": "墨乡",
>>            "category_id": 20050,
>>            "cover_url": "https://bookcover.yuewen.com/qdbimg/349573/c_7036856404321803/180?h=17bhw49513hpa",
>>            "intro": "intro",
>>            "keyword": "",
>>            "status": 50,
>>            "sub_category_id": 20051,
>>            "title": "完美机甲剑神",
>>            "category_name": "游戏",
>>            "sub_category_name": "电子竞技"
>>        }
>>    ],
>>    "message": "success",
>>    "paging": {
>>        "pi": 1,
>>        "ps": 30,
>>        "count": 13
>>    },
>>    "oid": "68fb2587fb3003322818fc6e",
>>    "st": 1761289607
>> }
>>```
>> 那如果想要获取小说列表数据，则需要填写：`$.data[*]`
> ____________________________
> ##### <font color="grey">searchRules</font>.<font color="249aff">bookListElementName</font>：
> 用于获取搜索结果列表的`CssSelector`选择器语法，适用于HTML页面，例如：返回的页面结构为：
>> ```html
>> <body>
>> <div class="book-list">
>>     <ul>
>>         <li>
>>             <div class="li-dd">
>>                 <a class="link" href="/book/1/" title="BOOK 001">BOOK 001</a>
>>                 <p class="title">BOOK 001</p>
>>             </div>
>>         </li>
>>         <li>
>>             <div class="li-dd">
>>                 <a class="link" href="/book/2/" title="BOOK 001">BOOK 002</a>
>>                 <p class="title">BOOK 002</p>
>>             </div>
>>         </li>
>>     </ul>
>> </div>
>> </body>
>> ```
>> 要获取到结果的列表，则需要填写：`.book-list ul li`。其实等同于js的`document.querySelectorAll('.book-list ul li')`
> ____________________________
> ##### <font color="grey">searchRules</font>.<font color="249aff">bookListUrlElement</font>：
> 用于获取搜索结果列表中每个结果的url（每本小说的链接）的`CssSelector`选择器语法，适用于HTML页面，注意：这里是以列表的单项的页面结构为基准，以上面的页面结构为例，则以：
>> ```html
>> <li>
>>     <div class="li-dd">
>>         <a class="link" href="/book/1/" title="BOOK 001">BOOK 001</a>
>>         <p class="title">BOOK 001</p>
>>     </div>
>> </li>
>> ```
>> 为基准，所以想要获取url信息，那么要填写：`.li-dd a`， 等同于js的`document.querySelectorAll('.book-list ul li')[0].querySelector('.li-dd a').href`。需要特别强调的是，获取的url信息是标签的`href`属性的值，所以最终需要定位的元素必须是a标签
> ____________________________
> ##### <font color="grey">searchRules</font>.<font color="249aff">bookListTitleElement</font>：
> 用于获取搜索结果列表中每个结果的标题（每本小说的标题）的`CssSelector`选择器语法，适用于HTML页面，注意：这里是以列表的单项的页面结构为基准，以`bookListElementName`的页面结构为例，则以：
>> ```html
>> <li>
>>     <div class="li-dd">
>>         <a class="link" href="/book/1/" title="BOOK 001">BOOK 001</a>
>>         <p class="title">BOOK 001</p>
>>     </div>
>> </li>
>>```
>> 为基准，所以要获取标题信息，你可以填写：`.li-dd a` 或 `.li-dd .link` 或 `.li-dd p` 或 `.li-dd .title`，等同于js的`document.querySelectorAll('.book-list ul li')[0].querySelector('.li-dd a').textContent`。
____________________________

#### <font color="#249aff">listMainRules</font>：
目录规则。
> ##### <font color="grey">listMainRules</font>.<font color="249aff">url</font>：
> 用于获取小说目录的列表，可以`置空`，可以是`url地址`，也可以是一段`java`或`js`代码。
>> 1. 只有一种情况可以置空，即`searchRules.url`这里请求的结果是`html页面`，并且`searchRules.bookListUrlElement`不为空。
>>
>> 2. `GET`和`POST`请求规则同上（`searchRules.url`已介绍），但这里的参数只有一个：`bookId`，表示小说id
>>
>> 3. `java`代码规则同上（`searchRules.url`已介绍），入口方法为`execute`，参数可以是`String bookId`或者`BookInfo`实体对象两者中的一个，不可以同时传入这两个参数，
>>   其中：
>>   - `bookId`：小说id，仅搜索小说返回结果是JSON数据时才有效；
>>   - `BookInfo`：小说信息实体对象，包含字段：
>>     - `bookId`：小说id；
>>     - `bookName`：小说名称；
>>     - `bookUrl`：小说链接；
>>     - `bookAuthor`：小说作者；
>>     - `bookDesc`：小说描述；
>>     - `bookImgUrl`：小说图片链接；
>>
>>   如果选择入参的参数类型是`BookInfo`对象，那么在`<package_import></package_import>`部分请填写：`import com.wei.wreader.pojo.BookInfo;`。  
>>   提示：
>>   - 当搜索小说返回的结果是HTML页面时，BookInfo为仅`bookName`和`bookUrl`有值；
>>   - 当搜索小说返回的结果是JSON数据时，在字段名称填写正确的情况下，只要返回结果中包含有的信息，那BookInfo中对应字段都有值；（对应字段名称的填写规则请跳转至`bookInfoRules`小说基本信息处理规则部分）；
>>   - **返回值**为请求获取小说目录的`url`链接。
>>
>> 4. `JavaScript`代码规则同上，这里不展开细说。  
>>   其中可用的全局参数为：
>>  - `bookInfo`：小说信息实体对象，其拥有字段为：
>>    - `bookId`：小说id；
>>    - `bookName`：小说名称；
>>    - `bookUrl`：小说链接；
>>    - `bookAuthor`：小说作者；
>>    - `bookDesc`：小说描述；
>>    - `bookImgUrl`：小说图片链接；
>>
>>示例：
>>```js
>> @js: 
>> let bookName = encodeURIComponent(bookInfo.bookName); 
>> let url = "http://www.example.com/chapter/${bookName}/" + bookInfo.bookId;
>> (url)
>>  ```
>>   - 当搜索小说返回的结果是HTML页面时，BookInfo为仅`bookName`和`bookUrl`有值；
>>   - 当搜索小说返回的结果是JSON数据时，在字段名称填写正确的情况下，只要返回结果中包含有的信息，那BookInfo中对应字段都有值；（对应字段名称的填写规则请跳转至`bookInfoRules`小说基本信息处理规则部分）；
>>   - **返回值**为请求获取小说目录的`url`链接。
>>
>
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">urlDataRule</font>：
> 获取目录列表的JSONPath语法规则，用于处理目录列表结果数据。只有当请求结果返回JSON格式的数据时才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取目录列表，例如：
>> ```json
>> {
>>   "code": 200,
>>   "data": {
>>      "bookId": "1001",
>>      "bookName": "book name",
>>      "author": "author name",
>>      "list": [
>>        {
>>           "chapterId": 1,
>>           "title": "chapter 1",
>>           "url": "http://www.example.com/chapter/1001/1" 
>>        },
>>        {
>>           "chapterId": 2,
>>           "title": "chapter 2",
>>           "url": "http://www.example.com/chapter/1001/2" 
>>        },
>>        {
>>           "chapterId": 3,
>>           "title": "chapter 3",
>>           "url": "http://www.example.com/chapter/1001/3" 
>>        }
>>      ]
>>   }
>> }
>> ```
>> 如上所示，目录列表数据为JSON格式，要获取目录列表数据，那么`urlDataRule`填写：`$.data.list[*]`
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">itemIdField</font>：
> 目录列表中各章节id对应的字段名称。当获取目录返回的结果为JSON格式数据时，在`urlDataRule`规则获取到数据后，通过`itemIdField`规则获取目录`每一个章节的id`。例如，获取的目录数据为：
>> ```json
>> [
>>     {
>>         "chapterId": 1,
>>         "title": "chapter 1",
>>         "url": "https://www.example.com/chapter/1001/1"
>>     },
>>     {
>>         "chapterId": 2,
>>         "title": "chapter 2",
>>         "url": "https://www.example.com/chapter/1001/2"
>>     }
>> ] 
>>```
>> 获取目录列表中每个目录的章节id，那么`itemIdField`填写：`chapterId`
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">itemTitleField</font>：
> 目录列表中各章节标题对应的字段名称。当获取目录返回的结果为JSON格式数据时，在`urlDataRule`规则获取到数据后，通过`itemTitleField`规则获取目录`每一个章节的标题`。 以上面的数据为例，获取目录列表中每个目录的章节标题，那么`itemTitleField`填写：`title`
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">itemUrlField</font>：
> 目录列表中各章节URL链接对应的字段名称。当获取目录返回的结果为JSON格式数据时，在`urlDataRule`规则获取到数据后，通过`itemUrlField`规则获取目录`每一个章节的链接`。 以上面的数据为例，获取目录列表中每个目录的章节链接，那么`itemUrlField`填写：`url`
>
> ----------------------------
> ##### <font color="grey">listMainRules</font>.<font color="249aff">urlDataHandleRule</font>：
> 目录列表各章节链接的处理规则，主要用于处理各章节的URL链接，其规则为`java`/`js`代码；
> 只有当请求结果为JSON类型数据，在通过`urlDataRule`规则获取到数据后，才执行此规则；  
> 例如：接口返回结果没有包含章节URL字段，或者URL字段不是完整的链接，那么就需要将目录列表数据中的URL字段进行拼接等。  
> `Java`/`Js`规则同上，不展开描述。  
> 参数为：
> - `result`：api接口返回的结果数据，`Java`环境下，`itemMap`为`Map`类型，`Js`环境下，`itemMap`为`Object`类型；
> - `itemMap`：通过`urlDataRule`规则提取出来的目录项数据，`Java`环境下，`itemMap`为`Map`类型，`Js`环境下，`itemMap`为`Object`类型；
> - `itemId`：目录列表中每个章节的id，`String`类型；
>
> js脚本示例：
> ```js
> @js:
> let shortContUrlPrefix = result.shortContUrlPrefix;
> let shortContUrlSuffix = itemMap.shortContUrlSuffix;
> let url = `${shortContUrlPrefix}/chapter/${itemId}/${shortContUrlSuffix}`;
> (url)
> ```
> - `urlDataHandleRule`与`urlDataRule`容易混淆，请注意区分；
> - `Java`代码的入口方法为`execute`；
> - 脚本返回值为单个章节的URL链接；
> 
> ----------------------------
> ##### <font color="grey">listMainRules</font>.<font color="249aff">listMainElementName</font>：
> 用于获取目录列表的`CssSelector`选择器语法，适用于HTML页面，例如页面结构为：
>> ```html
>> <body>
>> <div class="chapterList">
>>     <dl>
>>         <dd><a href="/chapter/1.html">第一章 001</a></dd>
>>         <dd><a href="/chapter/2.html">第二章 002</a></dd>
>>         <dd><a href="/chapter/3.html">第三章 003</a></dd>
>>     </dl>
>> </div>
>> </body>
>>``` 
>>想要获取目录列表，那么`listMainElementName`填写：`.chapterList dl dd`，等同于js的`document.querySelectorAll('.chapterList dl dd')`。
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">urlElement</font>：
> 章节链接`CssSelector`语法，以目录列表的单项结构作为基准，以上面的页面结构为例，那么将以下面的html作为获取数据的基准结构：
>> ```html
>> <dd><a href="/chapter/1.html">第一章 001</a></dd>
>> ``` 
>> 因此`urlElement`填写：`a`，等同于js的`document.querySelectorAll('.chapterList dl dd')[0].querySelector('a').href`
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">titleElement</font>：
> 章节标题`CssSelector`语法，同`urlElement`一样，以目录列表的单项结构作为基准，以上面的页面结构为例，`titleElement`填写：`a`， 等同于js的`document.querySelectorAll('.chapterList dl dd')[0].querySelector('a').textContent`。
> ____________________________
> ##### <font color="grey">listMainRules</font>.<font color="249aff">nextListMainUrl</font>：
> 目录列表的下一页目录的链接（针对某些网站会把目录列表分成多页的情况），可以填写`Java`和`Js`代码;  
> - 代码返回值为下一页目录的链接`
> - `Java`代码规则同上，入口方法为`execute`，有5个参数：
>   - `baseUrl`：<font color="MediumOrchid">String</font> 当前网站的根链接，例如：`https://www.example.com`；
>   - `pageIndex`：<font color="MediumOrchid">String</font>  页码，从2开始；
>   - `preUrl`：<font color="MediumOrchid">String</font>  上一页链接；
>   - `bodyElementStr`：<font color="MediumOrchid">String</font>  当前页面`<body>`标签内的HTML字符串，若是`Api`请求，则为接口请求的结果；
>   - `bodyElement`：<font color="MediumOrchid">Element</font>  `<body>`标签的DOM对象，此类属于`org.jsoup.nodes.Element`，例如获取倒数第二个`<li>`标签的DOM元素：
>      ```javascript
>     bodyElement.selectFirst(".pagination li:nth-last-of-type(2)");
>      ```
>     更多用法请参考`org.jsoup.nodes.Element`类。
> - `Js`代码规则同上，有5个参数：
>   - `baseUrl`：当前网站的根链接，例如：`https://www.example.com`；
>   - `pageIndex`：页码，从2开始；
>   - `preUrl`：上一页链接；
>   - `bodyElementStr`：当前页面`<body>`标签内的HTML字符串，若是`Api`请求，则为接口请求的结果；
>   - `bodyElement`：`<body>`标签的DOM对象，这是一个`Java`对象，此类属于`org.jsoup.nodes.Element`，可在`js`代码直接调用`java`方法，例如获取倒数第二个`<li>`标签的DOM元素：
>     ```javascript
>      let element = bodyElement.selectFirst(".pagination li:nth-last-of-type(2)");
>      let nextUrl = element.selectFirst("a").attr("href");
>      ```
>     更多用法请参考`org.jsoup.nodes.Element`类。
> ----------------------------
> ##### <font color="grey">listMainRules</font>.<font color="249aff">useNextListMainApi</font>：
> 目录列表的下一页目录是否为使用API请求的方式获取。
> - true: 使用API请求方式获取。
> - false: 使用HTML页面获取。
> ----------------------------
> ##### <font color="grey">listMainRules</font>.<font color="249aff">nextListMainApiDataRule</font>：
> 目录列表的下一页目录为API请求时，获取下一页目录的JSONPath规则
----------------------------

#### <font color="#249aff">chapterRules</font>：
章节处理规则，用于处理章节数据，获取章节内容。
> ##### <font color="grey">chapterRules</font>.<font color="249aff">url</font>：
> 用于获取章节内容，可以`置空`，可以是`url`地址。
>> 1. 只有一种情况可以置空，`listMainRules`这里获取到的结果是一个`html页面`，并且`listMainRules.listMainElementName`不为空。
>>
>> 2. `GET`和`POST`请求规则同上（`searchRules.url`已介绍），这里有两个参数：`bookId`、`itemId`，两个参数分别代表：  
>> - `bookId`：小说id；  
>> - `itemId`：章节id；
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">urlDataRule</font>：
> 获取章节内容的JSONPath规则，用于处理请求结果数据，获取章节内容。只有当请求结果返回JSON格式的数据时才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取章节内容，例如：
>> 返回结果为：
>> ```json
>> {
>>   "code": 200,
>>   "modelData": {
>>     "content": "chapter1 Hello World! chapter content test" 
>>   },
>>   "message": "success"
>> }
>> ```
>> 要获取章节内容，那么`urlDataRule`填写：`$.modelData.content`
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">contentHandleRule</font>：
> 章节内容处理规则，对获取到的章节内容进行处理，例如文字解码、规则替换等等。
>> 此处填写的规则为`java`/`Js`代码，若觉得使用代码比较繁琐，只想使用正则表达式对内容进行一个简单的替换，可以使用下面的`chapterRules.contentRegexList`；  
>> - `Java`代码入口方法为`execute`，参数为`String content`， **返回值**为处理后的章节内容，例如：  
>> 如果要对内容进行base64解码，那么`contentHandleRule`填写：
>> ```xml
>> <java>
>>   <package_import>import com.wei.wreader.utils.data.*;</package_import>
>>   <code>
>>     public String execute(String content){
>>       return StringUtil.base64Decode(content, true);
>>     }
>>   </code>
>> </java>
>> ```
>> - `Js`代码规则同上，参数为`content`， **返回值**为处理后的章节内容，例如：
>>  ```javascript
>>  let newContent = window.atob(content);
>>  newContent = newContent.replace(/\\n/g, '<br>');
>>  (newContent)
>>  ```
>> 
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">useContentOriginalStyle</font> `无效`：
> 是否使用原始样式，布尔类型。
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">replaceContentOriginalRegex</font>`无效`：
> 原始样式内容替换的正则表达式。
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">contentElementName</font>：
> 用于获取章节内容，`CssSelector`选择器语法，只有当获取到的结果是一个`html页面`时才需要填写，例如：
>> ```html
>> <body>
>> <h3>chapter1 Hello World</h3>
>> <div class="content">
>>     <p>chapter content test.chapter content test.</p>
>>     <p>chapter content test.chapter content test.</p> 
>> </div>
>> </body>
>> ```
>> 要获取章节内容，那么`contentElementName`填写：`.content`，等同于js的`document.querySelectorAll('.content')`。
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">nextContentUrl</font>：
> 本章节下一页内容的链接（针对某些网站会把一章内容分成多页的情况）。
>> 此处填写的规则为`Java`/`Js`代码：
>> 1. `Java`规则同上，有四个参数:   
>>     - `chapterUrl`<font color="MediumOrchid">String</font>：当前章节链接；
>>     - `loadingPage`<font color="MediumOrchid">int</font>：正在加载的页码，从1开始；
>>     - `preContentUrl`<font color="MediumOrchid">String</font>：上一页链接（即当前视图正在显示的页面链接），若为空时，代表是第一页；
>>     - `prePageContent`<font color="MediumOrchid">String</font>：上一页内容（即当前视图正在显示的页面），第一页则为第一页本身内容。 若是HTML页面，则为`<body></body>`标签内的HTML元素；若是JSON数据，则为返回的JSON数据字符串。
>> 
>> 说明：
>>    - **返回值**为下一页链接。
>>    - 返回的下一页链接的请求类型可以是`GET`或`POST`，其规则与`searchRules.url`中1和2介绍的一致，但没有参数占位符。  
>>    - 当章节所有页面都加载完成后，侧边栏滚动条会滚动回顶部。  
>>    - **特别注意的是，只有`execute`方法返回空值时，请求下一页的进程才会结束，否则系统会一直请求；例子：可以通过查找页面是否有下一页的按钮来判断是否为最后一页。**  
>>    
>> 示例：
>>```xml
>> <java>
>>     <package_import></package_import>
>>     <code>
>>         public String execute(String chapterUrl, int loadingPage, String preContentUrl, String prePageContent) {
>>             if (!prePageContent.contains("next.png")) { 
>>                 return "";
>>             } 
>>             String nextContentUrl = chapterUrl.replaceAll(".html", String.format("_%s.html", loadingPage));
>>             // 任选一种返回方式
>>             // 返回方式1：直接返回请求链接                    
>>             // return nextContentUrl;
>>             
>>             // 返回方式2：返回请求格式字符串
>>             // return String.format("{\"url\":\"%s\",\"method\":\"POST\",\"queryParams\":{},\"bodyParams\":{\"tt\":\"123456\",},\"header\":{}}", nextContentUrl);
>>         }
>>     </code>
>> </java>
>> ```
>> 2. `Js`规则同上，参数有五个：
>>    - `chapterUrl`：当前章节链接；
>>    - `loadingPage`：正在加载的页码，从1开始；
>>    - `preContentUrl`：上一页链接（即当前视图正在显示的页面链接），若为空时，代表是第一页；
>>    - `prePageContent`：上一页内容（即当前视图正在显示的页面），第一页则为第一页本身内容。 若是HTML页面，则为`<body></body>`标签内的HTML元素；若是JSON数据，则为返回的JSON数据字符串。
>>    - `prePageElement`：上一页内容（即当前视图正在显示的页面），第一页则为第一页本身内容。 为`<body></body>`标签的DOM对象，这是一个Java对象，此类属于org.jsoup.nodes.Element，可在js代码直接调用java方法。  
>> 
>> 说明：
>> - 返回值为下一页链接。
>> - 返回的下一页链接的请求类型可以是`GET`或`POST`，其规则与`searchRules.url`中1和2介绍的一致，但没有参数占位符。
>> - 当章节所有页面都加载完成后，侧边栏滚动条会滚动回顶部。
>> - 返回空值时，请求下一页的进程才会结束。
>> 
>> 示例：
>> ```javascript
>> @js: 
>> function execute() {
>>    let nextContentLinkHtmlElement = prePageElement.selectFirst('.read_nav a:nth-last-of-type(1)');
>>    if (nextContentLinkHtmlElement == null) {
>>        return "";
>>    }
>>    let nextContentUrl = nextContentLinkHtmlElement.attr("onclick");
>>    if (!nextContentUrl || nextContentUrl == '') {
>>        return "";
>>    }
>>    let url = nextContentUrl.substring(nextContentUrl.indexOf("='") + 2,
>>        nextContentUrl.length - 1);
>>    return (!url || nextUrurll == "") ?
>>        "" :
>>        chapterUrl.replace('.html', `_${loadingPage}.html`);
>> }
>> (execute())
>> ```
>> 
>>
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">useNextContentApi</font>：
> 获取下一页的返回结果是否为JSON格式数据，布尔类型。
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">nextContentUrlDataRule</font>：
> 获取下一页内容的`JSONPath`语法规则。只有当`useNextContentApi`为`true`时（即请求结果返回JSON格式的数据时）才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取下一页内容，例如：
>> ```json
>> {
>>   "code": 200,
>>   "modelData": {
>>     "title": "chapter1 Hello World", 
>>     "page": 2,
>>     "totalPage": 5,
>>     "content": "chapter1 page 2 Hello World! chapter content test" 
>>   },
>>   "message": "success"
>> 
>> ```
>> 要获取章节下一页内容，那么`nextContentUrlDataRule`填写：`$.modelData.content`
> ____________________________
> ##### <font color="grey">chapterRules</font>.<font color="249aff">contentRegexList</font>`数组`：
> 章节内容处理规则的正则表达式列表，可以通过正则表达式对内容进行过滤，如空行替换，删除广告等等。这里是一个数组，所以记得使用[]包起来。  
> 填写规则为：`正则表达式（或原内容）@replace:替换的内容`；  
> 例如：
> - 空的p标签替换空字符串：`<p\s*>\s*</p>@replace:`；
> - 替换文字：`hello world@replace:你好，世界`
____________________________

#### <font color="#249aff">bookInfoRules</font>：
小说基本信息处理规则，获取小说的基本信息。此规则在搜索小说列表时才会用到，有以下两种情况：
1. 搜索小说时请求的结果是html页面，此规则将用于存储从页面上获取到的小说名称和小说URL链接，此情况下，`bookInfoRules`中的字段名称任填，不与其它字段重复即可；
2. 搜索小说时请求的结果是json数据，通过`bookInfoRules`设定的字段名称从JSON结果数据中获取对应字段的值，此情况下，`bookInfoRules`中的字段名称必须与搜索结果数据中的字段名称一致，否则将无法获取字段值；  
   ***提示：如果部分字段不存在或者不了解，可以填写任意与其他字段不重复的字符，但不能为空。（不能重复！不能重复！不能重复！）***

> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookIdField</font>：
> 小说id字段名称，用于获取小说id。
> ____________________________
> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookNameField</font>：
> 小说名称字段名称，用于获取小说名称。
> ____________________________
> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookUrlField</font>：
> 小说链接字段名称，用于获取小说链接。
> ____________________________
> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookAuthorField</font>：
> 小说作者字段名称，用于获取小说作者。
> ____________________________
> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookDescField</font>：
> 小说简介字段名称，用于获取小说简介。
> ____________________________
> ##### <font color="grey">bookInfoRules</font>.<font color="249aff">bookImgUrlField</font>：
> 小说封面字段名称，用于获取小说封面。
____________________________
