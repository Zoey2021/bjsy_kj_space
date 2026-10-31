#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上册第3课《算法设计》的学习数据。在服务器项目目录执行。"""

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
OUT_DIR = os.path.join(ROOT, "export", "六年级上册第3课学情")
ZIP_PATH = os.path.join(ROOT, "export", "六年级上册第3课学情.zip")

ACT_NAME = {
    0: "阅读教材",
    1: "探究一：枚举算法框架",
    2: "探究二：鸡兔同笼三要素",
    3: "探究三：鸡兔同笼流程图",
    4: "探究四：枚举兔的数量",
    98: "学习评价",
    99: "课堂小测",
}

STATUS_NAME = {
    "NOT_STARTED": "未开始",
    "IN_PROGRESS": "学习中",
    "COMPLETED": "已完成",
}

# 现行：四个探究（密码锁 / 鸡兔同笼流程图）
QUIZ_NEW = {
    "k1": ("枚举法的基本思想是：", [
        "随机猜一个答案",
        "把可能的情况按顺序一一列举，逐个检验",
        "只试第一个想到的解",
        "不用告诉步骤，让计算机自己学会",
    ]),
    "k2": ("设计枚举算法，要先想清楚的三要素是：", [
        "字体、颜色和页面位置",
        "枚举的对象、枚举的范围、正确解的判断条件",
        "同学姓名、班级和学号",
        "开始框、结束框和箭头粗细",
    ]),
    "a1": ("密码锁前两位已知，第 3 位在 0～9。枚举范围写成 170～179，这是在确定：", [
        "正确解的判断条件", "枚举的范围", "输出语句", "结束框的位置",
    ]),
    "a2": ("鸡兔同笼枚举鸡的只数时，为什么范围是 0≤ji≤35，而不是 0 到 94？", [
        "因为 94 太小，试不完",
        "35 是头的个数，鸡最多 35 只；94 是脚数，用来判断对错",
        "因为兔没有脚",
        "因为流程图里不能出现 94",
    ]),
    "l1": ("枚举流程图里，外层绕回去的大圈和中间的菱形，主要分别是：", [
        "循环结构负责反复试，分支结构负责判断对不对",
        "两个都只是装饰，可以删掉",
        "菱形负责循环，大圈负责结束",
        "只有输出框才算算法",
    ]),
    "l2": ("登录网站时要限制密码输错次数，是因为：", [
        "密码和枚举法没有关系",
        "密码可以被一个一个试出来，所以要限制次数",
        "正确密码永远不应该被记住",
        "枚举法只能用在数学题里",
    ]),
}

