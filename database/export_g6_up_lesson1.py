#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上册第1课《算法与问题解决》的学习数据。在服务器项目目录执行。"""

import base64
import csv
import json
import os
import re
import subprocess
import sys
import zipfile
from collections import defaultdict
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "export", "六年级上册第1课学情")
ZIP_PATH = os.path.join(ROOT, "export", "六年级上册第1课学情.zip")

ACT_NAME = {
    0: "阅读教材",
    1: "探究一：问题分析与建模",
    2: "探究二：算法设计",
    3: "探究三：算法验证与优化",
    98: "学习评价",
    99: "课堂小测",
}

STATUS_NAME = {
    "NOT_STARTED": "未开始",
    "IN_PROGRESS": "学习中",
    "COMPLETED": "已完成",
}

QUIZ_NEW = {
    "k1": ("解决问题通常有哪两种主要途径？", [
        "人工实施和计算机实现",
        "上网搜索和问家长",
        "写报告和做手工",
        "观察记录和口头汇报",
    ]),
    "k2": ("用计算机解决投票问题的一般过程，正确的顺序是：", [
        "设计算法 → 问题分析 → 验证优化 → 抽象建模",
        "问题分析 → 抽象与建模 → 设计算法 → 验证与优化",
        "验证优化 → 设计算法 → 问题分析 → 抽象建模",
        "购买设备 → 安装软件 → 打印结果",
    ]),
    "a1": ("在投票程序中，变量 tp 最适合表示：", [
        "投票总人数",
        "A作品的总票数",
        "当前这一次输入的投票内容",
        "已经循环的次数",
    ]),
    "a2": ("课本程序用 if tp == \"a\" … else 给 b 加一。如果用户输入的既不是 a 也不是 b，结果会怎样？", [
        "程序立刻崩溃，无法继续",
        "这次输入会被忽略，a、b 都不增加",
        "会走到 else，B 作品的票数加 1，统计结果出错",
        "程序会自动把这次输入改成 a",
    ]),
    "s1": ("“重复输入和判断，直到 1000 名学生完成投票”使用了什么控制结构？", [
        "循环结构",
        "只有顺序结构",
        "输出结构",
        "随机结构",
    ]),
    "s2": ("发现输入既不是 a 也不是 b 时会被错误统计后，最合理的做法是：", [
        "忽略问题，因为程序没有报错",
        "直接删除变量 i",
        "增加输入检查并重新验证算法",
        "把所有输入都同时给 a 和 b 加 1",
    ]),
}

QUIZ_OLD = {
    "k1": ("解决问题通常有哪两种主要途径？", [
        "人工实施和计算机实现",
        "上网搜索和问家长",
        "写报告和做手工",
        "观察记录和口头汇报",
    ]),
    "k2": ("用计算机实现算法解决问题，一般包括哪三个过程？", [
        "抽象与建模 → 设计算法 → 验证算法",
        "购买设备 → 安装软件 → 打印结果",
        "收集资料 → 写论文 → 展示海报",
        "提问 → 讨论 → 投票",
    ]),
    "a1": ("在 Logo 投票问题中，用变量 a、b 记录得票数，主要训练哪项能力？", [
        "将现实问题抽象为变量和规则",
        "背诵键盘快捷键",
        "制作动画演示",
        "排版与设计 Logo",
    ]),
    "a2": ("探究二「验证算法」时，手工代入数据逐票统计，主要目的是什么？", [
        "检验算法步骤是否正确、结果是否合理",
        "用人工方式永久代替计算机",
        "减少流程图中的判断框",
        "跳过循环结构的学习",
    ]),
    "l1": ("经过本课学习，你认为设计算法时最应坚持的原则是？", [
        "步骤完整、逻辑清晰，能应对条件变化",
        "节点越多越好才显得专业",
        "必须使用最新的软件工具",
        "只要有一个同学能看懂即可",
    ]),
    "l2": ("如果发现算法没有处理「无效投票」等异常情况，这提醒我们在信息社会中应该？", [
        "追求严谨完整的算法，关注对学习生活的影响",
        "为了简单可以忽略所有特殊情况",
        "出错后只怪使用者操作不当",
        "算法与日常生活没有关系",
    ]),
}


