# 自定义书源规则说明
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
        "listMainElementName": "",
        "urlElement": "",
        "titleElement": ""
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
### 配置项说明:
`enabled`: 是否启用  
`id`：书源规则的ID，用于标识书源唯一性（一般为网站域名）。  
`name`：书源名称，即网站名称。  
`baseUrl`：书源的根URL，用于拼接其他URL（**注意要带上http://或https://**），例如：https://www.testsite.com。  
`header`：请求头，用于模拟请求（暂时没用，字段值留空，别删）。  
`hasHtml`：是否启用HTML解析，默认为`true`，如果为`false`则表示返回结果为JSON数据。
> 以下两种情况会用到`hasHtml`：
> * 1.搜索小说列表：用于判断当前请求的结果是否为HTML页面，如果搜索小说链接返回的是一个html页面，那么此字段必须设置为true;
> * 2.获取小说目录：用于判断获取小说目录时返回的结果是否为HTML页面，只有当listMainRules.url不为空，且hasHtml=false时，系统才会判定为请求结果为json数据，否则统一认定为html页面;  

`searchRules`： 搜索规则。  
> ##### `url`：用于获取搜索小说的结果列表，可以是url地址，也可以是一段java代码。
>> 1. 若url为`GET`请求，则直接填写url地址，请求参数用`${关键字/运算表达式}`占位符替代，这里有两个参数：`key`和`page`，分别表示搜索的关键字和页码， 但是因为目前没有翻页功能，所以页码参数实际上会固定设置为1，例如：https://www.testsite.com/search?q=${key}&page=${page}。
>> 
>> 2. 若url为`POST`请求，则使用以下JSON格式填写：
>> ```json
>> {
>>     "url": "URL地址",
>>     "method": "请求类型",
>>     "queryParams": {
>>         "参数1": "值1",
>>         "参数2": "值2"
>>     },     
>>     "bodyParams": {
>>         "参数1": "值1",
>>         "参数2": "值2"
>>     },
>>     "header":{
>>         "请求头参数1": "值1"
>>     }
>> }
>> ```
>> 其中url：URL地址，  
>> `method`：请求类型： `GET/POST`，默认为`GET`，  
>> `queryParams`：查询参数（即拼接在url后面的参数），  
>> `bodyParams`：请求体参数，  
>> `header`：请求头  
>> 请求参数用`${关键字/运算表达式}`占位符替代，这里有两个参数：`key`和`page`，分别表示搜索的关键字和页码，例如：
>> ```json
>> {
>>     "url":"http://www.testsite.com/search",
>>     "method": "POST",
>>         "queryParams": {
>>         "q": "${key}"
>>     },
>>     "bodyParams": {
>>         "keyword":"${key}",
>>         "page":"${page}"
>>     },
>>     "header":{
>>         "Content-Type": "text/html; charset=utf-8"
>>     }
>> }
>> ```
>> 
>> 3. 若为`java`代码，则需要使用以下格式填写：  
>> ```xml
>> <java>
>>     <package_import>导入的包</package_import>
>>     <code>要执行的代码</code>
>> </java>
>> ```
>> 例如:
>> ```xml
>> <java>
>>     <package_import>import java.util.*;</package_import>
>>     <code>
>>         public String execute(String key, String page) {
>>             String uuid = UUID.randomUUID().toString().replaceAll("-", "");
>>             return "http://www.wreaderbook.com/search?q=" + key + "&amp;page=" + page + "&amp;t=" + uuid;";
>>         }
>>     </code>
>> </java>
>> ```
>> 其中：入口方法为`execute`，参数为：`String key`和`String page`，
>> 两个参数分别代表：  
>> `key`：搜索的关键字；  
>> `page`：页码，当前版本固定为1；  
>> `execute`方法的返回值为请求小说搜索结果列表的`url`链接。
> 
> ##### `dataBookListRule`：获取小说列表的JSONPath语法规则，用于处理搜索结果数据，只有当搜索结果返回`JSON`格式的数据时才需要填写，填写规则请参考`JSONPath`，例如接口返回的数据如下：  
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
> 
> ##### `bookListElementName`：用于获取搜索结果列表的`cssQuery`选择器语法，适用于HTML页面，例如：返回的页面结构为：
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
> 
> ##### `bookListUrlElement`： 用于获取搜索结果列表中每个结果的url（每本小说的链接）的`cssQuery`选择器语法，适用于HTML页面，注意：这里是以列表的单项的页面结构为基准，以上面的页面结构为例，则以：
>> ```html
>> <li>
>>     <div class="li-dd">
>>         <a class="link" href="/book/1/" title="BOOK 001">BOOK 001</a>
>>         <p class="title">BOOK 001</p>
>>     </div>
>> </li>
>> ```
>> 为基准，所以想要获取url信息，那么要填写：`.li-dd a`， 等同于js的`document.querySelectorAll('.book-list ul li')[0].querySelector('.li-dd a').href`。需要特别强调的是，获取的url信息是标签的`href`属性的值，所以最终需要定位的元素必须是a标签
> 
> ##### `bookListTitleElement`：用于获取搜索结果列表中每个结果的标题（每本小说的标题）的`cssQuery`选择器语法，适用于HTML页面，注意：这里是以列表的单项的页面结构为基准，以`bookListElementName`的页面结构为例，则以：
>> ```html
>> <li>
>>     <div class="li-dd">
>>         <a class="link" href="/book/1/" title="BOOK 001">BOOK 001</a>
>>         <p class="title">BOOK 001</p>
>>     </div>
>> </li>
>>```
>> 为基准，所以要获取标题信息，你可以填写：`.li-dd a` 或 `.li-dd .link` 或 `.li-dd p` 或 `.li-dd .title`，等同于js的`document.querySelectorAll('.book-list ul li')[0].querySelector('.li-dd a').textContent`。

