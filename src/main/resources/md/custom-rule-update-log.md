# 自定义书源规则更新日志

* **V0.1.3:**
  - 【更新】 JSON模板格式更新：
    - `listMainRules`下新增字段：
      - `itemUrlField`：目录列表项URL字段名称
      - `urlDataHandleRule`：目录列表内容处理规则；只有当请求结果为JSON类型数据，并且通过`urlDataRule`规则获取到数据后，才执行此规则；此规则会对获取的目录列表数据进行进一步处理，例如：将目录列表数据中的URL字段进行拼接等；
      - `nextListMainUrl`: 目录列表的下一页目录的链接（针对某些网站会把目录列表分成多页的情况）。<br>可以是请求链接，也可以用代码，然后返回一个链接
      - `useNextListMainApi`: 目录列表的下一页目录是否为使用API请求的方式获取。<br>true: 使用API请求方式获取。<br> false: 使用HTML页面获取。
      - `nextListMainApiDataRule`: 目录列表的下一页目录为API请求时，获取下一页目录的JSONPath规则
  - 【新增】 部分规则支持`JavaScript`代码的执行，详细规则请查阅规则说明，支持的字段：
    - `searchRules.url`
    - 
      






