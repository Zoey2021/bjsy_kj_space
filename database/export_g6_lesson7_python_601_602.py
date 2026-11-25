#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上第7课 · 601/602 班学生提交的 Python 代码。

在服务器项目根目录执行（learn-mysql 需在跑）：
  python3 database/export_g6_lesson7_python_601_602.py

产物写到项目目录：
  六年级上第7课_601_602_Python代码/
  六年级上第7课_601_602_Python代码.zip
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
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "六年级上第7课_601_602_Python代码")
OUT_ZIP = OUT_DIR + ".zip"

# 班级名匹配：本库多为「2021级1班/2班」（六年级 601/602）；也兼容字面 601/602
CLASS_PATTERNS = [
    re.compile(r"601"),
    re.compile(r"602"),
    re.compile(r"^2021级\s*0*1\s*班$"),
    re.compile(r"^2021级\s*0*2\s*班$"),
    re.compile(r"六\s*[年年级]*\s*[（(]?\s*0*1\s*[）)]?\s*班"),
    re.compile(r"六\s*[年年级]*\s*[（(]?\s*0*2\s*[）)]?\s*班"),
]


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
        return "%02d" % int(match.group(2))
    if re.match(r"^\d{1,2}$", raw):
        return "%02d" % int(raw)
    return raw or "unknown"


def safe_name(text):
    text = re.sub(r'[\\/:*?"<>|\s]+', "_", str(text or "").strip())
    return text[:80] or "unnamed"


def class_bucket(name):
    text = str(name or "").strip()
    if "601" in text or re.search(r"^2021级\s*0*1\s*班$", text) or re.search(
        r"六\s*[年年级]*\s*[（(]?\s*0*1\s*[）)]?\s*班", text
    ):
        return "601班"
    if "602" in text or re.search(r"^2021级\s*0*2\s*班$", text) or re.search(
        r"六\s*[年年级]*\s*[（(]?\s*0*2\s*[）)]?\s*班", text
    ):
        return "602班"
    return safe_name(text)


def match_target_class(name):
    text = str(name or "").strip()
    return any(p.search(text) for p in CLASS_PATTERNS)


def looks_like_python(text):
    if not text or not isinstance(text, str):
        return False
    s = text.strip()
    if len(s) < 20:
        return False
    hits = 0
    for token in ("import ", "def ", "while ", "if ", "print(", "input(", "random", "for "):
        if token in s:
            hits += 1
    return hits >= 2 or ("import random" in s and "input" in s)


def pick_field(fields, field_id):
    if not isinstance(fields, list):
        return ""
    for f in fields:
        if not isinstance(f, dict):
            continue
        if str(f.get("id") or "") == field_id:
            return str(f.get("value") or "")
    return ""


def extract_from_obj(obj):
    """从提交 JSON 中尽量取出代码、提示词、分数。"""
    code = ""
    prompt = ""
    score = ""
    source = ""

    if not isinstance(obj, dict):
        return code, prompt, score, source

    for key in ("code", "pythonCode", "codeText", "sourceCode", "pyCode"):
        val = obj.get(key)
        if isinstance(val, str) and looks_like_python(val):
            code = val
            source = "top." + key
            break

    for key in ("prompt", "promptText", "userPrompt"):
        val = obj.get(key)
        if isinstance(val, str) and val.strip():
            prompt = val.strip()
            break

    for key in ("gradeTotal", "codeScore", "userScore", "score"):
        if obj.get(key) not in (None, ""):
            score = str(obj.get(key))
            break

    drafts = obj.get("activityDrafts") or {}
    if isinstance(drafts, dict):
        # 优先探究三 index=3，再扫全部
        keys = []
        for k in drafts.keys():
            try:
                keys.append((0 if str(k) == "3" else 1, str(k)))
            except Exception:
                keys.append((1, str(k)))
        keys.sort()
        for _, k in keys:
            draft = drafts.get(k) or {}
            if not isinstance(draft, dict):
                continue
            fields = draft.get("fields") or []
            c = pick_field(fields, "codeInput")
            p = pick_field(fields, "promptInput")
            if c and (not code or looks_like_python(c)):
                if looks_like_python(c) or len(c.strip()) > len(code.strip()):
                    code = c
                    source = "activityDrafts[%s].codeInput" % k
            if p and not prompt:
                prompt = p
            extra = draft.get("extra") or {}
            if isinstance(extra, dict):
                for key in ("code", "pythonCode", "codeText"):
                    val = extra.get(key)
                    if isinstance(val, str) and looks_like_python(val) and not looks_like_python(code):
                        code = val
                        source = "activityDrafts[%s].extra.%s" % (k, key)

    # 再扫一遍任意嵌套字符串
    if not looks_like_python(code):
        stack = [obj]
        while stack:
            cur = stack.pop()
            if isinstance(cur, dict):
                for v in cur.values():
                    stack.append(v)
            elif isinstance(cur, list):
                stack.extend(cur)
            elif isinstance(cur, str) and looks_like_python(cur) and len(cur) > len(code):
                code = cur
                source = "nested-scan"

    return code, prompt, score, source