`listMainRules`: 目录规则。
> ##### `url`： 用于获取小说目录的列表，可以`置空`，可以是`url地址`，也可以是一段`java`代码。
>> 1. 只有一种情况可以置空，即`searchRules.url`这里请求的结果是`html页面`，并且`searchRules.bookListUrlElement`不为空。
>>
>> 2. `GET`和`POST`请求规则同上（`searchRules.url`已介绍），但这里的参数只有一个：`bookId`，表示小说id
>>
>> 3. `java`代码规则同上（`searchRules.url`已介绍），入口方法为`execute`，参数可以是`String bookId`或者`BookInfo`实体对象两者中的一个，不可以同时传入这两个参数，
> 其中：  
> `bookId`表示小说id；  
> `BookInfo`对象表示小说信息，包含字段：`bookId`（小说id）、`bookName`（小说名称）、`bookUrl`（小说链接）、
> `bookAuthor`（小说作者）、`bookDesc（`小说描述）、`bookImgUrl`（小说图片链接）；  
> 返回值为请求获取小说目录的`url`链接。
> 
> ##### `urlDataRule`：获取目录列表的JSONPath语法规则，用于处理目录列表结果数据。只有当请求结果返回JSON格式的数据时才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取目录列表，例如：
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
>> 
>
> ##### `itemIdField`：目录列表中各章节id对应的字段名称。当获取目录返回的结果为JSON格式数据时，在`urlDataRule`规则获取到数据后，通过`itemIdField`规则获取目录`每一个章节的id`。例如，获取的目录数据为：
>> ```json
>> [
>>     {
>>         "chapterId": 1,
>>         "title": "chapter 1"
>>     },
>>     {
>>         "chapterId": 2,
>>         "title": "chapter 2"
>>     }
>> ] 
>>```
>> 获取目录列表中每个目录的章节id，那么`itemIdField`填写：`chapterId`
> 
> ##### `itemTitleField`： 目录列表中各章节标题对应的字段名称。当获取目录返回的结果为JSON格式数据时，在`urlDataRule`规则获取到数据后，通过`itemTitleField`规则获取目录`每一个章节的标题`。 以上面的数据为例，获取目录列表中每个目录的章节标题，那么`itemTitleField`填写：`title`
> 
> ##### `listMainElementName`： 用于获取目录列表的`cssQuery`选择器语法，适用于HTML页面，例如页面结构为：
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
> 
> ##### `urlElement`：章节链接`cssQuery`语法，以目录列表的单项结构作为基准，以上面的页面结构为例，那么将以下面的html作为获取数据的基准结构：
>> ```html
>> <dd><a href="/chapter/1.html">第一章 001</a></dd>
>> ``` 
>> 因此`urlElement`填写：`a`，等同于js的`document.querySelectorAll('.chapterList dl dd')[0].querySelector('a').href`
> 
> ##### `titleElement`：章节标题`cssQuery`语法，同`urlElement`一样，以目录列表的单项结构作为基准，以上面的页面结构为例，`titleElement`填写：`a`， 等同于js的`document.querySelectorAll('.chapterList dl dd')[0].querySelector('a').textContent`
>

