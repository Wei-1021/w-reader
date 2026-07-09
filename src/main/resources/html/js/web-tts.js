// 初始化语音合成对象
const synth = window.speechSynthesis;

// 获取HTML元素
const inputForm = document.querySelector("form");
const inputTxt = document.querySelector(".txt");
const voiceSelect = document.querySelector("select");

const pitch = document.querySelector("#pitch");
const pitchValue = document.querySelector(".pitch-value");
const rate = document.querySelector("#rate");
const rateValue = document.querySelector(".rate-value");

// 语音列表数组
let voices = [];

/**
 * 填充语音列表
 * 获取可用的语音列表并按名称排序，然后动态生成下拉选项
 */
function populateVoiceList() {
    voices = synth.getVoices().sort(function (a, b) {
        const aname = a.name.toUpperCase();
        const bname = b.name.toUpperCase();

        if (aname < bname) {
            return -1;
        } else if (aname == bname) {
            return 0;
        } else {
            return +1;
        }
    });
    const selectedIndex = voiceSelect.selectedIndex < 0 ? 0 : voiceSelect.selectedIndex;
    voiceSelect.innerHTML = "";

    for (let i = 0; i < voices.length; i++) {
        const option = document.createElement("option");
        option.textContent = `${voices[i].name} (${voices[i].lang})`;

        if (voices[i].default) {
            option.textContent += " -- DEFAULT";
        }

        option.setAttribute("data-lang", voices[i].lang);
        option.setAttribute("data-name", voices[i].name);
        voiceSelect.appendChild(option);
    }
    voiceSelect.selectedIndex = selectedIndex;
}

// 初始填充语音列表
populateVoiceList();

// 当语音列表变化时，重新填充语音列表
if (speechSynthesis.onvoiceschanged !== undefined) {
    speechSynthesis.onvoiceschanged = populateVoiceList;
}

/**
 * 发声函数
 * 检查当前是否有语音正在播放，如果有则不执行，否则根据用户输入和设置的参数发声
 */
function speak() {
    if (synth.speaking) {
        console.error("speechSynthesis.speaking");
        return;
    }

    if (inputTxt.value !== "") {
        const utterThis = new SpeechSynthesisUtterance(inputTxt.value);

        utterThis.onend = function (event) {
            console.log("SpeechSynthesisUtterance.onend");
        };

        utterThis.onerror = function (event) {
            console.error("SpeechSynthesisUtterance.onerror");
        };

        const selectedOption = voiceSelect.selectedOptions[0].getAttribute("data-name");

        for (let i = 0; i < voices.length; i++) {
            if (voices[i].name === selectedOption) {
                utterThis.voice = voices[i];
                break;
            }
        }
        utterThis.pitch = pitch.value;
        utterThis.rate = rate.value;
        synth.speak(utterThis);
    }
}

// 表单提交事件处理函数
inputForm.onsubmit = function (event) {
    event.preventDefault();

    speak();

    inputTxt.blur();
};

// 音高变化事件处理函数
pitch.onchange = function () {
    pitchValue.textContent = pitch.value;
};

// 语速变化事件处理函数
rate.onchange = function () {
    rateValue.textContent = rate.value;
};

// 语音选择变化事件处理函数
voiceSelect.onchange = function () {
    speak();
};

document.writeln("<script src='\/js\/style.js'><\/script>");
//内容页设置
function readconf(type) {
    var huyan = document.getElementById("huyan");
    var light = document.getElementById("light");
    if (type == "huyan") {
        var value = (huyan.className == "button huyan-on") ? 'huyan' : 'no';
        store.set('setting_light', value);
        set("light", value);
    }
    if (type == "light") {
        var value = (light.innerHTML == "关灯") ? 'yes' : 'no';
        store.set('setting_light', value)
        set("light", value);
    }
    var font_size = 'middle';
    if (type == "big") {
        font_size = 'big';
    } else if (type == 'small') {
        font_size = 'small';
    }
    store.set('setting_font', font_size)
    set("font", font_size);
}

//内容页读取设置
function getset() {
    //关灯和护眼配置
    light = store.get('setting_light') || 'no';
    set("light", light);

    //字体配置
    font = store.get('setting_font') || 'middle';
    set("font", font);
}

