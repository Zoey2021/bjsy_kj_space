package com.itech.learnspace.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityHtmlService {

    public String generateMiniActivity(String title, String content) {
        String safeTitle = escapeHtml(StringUtils.hasText(title) ? title : "课堂活动");
        String body = formatContent(content != null ? content : "");
        return "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "<title>" + safeTitle + "</title>\n"
                + "<style>\n"
                + "body{font-family:-apple-system,BlinkMacSystemFont,\"Microsoft YaHei\",sans-serif;"
                + "margin:0;background:#f7f6f2;color:#333;line-height:1.7}\n"
                + ".wrap{max-width:860px;margin:0 auto;padding:24px}\n"
                + ".card{background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);padding:24px}\n"
                + "h1{font-size:22px;color:#264653;margin:0 0 16px}\n"
                + ".content{white-space:pre-wrap;font-size:15px}\n"
                + "textarea{width:100%;min-height:120px;border:1px solid #ddd;border-radius:8px;"
                + "padding:12px;font:inherit;resize:vertical}\n"
                + "button{margin-top:16px;background:#2A9D8F;color:#fff;border:none;border-radius:8px;"
                + "padding:10px 18px;font-size:14px;cursor:pointer}\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"wrap\"><div class=\"card\">\n"
                + "<h1>" + safeTitle + "</h1>\n"
                + "<div class=\"content\">" + body + "</div>\n"
                + "<label><strong>我的作答</strong></label>\n"
                + "<textarea id=\"answer\" placeholder=\"请在此记录你的学习成果…\"></textarea>\n"
                + "<button type=\"button\" onclick=\"submitAnswer()\">提交本活动</button>\n"
                + "</div></div>\n"
                + "<script>\n"
                + "function submitAnswer(){\n"
                + "  var text=document.getElementById('answer').value.trim();\n"
                + "  if(!text){alert('请先填写作答内容');return;}\n"
                + "  var payload={type:'activity',answer:text,stepCompleted:true};\n"
                + "  if(window.parent!==window){window.parent.postMessage({type:'LEARN_SPACE_SUBMIT',payload:payload},'*');}\n"
                + "  alert('已提交，请在学习空间页面确认。');\n"
                + "}\n"
                + "</script>\n"
                + "</body></html>";
    }

    private String formatContent(String content) {
        String escaped = escapeHtml(content.trim());
        if (!StringUtils.hasText(escaped)) {
            return "请阅读活动说明，完成探究任务。";
        }
        return escaped.replace("\n", "<br>");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
