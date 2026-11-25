#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上册指定课次学情。用法：
  python3 database/export_g6_up_lessons.py 4 5
在服务器项目根目录、且 learn-mysql 容器运行时执行。
"""

from __future__ import print_function

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

LESSON_META = {
    4: {
        "like": "第4课%",
        "prefer": ("程序", "算法"),
        "folder": "六年级上册第4课学情",
        "report": "六年级上册第4课《用程序设计语言描述算法》学情总览",
        "acts": {
            0: "阅读教材",
            1: "探究一：第一行代码",
            2: "探究二：鸡兔同笼程序",
            3: "探究三：三角形计算",
            4: "探究四：抽盲盒",
            98: "学习评价",
            99: "课堂小测",
        },
        "act_count": 4,
        "folders": ("探究一", "探究二", "探究三", "探究四"),
        "quiz": {
            "k1": ("print(\"hello, world!\") 的作用是：", [
                "在屏幕上显示 hello, world!", "把这句话存成文件", "先算出一个加法", "画出流程图",
            ]),
            "k2": ("print(3+5) 和 print(\"3+5\") 的区别是：", [
                "完全一样", "前者输出 8，后者原样输出 3+5", "前者会报错", "后者输出 8",
            ]),
            "a1": ("鸡兔同笼程序里，while ji <= 35 中的 35 表示：", [
                "脚的只数", "鸡最多可以试到 35 只", "正确答案是 35", "每次让 ji 加 35",
            ]),
            "a2": ("代码里的 ji = ji + 1 是在：", [
                "输出结果", "结束程序", "试下一只鸡", "计算兔的只数",
            ]),
            "l1": ("写 Python 时，括号和引号应该：", [
                "用中文全角也可以", "用英文半角，而且要成对", "可以全部不写", "只能用中文引号",
            ]),
            "l2": ("三边是 3、4、5 的三角形，周长是：", [
                "12", "5", "3", "35",
            ]),
        },
    },
    5: {
        "like": "第5课%",
        "prefer": ("算法执行", "算法的执行"),
        "folder": "六年级上册第5课学情",
        "report": "六年级上册第5课《算法执行》学情总览",
        "acts": {
            0: "阅读教材",
            1: "探究一：Bug修复",
            2: "探究二：优化程序",
            3: "探究三：通用化程序",
            98: "学习评价",
            99: "课堂小测",
        },
        "act_count": 3,
        "folders": ("探究一", "探究二", "探究三"),
        "quiz": {
            "k1": ("计算机完成算法执行，通常需要：", [
                "只画流程图即可", "运行程序，让计算机按步骤执行", "把代码写在纸上就行", "不需要任何操作",
            ]),
            "k2": ("NameError: name 'tu' is not defined 最可能是因为：", [
                "变量名大小写不一致", "缺少冒号", "缩进错误", "程序运行太快",
            ]),
            "a1": ("调试程序时，「调试四查」不包括：", [
                "检查大小写", "检查标点符号", "检查缩进", "检查电脑品牌",
            ]),
            "a2": ("添加 jie 变量的主要作用是：", [
                "让程序运行更快", "记录是否找到解，无解时提示用户", "减少代码行数", "替换 while 循环",
            ]),
            "l1": ("把固定数字改成 input() 输入，体现了：", [
                "让程序能解同一类问题，而不仅是一道题", "程序越复杂越好", "不需要调试了", "算法与程序无关",
            ]),
            "l2": ("遇到报错时，正确的做法是：", [
                "先读报错信息再定位修改", "立刻删除全部代码重写", "忽略报错继续运行", "认为程序不可能修好",
            ]),
        },
    },
}

STATUS_NAME = {
    "NOT_STARTED": "未开始",
    "IN_PROGRESS": "学习中",
    "COMPLETED": "已完成",
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
    for key in ("tableHit", "qAll", "openedAssistant", "scored", "voted"):
        if extra.get(key):
            flags.append(str(key))
    picks = extra.get("picks") if isinstance(extra.get("picks"), dict) else {}
    if picks:
        flags.append("选项=" + ",".join("%s:%s" % (k, picks[k]) for k in sorted(picks)))
    filled = extra.get("filled") or []
    if filled:
        flags.append("已填空位=" + ",".join(str(x) for x in filled))
    notified = extra.get("notified") or []
    if notified:
        flags.append("已提交环节=" + ",".join(str(n) for n in notified))
    if flags:
        parts.append("；".join(flags))
    return "；".join(parts)[:4000]


def export_one(lesson_no):
    meta = LESSON_META[lesson_no]
    act_count = meta["act_count"]
    act_name = meta["acts"]
    quiz_bank = meta["quiz"]
    out_dir = os.path.join(ROOT, "export", meta["folder"])
    zip_path = os.path.join(ROOT, "export", meta["folder"] + ".zip")

    if subprocess.run(
        ["docker", "inspect", "-f", "{{.State.Running}}", "learn-mysql"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    ).returncode != 0:
        raise SystemExit("没有找到正在运行的 learn-mysql")

    lessons = mysql(
        "SELECT cl.id, cl.title, cg.name "
        "FROM course_lesson cl "
        "JOIN course_unit cu ON cu.id = cl.unit_id "
        "JOIN course_grade cg ON cg.id = cu.grade_id "
        "WHERE cg.textbook_type='MAIN' AND cg.name='六年级上册' "
        "AND cl.title LIKE " + q(meta["like"]) + " "
        "ORDER BY cl.id"
    )
    if not lessons:
        raise SystemExit("没有找到六年级上册第%d课" % lesson_no)
    lesson = lessons[0]
    for row in lessons:
        title = row[1] or ""
        if any(p in title for p in meta["prefer"]):
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
    for row in mysql(
        "SELECT s.student_id, IFNULL(s.score,''), IFNULL(s.study_seconds,0), "
        "DATE_FORMAT(s.submitted_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "DATE_FORMAT(s.updated_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "REPLACE(REPLACE(TO_BASE64(s.content_json),'\\n',''),'\\r','') "
        "FROM learn_submission s "
        "JOIN course_task t ON t.id=s.task_id "
        "WHERE t.lesson_id=%d" % lesson_id
    ):
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

    if not os.path.isdir(out_dir):
        os.makedirs(out_dir)

    # student_rows layout:
    # 0班级 1教师 2姓名 3学号 4账号 5前测等级 6前测分 7前测时间 8支架
    # 9状态 10进度 11秒 12分钟 13首次 14最近 15完成标记
    # 16..(16+n-1) 探究完成时间  next: 阅读时间, 探究完成是否, 小测..., 评价..., 提示积分访问, 填写摘要
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
        ]
        for i in range(1, act_count + 1):
            row.append(first_done[sid].get(i, ""))
        row.append(first_done[sid].get(0, ""))
        for i in range(1, act_count + 1):
            row.append("是" if i in first_done[sid] else "否")
        row.extend([
            quiz_score if quiz_score is not None else "",
            correct_count, total_q, q_when,
            eval_labels[0], eval_labels[1], eval_labels[2], e_when,
            hint_count[sid], point_sum[sid],
            vstat["count"], vstat["seconds"], minutes(vstat["seconds"]),
        ])
        for i in range(1, act_count + 1):
            row.append(field_summary(drafts.get(str(i)) or drafts.get(i)))
        student_rows.append(row)
        class_bucket[person["class_name"]].append(row)

        for idx, answer in enumerate(q_answers, start=1):
            if not isinstance(answer, dict):
                continue
            qid = answer.get("questionId") or ""
            selected = answer.get("selected")
            stem = quiz_bank.get(qid, ("", []))[0] if qid in quiz_bank else ""
            quiz_rows.append([
                person["class_name"], person["name"], person["no"], q_when, "现行小测",
                idx, qid, answer.get("dimension") or "", stem,
                "" if selected is None else int(selected) + 1,
                option_text(quiz_bank, qid, selected),
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

    # column indexes
    # base 0-15, then act times [16 .. 16+n-1], read=16+n
    # done flags start = 17+n, length n
    # quiz_score = 17+n+n = 17+2n
    t0 = 16
    read_idx = 16 + act_count
    done0 = 17 + act_count
    quiz_score_idx = 17 + 2 * act_count
    quiz_when_idx = quiz_score_idx + 3
    eval_when_idx = quiz_score_idx + 7
    draft0 = quiz_score_idx + 13
    study_min_idx = 12
    status_idx = 9
    first_idx = 13

    header_student = [
        "班级", "任课教师", "姓名", "学号", "账号",
        "前测等级", "前测总分", "前测提交时间", "教师指定支架档",
        "本课状态", "进度百分比", "累计学习秒", "累计学习分钟",
        "首次进入", "最近进入", "标记完成时间",
    ]
    for i in range(1, act_count + 1):
        header_student.append("探究%d完成时间" % i)
    header_student.append("阅读教材时间")
    for i in range(1, act_count + 1):
        header_student.append("探究%d完成" % i)
    header_student.extend([
        "小测得分", "小测答对题数", "小测题数", "小测提交时间",
        "学习评价1", "学习评价2", "学习评价3", "学习评价提交时间",
        "支架提示次数", "本课积分",
        "访问次数", "访问记录秒数", "访问记录分钟",
    ])
    for i in range(1, act_count + 1):
        header_student.append("探究%d填写" % i)

    write_csv(os.path.join(out_dir, "01-学生总表.csv"), header_student, student_rows)

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
                if str(item[quiz_score_idx]).strip() != "":
                    scores.append(float(item[quiz_score_idx]))
            except Exception:
                pass
        studied = sum(
            1 for item in rows
            if item[status_idx] != "未开始" or item[first_idx] or any(item[t0 + i - 1] for i in range(1, act_count + 1))
        )
        done_counts = [count_yes(done0 + i - 1) for i in range(1, act_count + 1)]
        all_done = sum(
            1 for item in rows
            if all(item[done0 + i - 1] == "是" for i in range(1, act_count + 1))
        )
        summary.append([
            class_name, rows[0][1], len(rows), studied,
        ] + done_counts + [
            all_done,
            count_filled(quiz_when_idx),
            round(sum(scores) / len(scores), 1) if scores else "",
            count_filled(eval_when_idx),
            round(sum(float(item[study_min_idx] or 0) for item in rows) / len(rows), 1),
        ])

    header_summary = ["班级", "任课教师", "学生人数", "进入过本课"]
    for i in range(1, act_count + 1):
        header_summary.append("完成探究%d" % i)
    header_summary.extend(["全部探究都完成", "已交课堂小测", "小测平均分", "已交学习评价", "人均学习分钟"])
    write_csv(os.path.join(out_dir, "00-班级汇总.csv"), header_summary, summary)
    write_csv(os.path.join(out_dir, "班级完成情况汇总.csv"), header_summary, summary)

    timeline = []
    for items in by_student_logs.values():
        for row, obj in items:
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
                act_name.get(act, cell(row, 2) or "提交"),
                cell(row, 3), cell(row, 4),
            ])
    write_csv(
        os.path.join(out_dir, "02-操作时间.csv"),
        ["时间", "班级", "姓名", "学号", "环节", "本次得分", "本次页面停留秒"],
        timeline,
    )
    write_csv(
        os.path.join(out_dir, "03-课堂小测.csv"),
        ["班级", "姓名", "学号", "提交时间", "题目版本", "题序", "题号", "维度", "题目", "所选序号", "所选文字", "对错", "正确序号"],
        quiz_rows,
    )
    write_csv(
        os.path.join(out_dir, "04-学习评价.csv"),
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
        os.path.join(out_dir, "05-访问记录.csv"),
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
            act_name.get(act, row[1]), row[1], row[2],
        ])
    write_csv(
        os.path.join(out_dir, "06-支架提示.csv"),
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
        os.path.join(out_dir, "07-积分记录.csv"),
        ["时间", "班级", "姓名", "学号", "类型", "积分", "说明"],
        point_rows,
    )

    raw_path = os.path.join(out_dir, "08-提交原文.jsonl")
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
        for items in by_student_logs.values():
            for row, obj in items:
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

    for i, folder in enumerate(meta["folders"], start=1):
        path = os.path.join(out_dir, folder)
        if not os.path.isdir(path):
            os.makedirs(path)
        done_idx = done0 + i - 1
        time_idx = t0 + i - 1
        draft_idx = draft0 + i - 1
        done_rows = []
        draft_rows = []
        for item in student_rows:
            if item[done_idx] == "是":
                done_rows.append([
                    item[0], item[1], item[2], item[3], item[4], item[time_idx], item[draft_idx],
                ])
            if str(item[draft_idx] or "").strip():
                draft_rows.append([
                    item[0], item[1], item[2], item[3], item[4], item[done_idx], item[time_idx], item[draft_idx],
                ])
        write_csv(
            os.path.join(path, "完成名单.csv"),
            ["班级", "任课教师", "姓名", "学号", "账号", "完成时间", "填写摘要"],
            done_rows,
        )
        write_csv(
            os.path.join(path, "填写内容.csv"),
            ["班级", "任课教师", "姓名", "学号", "账号", "是否完成", "完成时间", "填写摘要"],
            draft_rows,
        )

    eval_dir = os.path.join(out_dir, "学习评价")
    quiz_dir = os.path.join(out_dir, "课堂小测")
    if not os.path.isdir(eval_dir):
        os.makedirs(eval_dir)
    if not os.path.isdir(quiz_dir):
        os.makedirs(quiz_dir)
    write_csv(
        os.path.join(eval_dir, "评价明细.csv"),
        ["班级", "姓名", "学号", "提交时间", "题序", "题号", "方面", "所选序号", "所选文字"],
        eval_rows,
    )
    write_csv(
        os.path.join(quiz_dir, "答题明细.csv"),
        ["班级", "姓名", "学号", "提交时间", "题目版本", "题序", "题号", "维度", "题目", "所选序号", "所选文字", "对错", "正确序号"],
        quiz_rows,
    )
    quiz_score_rows = []
    for item in student_rows:
        if str(item[quiz_score_idx]).strip() != "" or str(item[quiz_when_idx]).strip() != "":
            quiz_score_rows.append([
                item[0], item[1], item[2], item[3], item[4],
                item[quiz_score_idx], item[quiz_score_idx + 1], item[quiz_score_idx + 2], item[quiz_when_idx],
            ])
    write_csv(
        os.path.join(quiz_dir, "成绩汇总.csv"),
        ["班级", "任课教师", "姓名", "学号", "账号", "得分", "答对题数", "题数", "提交时间"],
        quiz_score_rows,
    )

    total_students = len(student_rows)
    entered = sum(
        1 for item in student_rows
        if item[status_idx] != "未开始" or item[first_idx] or any(item[t0 + i - 1] for i in range(1, act_count + 1))
    )
    done_ns = [sum(1 for item in student_rows if item[done0 + i - 1] == "是") for i in range(1, act_count + 1)]
    call = sum(
        1 for item in student_rows
        if all(item[done0 + i - 1] == "是" for i in range(1, act_count + 1))
    )
    quiz_n = sum(1 for item in student_rows if str(item[quiz_when_idx]).strip() != "")
    eval_n = sum(1 for item in student_rows if str(item[eval_when_idx]).strip() != "")
    scores = []
    for item in student_rows:
        try:
            if str(item[quiz_score_idx]).strip() != "":
                scores.append(float(item[quiz_score_idx]))
        except Exception:
            pass
    avg_score = round(sum(scores) / len(scores), 1) if scores else "—"

    report = []
    report.append("# %s" % meta["report"])
    report.append("")
    report.append("导出时间：%s" % datetime.now().strftime("%Y-%m-%d %H:%M"))
    report.append("课时：%s（id=%s）" % (lesson_title, lesson_id))
    report.append("")
    report.append("## 整体完成情况")
    report.append("")
    report.append("| 项目 | 人数 |")
    report.append("| --- | ---: |")
    report.append("| 学生总数 | %d |" % total_students)
    report.append("| 进入过本课 | %d |" % entered)
    for i, n in enumerate(done_ns, start=1):
        report.append("| 完成探究%d（%s） | %d |" % (i, act_name.get(i, ""), n))
    report.append("| 全部探究都完成 | %d |" % call)
    report.append("| 已交课堂小测 | %d |" % quiz_n)
    report.append("| 小测平均分 | %s |" % avg_score)
    report.append("| 已交学习评价 | %d |" % eval_n)
    report.append("")
    report.append("## 各班完成情况")
    report.append("")
    head_cols = ["班级", "任课教师", "人数", "进入"] + ["探究%d" % i for i in range(1, act_count + 1)] + ["全完", "小测", "小测均分", "评价", "人均分钟"]
    report.append("| " + " | ".join(head_cols) + " |")
    report.append("| " + " | ".join(["---"] * 4 + [":---:"] * (act_count + 5)) + " |")
    for row in summary:
        report.append("| " + " | ".join(str(x) for x in row) + " |")
    report.append("")
    with open(os.path.join(out_dir, "总览报告.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(report) + "\n")

    if os.path.exists(zip_path):
        os.remove(zip_path)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
        for dirpath, _, filenames in os.walk(out_dir):
            for name in sorted(filenames):
                full = os.path.join(dirpath, name)
                archive.write(full, os.path.relpath(full, out_dir))

    print("班级汇总 %d 行" % len(summary))
    print("学生总表 %d 行" % len(student_rows))
    print("小测答题 %d 条" % len(quiz_rows))
    print("学习评价 %d 条" % len(eval_rows))
    print("已打包：%s" % zip_path)
    return zip_path


def main():
    args = [a for a in sys.argv[1:] if a.strip()]
    if not args:
        args = ["4", "5"]
    nos = []
    for a in args:
        try:
            n = int(a)
        except Exception:
            raise SystemExit("课次请写数字，例如：python3 database/export_g6_up_lessons.py 4 5")
        if n not in LESSON_META:
            raise SystemExit("当前脚本支持课次：%s" % sorted(LESSON_META.keys()))
        nos.append(n)
    paths = []
    for n in nos:
        print("======== 导出第%d课 ========" % n)
        paths.append(export_one(n))
    print("全部完成：")
    for p in paths:
        print("  " + p)


if __name__ == "__main__":
    main()