//内容页应用设置
function set(type, p) {
    var nr_body = document.getElementById("read"); //页面body
    var huyan = document.getElementById("huyan"); //护眼div
    var light = document.getElementById("light"); //灯光div
    var fontfont = document.getElementById("fontfont"); //字体div
    var fontbig = document.getElementById("fontbig"); //大字体div
    var fontmiddle = document.getElementById("fontmiddle"); //中字体div
    var fontsmall = document.getElementById("fontsmall"); //小字体div
    var content = document.getElementById("content"); //内容div

    //灯光
    if (type == "light") {
        if (p == "yes") {
            //关灯
            light.innerHTML = "开灯";
            light.className = "button light-on";
            nr_body.style.backgroundColor = "#000";
            huyan.className = "button huyan-on";
            content.style.color = "#999";
        } else if (p == "no") {
            //开灯
            light.innerHTML = "关灯";
            light.className = "button light-off";
            nr_body.style.backgroundColor = "#fff";
            content.style.color = "#000";
            huyan.className = "button huyan-on";
        } else if (p == "huyan") {
            //护眼
            light.innerHTML = "关灯";
            light.className = "button light-off";
            huyan.className = "button huyan-off";
            nr_body.style.backgroundColor = "#DCECD2";
            content.style.color = "#000";
        }
    }
    //字体
    if (type == "font") {
        fontbig.className = "sizebg";
        fontmiddle.className = "sizebg";
        fontsmall.className = "sizebg";
        if (p == "big") {
            fontbig.className = "button size-on";
            content.style.fontSize = "25px";
        }
        if (p == "middle") {
            fontmiddle.className = "button size-on";
            content.style.fontSize = "20px";
        }
        if (p == "small") {
            fontsmall.className = "button size-on";
            content.style.fontSize = "14px";
        }
    }
}

function getQuery(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if(pair[0] == variable){return pair[1];}
    }
    return(false);
}

function getSelectedCheckbox(div,name){
    var checks = "";
    var val = "";
    $("#"+div+" input[name='"+name+"']").each(function(){
        if($(this).prop("checked")){
            val = $(this).val();
            if("" == checks){
                checks += val;
            }else{
                checks += ","+val;
            }
        }
    });
    return checks;
}

function refreshVcode(div){
//	var t = Date.now();
//	$("#"+div).attr("src", "/index/vcode?t="+t);
}


//提示信息框
function toast(msg, timeout) {
    timeout = timeout || 3000;
    if (!document.getElementsByClassName('toast-wrap').length) {
        var div = document.createElement('div');
        div.className = 'toast-wrap';
        div.innerHTML = '<span class="toast-msg"></span>';
        document.getElementsByTagName("body")[0].appendChild(div);
    }
    document.getElementsByClassName('toast-wrap')[0].getElementsByClassName('toast-msg')[0].innerHTML = msg;
    var toastTag = document.getElementsByClassName('toast-wrap')[0];
    toastTag.style.display = "block";
    setTimeout(function () {
        toastTag.style.display = 'none';
    }, timeout);
}

function page() {
    var $pagenum ;
    var $pagenum2 = 0;
    $('.page_txt').focus(function(){
        if($pagenum2 == ""){
            $pagenum = $(this).val();
        }
        $(this).val("");
        $(this).next().text('转　到');
        $(this).next().addClass('goto');
    });
    $('.page_txt').blur(function(){
        if($(this).val() == ""){
            if($pagenum2 == ""){
                $(this).val($pagenum);
            }else{
                $(this).val($pagenum2);
            }
            $(this).next().text('下一页');
            $(this).next().removeClass('goto');
        }else{
            $pagenum2 = $pagenum;
            $page = $(this).val();
            var url = decodeURI($(".goto").attr("href"));
            if($page != ""){
                url = url.split("/");
                var urllength = url.length;
                var lasturl = url[urllength-1];
                lasturl = lasturl.replace(/[0-9]/ig,$page);
                url[urllength-1] = lasturl;
                url = url.join("/");
                $(".goto").attr("href",encodeURI(url));
            }
        }
        if($("#nextPage").attr("class")=="goto"){
            var url=$("#nextPage").attr("href");
            url=url.replace(/page\=\d+/,"page="+$("#txtPage").val());
            $("#nextPage").attr("href",url);
        }
    });
}

