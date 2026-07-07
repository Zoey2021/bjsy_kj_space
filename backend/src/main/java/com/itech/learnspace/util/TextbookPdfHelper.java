package com.itech.learnspace.util;

import com.itech.learnspace.entity.CourseGrade;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TextbookPdfHelper {

    private static final Map<String, String> DEFAULT_PDF_BY_NAME;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("三年级上册", "/textbooks/grade3-up.pdf");
        map.put("三年级下册", "/textbooks/grade3-down.pdf");
        map.put("四年级上册", "/textbooks/grade4-up.pdf");
        map.put("四年级下册", "/textbooks/grade4-down.pdf");
        map.put("五年级上册", "/textbooks/grade5-up.pdf");
        map.put("五年级下册", "/textbooks/grade5-down.pdf");
        map.put("六年级上册", "/textbooks/grade6-up.pdf");
        map.put("六年级下册", "/textbooks/grade6-down.pdf");
        DEFAULT_PDF_BY_NAME = Collections.unmodifiableMap(map);
    }

    private TextbookPdfHelper() {
    }

    public static String resolvePdfUrl(CourseGrade grade) {
        if (grade == null) {
            return "";
        }
        if (StringUtils.hasText(grade.getPdfUrl())) {
            return grade.getPdfUrl();
        }
        String name = grade.getName() != null ? grade.getName().trim() : "";
        return DEFAULT_PDF_BY_NAME.getOrDefault(name, "");
    }
}