`chapterRules`：章节处理规则，用于处理章节数据，获取章节内容。
> ##### `url`: 用于获取章节内容，可以`置空`，可以是`url`地址。
>> 1. 只有一种情况可以置空，`listMainRules`这里获取到的结果是一个`html页面`，并且`listMainRules.listMainElementName`不为空。
>>
>> 2. `GET`和`POST`请求规则同上（`searchRules.url`已介绍），这里有两个参数：`bookId`、`itemId`，两个参数分别代表：  
> `bookId`：小说id；  
> `itemId`：章节id；  
>> 
> ##### `urlDataRule`：获取章节内容的JSONPath规则，用于处理请求结果数据，获取章节内容。只有当请求结果返回JSON格式的数据时才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取章节内容，例如：
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
> 
> ##### `contentHandleRule`：章节内容处理规则，对获取到的章节内容进行处理，例如文字解码、规则替换等等。此处填写的规则为`java`代码，入口方法为`execute`，参数为`String content`，返回值为处理后的章节内容，例如：  
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
> ##### `useContentOriginalStyle`（无效）：是否使用原始样式，布尔类型。
> 
> ##### `replaceContentOriginalRegex`（无效）：原始样式内容替换的正则表达式。
> 
> ##### `contentElementName`：用于获取章节内容，`cssQuery`选择器语法，只有当获取到的结果是一个`html页面`时才需要填写，例如：  
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
>> 
> ##### `nextContentUrl`：本章节下一页内容的链接（针对某些网站会把一章内容分成多页的情况），此处填写的规则为java代码，入口方法为`execute`：
>> 这里有三个参数:   
> `String chapterUrl`、`String preContentUrl`，`String prePageContent`，  
> 三个参数分别代表：  
> `chapterUrl`：章节链接；   
> `preContentUrl`：上一页链接；  
> `prePageContent`：上一页内容。  
> 返回值为下一页链接。  
> 提示：下一页链接的请求类型可以是`GET`或`POST`，其规则与`searchRules.url`介绍的一致，但没有参数占位符。
> **特别注意的是，只有`execute`方法返回空值时，请求获取下一页的进程才会结束，否则系统会一直请求，例如：可以通过查找页面是否有下一页的按钮来判断是否为最后一页。**
>
> ##### `useNextContentApi`：获取下一页链接的返回结果是否为JSON格式数据，布尔类型。
> 
> ##### `nextContentUrlDataRule`：获取下一页内容的数据处理规则。只有当`useNextContentApi`为`true`时（即请求结果返回JSON格式的数据时）才需要填写，填写规则请参考`JSONPath`，需要通过`JSONPath`获取下一页内容，例如：
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
> 
> ##### `contentRegexList`（数组）：章节内容处理规则的正则表达式列表，可以通过正则表达式对内容进行过滤，例如：空行替换，删除广告等等。这里是一个数组，所以记得使用[]包起来。填写规则为：`正则表达式（或原内容）@replace:替换的内容`，例如：空的p标签替换空字符串`<p\s*>\s*</p>@replace:`、替换文字`hello world@replace:你好，世界`

`bookInfoRules`: 小说信息处理规则，用于处理小说信息，获取小说信息。此规则在搜索小说列表时才会用到，有以下两种情况会用到：  
1. 搜索小说请求的结果是html页面时，此规则用于存储从页面上获取到的小说名称和小说URL链接；
2. 搜索小说请求的结果是json数据时，通过`bookInfoRules`设定的字段名称从JSON结果数据中获取对应字段的值；  
提示：如果部分字段不存在或者不了解，可以填写任意与其他字段不重复的字符，但不建议为空。  

> ##### `bookIdField`: 小说id字段名称，用于获取小说id。
> ##### `bookNameField`: 小说名称字段名称，用于获取小说名称。
> ##### `bookUrlField`: 小说链接字段名称，用于获取小说链接。
> ##### `bookAuthorField`: 小说作者字段名称，用于获取小说作者。
> ##### `bookDescField`: 小说简介字段名称，用于获取小说简介。
> ##### `bookImgUrlField`: 小说封面字段名称，用于获取小说封面。
> 
>
>