def mysql(sql):
    proc = subprocess.run(
        [
            "docker", "exec", "-i", "learn-mysql",
            "mysql", "--default-character-set=utf8mb4", "-N", "-B",
            "-ulearnuser", "-plearnpass123", "learn_space",
            "-e", sql,
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    err = proc.stderr.decode("utf-8", "replace")
    err = "\n".join(
        line for line in err.splitlines()
        if "Using a password on the command line interface can be insecure" not in line
    ).strip()
    if proc.returncode != 0:
        sys.stderr.write(err + "\n")
        raise SystemExit("读取数据库失败")
    rows = []
    for line in proc.stdout.decode("utf-8", "replace").splitlines():
        if line == "":
            continue
        rows.append([None if cell == r"\N" else cell for cell in line.split("\t")])
    return rows


def cell(row, idx, default=""):
    if idx >= len(row) or row[idx] is None:
        return default
    return row[idx]


def q(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def table_exists(name):
    rows = mysql(
        "SELECT COUNT(*) FROM information_schema.tables "
        "WHERE table_schema='learn_space' AND table_name=" + q(name)
    )
    return rows and rows[0][0] not in (None, "0")


def column_exists(table, column):
    rows = mysql(
        "SELECT COUNT(*) FROM information_schema.columns "
        "WHERE table_schema='learn_space' AND table_name=" + q(table)
        + " AND column_name=" + q(column)
    )
    return rows and rows[0][0] not in (None, "0")


def decode_json(b64):
    if not b64:
        return {}
    try:
        raw = base64.b64decode(b64)
        return json.loads(raw.decode("utf-8"))
    except Exception:
        return {}


def student_no(username):
    raw = (username or "").strip()
    match = re.search(r"20\d{2}(\d{2})(\d{2})$", raw)
    if match:
        return str(int(match.group(2)))
    if re.match(r"^\d{1,2}$", raw):
        return str(int(raw))
    return ""


def write_csv(path, header, rows):
    with open(path, "w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(header)
        writer.writerows(rows)


def minutes(seconds):
    try:
        return round(int(seconds or 0) / 60.0, 1)
    except Exception:
        return 0


def done_set(obj):
    found = set()
    if not isinstance(obj, dict):
        return found
    if obj.get("stepCompleted") and obj.get("activityIndex") not in (None, ""):
        try:
            found.add(int(obj.get("activityIndex")))
        except Exception:
            pass
    for item in obj.get("completedActivities") or []:
        try:
            found.add(int(item))
        except Exception:
            pass
    if obj.get("type") == "reading" or obj.get("readingCompleted"):
        found.add(0)
    if obj.get("type") == "quiz" or obj.get("quizAnswers") or obj.get("quizScore") is not None:
        found.add(99)
    if obj.get("type") == "evaluation" or obj.get("evaluationAnswers"):
        found.add(98)
    return found


def quiz_bank(answers):
    ids = [a.get("questionId") for a in answers if isinstance(a, dict)]
    if any(str(i).startswith("s") for i in ids if i):
        return "现行小测", QUIZ_NEW
    if any(str(i).startswith("l") for i in ids if i):
        return "早期小测", QUIZ_OLD
    return "未识别版本", {}


def option_text(bank, question_id, selected):
    item = bank.get(question_id)
    if not item:
        return ""
    options = item[1]
    try:
        idx = int(selected)
    except Exception:
        return ""
    if 0 <= idx < len(options):
        return options[idx]
    return ""


def field_summary(draft):
    if not isinstance(draft, dict):
        return ""
    parts = []
    for field in draft.get("fields") or []:
        if not isinstance(field, dict):
            continue
        kind = field.get("kind")
        if kind == "radio":
            parts.append("%s=%s" % (field.get("name") or "选项", field.get("value") or ""))
        elif kind == "check":
            if field.get("checked"):
                parts.append(field.get("id") or field.get("name") or "已勾选")
        else:
            value = str(field.get("value") or "").replace("\n", " ").strip()
            if not value:
                continue
            key = field.get("id") or field.get("h") or field.get("name") or field.get("table") or "填写"
            parts.append("%s=%s" % (key, value))
    extra = draft.get("extra") if isinstance(draft.get("extra"), dict) else {}
    flags = []
    if extra.get("tablePassed"):
        flags.append("表格已通过")
    if extra.get("modelPassed"):
        flags.append("模型已通过")
    if extra.get("sum1"):
        flags.append("已看到探究一小结")
    if extra.get("sum2"):
        flags.append("已看到探究二小结")
    if extra.get("sum3"):
        flags.append("已看到探究三小结")
    if extra.get("chal3"):
        flags.append("已打开探究三挑战")
    notified = extra.get("notified") or []
    if notified:
        flags.append("页面已提交环节" + ",".join(str(n) for n in notified))
    if flags:
        parts.append("；".join(flags))
    return "；".join(parts)[:4000]


def main():
    if subprocess.run(["docker", "inspect", "-f", "{{.State.Running}}", "learn-mysql"],
                      stdout=subprocess.PIPE, stderr=subprocess.PIPE).returncode != 0:
        raise SystemExit("没有找到正在运行的 learn-mysql")

    lessons = mysql(
        "SELECT cl.id, cl.title, cg.name "
        "FROM course_lesson cl "
        "JOIN course_unit cu ON cu.id = cl.unit_id "
        "JOIN course_grade cg ON cg.id = cu.grade_id "
        "WHERE cg.textbook_type='MAIN' AND cg.name='六年级上册' "
        "AND cl.title LIKE '第1课%' "
        "ORDER BY cl.id"
    )
    if not lessons:
        raise SystemExit("没有找到六年级上册第1课")
    lesson = lessons[0]
    for row in lessons:
        if row[1] and "算法" in row[1]:
            lesson = row
            break
    lesson_id = int(lesson[0])
    lesson_title = lesson[1]
    print("课时：%s（id=%s）" % (lesson_title, lesson_id))

    class_ids = set()
    for row in mysql(
        "SELECT DISTINCT u.class_id FROM learn_progress p "
        "JOIN sys_user u ON u.id=p.student_id "
        "WHERE p.lesson_id=%d AND u.class_id IS NOT NULL" % lesson_id
    ):
        class_ids.add(int(row[0]))
    if table_exists("learn_submission_log"):
        for row in mysql(
            "SELECT DISTINCT u.class_id FROM learn_submission_log l "
            "JOIN sys_user u ON u.id=l.student_id "
            "WHERE l.lesson_id=%d AND u.class_id IS NOT NULL" % lesson_id
        ):
            class_ids.add(int(row[0]))
    for row in mysql(
        "SELECT id FROM sys_class WHERE grade_name='六年级' OR name LIKE '%六年级%'"
    ):
        class_ids.add(int(row[0]))
    if not class_ids:
        raise SystemExit("没有找到六年级班级，也没有这节课的学习记录")

    id_sql = ",".join(str(i) for i in sorted(class_ids))
    tier_col = "u.tier_override" if column_exists("sys_user", "tier_override") else "NULL"
    record_sql = "u.id IN (SELECT student_id FROM learn_progress WHERE lesson_id=%d)" % lesson_id
    if table_exists("learn_submission_log"):
        record_sql += " OR u.id IN (SELECT student_id FROM learn_submission_log WHERE lesson_id=%d)" % lesson_id
    students = mysql(
        "SELECT u.id, u.username, u.real_name, IFNULL(u.class_id,''), IFNULL(c.name,'未分班'), "
        "IFNULL(t.real_name,''), " + tier_col + " "
        "FROM sys_user u "
        "LEFT JOIN sys_class c ON c.id=u.class_id "
        "LEFT JOIN sys_user t ON t.id=c.teacher_id "
        "WHERE u.role='STUDENT' AND ("
        "(u.status=1 AND u.class_id IN (" + id_sql + ")) OR " + record_sql + ") "
        "ORDER BY c.name, u.username"
    )
    print("学生：%d 人，班级：%d 个" % (len(students), len(class_ids)))

    people = {}
    for row in students:
        sid = int(row[0])
        people[sid] = {
            "id": sid,
            "username": cell(row, 1),
            "name": cell(row, 2),
            "class_id": cell(row, 3),
            "class_name": cell(row, 4),
            "teacher": cell(row, 5),
            "tier": cell(row, 6),
            "no": student_no(cell(row, 1)),
        }

    progress = {}
    for row in mysql(
        "SELECT student_id, status, progress_percent, study_seconds, "
        "DATE_FORMAT(last_visit_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "DATE_FORMAT(completed_at,'%%Y-%%m-%%d %%H:%%i:%%s') "
        "FROM learn_progress WHERE lesson_id=%d" % lesson_id
    ):
        progress[int(row[0])] = row

    pretest = {}
    if table_exists("learn_pretest_g6"):
        for row in mysql(
            "SELECT student_id, IFNULL(level_code,''), total_score, "
            "DATE_FORMAT(submitted_at,'%Y-%m-%d %H:%i:%s') FROM learn_pretest_g6"
        ):
            pretest[int(row[0])] = row

    logs = []
    if table_exists("learn_submission_log"):
        logs = mysql(
            "SELECT l.student_id, IFNULL(l.activity_index,''), IFNULL(l.activity_key,''), "
            "IFNULL(l.score,''), IFNULL(l.study_seconds,0), "
            "DATE_FORMAT(l.created_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
            "REPLACE(REPLACE(TO_BASE64(l.content_json),'\\n',''),'\\r','') "
            "FROM learn_submission_log l WHERE l.lesson_id=%d ORDER BY l.created_at, l.id" % lesson_id
        )

    latest = {}
    latest_rows = mysql(
        "SELECT s.student_id, IFNULL(s.score,''), IFNULL(s.study_seconds,0), "
        "DATE_FORMAT(s.submitted_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "DATE_FORMAT(s.updated_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "REPLACE(REPLACE(TO_BASE64(s.content_json),'\\n',''),'\\r','') "
        "FROM learn_submission s "
        "JOIN course_task t ON t.id=s.task_id "
        "WHERE t.lesson_id=%d" % lesson_id
    )
    for row in latest_rows:
        latest[int(row[0])] = row

    visits = []
    if table_exists("learn_visit_log"):
        visits = mysql(
            "SELECT student_id, IFNULL(duration_sec,0), IFNULL(page_url,''), "
            "DATE_FORMAT(visited_at,'%%Y-%%m-%%d %%H:%%i:%%s') "
            "FROM learn_visit_log WHERE lesson_id=%d ORDER BY visited_at, id" % lesson_id
        )

    hints = []
    if table_exists("learn_hint_log"):
        hints = mysql(
            "SELECT student_id, activity_index, hint_index, "
            "DATE_FORMAT(created_at,'%%Y-%%m-%%d %%H:%%i:%%s') "
            "FROM learn_hint_log WHERE lesson_id=%d ORDER BY created_at, id" % lesson_id
        )

    points = mysql(
        "SELECT student_id, source_type, IFNULL(points,0), IFNULL(description,''), "
        "DATE_FORMAT(created_at,'%%Y-%%m-%%d %%H:%%i:%%s') "
        "FROM learn_points WHERE source_id DIV 100 = %d "
        "OR description LIKE %s ORDER BY created_at, id" % (lesson_id, q("%" + lesson_title + "%"))
    )

    by_student_logs = defaultdict(list)
    for row in logs:
        sid = int(row[0])
        obj = decode_json(cell(row, 6))
        by_student_logs[sid].append((row, obj))

    first_done = defaultdict(dict)
    quiz_final = {}
    eval_final = {}
    for sid, items in by_student_logs.items():
        for row, obj in items:
            when = cell(row, 5)
            for act in done_set(obj):
                if act not in first_done[sid]:
                    first_done[sid][act] = when
            if obj.get("type") == "quiz":
                quiz_final[sid] = (when, obj, cell(row, 3))
            if obj.get("type") == "evaluation":
                eval_final[sid] = (when, obj)

    for sid, row in latest.items():
        obj = decode_json(cell(row, 5))
        when = cell(row, 4) or cell(row, 3)
        for act in done_set(obj):
            if act not in first_done[sid]:
                first_done[sid][act] = when
        if sid not in quiz_final and (obj.get("type") == "quiz" or obj.get("quizAnswers")):
            quiz_final[sid] = (when, obj, cell(row, 1))
        if sid not in eval_final and (obj.get("type") == "evaluation" or obj.get("evaluationAnswers")):
            eval_final[sid] = (when, obj)

    visit_stat = defaultdict(lambda: {"count": 0, "seconds": 0, "first": "", "last": ""})
    for row in visits:
        sid = int(row[0])
        stat = visit_stat[sid]
        stat["count"] += 1
        try:
            stat["seconds"] += int(cell(row, 1) or 0)
        except Exception:
            pass
        when = cell(row, 3)
        if not stat["first"]:
            stat["first"] = when
        stat["last"] = when

    hint_count = defaultdict(int)
    for row in hints:
        hint_count[int(row[0])] += 1

    point_sum = defaultdict(int)
    for row in points:
        try:
            point_sum[int(row[0])] += int(cell(row, 2) or 0)
        except Exception:
            pass

    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)

    student_rows = []
    quiz_rows = []
    eval_rows = []
    class_bucket = defaultdict(list)

    for sid, person in people.items():
        prog = progress.get(sid)
        status = cell(prog, 1) if prog else "NOT_STARTED"
        study_seconds = cell(prog, 3, "0") if prog else "0"
        pre = pretest.get(sid)
        drafts = {}
        latest_obj = decode_json(cell(latest.get(sid), 5)) if sid in latest else {}
        if isinstance(latest_obj.get("activityDrafts"), dict):
            drafts = latest_obj["activityDrafts"]
        q_when, q_obj, q_score_col = quiz_final.get(sid, ("", {}, ""))
        e_when, e_obj = eval_final.get(sid, ("", {}))
        q_answers = q_obj.get("quizAnswers") or []
        if q_obj.get("type") == "quiz" and isinstance(q_obj.get("answers"), list):
            q_answers = q_obj.get("answers")
        e_answers = e_obj.get("evaluationAnswers") or []
        if e_obj.get("type") == "evaluation" and isinstance(e_obj.get("answers"), list):
            e_answers = e_obj.get("answers")
        if not isinstance(q_answers, list):
            q_answers = []
        if not isinstance(e_answers, list):
            e_answers = []
        quiz_score = q_obj.get("quizScore")
        if quiz_score is None and q_score_col != "":
            quiz_score = q_score_col
        correct_count = q_obj.get("correctCount", "")
        total_q = q_obj.get("totalQuestions", "")
        eval_labels = []
        for answer in e_answers:
            if isinstance(answer, dict):
                eval_labels.append(answer.get("label") or "")
        while len(eval_labels) < 3:
            eval_labels.append("")
        vstat = visit_stat[sid]
        row = [
            person["class_name"], person["teacher"], person["name"], person["no"], person["username"],
            cell(pre, 1) if pre else "", cell(pre, 2) if pre else "", cell(pre, 3) if pre else "",
            person["tier"],
            STATUS_NAME.get(status, status),
            cell(prog, 2, "0") if prog else "0",
            study_seconds, minutes(study_seconds),
            vstat["first"] or (cell(prog, 4) if prog else ""),
            cell(prog, 4) if prog else vstat["last"],
            cell(prog, 5) if prog else "",
            first_done[sid].get(1, ""),
            first_done[sid].get(2, ""),
            first_done[sid].get(3, ""),
            first_done[sid].get(0, ""),
            "是" if 1 in first_done[sid] else "否",
            "是" if 2 in first_done[sid] else "否",
            "是" if 3 in first_done[sid] else "否",
            quiz_score if quiz_score is not None else "",
            correct_count, total_q, q_when,
            eval_labels[0], eval_labels[1], eval_labels[2], e_when,
            hint_count[sid], point_sum[sid],
            vstat["count"], vstat["seconds"], minutes(vstat["seconds"]),
            field_summary(drafts.get("1") or drafts.get(1)),
            field_summary(drafts.get("2") or drafts.get(2)),
            field_summary(drafts.get("3") or drafts.get(3)),
        ]
        student_rows.append(row)
        class_bucket[person["class_name"]].append(row)

        version, bank = quiz_bank(q_answers)
        for idx, answer in enumerate(q_answers, start=1):
            if not isinstance(answer, dict):
                continue
            qid = answer.get("questionId") or ""
            selected = answer.get("selected")
            stem = bank.get(qid, ("", []))[0] if qid in bank else ""
            quiz_rows.append([
                person["class_name"], person["name"], person["no"], q_when, version,
                idx, qid, answer.get("dimension") or "", stem,
                "" if selected is None else int(selected) + 1,
                option_text(bank, qid, selected),
                "对" if answer.get("isCorrect") else "错",
                "" if answer.get("correct") is None else int(answer.get("correct")) + 1,
            ])
        for idx, answer in enumerate(e_answers, start=1):
            if not isinstance(answer, dict):
                continue
            eval_rows.append([
                person["class_name"], person["name"], person["no"], e_when,
                idx, answer.get("questionId") or "", answer.get("aspect") or "",
                "" if answer.get("selected") is None else int(answer.get("selected")) + 1,
                answer.get("label") or "",
            ])

    write_csv(
        os.path.join(OUT_DIR, "01-学生总表.csv"),
        [
            "班级", "任课教师", "姓名", "学号", "账号",
            "前测等级", "前测总分", "前测提交时间", "教师指定支架档",
            "本课状态", "进度百分比", "累计学习秒", "累计学习分钟",
            "首次进入", "最近进入", "标记完成时间",
            "探究一完成时间（问题分析与建模）", "探究二完成时间（算法设计）", "探究三完成时间（算法验证与优化）", "阅读教材时间",
            "探究一完成", "探究二完成", "探究三完成",
            "小测得分", "小测答对题数", "小测题数", "小测提交时间",
            "学习评价1", "学习评价2", "学习评价3", "学习评价提交时间",
            "支架提示次数", "本课积分",
            "访问次数", "访问记录秒数", "访问记录分钟",
            "探究一填写（问题分析与建模）", "探究二填写（算法设计）", "探究三填写（算法验证与优化）",
        ],
        student_rows,
    )

    summary = []
    for class_name in sorted(class_bucket):
        rows = class_bucket[class_name]
        def count_yes(index):
            return sum(1 for item in rows if item[index] == "是")
        def count_filled(index):
            return sum(1 for item in rows if str(item[index]).strip() != "")
        scores = []
        for item in rows:
            try:
                if str(item[23]).strip() != "":
                    scores.append(float(item[23]))
            except Exception:
                pass
        studied = sum(1 for item in rows if item[9] != "未开始" or item[13] or item[16] or item[17] or item[18])
        summary.append([
            class_name, rows[0][1], len(rows), studied,
            count_yes(20), count_yes(21), count_yes(22),
            sum(1 for item in rows if item[20] == "是" and item[21] == "是" and item[22] == "是"),
            count_filled(26),
            round(sum(scores) / len(scores), 1) if scores else "",
            count_filled(30),
            round(sum(float(item[12] or 0) for item in rows) / len(rows), 1),
        ])
    write_csv(
        os.path.join(OUT_DIR, "00-班级汇总.csv"),
        [
            "班级", "任课教师", "学生人数", "进入过本课",
            "完成探究一", "完成探究二", "完成探究三", "三个探究都完成",
            "已交课堂小测", "小测平均分", "已交学习评价", "人均学习分钟",
        ],
        summary,
    )

    timeline = []
    for row, obj in ((item, obj) for items in by_student_logs.values() for item, obj in items):
        sid = int(row[0])
        person = people.get(sid)
        if not person:
            continue
        try:
            act = int(cell(row, 1)) if cell(row, 1) != "" else None
        except Exception:
            act = None
        if act is None:
            if obj.get("type") == "quiz":
                act = 99
            elif obj.get("type") == "evaluation":
                act = 98
            elif obj.get("type") == "reading":
                act = 0
        timeline.append([
            cell(row, 5), person["class_name"], person["name"], person["no"],
            ACT_NAME.get(act, cell(row, 2) or "提交"),
            cell(row, 3), cell(row, 4),
        ])
    write_csv(
        os.path.join(OUT_DIR, "02-操作时间.csv"),
        ["时间", "班级", "姓名", "学号", "环节", "本次得分", "本次页面停留秒"],
        timeline,
    )
    write_csv(
        os.path.join(OUT_DIR, "03-课堂小测.csv"),
        ["班级", "姓名", "学号", "提交时间", "题目版本", "题序", "题号", "维度", "题目", "所选序号", "所选文字", "对错", "正确序号"],
        quiz_rows,
    )
    write_csv(
        os.path.join(OUT_DIR, "04-学习评价.csv"),
        ["班级", "姓名", "学号", "提交时间", "题序", "题号", "方面", "所选序号", "所选文字"],
        eval_rows,
    )

    visit_rows = []
    for row in visits:
        sid = int(row[0])
        person = people.get(sid)
        if not person:
            continue
        visit_rows.append([
            cell(row, 3), person["class_name"], person["name"], person["no"],
            cell(row, 1), minutes(cell(row, 1)), cell(row, 2),
        ])
    write_csv(
        os.path.join(OUT_DIR, "05-访问记录.csv"),
        ["进入时间", "班级", "姓名", "学号", "本次停留秒", "本次停留分钟", "页面"],
        visit_rows,
    )

    hint_rows = []
    for row in hints:
        sid = int(row[0])
        person = people.get(sid)
        if not person:
            continue
        try:
            act = int(row[1])
        except Exception:
            act = None
        hint_rows.append([
            cell(row, 3), person["class_name"], person["name"], person["no"],
            ACT_NAME.get(act, row[1]), row[1], row[2],
        ])
    write_csv(
        os.path.join(OUT_DIR, "06-支架提示.csv"),
        ["时间", "班级", "姓名", "学号", "环节", "环节序号", "第几层提示"],
        hint_rows,
    )

    point_rows = []
    for row in points:
        sid = int(row[0])
        person = people.get(sid)
        if not person:
            continue
        point_rows.append([
            cell(row, 4), person["class_name"], person["name"], person["no"],
            cell(row, 1), cell(row, 2), cell(row, 3),
        ])
    write_csv(
        os.path.join(OUT_DIR, "07-积分记录.csv"),
        ["时间", "班级", "姓名", "学号", "类型", "积分", "说明"],
        point_rows,
    )

    raw_path = os.path.join(OUT_DIR, "08-提交原文.jsonl")
    with open(raw_path, "w", encoding="utf-8") as handle:
        for sid, row in latest.items():
            person = people.get(sid, {})
            handle.write(json.dumps({
                "kind": "latest",
                "className": person.get("class_name", ""),
                "name": person.get("name", ""),
                "username": person.get("username", ""),
                "submittedAt": cell(row, 3),
                "updatedAt": cell(row, 4),
                "score": cell(row, 1),
                "content": decode_json(cell(row, 5)),
            }, ensure_ascii=False) + "\n")
        for row, obj in ((item, obj) for items in by_student_logs.values() for item, obj in items):
            sid = int(row[0])
            person = people.get(sid, {})
            handle.write(json.dumps({
                "kind": "log",
                "time": cell(row, 5),
                "className": person.get("class_name", ""),
                "name": person.get("name", ""),
                "username": person.get("username", ""),
                "activity": cell(row, 2),
                "score": cell(row, 3),
                "content": obj,
            }, ensure_ascii=False) + "\n")

    if os.path.exists(ZIP_PATH):
        os.remove(ZIP_PATH)
    with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as archive:
        for name in sorted(os.listdir(OUT_DIR)):
            archive.write(os.path.join(OUT_DIR, name), name)

    print("班级汇总 %d 行" % len(summary))
    print("学生总表 %d 行" % len(student_rows))
    print("操作记录 %d 条" % len(timeline))
    print("小测答题 %d 条" % len(quiz_rows))
    print("学习评价 %d 条" % len(eval_rows))
    print("访问记录 %d 条" % len(visit_rows))
    print("已打包：%s" % ZIP_PATH)


if __name__ == "__main__":
    main()
