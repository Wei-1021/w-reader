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