def find_lesson():
    rows = mysql(
        "SELECT cl.id, cl.title FROM course_lesson cl "
        "JOIN course_unit cu ON cu.id=cl.unit_id "
        "JOIN course_grade cg ON cg.id=cu.grade_id "
        "WHERE cg.textbook_type='MAIN' AND cg.name='六年级上册' "
        "AND (cl.title LIKE '第7课%' OR cl.title LIKE '%人工智能辅助算法%' OR cl.title LIKE '%猜数字%') "
        "ORDER BY "
        "CASE WHEN cl.title LIKE '%人工智能辅助算法%' THEN 0 "
        "WHEN cl.title LIKE '第7课%' THEN 1 ELSE 2 END, cl.id "
        "LIMIT 1"
    )
    if not rows:
        raise SystemExit("没有找到六年级上册第7课")
    return int(rows[0][0]), rows[0][1]


def main():
    lesson_id, lesson_title = find_lesson()
    print("课时：%s（id=%s）" % (lesson_title, lesson_id))

    all_classes = mysql("SELECT id, IFNULL(name,''), IFNULL(grade_name,'') FROM sys_class ORDER BY name")
    print("系统班级一览：")
    for row in all_classes:
        print("  id=%s name=%s grade=%s" % (cell(row, 0), cell(row, 1), cell(row, 2)))

    target_ids = []
    for row in all_classes:
        cid, name = cell(row, 0), cell(row, 1)
        if match_target_class(name):
            target_ids.append(int(cid))
            print("命中班级：%s (id=%s)" % (name, cid))

    if not target_ids:
        # 兜底：名字里带 01/02 且六年级
        for row in all_classes:
            cid, name, grade = cell(row, 0), cell(row, 1), cell(row, 2)
            text = name + " " + grade
            if ("六年级" in text or "六" in name) and (
                re.search(r"(^|[^\d])0?1($|[^\d])", name) or re.search(r"(^|[^\d])0?2($|[^\d])", name)
            ):
                if "601" in name or "602" in name or "01" in name or "02" in name or "1班" in name or "2班" in name:
                    target_ids.append(int(cid))
                    print("兜底命中：%s (id=%s)" % (name, cid))

    if not target_ids:
        raise SystemExit("没有匹配到 601/602 班，请对照上面班级名调整脚本 CLASS_PATTERNS")

    id_sql = ",".join(str(i) for i in sorted(set(target_ids)))
    students = mysql(
        "SELECT u.id, u.username, u.real_name, IFNULL(c.name,'未分班') "
        "FROM sys_user u "
        "JOIN sys_class c ON c.id=u.class_id "
        "WHERE u.role='STUDENT' AND u.class_id IN (" + id_sql + ") "
        "ORDER BY c.name, u.username"
    )
    print("目标班学生：%d 人" % len(students))

    people = {}
    for row in students:
        sid = int(row[0])
        people[sid] = {
            "id": sid,
            "username": cell(row, 1),
            "name": cell(row, 2),
            "class_name": cell(row, 3),
            "bucket": class_bucket(cell(row, 3)),
            "no": student_no(cell(row, 1)),
        }

    # 最新提交
    latest = {}
    for row in mysql(
        "SELECT s.student_id, IFNULL(s.score,''), "
        "DATE_FORMAT(IFNULL(s.updated_at,s.submitted_at),'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "REPLACE(REPLACE(TO_BASE64(s.content_json),'\\n',''),'\\r','') "
        "FROM learn_submission s "
        "JOIN course_task t ON t.id=s.task_id "
        "WHERE t.lesson_id=%d AND s.student_id IN (%s)"
        % (lesson_id, ",".join(str(i) for i in people) or "0")
    ):
        latest[int(row[0])] = row

    # 提交日志（可能有更完整草稿）
    logs_by = {}
    if table_exists("learn_submission_log"):
        for row in mysql(
            "SELECT l.student_id, "
            "DATE_FORMAT(l.created_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
            "REPLACE(REPLACE(TO_BASE64(l.content_json),'\\n',''),'\\r','') "
            "FROM learn_submission_log l "
            "WHERE l.lesson_id=%d AND l.student_id IN (%s) "
            "ORDER BY l.created_at, l.id"
            % (lesson_id, ",".join(str(i) for i in people) or "0")
        ):
            sid = int(row[0])
            logs_by.setdefault(sid, []).append(row)

    if os.path.isdir(OUT_DIR):
        for root, _dirs, files in os.walk(OUT_DIR, topdown=False):
            for name in files:
                os.remove(os.path.join(root, name))
            for name in _dirs:
                os.rmdir(os.path.join(root, name))
    os.makedirs(OUT_DIR, exist_ok=True)
    for bucket in ("601班", "602班"):
        os.makedirs(os.path.join(OUT_DIR, bucket), exist_ok=True)

    index_rows = []
    with_code = 0
    without_code = 0

    for sid, person in sorted(people.items(), key=lambda x: (x[1]["bucket"], x[1]["no"], x[1]["username"])):
        best_code = ""
        best_prompt = ""
        best_score = ""
        best_source = ""
        best_when = ""

        candidates = []
        if sid in latest:
            candidates.append(("latest", cell(latest[sid], 2), decode_json(cell(latest[sid], 3)), cell(latest[sid], 1)))
        for row in logs_by.get(sid, []):
            candidates.append(("log", cell(row, 1), decode_json(cell(row, 2)), ""))

        for kind, when, obj, score_hint in candidates:
            code, prompt, score, source = extract_from_obj(obj)
            if score_hint and not score:
                score = score_hint
            if looks_like_python(code) and len(code) >= len(best_code):
                best_code = code
                best_prompt = prompt or best_prompt
                best_score = score or best_score
                best_source = "%s:%s" % (kind, source)
                best_when = when or best_when
            elif prompt and not best_prompt:
                best_prompt = prompt
            if score and not best_score:
                best_score = score

        bucket = person["bucket"]
        base = "%s_%s_%s" % (person["no"], safe_name(person["name"]), safe_name(person["username"]))
        class_dir = os.path.join(OUT_DIR, bucket)
        os.makedirs(class_dir, exist_ok=True)

        has = looks_like_python(best_code)
        if has:
            with_code += 1
            py_path = os.path.join(class_dir, base + ".py")
            with open(py_path, "w", encoding="utf-8") as f:
                f.write(best_code if best_code.endswith("\n") else best_code + "\n")
            status = "有代码"
        else:
            without_code += 1
            miss_path = os.path.join(class_dir, base + "_无代码.txt")
            with open(miss_path, "w", encoding="utf-8") as f:
                f.write("该生第7课提交记录中未找到可识别的 Python 代码。\n")
                f.write("班级：%s\n姓名：%s\n账号：%s\n" % (person["class_name"], person["name"], person["username"]))
                f.write("说明：代码主要来自课堂草稿 activityDrafts 中的 codeInput；若当时未粘贴/未触发草稿保存，库中会没有正文。\n")
            status = "无代码"

        if best_prompt:
            with open(os.path.join(class_dir, base + "_提示词.txt"), "w", encoding="utf-8") as f:
                f.write(best_prompt if best_prompt.endswith("\n") else best_prompt + "\n")

        index_rows.append([
            bucket, person["class_name"], person["no"], person["name"], person["username"],
            status, best_score, best_when, best_source, len(best_code),
        ])

    index_path = os.path.join(OUT_DIR, "导出清单.csv")
    with open(index_path, "w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["分班", "班级名", "学号", "姓名", "账号", "状态", "分数", "时间", "来源", "代码字符数"])
        writer.writerows(index_rows)

    readme = os.path.join(OUT_DIR, "说明.txt")
    with open(readme, "w", encoding="utf-8") as f:
        f.write("六年级上第7课 Python 代码导出\n")
        f.write("课时：%s (id=%s)\n" % (lesson_title, lesson_id))
        f.write("导出时间：%s\n" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        f.write("班级：601班、602班（按班级名匹配）\n")
        f.write("有代码：%d 人；无代码：%d 人；合计：%d 人\n" % (with_code, without_code, len(people)))
        f.write("文件命名：学号_姓名_账号.py\n")

    if os.path.exists(OUT_ZIP):
        os.remove(OUT_ZIP)
    with zipfile.ZipFile(OUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, _dirs, files in os.walk(OUT_DIR):
            for name in files:
                full = os.path.join(root, name)
                arc = os.path.relpath(full, ROOT)
                zf.write(full, arc)

    print("有代码：%d，无代码：%d" % (with_code, without_code))
    print("目录：%s" % OUT_DIR)
    print("压缩包：%s" % OUT_ZIP)


if __name__ == "__main__":
    main()