function login_page() {
    var html = '';
    var isLogin = Object.keys(store.get('user_info') || {}).length ? true : false;
    if (isLogin) {
        var user_info = store.get('user_info') || {};
        html = '<a href="/user/userinfo.html" class="userin">' + user_info.username + '</a>';
    } else {
        html = '<a class="user" href="/user/login.html">登录</a>' +
            '<a href="/user/register.html" class="user">注册</a>';
    }
    document.writeln(html);
}

function search_page() {
    var html = '<form method="get" class=\"searchForm\" accept-charset=\"UTF-8\" action="/user/search.html">\n' +
        '<input id="q" name="q" type="text" class="searchForm_input" onclick="this.value=\'\'" value="输入书名或作者"/>\n' +
        '<input type="submit" value=\"搜索\" class="searchForm_btn">\n' +
        '</form>';
    document.writeln(html);
}

function search_before() {
    if (document.cookie != "") {
        bgt=bgt.slice(0, bgt.length-1);
    }
    $("#loading").css('display', 'block');
}

function search_after() {
    $("#loading").css('display', 'none');
}

function Book() {
    this.user_info = store.get('user_info') || {};
    this.token = Object.keys(this.user_info).length ? this.user_info.token : '';
}

Book.prototype = {
    addBook: function (book_id) {
        this.checkLogin();
        var url = '/api/addBook';
        var params = {'token': this.token, 'bid': book_id};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                toast('恭喜您已成功加入到书架中！');
                $("#saveBookBtn").html("<font color=red>已加入书架</font>");
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    getBook: function () {
        this.checkLogin();
        var url = '/api/findBook';
        var params = {'token': this.token};
        $.post(url, params, function (json) {
            var bookList = json.data;
            if (json.code == 0) {
                var interText = doT.template($("#tpl-bookcase").text());
                $("#main").html(interText(bookList));
            } else if (json.code == 201) {
                location.href = '/user/login.html';
            }
        }, 'json');
    },
    delBook: function (book_id) {
        this.checkLogin();
        var url = '/api/delBook';
        var params = {'token': this.token, 'bid': book_id};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                $('#book' + book_id).remove();
                toast('删除成功！');
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    getBookcase: function (site_book_id) {
        if (this.token == '' || this.token == undefined) {
            return ;
        }
        var url = '/api/getBookMark';
        var params = {'token': this.token, 'bid': site_book_id};
        $.get(url, params, function (json) {
            if (json.code == 0) {
                if (json.data.id > 0) {
                    $("#saveBookBtn").html("<font color=red>已加入书架</font>");
                }
            }
        }, 'json');
    },
    getBookMark: function (site_book_id, site_chapter_id) {
        if (this.token == '' || this.token == undefined) {
            return ;
        }
        var url = '/api/getBookMark';
        var params = {'token': this.token, 'bid': site_book_id};
        $.get(url, params, function (json) {
            if (json.code == 0) {
                if (json.data.site_chapter_id == site_chapter_id) {
                    $("#saveMarkBottomBtn").html("<font color=red>已加入书签</font>");
                }
            }
        }, 'json');
    },
    readLog: function (type) {
        type = parseInt(type) || 0;
        if (type == 1) {
            bookList = store.get('book_readlog') || [];
            if (bookList.length) {
                if (bookList.length >= 10) {
                    bookList.pop();
                }
                for (var i in bookList) {
                    if (parseInt(bookList[i].book_id) == parseInt(info.book_id)) {
                        bookList.splice(i, 1);
                        break;
                    }
                }
            }
            bookList.unshift(info);
            store.set('book_readlog', bookList);
        } else if (typeof (doT) == 'object') {
            bookList = store.get('book_readlog') || [];
            var interText = doT.template($("#tpl-readlog").text());
            $("#main").html(interText(bookList));
        }
    },
    removeLog: function(book_id) {
        bookList = store.get('book_readlog') || [];
        for (var i in bookList) {
            if (parseInt(bookList[i].book_id) == book_id) {
                bookList.splice(i, 1);
                break;
            }
        }
        store.set('book_readlog', bookList);
        $("#log" + book_id).remove();
    },
    chapterSort: function () { //章节排序
        var oUl = document.getElementById('chapter-list');
        var oLi = oUl.getElementsByTagName('li');
        for (var i = 0, arr = []; i < oLi.length; i++) {
            arr[i] = oLi[i];
        }
        arr.reverse();
        for (var i = 0; i < arr.length; i++) {
            oUl.appendChild(arr[i]);
        }
        var text = document.getElementById('order').innerText;
        document.getElementById('order').innerText = (text == '[倒序]') ? '[正序]' : '[倒序]';
    },
    checkLogin: function () { //验证登录
        if (this.token == '' || this.token == undefined) {
            var referer = window.location.href;
            location.href = '/user/login.html?referer='+encodeURIComponent(referer);
        }
    },
    userLogin: function () { //用户登录
        var username = $("#loginForm #username").val();
        var password = $("#loginForm #password").val();
        //var expire = $("#loginForm #usecookie").find('option:checked').val();
        if (username == '') {
            toast('请输入用户名！');
            return false;
        }
        if (password == '') {
            toast('请输入密码！');
            return false;
        }
        var referer = getQuery('referer');
        var url = '/api/login';
        var params = {'username': username, 'password': password};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                store.set('user_info', json.data);
                if ("" != referer && false != referer) {
                    location.href = decodeURIComponent(referer);
                }else{
                    location.href = '/';
                }
            } else {
                toast(json.msg);
            }
        }, 'json');
        return false;
    },
    userRegister: function () {
        var username   = $("#regForm #username").val();
        var password   = $("#regForm #password").val();
        var repassword = $("#regForm #repassword").val();
        var email      = $("#regForm #email").val();

        var xarg = /^[a-z0-9][a-z0-9@\-._]{5,31}$/i;
        if (username == '' || !xarg.test(username)) {
            toast('用户名格式有误，6-32位字母、数字或_.-@组成！');
            return false;
        }
        if (password == '' || !xarg.test(password)) {
            toast('密码格式有误，6-32位字母、数字或_.-@组成！');
            return false;
        }
        if (repassword == '') {
            toast('请输入确认密码！');
            return false;
        }
        if (password != repassword) {
            toast('输入的两次密码不一致！');
            return false;
        }
        var url = '/api/register';
        var params = {'username': username, 'password': password, 'email': email};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                store.set('user_info', json.data);
                location.href = '/';
            } else {
                toast(json.msg);
            }
        }, 'json');
        return false;
    },
    editPassword: function () {
        this.checkLogin();
        var password = $("#password").val();
        var repassword = $("#repassword").val();

        var xarg = /^[a-z0-9][a-z0-9@\-._]{5,31}$/i;
        if (password == '' || !xarg.test(password)) {
            toast('密码格式有误，6-32位字母、数字或_.-@组成！');
            return false;
        }
        if (repassword == '') {
            toast('请输入确认密码！');
            return false;
        }
        if (password != repassword) {
            toast('输入的两次密码不一致！');
            return false;
        }
        var url = '/api/editPassword';
        var params = {'token': this.token, 'password': password};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                toast('密码修改成功');
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    logout: function () {
        store.remove('user_info');
        location.href = '/';
    },
    userInfo: function () {
        if (this.token) {
            document.writeln(this.user_info.username);
        } else {
            location.href = '/user/login.html';
        }
    },
    addMark: function (bid, cid, cname) {
        this.checkLogin();
        //cname = encodeURIComponent(cname || '');
        var url = '/api/mark';
        var params = {'token': this.token, 'bid': bid, 'cid': cid, 'cname': cname};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                toast('书签加入成功！');
                $("#saveMarkBottomBtn").html("<font color=red>已加入书签</font>");
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    userVote: function (bid) {
        this.checkLogin();
        var url = '/api/vote';
        var params = {'token': this.token, 'bid': bid};
        $.post(url, params, function (json) {
            if (json.code == 0) {
                toast('投票成功！');
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    bookStats: function (bid) {
        var url = '/api/access';
        $.post(url, {'bid': bid}, function (json) {}, 'json');
    },
    search: function () {
        if ("" == vw) {
            return ;
        }
        if ("" == abw) {
            return ;
        }
        if ("" == ru) {
            return ;
        }
        if ("" == jrt) {
            return ;
        }
        if ("" == van) {
            return ;
        }
        if ("" == fw) {
            return ;
        }
        if ("" == cwl) {
            return ;
        }
        if ("" == gpr) {
            return ;
        }
        if ("" == uyoo) {
            return ;
        }
        if ("" == tz) {
            return ;
        }
        if ("" == euu) {
            return ;
        }
        if ("" == tsn) {
            return ;
        }
        if ("" == eju) {
            return ;
        }
        if ("" == um) {
            return ;
        }
        if ("" == fp) {
            return ;
        }
        if ("" == dvm) {
            return ;
        }
        if ("" == jpk) {
            return ;
        }
        if ("" == deblkx) {
            return ;
        }
        if ("" == ht) {
            return ;
        }
        if ("" == azy) {
            return ;
        }
        if ("" == sna) {
            return ;
        }
        if ("" == wqx) {
            return ;
        }
        if ("" == fpp) {
            return ;
        }
        if ("" == rup) {
            return ;
        }
        if ("" == jwj) {
            return ;
        }
        if ("" == bgt) {
            return ;
        }
        if ("" == qp) {
            return ;
        }
        if ("" == yf) {
            return ;
        }
        if ("" == cw) {
            return ;
        }
        if ("" == wq) {
            return ;
        }
        var keyword = decodeURIComponent(getQuery('q'));
        if ("" == keyword) {
            bookList = new Array();
            bookList['search'] = new Array();
            bookList['keyword'] = keyword || '';
            var interText = doT.template($("#tpl-search").text());
            $("#main").html(interText(bookList));
            return ;
        }
        search_before();
        var url = '/api/search';
        var params = {'q':keyword,'vw':vw,'abw':abw,'ru':ru,'jrt':jrt,'van':van,'fw':fw,'cwl':cwl,'gpr':gpr,'uyoo':uyoo,'tz':tz,'euu':euu,'tsn':tsn,'eju':eju,'um':um,'fp':fp,'dvm':dvm,'jpk':jpk,'deblkx':deblkx,'ht':ht,'azy':azy,'sna':sna,'wqx':wqx,'fpp':fpp,'rup':rup,'jwj':jwj,'bgt':bgt,'qp':qp,'yf':yf,'cw':cw,'wq':wq,'sign':sign};
        $.post(url, params, function (json) {
            search_after();
            if (json.code == 0) {
                bookList = json.data;
                bookList['keyword'] = keyword || '';
                var interText = doT.template($("#tpl-search").text());
                $("#main").html(interText(bookList));
            } else {
                toast(json.msg);
            }
        }, 'json');
    },
    report: function () {
        var url = '/api/report';
        var referer = getQuery('referer');
        var bid     = $("#reportForm #bid").val();
        var cid     = $("#reportForm #cid").val();
        var content = $("#reportForm #content").val();
        //var vcode   = $("#reportForm #vcode").val();
        var report_types = getSelectedCheckbox('reportForm', 'report_type');
        if ("" == report_types) {
            toast('请选择一项错误类型');
            return false;
        }
//        if ("" == vcode) {
//        	toast('请输入计算结果！');
//            return false;
//        }

        //var params = {'token': this.token, 'bid': bid, 'cid': cid, 'type': report_types, 'content': content, 'vcode': vcode};
        var params = {'token': this.token, 'bid': bid, 'cid': cid, 'type': report_types, 'content': content};
        $.post(url, params, function (json) {
            refreshVcode('img_vcode');
            if (json.code == 0) {
                toast('反馈成功！');
                if ("" != referer && false != referer) {
                    location.href = decodeURIComponent(referer);
                } else {
                    location.href = '/';
                }
            } else {
                toast(json.msg);
            }
        }, 'json');
    }
}

function Chapter() {
}

Chapter.prototype = {
    detail: function (bid) {
        var url = '/api/detail';
        var params = {'bid': bid};
        $.get(url, params, function (json) {
            var interText = doT.template($("#tpl-chapter-detail").text());
            $("#chapter-detail").html(interText(json.data));
        }, 'json');
    },
    list: function (bid, page_num) {
        var url = '/api/list';
        var params = {'bid': bid,'page_num': page_num};
        $.get(url, params, function (json) {
            var interText = doT.template($("#tpl-chapter-list").text());
            $("#chapter-list").html(interText(json.data));

            if ("" != json.data.page_style3) {
                var interTextPage = doT.template($("#tpl-chapter-list-page").text());
                $("#chapter-list-page").html(interTextPage(json.data));
            }
        }, 'json');
    },
    content: function (cid) {
        var url = '/api/content';
        var params = {'cid': cid};
        $.get(url, params, function (json) {
            var interText = doT.template($("#tpl-chapter-content").text());
            $("#chapter-content").html(interText(json.data));
        }, 'json');
    }
}

window.book = new Book();
window.chapter = new Chapter();