# 早期：三探究（含百钱买百鸡表述）
QUIZ_OLD = {
    "k1": ("枚举法的基本思想是：", [
        "随机猜一个答案",
        "把所有可能解一个一个列举出来，逐一检验",
        "只试第一个想到的解",
        "让计算机自己学会，不用告诉它步骤",
    ]),
    "k2": ("设计枚举算法时，两个最关键的要确定：", [
        "题目颜色和字体大小",
        "枚举范围 和 正确解的判断条件",
        "同学姓名和班级",
        "电脑品牌和使用年限",
    ]),
    "a1": ("鸡兔同笼用枚举法时，枚举「鸡的只数 ji 从 0 到 35」属于确定：", [
        "判断条件", "枚举范围", "输出格式", "程序界面",
    ]),
    "a2": ("百钱买百鸡需要「外层枚举公鸡、内层枚举母鸡」，这体现了：", [
        "嵌套循环（循环里再循环）",
        "只需要一个 if 判断",
        "不需要枚举，直接心算",
        "流程图不能有回连线",
    ]),
    "l1": ("上节课建好的模型「ji+tu=35, 2×ji+4×tu=94」，本课设计枚举算法是为了：", [
        "把模型变成计算机可执行的求解步骤",
        "重新做一遍表格填空",
        "忘记模型，只背流程图形状",
        "证明枚举法比建模更重要",
    ]),
    "l2": ("「试密码、连续输错锁定」说明枚举法：", [
        "只能是数学课的内容",
        "既是解决问题的工具，也涉及信息安全",
        "与信息科技课无关",
        "应该无限次尝试密码",
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
    # 两套题号相同；用 a2 标准答案区分：现行=1，早期百钱买百鸡=0
    for answer in answers:
        if not isinstance(answer, dict):
            continue
        if str(answer.get("questionId") or "") != "a2":
            continue
        try:
            correct = int(answer.get("correct"))
        except Exception:
            continue
        if correct == 0:
            return "早期小测（含百钱买百鸡）", QUIZ_OLD
        if correct == 1:
            return "现行小测（枚举四探究）", QUIZ_NEW
    if answers:
        return "现行小测（枚举四探究）", QUIZ_NEW
    return "未作答", {}


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
    if extra.get("tableHit"):
        flags.append("枚举表已命中正确解")
    if extra.get("qAll"):
        flags.append("选择题已全部答对")
    picks = extra.get("picks") if isinstance(extra.get("picks"), dict) else {}
    if picks:
        flags.append("选择题选项=" + ",".join("%s:%s" % (k, picks[k]) for k in sorted(picks)))
    filled = extra.get("filled") or []
    if filled:
        flags.append("已填空位=" + ",".join(str(x) for x in filled))
    placed = extra.get("placed") or []
    if placed:
        flags.append("流程图卡片已放置%d处" % len(placed))
    flow = extra.get("flow") or []
    if isinstance(flow, list) and flow:
        kinds = [str((n or {}).get("kind") or "") for n in flow if isinstance(n, dict)]
        flags.append("自画流程图节点=" + "-".join(k for k in kinds if k))
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
        "AND cl.title LIKE '第3课%' "
        "ORDER BY cl.id"
    )
    if not lessons:
        raise SystemExit("没有找到六年级上册第3课")
    lesson = lessons[0]
    for row in lessons:
        if row[1] and ("算法设计" in row[1] or "枚举" in row[1]):
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
            first_done[sid].get(4, ""),
            first_done[sid].get(0, ""),
            "是" if 1 in first_done[sid] else "否",
            "是" if 2 in first_done[sid] else "否",
            "是" if 3 in first_done[sid] else "否",
            "是" if 4 in first_done[sid] else "否",
            quiz_score if quiz_score is not None else "",
            correct_count, total_q, q_when,
            eval_labels[0], eval_labels[1], eval_labels[2], e_when,
            hint_count[sid], point_sum[sid],
            vstat["count"], vstat["seconds"], minutes(vstat["seconds"]),
            field_summary(drafts.get("1") or drafts.get(1)),
            field_summary(drafts.get("2") or drafts.get(2)),
            field_summary(drafts.get("3") or drafts.get(3)),
            field_summary(drafts.get("4") or drafts.get(4)),
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

    # 学生总表列索引（后面分文件夹会用到）
    # 0班级 1教师 2姓名 3学号 4账号 9状态 12分钟
    # 16-19 探究1-4完成时间  20阅读  21-24 探究1-4完成(是/否)
    # 25小测分 26答对 27题数 28小测时间  29-31评价 32评价时间
    # 37-40 探究1-4填写
    write_csv(
        os.path.join(OUT_DIR, "01-学生总表.csv"),
        [
            "班级", "任课教师", "姓名", "学号", "账号",
            "前测等级", "前测总分", "前测提交时间", "教师指定支架档",
            "本课状态", "进度百分比", "累计学习秒", "累计学习分钟",
            "首次进入", "最近进入", "标记完成时间",
            "探究一完成时间", "探究二完成时间", "探究三完成时间", "探究四完成时间", "阅读教材时间",
            "探究一完成", "探究二完成", "探究三完成", "探究四完成",
            "小测得分", "小测答对题数", "小测题数", "小测提交时间",
            "学习评价1", "学习评价2", "学习评价3", "学习评价提交时间",
            "支架提示次数", "本课积分",
            "访问次数", "访问记录秒数", "访问记录分钟",
            "探究一填写", "探究二填写", "探究三填写", "探究四填写",
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
                if str(item[25]).strip() != "":
                    scores.append(float(item[25]))
            except Exception:
                pass
        studied = sum(
            1 for item in rows
            if item[9] != "未开始" or item[13] or item[16] or item[17] or item[18] or item[19]
        )
        summary.append([
            class_name, rows[0][1], len(rows), studied,
            count_yes(21), count_yes(22), count_yes(23), count_yes(24),
            sum(1 for item in rows if item[21] == "是" and item[22] == "是" and item[23] == "是" and item[24] == "是"),
            count_filled(28),
            round(sum(scores) / len(scores), 1) if scores else "",
            count_filled(32),
            round(sum(float(item[12] or 0) for item in rows) / len(rows), 1),
        ])
    write_csv(
        os.path.join(OUT_DIR, "00-班级汇总.csv"),
        [
            "班级", "任课教师", "学生人数", "进入过本课",
            "完成探究一", "完成探究二", "完成探究三", "完成探究四", "四个探究都完成",
            "已交课堂小测", "小测平均分", "已交学习评价", "人均学习分钟",
        ],
        summary,
    )
    # 与前两课整理后的目录名对齐
    write_csv(
        os.path.join(OUT_DIR, "班级完成情况汇总.csv"),
        [
            "班级", "任课教师", "学生人数", "进入过本课",
            "完成探究一", "完成探究二", "完成探究三", "完成探究四", "四个探究都完成",
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

    # 按前两课整理后的文件夹结构输出
    for act_no, folder, done_idx, time_idx, draft_idx in (
        (1, "探究一", 21, 16, 38),
        (2, "探究二", 22, 17, 39),
        (3, "探究三", 23, 18, 40),
        (4, "探究四", 24, 19, 41),
    ):
        path = os.path.join(OUT_DIR, folder)
        if not os.path.isdir(path):
            os.makedirs(path)
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

    eval_dir = os.path.join(OUT_DIR, "学习评价")
    quiz_dir = os.path.join(OUT_DIR, "课堂小测")
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
        if str(item[25]).strip() != "" or str(item[28]).strip() != "":
            quiz_score_rows.append([
                item[0], item[1], item[2], item[3], item[4],
                item[25], item[26], item[27], item[28],
            ])
    write_csv(
        os.path.join(quiz_dir, "成绩汇总.csv"),
        ["班级", "任课教师", "姓名", "学号", "账号", "得分", "答对题数", "题数", "提交时间"],
        quiz_score_rows,
    )

    total_students = len(student_rows)
    entered = sum(1 for item in student_rows if item[9] != "未开始" or item[13] or item[16] or item[17] or item[18] or item[19])
    c1 = sum(1 for item in student_rows if item[21] == "是")
    c2 = sum(1 for item in student_rows if item[22] == "是")
    c3 = sum(1 for item in student_rows if item[23] == "是")
    c4 = sum(1 for item in student_rows if item[24] == "是")
    call = sum(1 for item in student_rows if item[21] == "是" and item[22] == "是" and item[23] == "是" and item[24] == "是")
    quiz_n = sum(1 for item in student_rows if str(item[28]).strip() != "")
    eval_n = sum(1 for item in student_rows if str(item[32]).strip() != "")
    scores = []
    for item in student_rows:
        try:
            if str(item[25]).strip() != "":
                scores.append(float(item[25]))
        except Exception:
            pass
    avg_score = round(sum(scores) / len(scores), 1) if scores else "—"
    report = []
    report.append("# 六年级上册第3课《算法设计》学情总览")
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
    report.append("| 完成探究一（枚举算法框架） | %d |" % c1)
    report.append("| 完成探究二（鸡兔同笼三要素） | %d |" % c2)
    report.append("| 完成探究三（鸡兔同笼流程图） | %d |" % c3)
    report.append("| 完成探究四（枚举兔的数量） | %d |" % c4)
    report.append("| 四个探究都完成 | %d |" % call)
    report.append("| 已交课堂小测 | %d |" % quiz_n)
    report.append("| 小测平均分 | %s |" % avg_score)
    report.append("| 已交学习评价 | %d |" % eval_n)
    report.append("")
    report.append("## 各班完成情况")
    report.append("")
    report.append("| 班级 | 任课教师 | 人数 | 进入 | 探究一 | 探究二 | 探究三 | 探究四 | 四探全完 | 小测 | 小测均分 | 评价 | 人均分钟 |")
    report.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    for row in summary:
        report.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % tuple(row))
    report.append("")
    report.append("## 文件夹说明")
    report.append("")
    report.append("- `班级完成情况汇总.csv`：各班人数与完成统计")
    report.append("- `探究一`～`探究四`：完成名单与填写摘要")
    report.append("- `课堂小测`：成绩汇总与逐题答题明细")
    report.append("- `学习评价`：三条元认知评价明细")
    report.append("- `00`～`08` 编号表：与前两课导出脚本同结构的原始汇总表，便于核对")
    report.append("")
    with open(os.path.join(OUT_DIR, "总览报告.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(report) + "\n")

    if os.path.exists(ZIP_PATH):
        os.remove(ZIP_PATH)
    with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as archive:
        for dirpath, _, filenames in os.walk(OUT_DIR):
            for name in sorted(filenames):
                full = os.path.join(dirpath, name)
                archive.write(full, os.path.relpath(full, OUT_DIR))

    print("班级汇总 %d 行" % len(summary))
    print("学生总表 %d 行" % len(student_rows))
    print("操作记录 %d 条" % len(timeline))
    print("小测答题 %d 条" % len(quiz_rows))
    print("学习评价 %d 条" % len(eval_rows))
    print("访问记录 %d 条" % len(visit_rows))
    print("已打包：%s" % ZIP_PATH)


if __name__ == "__main__":
    main()
