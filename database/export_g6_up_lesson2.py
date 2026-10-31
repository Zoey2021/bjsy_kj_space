#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上册第2课《抽象与建模》的学习数据。在服务器项目目录执行。"""

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
OUT_DIR = os.path.join(ROOT, "export", "六年级上册第2课学情")
ZIP_PATH = os.path.join(ROOT, "export", "六年级上册第2课学情.zip")

ACT_NAME = {
    0: "阅读教材",
    1: "探究一：情景与问题描述",
    2: "探究二：表格抽象与验证",
    3: "探究三：百钱买百鸡",
    98: "学习评价",
    99: "课堂小测",
}

STATUS_NAME = {
    "NOT_STARTED": "未开始",
    "IN_PROGRESS": "学习中",
    "COMPLETED": "已完成",
}

QUIZ_NEW = {
    "k1": ("解决鸡兔同笼时，下面哪一项属于应该保留的关键要素？", [
        "把「上有」「下有」当成位置来计算",
        "保留鸡、兔，以及 35 个头、94 只脚",
        "把古文里的每一个字都变成一个计算量",
        "只保留最后问的那一句，数字都可以先忽略",
    ]),
    "k2": ("把「上有三十五头，下有九十四足」写成现代问题，已知条件是：", [
        "35 只鸡、94 只兔",
        "一共 35 个头、94 只脚",
        "每只鸡 35 只脚、每只兔 94 只脚",
        "一共 94 个头、35 只脚",
    ]),
    "a1": ("鸡用 ji、兔用 tu 表示只数。哪一组算式表达了鸡兔同笼的数量关系？", [
        "ji+tu=35，2×ji+4×tu=94",
        "ji+tu=94，2×ji+4×tu=35",
        "ji×tu=35，ji+tu=94",
        "2×ji=35，4×tu=94",
    ]),
    "a2": ("已知鸡的变量是 12、兔的变量是 23。代入后头数是 35、脚数是 116。这组数：", [
        "两个方程都成立，可以作为答案",
        "头数成立、脚数不成立，不能作为答案",
        "脚数成立、头数不成立，不能作为答案",
        "只要算出了两个结果，就算验证通过",
    ]),
    "s1": ("百钱买百鸡中，鸡雏「三只值钱一」。c 只鸡雏的总钱数应写成：", [
        "1×c，因为题目里有「值钱一」",
        "3×c，因为三只鸡雏",
        "c÷3，因为三只才值 1 钱",
        "c+3，把只数和钱数加起来",
    ]),
    "s2": ("从鸡兔同笼迁移到百钱买百鸡，比较合适的顺序是：", [
        "先随便设三个字母，再回头看题目",
        "先描述问题，再列表抽象，然后定变量、列模型",
        "只把 100 抄进表格，不必写数量关系",
        "先算出唯一答案，表格和方程都可以省略",
    ]),
}

QUIZ_OLD = {
    "k1": ("在计算机科学中，「抽象」是指：", [
        "把问题变得更复杂",
        "抓住问题的关键信息，忽略不重要的细节",
        "用各种颜色标记步骤",
        "在问题中加入许多细节",
    ]),
    "k2": ("「建模」是指：", [
        "制作一个真实大小的模型",
        "用不同字体排版文字",
        "把抽象出的数据和关系，用算式或方程表达出来",
        "画一幅示意图",
    ]),
    "a1": ("抽象建模时，我们用什么工具来整理「对象、数量、关系」？", [
        "思维导图", "表格", "日记本", "手机备忘录",
    ]),
    "a2": ("鸡兔同笼表格的「合计行」主要用来做什么？", [
        "装饰表格使其美观",
        "汇总数据并读出方程/算式",
        "记录同学的姓名",
        "代替问题描述",
    ]),
    "l1": ("把具体数字换成字母，让模型适用于更多问题，这体现了：", [
        "模型可以通用化，建一次反复用",
        "字母比数字更难，应该避免",
        "建模完成后不能再修改",
        "表格只是数学课的工具",
    ]),
    "l2": ("「模型不等于现实，但模型能帮我们高效解决问题」说明：", [
        "可以忽略所有现实条件",
        "抽象建模是为了更好地用计算机求解",
        "表格填错也没关系",
        "信息科技课与数学课完全无关",
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
        "AND cl.title LIKE '第2课%' "
        "ORDER BY cl.id"
    )
    if not lessons:
        raise SystemExit("没有找到六年级上册第2课")
    lesson = lessons[0]
    for row in lessons:
        if row[1] and "抽象" in row[1]:
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
            "探究一完成时间", "探究二完成时间", "探究三完成时间", "阅读教材时间",
            "探究一完成", "探究二完成", "探究三完成",
            "小测得分", "小测答对题数", "小测题数", "小测提交时间",
            "学习评价1", "学习评价2", "学习评价3", "学习评价提交时间",
            "支架提示次数", "本课积分",
            "访问次数", "访问记录秒数", "访问记录分钟",
            "探究一填写", "探究二填写", "探究三填写",
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
