<template>
  <div class="cld-page" v-loading="loading">
    <header class="cld-header">
      <div class="header-row header-row-filters">
        <h1 class="header-title">
          <span class="title-icon">📊</span>
          全班学习情况看板
        </h1>
        <div class="header-pills">
          <div class="pill pill-select">
            <span class="pill-label">年级</span>
            <el-select
              v-model="gradeId"
              size="small"
              class="filter-select grade-select"
              placeholder="选择年级"
              filterable
              @change="onGradeChange"
            >
              <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
            </el-select>
          </div>
          <div class="pill pill-select">
            <span class="pill-label">课程</span>
            <el-select
              v-model="lessonId"
              size="small"
              class="filter-select lesson-select"
              placeholder="选择课时"
              filterable
              :disabled="!lessons.length"
              @change="onLessonChange"
            >
              <el-option v-for="les in lessons" :key="les.id" :label="les.title" :value="les.id" />
            </el-select>
          </div>
          <div class="pill pill-select">
            <span class="pill-label">班级</span>
            <el-select
              v-model="classId"
              size="small"
              class="filter-select class-select"
              placeholder="选择班级"
              @change="onClassChange"
            >
              <el-option v-for="c in classes" :key="c.id" :label="classLabel(c)" :value="c.id" />
            </el-select>
          </div>
          <div class="pill">
            <span class="pill-label">人数</span>
            <span class="pill-value">{{ data.totalStudents || 0 }} 人</span>
          </div>
        </div>
      </div>
      <div class="header-row header-row-actions">
        <button
          v-if="classId && lessonId"
          type="button"
          class="action-btn indigo"
          @click="markCurrentLesson"
        >📍 设为当前课时</button>
        <button
          v-if="classId && lessonId"
          type="button"
          class="action-btn red"
          :disabled="clearingLesson"
          @click="clearCurrentLessonRecords"
        >🗑 清除当前课记录</button>
        <button type="button" class="action-btn indigo" @click="$router.push('/teacher/pretest/g4')">📝 四年级前测</button>
        <button type="button" class="action-btn indigo" @click="$router.push('/teacher/pretest/g6')">📝 六年级前测</button>
        <button type="button" class="action-btn orange" @click="$router.push('/teacher/points')">🏆 班级积分榜单</button>
        <button type="button" class="icon-btn" title="刷新" @click="loadData">↻</button>
      </div>
    </header>

    <el-alert
      v-if="classesReady && !classes.length"
      type="info"
      show-icon
      :closable="false"
      title="还没有匹配到任课班级。请学校管理人员在「后台管理 → 教师管理」里为这位老师勾选班级。"
      style="margin-bottom: 12px"
    />
    <section v-if="classId" class="class-code-banner">
      <div class="banner-text">
        <strong>班级登录码</strong>
        <span>学生：登录页 → 班级码登录 → 输入 6 位码 → 选择姓名进入课程</span>
      </div>
      <div class="banner-code-wrap">
        <span class="banner-code">{{ classLoginCode || (codeLoading ? '生成中…' : '—') }}</span>
        <button type="button" class="banner-btn" :disabled="!classLoginCode" @click="copyLoginCode">复制</button>
        <button type="button" class="banner-btn" @click="refreshLoginCode">刷新</button>
      </div>
      <p v-if="codeExpiresHint" class="banner-expires">{{ codeExpiresHint }}</p>
    </section>

    <template v-if="lessonId">
      <!-- 中部三卡片 -->
      <section class="cards-row">
        <div class="card chart-card">
          <div class="card-head">
            <h3>💻 提交情况与提交率</h3>
            <div class="legend">
              <span><i class="dot green" />已提交</span>
              <span><i class="dot red" />未提交</span>
              <span><i class="dot blue" />提交率</span>
            </div>
          </div>
          <div class="chart-wrap">
            <canvas ref="chartRef" />
          </div>
          <p v-if="!data.activities?.length" class="empty-hint">暂无活动数据</p>
        </div>

        <div class="card summary-card radar-card">
          <h3>✨ 课程小测 概况</h3>
          <p class="radar-meta">
            参与 {{ quiz.participantCount || 0 }} 人
            <span v-if="quiz.avgPercent != null"> · 均分 {{ quiz.avgPercent }}分</span>
            <span v-else-if="quiz.avgTotalScore != null"> · 均分 {{ quiz.avgTotalScore }}/{{ quiz.totalMaxScore || 6 }}</span>
            <span v-if="quiz.passRate != null"> · 及格率 {{ quiz.passRate }}%</span>
          </p>
          <div class="radar-wrap">
            <canvas ref="quizRadarRef" />
          </div>
          <p v-if="!(quiz.dimensions || []).length" class="empty-hint">暂无小测数据</p>
        </div>

        <div class="card summary-card radar-card">
          <h3>📝 学习评价 概况</h3>
          <p class="radar-meta">
            参与 {{ evaluation.participantCount || 0 }} 人
            <span v-if="evaluation.submitRate != null"> · 提交率 {{ evaluation.submitRate }}%</span>
          </p>
          <div class="radar-wrap">
            <canvas ref="evalRadarRef" />
          </div>
          <p v-if="!(evaluation.dimensions || []).length" class="empty-hint">暂无评价数据</p>
        </div>
      </section>

      <section class="analysis-section">
        <div class="analysis-col">
          <div class="section-head">
            <h3><span class="sec-icon">📊</span> 课堂小测汇总</h3>
            <div class="analysis-actions">
              <button
                type="button"
                class="act-btn sm"
                :disabled="!(quiz.lowScoreStudents || []).length"
                @click="guideLowQuizStudents"
              >给低分学生发建议</button>
              <button
                type="button"
                class="act-btn sm outline"
                :disabled="!(quiz.unsubmittedStudents || []).length"
                @click="remindQuizUnsubmitted"
              >提醒未交</button>
            </div>
          </div>
          <p class="analysis-tip">低于 {{ quiz.lowThreshold || 60 }} 分视为低分。薄弱维度指正确率不足 60%。</p>
          <div class="stat-pills">
            <span>提交 {{ quiz.participantCount || 0 }}/{{ data.totalStudents || 0 }}</span>
            <span>及格率 {{ quiz.passRate || 0 }}%</span>
            <span>低分 {{ (quiz.lowScoreStudents || []).length }} 人</span>
            <span>未交 {{ (quiz.unsubmittedStudents || []).length }} 人</span>
          </div>
          <div v-if="(quiz.weakDimensions || []).length" class="weak-row">
            <b>薄弱维度</b>
            <span v-for="d in quiz.weakDimensions" :key="d.name" class="weak-tag">
              {{ d.name }} {{ d.correctRate }}%
            </span>
          </div>
          <div class="name-block">
            <div class="name-label warn">低分学生</div>
            <div v-if="(quiz.lowScoreStudents || []).length" class="name-chips">
              <span v-for="s in quiz.lowScoreStudents" :key="'q-low-' + s.studentId" class="name-chip warn">
                {{ s.realName }}<small>{{ s.scoreLabel || (s.score + '分') }}</small>
              </span>
            </div>
            <p v-else class="empty-hint">暂无低分学生</p>
          </div>
          <div class="name-block">
            <div class="name-label">未交小测</div>
            <div v-if="(quiz.unsubmittedStudents || []).length" class="name-chips">
              <span v-for="s in quiz.unsubmittedStudents" :key="'q-miss-' + s.studentId" class="name-chip">
                {{ s.realName }}
              </span>
            </div>
            <p v-else class="empty-hint">全部已交</p>
          </div>
        </div>

        <div class="analysis-col">
          <div class="section-head">
            <h3><span class="sec-icon">📝</span> 学习评价汇总</h3>
            <button
              type="button"
              class="act-btn sm outline"
              :disabled="!(evaluation.unsubmittedStudents || []).length"
              @click="remindEvalUnsubmitted"
            >提醒未交</button>
          </div>
          <p class="analysis-tip">兴趣选 C/D、信心选 C/D 的学生会列入需关注；难度题选「都不难」不算低分。</p>
          <div class="stat-pills">
            <span>提交 {{ evaluation.participantCount || 0 }}/{{ data.totalStudents || 0 }}</span>
            <span>需关注 {{ (evaluation.concernStudents || []).length }} 人</span>
            <span>未交 {{ (evaluation.unsubmittedStudents || []).length }} 人</span>
          </div>
          <div class="eval-dim-row">
            <span v-for="d in evaluation.dimensions || []" :key="d.name">
              {{ d.name }} 正向 {{ d.positiveRate || 0 }}%
            </span>
          </div>
          <div class="name-block">
            <div class="name-label warn">需关注学生</div>
            <div v-if="(evaluation.concernStudents || []).length" class="name-chips">
              <span v-for="s in evaluation.concernStudents" :key="'e-low-' + s.studentId" class="name-chip warn">
                {{ s.realName }}<small>{{ (s.reasons || []).join('、') }}</small>
              </span>
            </div>
            <p v-else class="empty-hint">暂无需关注学生</p>
          </div>
          <div class="name-block">
            <div class="name-label">未交评价</div>
            <div v-if="(evaluation.unsubmittedStudents || []).length" class="name-chips">
              <span v-for="s in evaluation.unsubmittedStudents" :key="'e-miss-' + s.studentId" class="name-chip">
                {{ s.realName }}
              </span>
            </div>
            <p v-else class="empty-hint">全部已交</p>
          </div>
        </div>
      </section>

      <!-- 活动提交进度 -->
      <section class="progress-section">
        <div class="section-head">
          <h3><span class="sec-icon">📚</span> 活动提交进度概览</h3>
          <div class="view-toggle">
            <button type="button" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">网格视图</button>
            <button type="button" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">列表视图</button>
          </div>
        </div>

        <div v-if="viewMode === 'list'" class="activity-list">
          <div v-for="act in data.activities || []" :key="act.index" class="activity-row">
            <div class="act-index">#{{ act.index }}</div>
            <div class="act-info">
              <div class="act-title">
                {{ activityLabel(act) }}
              </div>
              <div class="act-meta">{{ act.submittedCount || 0 }} / {{ data.totalStudents || 0 }} 人已提交</div>
              <div v-if="act.tierStats" class="act-tiers">{{ formatTierStats(act.tierStats) }}</div>
            </div>
            <div class="act-progress">
              <el-progress
                :percentage="act.submitRate || 0"
                :stroke-width="10"
                :color="progressColor(act.submitRate)"
              />
            </div>
            <div class="act-rate">{{ act.submitRate || 0 }}%</div>
            <div class="act-actions">
              <button type="button" class="act-btn" @click="openDetail(act, 'submitted')">提交详情</button>
              <button type="button" class="act-btn outline" @click="openDetail(act, 'unsubmitted')">未交名单</button>
            </div>
          </div>
          <p v-if="!(data.activities || []).length" class="empty-hint">该课时暂无活动数据</p>
        </div>

        <div v-else class="activity-grid">
          <div v-for="act in data.activities || []" :key="'g-' + act.index" class="grid-card">
            <div class="grid-head">
              <span class="grid-num">#{{ act.index }}</span>
            </div>
            <div class="grid-title">{{ activityLabel(act) }}</div>
            <div class="grid-count">{{ act.submittedCount || 0 }} / {{ data.totalStudents || 0 }}</div>
            <div v-if="act.tierStats" class="grid-tiers">{{ formatTierStats(act.tierStats) }}</div>
            <el-progress
              :percentage="act.submitRate || 0"
              :stroke-width="8"
              :color="progressColor(act.submitRate)"
            />
            <div class="grid-actions">
              <button type="button" class="act-btn sm" @click="openDetail(act, 'submitted')">提交详情</button>
            </div>
          </div>
          <p v-if="!(data.activities || []).length" class="empty-hint">该课时暂无活动数据</p>
        </div>
      </section>

      <section class="park-section">
        <div class="section-head">
          <h3>
            <span class="sec-icon">🎮</span>
            游学乐园授权
            <span v-if="lessonId" class="park-lesson">· {{ currentLessonTitle }}</span>
            <el-tag v-if="parkPendingCount" size="small" type="warning" style="margin-left:8px">
              {{ parkPendingCount }} 待审
            </el-tag>
          </h3>
          <div class="park-actions">
            <button type="button" class="act-btn sm" :disabled="!lessonId" @click="reviewParkClass('approve')">一键授权</button>
            <button type="button" class="act-btn sm" :disabled="!lessonId" @click="reviewParkClass('revoke')">一键关闭</button>
            <button type="button" class="act-btn sm" @click="loadParkApps">刷新</button>
          </div>
        </div>
        <p class="park-tip">上方选中哪一课，这里就只控制哪一课。第1课的授权和游玩记录不会带到第2课，关闭第2课也不会关掉第1课。</p>
        <el-table :data="parkApps" stripe size="small" max-height="280" v-loading="parkLoading">
          <el-table-column prop="realName" label="学生" width="100" />
          <el-table-column prop="username" label="学号" width="110" />
          <el-table-column prop="lessonTitle" label="关联课时" min-width="160">
            <template #default="{ row }">{{ row.lessonTitle || '—' }}</template>
          </el-table-column>
          <el-table-column label="任务" width="90" align="center">
            <template #default="{ row }">
              <span :class="row.taskCompleted ? 'done' : 'undone'">
                {{ row.taskCompleted ? '已完成' : '未完成' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="parkStatusType(row.status)">{{ parkStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.appliedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING' || row.status === 'REJECTED' || row.status === 'REVOKED'"
                link
                type="success"
                size="small"
                :disabled="!row.taskCompleted"
                @click="reviewPark(row, 'approve')"
              >授权</el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                link
                type="warning"
                size="small"
                @click="reviewPark(row, 'reject')"
              >拒绝</el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="danger"
                size="small"
                @click="reviewPark(row, 'revoke')"
              >关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-if="!parkApps.length && !parkLoading" class="empty-hint">这一课还没有游学乐园申请</p>

        <h4 class="park-sub">游玩记录 · {{ currentLessonTitle }}</h4>
        <el-table :data="parkSummary" stripe size="small" empty-text="这一课还没有游玩记录">
          <el-table-column prop="gameTitle" label="游戏名称" min-width="120" />
          <el-table-column prop="playerCount" label="游玩人数" width="100" align="center" />
          <el-table-column label="总时长" width="120" align="center">
            <template #default="{ row }">{{ formatDuration(row.totalSeconds) }}</template>
          </el-table-column>
          <el-table-column label="人均时长" width="120" align="center">
            <template #default="{ row }">{{ formatDuration(row.avgSeconds) }}</template>
          </el-table-column>
        </el-table>
        <el-table :data="parkRecent" stripe size="small" max-height="220" empty-text="暂无明细" style="margin-top:8px">
          <el-table-column prop="realName" label="学生" width="100" />
          <el-table-column prop="gameTitle" label="游戏名称" min-width="120" />
          <el-table-column label="游戏时长" width="120">
            <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
          </el-table-column>
          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.playedAt) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="themeActivity" class="matrix-section theme-section">
        <div class="section-head">
          <h3><span class="sec-icon">◎</span> {{ themeSectionTitle }}</h3>
          <span class="theme-count">已填写 {{ themeGroups.filled }} / {{ themeGroups.rows.length }} 人</span>
        </div>
        <p class="park-tip">{{ themeSectionTip }}</p>
        <div v-if="themeGroups.groups.length && themeIsVote" class="vote-bars">
          <div v-for="group in themeVoteBars" :key="group.theme" class="vote-bar-row">
            <div class="vote-bar-label">{{ group.theme }}</div>
            <div class="vote-bar-track">
              <i :style="{ width: group.percent + '%' }" />
            </div>
            <div class="vote-bar-count">{{ group.names.length }} 人</div>
          </div>
        </div>
        <div v-if="themeGroups.groups.length" class="theme-list">
          <div v-for="group in themeGroups.groups" :key="group.theme" class="theme-card">
            <div class="theme-head">
              <strong>{{ group.theme }}</strong>
              <span>{{ group.names.length }} 人</span>
            </div>
            <p>{{ group.names.join('、') || '—' }}</p>
          </div>
        </div>
        <p v-else class="empty-hint">{{ themeIsVote ? '还没有学生提交观点。' : (themeIsOpenIdea ? '还没有学生提交想法。' : '还没有学生写下调查主题。') }}</p>
        <p v-if="themeGroups.missing.length" class="theme-missing">还没写：{{ themeGroups.missing.join('、') }}</p>
      </section>

      <!-- 全班任务完成矩阵 -->
      <section class="matrix-section">
        <div class="section-head">
          <h3><span class="sec-icon">📋</span> 全班任务完成矩阵</h3>
        </div>
        <div class="matrix-wrap">
          <el-table :data="matrixRows" border stripe size="small" max-height="480">
            <el-table-column prop="realName" label="学生" fixed width="100" />
            <el-table-column prop="username" label="学号" fixed width="110" />
            <el-table-column
              v-for="col in matrixColumns"
              :key="col.index"
              :label="col.label"
              min-width="88"
              align="center"
            >
              <template #default="{ row }">
                <span
                  :class="[cellClass(row.cells[col.index]), { 'is-fc': !!row.cells[col.index]?.flowchart }]"
                  :title="row.cells[col.index]?.flowchart ? '点击放大流程图' : ''"
                  @click="openCellFlowchart(row, col.index)"
                >
                  {{ cellText(row.cells[col.index]) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <p class="matrix-legend">
          <span class="done">✓ 已完成</span>
          <span class="undone">✗ 未完成</span>
        </p>
      </section>

      <section class="matrix-section tier-section">
        <div class="section-head">
          <h3><span class="sec-icon">🎯</span> 本课表现与第2课分档</h3>
          <button
            type="button"
            class="action-btn indigo"
            :disabled="!tierStudents.length || applyingTiers"
            @click="applyAllSuggested"
          >按建议一键分档</button>
        </div>
        <p class="park-tip">
          教师端已能读取本课提交：探究完成、流程图得分、课堂小测。建议规则：
          暂无提交沿用当前档；探究完成 ≤ 1 或流程图 &lt; 50 建议 C；三个探究都完成且流程图 ≥ 80 建议 A；其余建议 B。
          指定后覆盖前测档位，六年级第2课及后续课时按此呈现支架，学生看不到 A/B/C 标签。
        </p>
        <div class="tier-counts">
          <span>当前 A {{ tierRoster.countA || 0 }} · B {{ tierRoster.countB || 0 }} · C {{ tierRoster.countC || 0 }}</span>
          <span>建议 A {{ tierRoster.suggestA || 0 }} · B {{ tierRoster.suggestB || 0 }} · C {{ tierRoster.suggestC || 0 }}</span>
          <span>有本课数据 {{ tierRoster.withData || 0 }} / {{ data.totalStudents || 0 }} 人</span>
        </div>
        <el-table :data="tierStudents" border stripe size="small" max-height="480">
          <el-table-column prop="realName" label="学生" fixed width="100" />
          <el-table-column prop="username" label="学号" width="110" />
          <el-table-column label="探究完成" width="100" align="center">
            <template #default="{ row }">{{ row.inquiryDone || 0 }} / {{ row.inquiryTotal || 0 }}</template>
          </el-table-column>
          <el-table-column label="流程图" width="88" align="center">
            <template #default="{ row }">{{ row.flowchartScore ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="小测" width="80" align="center">
            <template #default="{ row }">{{ row.quizScore == null ? '—' : `${row.quizScore}/6` }}</template>
          </el-table-column>
          <el-table-column label="当前档" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="tierTagType(row.currentTier)">{{ row.currentTier || 'B' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="建议档" min-width="180">
            <template #default="{ row }">
              <el-tag size="small" :type="tierTagType(row.suggestedTier)">{{ row.suggestedTier || 'B' }}</el-tag>
              <span class="suggest-reason">{{ row.suggestReason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="指定档位" width="220" fixed="right">
            <template #default="{ row }">
              <el-select
                :model-value="row.tierOverride || 'FOLLOW'"
                size="small"
                style="width: 118px"
                :disabled="applyingTiers"
                @change="(v) => changeRosterTier(row, v)"
              >
                <el-option label="跟随前测" value="FOLLOW" />
                <el-option label="指定 A" value="A" />
                <el-option label="指定 B" value="B" />
                <el-option label="指定 C" value="C" />
              </el-select>
              <el-button
                link
                type="primary"
                size="small"
                :disabled="applyingTiers"
                @click="applySuggestedOne(row)"
              >用建议</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <div v-else class="empty-page">
      <el-empty description="请先选择年级与课程" :image-size="88" />
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" :width="detailHasTheme ? '720px' : '520px'">
      <el-table :data="detailRows" stripe max-height="360" size="small">
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="学号" width="100" />
        <el-table-column v-if="detailHasTheme" :label="themeFieldLabel" min-width="180">
          <template #default="{ row }">{{ row.surveyTheme || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="detailMode === 'submitted'" label="得分" width="70">
          <template #default="{ row }">{{ row.score ?? '—' }}</template>
        </el-table-column>
        <el-table-column v-if="detailMode === 'submitted'" label="提交时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="168" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="detailMode === 'unsubmitted'"
              link
              type="warning"
              size="small"
              @click="remindStudent(row)"
            >提醒完成</el-button>
            <el-button
              v-if="detailMode === 'submitted' && row.flowchart"
              link
              type="success"
              size="small"
              @click="openFlowchart(row)"
            >放大流程图</el-button>
            <el-button
              v-if="detailMode === 'submitted' && isLowScore(row)"
              link
              type="primary"
              size="small"
              @click="guideStudent(row)"
            >学习建议</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!detailRows.length" class="empty-hint">暂无记录</p>
    </el-dialog>

    <el-dialog
      v-model="flowchartVisible"
      :title="flowchartTitle"
      width="92%"
      top="4vh"
      class="fc-zoom-dialog"
      destroy-on-close
    >
      <div v-if="flowchartStudent" class="fc-zoom-meta">
        <strong>{{ flowchartStudent.realName }}</strong>
        <span>得分 {{ flowchartStudent.flowchartScore ?? flowchartStudent.score ?? '—' }}</span>
        <span v-if="flowchartStudent.flowchartVersion">第 {{ flowchartStudent.flowchartVersion }} 次提交</span>
        <span v-if="flowchartStudent.flowchartPassed">已达标，可体验投票器</span>
        <span v-else>未达标，讲解后可让学生二次修改</span>
      </div>
      <FlowchartPreview v-if="flowchartStudent?.flowchart" :flowchart="flowchartStudent.flowchart" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Chart,
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  RadarController,
  Filler,
  Tooltip,
  Legend
} from 'chart.js'
import {
  getClasses,
  getLessonActivityDashboard,
  clearLessonClassRecords,
  teacherGetTextbooks,
  teacherGetOutline,
  getClassLoginCode,
  refreshClassLoginCode,
  teacherIntervene,
  setClassCurrentLesson,
  getParkApplications,
  reviewParkAccess,
  reviewParkClass as reviewParkClassAccess,
  getParkPlays,
  teacherOverrideTier
} from '../../api'
import FlowchartPreview from '../../components/teacher/FlowchartPreview.vue'

Chart.register(
  BarController, BarElement, LineController, LineElement, PointElement,
  CategoryScale, LinearScale, RadialLinearScale, RadarController, Filler,
  Tooltip, Legend
)

const QUIZ_RADAR_LABELS = ['知识掌握', '能力提升', '素养变化']
const EVAL_RADAR_LABELS = ['学习兴趣', '学习难度', '学习信心']

const route = useRoute()
const router = useRouter()

const grades = ref([])
const gradeId = ref(null)
const lessons = ref([])
const lessonId = ref(null)
const classes = ref([])
const classesReady = ref(false)
const classId = ref(null)
const classLabel = (row) => {
  const grade = row?.gradeName || ''
  const name = row?.name || ''
  if (!grade || name.includes(grade)) return name
  return `${grade} ${name}`
}
const classLoginCode = ref('')
const codeExpiresAt = ref('')
const codeLoading = ref(false)
const data = ref({})
const loading = ref(false)
const viewMode = ref('list')
const chartRef = ref(null)
const quizRadarRef = ref(null)
const evalRadarRef = ref(null)
const sseConnected = ref(false)
let chart = null
let quizRadar = null
let evalRadar = null
let eventSource = null

const detailVisible = ref(false)
const detailMode = ref('submitted')
const detailAct = ref(null)
const flowchartVisible = ref(false)
const flowchartStudent = ref(null)

const parkApps = ref([])
const parkLoading = ref(false)
const parkSummary = ref([])
const parkRecent = ref([])
const parkPendingCount = computed(() => parkApps.value.filter((r) => r.status === 'PENDING').length)

const applyingTiers = ref(false)
const clearingLesson = ref(false)
const quiz = computed(() => data.value.quizSummary || {})
const tierRoster = computed(() => data.value.tierRoster || {})
const tierStudents = computed(() => tierRoster.value.students || [])
const evaluation = computed(() => data.value.evaluationSummary || {})

const codeExpiresHint = computed(() => {
  if (!codeExpiresAt.value) return ''
  const dt = new Date(codeExpiresAt.value)
  if (Number.isNaN(dt.getTime())) return ''
  return `有效期至 ${dt.toLocaleString('zh-CN', { hour12: false })}（1 小时内有效）`
})

const detailTitle = computed(() => {
  if (!detailAct.value) return '提交详情'
  const label = activityLabel(detailAct.value)
  return detailMode.value === 'submitted' ? `${label} · 已交名单` : `${label} · 未交名单`
})

const detailRows = computed(() => {
  if (!detailAct.value) return []
  return detailMode.value === 'submitted'
    ? (detailAct.value.submittedStudents || [])
    : (detailAct.value.unsubmittedStudents || [])
})

const detailHasTheme = computed(() =>
  detailMode.value === 'submitted' && detailRows.value.some((row) => row.surveyTheme)
)

const themeActivity = computed(() =>
  (data.value.activities || []).find((act) => Array.isArray(act.surveyThemes)) || null
)

const themeIsVote = computed(() => {
  const act = themeActivity.value
  if (!act) return false
  if (act.voteChart || act.type === 'VOTE' || act.type === 'SURVEY') return true
  const title = String(act.title || '')
  return title.includes('投票') || title.includes('观点')
})

const themeIsOpenIdea = computed(() => {
  const act = themeActivity.value
  if (!act || themeIsVote.value) return false
  if (act.openResponses || act.surveyTheme) return true
  const title = String(act.title || '')
  return title.includes('验证与升级') || title.includes('开放')
})

const themeFieldLabel = computed(() => {
  if (themeIsVote.value) return '选项'
  if (themeIsOpenIdea.value) return '学生想法'
  return '调查主题'
})

const themeSectionTitle = computed(() => {
  const act = themeActivity.value
  if (!act) return '调查汇总'
  if (themeIsVote.value) {
    return `探究${chineseNum(act.index)} · ${act.title || '观点投票'}`
  }
  if (themeIsOpenIdea.value) {
    return `探究${chineseNum(act.index)} · ${act.title || '开放想法'}（想法汇总）`
  }
  return `探究${chineseNum(act.index)} · ${act.title || '调查主题'}`
})

const themeSectionTip = computed(() =>
  themeIsVote.value
    ? '按选项统计人数，下方列出每位学生的选择，方便课堂讲评。'
    : themeIsOpenIdea.value
      ? '学生提交的开放想法按原文分组汇总，方便课堂点评和归类。'
      : '学生写下的原话按完全相同分成一组，方便按主题分组。说法接近但不一样的，可以再合并到同一组。'
)

const themeGroups = computed(() => {
  const rows = themeActivity.value?.surveyThemes || []
  const grouped = new Map()
  const missing = []
  rows.forEach((row) => {
    const theme = String(row.surveyTheme || '').trim()
    if (!theme) {
      missing.push(row.realName)
      return
    }
    if (!grouped.has(theme)) grouped.set(theme, [])
    grouped.get(theme).push(row.realName)
  })
  const groups = [...grouped.entries()]
    .map(([theme, names]) => ({ theme, names }))
    .sort((a, b) => {
      const ka = String(a.theme).charAt(0)
      const kb = String(b.theme).charAt(0)
      if (/[A-D]/.test(ka) && /[A-D]/.test(kb) && ka !== kb) return ka.localeCompare(kb)
      return b.names.length - a.names.length || a.theme.localeCompare(b.theme, 'zh-CN')
    })
  return { rows, groups, missing, filled: rows.length - missing.length }
})

const themeVoteBars = computed(() => {
  const total = Math.max(1, themeGroups.value.filled)
  return themeGroups.value.groups.map((g) => ({
    ...g,
    percent: Math.round((g.names.length * 1000) / total) / 10
  }))
})

const matrixColumns = computed(() =>
  (data.value.activities || []).map((act) => ({
    index: act.index,
    label: act.type === 'QUIZ' ? '小测' : act.type === 'EVALUATION' ? '评价' : `#${act.index}`
  }))
)

const matrixRows = computed(() => {
  const acts = data.value.activities || []
  if (!acts.length) return []

  const studentMap = new Map()
  for (const act of acts) {
    const submittedIds = new Set((act.submittedStudents || []).map((s) => s.studentId))
    const allStudents = [...(act.submittedStudents || []), ...(act.unsubmittedStudents || [])]
    for (const s of allStudents) {
      if (!studentMap.has(s.studentId)) {
        studentMap.set(s.studentId, {
          studentId: s.studentId,
          realName: s.realName,
          username: s.username,
          cells: {}
        })
      }
      const row = studentMap.get(s.studentId)
      const submitted = act.submittedStudents?.find((x) => x.studentId === s.studentId)
      row.cells[act.index] = {
        done: submittedIds.has(s.studentId),
        score: submitted?.score ?? null,
        flowchart: submitted?.flowchart || null,
        flowchartScore: submitted?.flowchartScore ?? submitted?.score ?? null,
        flowchartVersion: submitted?.flowchartVersion,
        flowchartPassed: submitted?.flowchartPassed
      }
    }
  }
  return Array.from(studentMap.values()).sort((a, b) =>
    String(a.realName).localeCompare(String(b.realName), 'zh-CN')
  )
})

const chineseNum = (n) => ({ 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六' }[n] || n)

const activityLabel = (act) => {
  if (act.type === 'EVALUATION') return '学习评价'
  if (act.type === 'QUIZ') return '课堂小测'
  return `探究活动${chineseNum(act.index)}：${act.title}`
}

const formatTierStats = (stats) => {
  if (!stats) return ''
  const line = (code) => {
    const t = stats[code] || {}
    return `${code} ${t.count || 0}人 · 完成 ${t.rate || 0}% · 提示均 ${t.hintAvg ?? 0} 次`
  }
  return [line('A'), line('B'), line('C')].join(' ｜ ')
}

const buildQuizRadarValues = () => {
  const map = {}
  ;(quiz.value.dimensions || []).forEach((d) => {
    map[d.name] = Number(d.correctRate ?? 0)
  })
  return QUIZ_RADAR_LABELS.map((label) => map[label] ?? 0)
}

const buildEvalRadarValues = () => {
  const map = {}
  ;(evaluation.value.dimensions || []).forEach((d) => {
    const label = d.name === '难度感知' ? '学习难度' : d.name
    map[label] = Number(d.positiveRate ?? 0)
  })
  return EVAL_RADAR_LABELS.map((label) => map[label] ?? 0)
}

const renderRadarChart = (canvas, labels, values, color) => {
  if (!canvas) return null
  return new Chart(canvas, {
    type: 'radar',
    data: {
      labels,
      datasets: [{
        label: '班级概况',
        data: values,
        backgroundColor: `${color}33`,
        borderColor: color,
        borderWidth: 2,
        pointBackgroundColor: color,
        pointRadius: 3
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        r: {
          beginAtZero: true,
          max: 100,
          ticks: { stepSize: 25, backdropColor: 'transparent', font: { size: 10 } },
          pointLabels: { font: { size: 12, weight: '600' }, color: '#475569' },
          grid: { color: 'rgba(148, 163, 184, 0.35)' }
        }
      }
    }
  })
}

const renderRadarCharts = () => {
  if (quizRadar) {
    quizRadar.destroy()
    quizRadar = null
  }
  if (evalRadar) {
    evalRadar.destroy()
    evalRadar = null
  }
  if (quizRadarRef.value) {
    quizRadar = renderRadarChart(quizRadarRef.value, QUIZ_RADAR_LABELS, buildQuizRadarValues(), '#6366f1')
  }
  if (evalRadarRef.value) {
    evalRadar = renderRadarChart(evalRadarRef.value, EVAL_RADAR_LABELS, buildEvalRadarValues(), '#8b5cf6')
  }
}

const progressColor = (rate) => {
  if (rate >= 80) return '#22c55e'
  if (rate >= 40) return '#6366f1'
  return '#f87171'
}

const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

const cellClass = (cell) => (cell?.done ? 'done' : 'undone')
const cellText = (cell) => {
  if (!cell?.done) return '✗'
  if (cell.flowchart && cell.flowchartScore != null) return `✓${cell.flowchartScore}`
  return cell.score != null ? `✓${cell.score}` : '✓'
}

const tierTagType = (lv) => {
  if (lv === 'A') return 'success'
  if (lv === 'B') return 'warning'
  return 'danger'
}

const changeRosterTier = async (row, value) => {
  const next = value === 'FOLLOW' ? null : value
  try {
    await ElMessageBox.confirm(
      '指定后六年级第2课及后续课时将按此档呈现支架，确定？',
      '调整课堂档位',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  applyingTiers.value = true
  try {
    await teacherOverrideTier({ studentId: row.studentId, tier: next || 'CLEAR' })
    ElMessage.success(`${row.realName} 的档位已更新`)
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '档位更新失败')
  } finally {
    applyingTiers.value = false
  }
}

const applySuggestedOne = async (row) => {
  applyingTiers.value = true
  try {
    await teacherOverrideTier({ studentId: row.studentId, tier: row.suggestedTier || 'B' })
    ElMessage.success(`已按建议将 ${row.realName} 指定为 ${row.suggestedTier}`)
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '指定失败')
  } finally {
    applyingTiers.value = false
  }
}

const applyAllSuggested = async () => {
  if (!tierStudents.value.length) return
  try {
    await ElMessageBox.confirm(
      `将按本课表现建议，为 ${tierStudents.value.length} 名学生指定课堂档位，供第2课使用。确定？`,
      '按建议一键分档',
      { type: 'warning', confirmButtonText: '确定分档', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  applyingTiers.value = true
  try {
    for (const row of tierStudents.value) {
      await teacherOverrideTier({ studentId: row.studentId, tier: row.suggestedTier || 'B' })
    }
    ElMessage.success('已按本课表现指定课堂档位，第2课将按此分档')
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '批量分档失败')
  } finally {
    applyingTiers.value = false
  }
}

const openDetail = (act, mode) => {
  detailAct.value = act
  detailMode.value = mode
  detailVisible.value = true
}

const flowchartTitle = computed(() => {
  const name = flowchartStudent.value?.realName || '学生'
  return `${name} 的流程图`
})

const openFlowchart = (row) => {
  flowchartStudent.value = row
  flowchartVisible.value = true
}

const openCellFlowchart = (row, actIndex) => {
  const cell = row?.cells?.[actIndex]
  if (!cell?.flowchart) return
  openFlowchart({
    realName: row.realName,
    flowchart: cell.flowchart,
    flowchartScore: cell.flowchartScore,
    score: cell.score,
    flowchartVersion: cell.flowchartVersion,
    flowchartPassed: cell.flowchartPassed
  })
}

const parkStatusText = (s) => ({
  PENDING: '待审批',
  APPROVED: '已授权',
  REJECTED: '已拒绝',
  REVOKED: '已关闭'
}[s] || s)

const parkStatusType = (s) => ({
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'info',
  REVOKED: 'danger'
}[s] || 'info')

const formatDuration = (seconds) => {
  const n = Math.max(0, Number(seconds) || 0)
  const m = Math.floor(n / 60)
  const s = n % 60
  if (m <= 0) return `${s}秒`
  return `${m}分${s}秒`
}

const loadParkApps = async () => {
  if (!classId.value || !lessonId.value) {
    parkApps.value = []
    parkSummary.value = []
    parkRecent.value = []
    return
  }
  parkLoading.value = true
  try {
    const apps = await getParkApplications(classId.value, lessonId.value)
    parkApps.value = apps.data || []
  } catch {
    parkApps.value = []
  }
  try {
    const plays = await getParkPlays(classId.value, lessonId.value)
    parkSummary.value = plays.data?.summary || []
    parkRecent.value = plays.data?.recent || []
  } catch {
    parkSummary.value = []
    parkRecent.value = []
  } finally {
    parkLoading.value = false
  }
}

const reviewParkClass = async (action) => {
  if (!classId.value || !lessonId.value) return
  const lessonName = currentLessonTitle.value
  const title = action === 'approve' ? '一键授权' : '一键关闭'
  const message = action === 'approve'
    ? `只为已完成「${lessonName}」全部探究活动、课堂小测和学习评价的学生开启这一课的游学乐园。其他课不变。`
    : `只关闭「${lessonName}」的游学乐园。其他课的授权还在，学生可以继续上课。`
  try {
    await ElMessageBox.confirm(message, title, { type: 'warning' })
    const res = await reviewParkClassAccess({ classId: classId.value, lessonId: lessonId.value, action })
    const changed = res.data?.changed || 0
    const skipped = res.data?.skipped || 0
    ElMessage.success(action === 'approve'
      ? `「${lessonName}」已授权 ${changed} 人，未完成的跳过 ${skipped} 人`
      : `已关闭「${lessonName}」的游学乐园，其他课不受影响`)
    await loadParkApps()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败')
  }
}

const reviewPark = async (row, action) => {
  try {
    await reviewParkAccess({ studentId: row.studentId, lessonId: row.lessonId || lessonId.value, action })
    ElMessage.success(action === 'approve' ? '已授权这一课' : action === 'reject' ? '已拒绝这一课的申请' : '已关闭这一课')
    await loadParkApps()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const isLowScore = (row) => {
  if (row.score == null) return false
  const act = detailAct.value
  if (act?.type === 'QUIZ') return row.score < 4
  return row.score < 60
}

const remindStudent = async (row) => {
  if (!classId.value || !lessonId.value || !detailAct.value) return
  const act = detailAct.value
  const actIndex = act.type === 'QUIZ' || act.type === 'EVALUATION' ? act.index : act.index
  const label = activityLabel(act)
  try {
    await teacherIntervene({
      type: 'remind',
      classId: classId.value,
      targetStudentId: row.studentId,
      lessonId: lessonId.value,
      activityIndex: actIndex,
      message: `请尽快完成${label}`
    })
    ElMessage.success(`已向 ${row.realName} 发送提醒`)
  } catch {
    ElMessage.error('发送失败')
  }
}

const guideStudent = async (row) => {
  if (!classId.value) return
  try {
    await teacherIntervene({
      type: 'guide',
      classId: classId.value,
      targetStudentId: row.studentId,
      lessonId: lessonId.value,
      content: '建议回顾本课内容与教材，巩固薄弱知识点后再继续练习'
    })
    ElMessage.success(`已向 ${row.realName} 发送学习建议`)
  } catch {
    ElMessage.error('发送失败')
  }
}

const findActivityByType = (type) => (data.value.activities || []).find((a) => a.type === type)

const remindListedStudents = async (students, act, message) => {
  if (!classId.value || !lessonId.value || !act || !students?.length) return
  let ok = 0
  for (const row of students) {
    try {
      await teacherIntervene({
        type: 'remind',
        classId: classId.value,
        targetStudentId: row.studentId,
        lessonId: lessonId.value,
        activityIndex: act.index,
        message
      })
      ok += 1
    } catch { /* 单个失败继续 */ }
  }
  if (ok) ElMessage.success(`已向 ${ok} 名学生发送提醒`)
  else ElMessage.error('发送失败')
}

const remindQuizUnsubmitted = () => {
  const act = findActivityByType('QUIZ')
  return remindListedStudents(quiz.value.unsubmittedStudents, act, '请尽快完成课堂小测')
}

const remindEvalUnsubmitted = () => {
  const act = findActivityByType('EVALUATION')
  return remindListedStudents(evaluation.value.unsubmittedStudents, act, '请尽快完成学习评价')
}

const guideLowQuizStudents = async () => {
  const list = quiz.value.lowScoreStudents || []
  if (!classId.value || !list.length) return
  let ok = 0
  for (const row of list) {
    try {
      await teacherIntervene({
        type: 'guide',
        classId: classId.value,
        targetStudentId: row.studentId,
        lessonId: lessonId.value,
        content: '课堂小测得分偏低，建议回顾本课探究与教材后再练习。'
      })
      ok += 1
    } catch { /* 单个失败继续 */ }
  }
  if (ok) ElMessage.success(`已向 ${ok} 名低分学生发送学习建议`)
  else ElMessage.error('发送失败')
}

const markCurrentLesson = async () => {
  if (!classId.value || !lessonId.value) return
  try {
    await setClassCurrentLesson(classId.value, lessonId.value)
    ElMessage.success('已设为班级当前课时，学生登录后将自动进入')
  } catch {
    ElMessage.error('设置失败')
  }
}

const currentLessonTitle = computed(() => {
  const fromData = data.value.lessonTitle
  if (fromData) return fromData
  return lessons.value.find((l) => l.id === lessonId.value)?.title || '当前课时'
})

const currentClassName = computed(() => {
  return data.value.className || classes.value.find((c) => c.id === classId.value)?.name || '当前班级'
})

const clearCurrentLessonRecords = async () => {
  if (!classId.value || !lessonId.value) return
  try {
    await ElMessageBox.confirm(
      `确定清除「${currentClassName.value}」在「${currentLessonTitle.value}」的全部上课记录吗？探究提交、流程图、小测、评价、本课积分和访问记录都会删除，学生可重新作答。前测和课堂分档不会清除。此操作不可恢复。`,
      '清除当前课记录',
      { type: 'warning', confirmButtonText: '确定清除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  clearingLesson.value = true
  try {
    const res = await clearLessonClassRecords(lessonId.value, classId.value)
    ElMessage.success(res.message || '已清除本课上课记录')
    await loadData()
    await loadParkApps()
  } catch (e) {
    ElMessage.error(e?.message || '清除失败')
  } finally {
    clearingLesson.value = false
  }
}

const syncQuery = () => {
  router.replace({
    query: {
      gradeId: gradeId.value || undefined,
      lessonId: lessonId.value || undefined,
      classId: classId.value || undefined
    }
  })
}

const flattenLessons = (outline) => {
  const list = []
  for (const unit of outline?.units || []) {
    for (const lesson of unit.lessons || []) {
      list.push({ id: lesson.id, title: lesson.title, unitName: unit.name })
    }
  }
  return list
}

const loadLessonsForGrade = async (keepLessonId) => {
  if (!gradeId.value) {
    lessons.value = []
    lessonId.value = null
    return
  }
  const outline = await teacherGetOutline(gradeId.value)
  lessons.value = flattenLessons(outline.data)
  if (keepLessonId && lessons.value.some((l) => l.id === keepLessonId)) {
    lessonId.value = keepLessonId
  } else if (lessons.value.length) {
    lessonId.value = lessons.value[0].id
  } else {
    lessonId.value = null
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (chart) chart.destroy()
  const acts = data.value.activities || []
  if (!acts.length) return

  const labels = acts.map((a) => {
    if (a.type === 'EVALUATION') return '学习评价'
    if (a.type === 'QUIZ') return '课堂小测'
    return `探究活动${chineseNum(a.index)}`
  })
  const submitted = acts.map((a) => a.submittedCount || 0)
  const unsubmitted = acts.map((a) => a.unsubmittedCount || 0)
  const rates = acts.map((a) => a.submitRate || 0)

  chart = new Chart(chartRef.value, {
    data: {
      labels,
      datasets: [
        {
          type: 'bar',
          label: '已提交',
          data: submitted,
          backgroundColor: '#22c55e',
          borderRadius: 4,
          yAxisID: 'y'
        },
        {
          type: 'bar',
          label: '未提交',
          data: unsubmitted,
          backgroundColor: '#f87171',
          borderRadius: 4,
          yAxisID: 'y'
        },
        {
          type: 'line',
          label: '提交率',
          data: rates,
          borderColor: '#6366f1',
          backgroundColor: '#6366f1',
          tension: 0.3,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { display: false } },
      scales: {
        y: {
          type: 'linear',
          position: 'left',
          beginAtZero: true,
          max: Math.max(data.value.totalStudents || 10, 10),
          title: { display: true, text: '人数' }
        },
        y1: {
          type: 'linear',
          position: 'right',
          beginAtZero: true,
          max: 100,
          grid: { drawOnChartArea: false },
          title: { display: true, text: '提交率%' }
        }
      }
    }
  })
}

const loadClassLoginCode = async () => {
  if (!classId.value) {
    classLoginCode.value = ''
    codeExpiresAt.value = ''
    return
  }
  codeLoading.value = true
  try {
    const res = await getClassLoginCode(classId.value)
    classLoginCode.value = res.data?.loginCode || ''
    codeExpiresAt.value = res.data?.expiresAt || ''
  } catch {
    classLoginCode.value = ''
    codeExpiresAt.value = ''
    ElMessage.error('班级码获取失败，请确认已登录教师账号')
  } finally {
    codeLoading.value = false
  }
}

const refreshLoginCode = async () => {
  if (!classId.value) return
  codeLoading.value = true
  try {
    const res = await refreshClassLoginCode(classId.value)
    classLoginCode.value = res.data?.loginCode || ''
    codeExpiresAt.value = res.data?.expiresAt || ''
    ElMessage.success(`班级码已更新：${classLoginCode.value}`)
  } catch {
    ElMessage.error('刷新班级码失败')
  } finally {
    codeLoading.value = false
  }
}

const copyLoginCode = async () => {
  if (!classLoginCode.value) return
  try {
    await navigator.clipboard.writeText(classLoginCode.value)
    ElMessage.success('班级码已复制')
  } catch {
    ElMessage.info(`班级码：${classLoginCode.value}`)
  }
}

let dashboardLoading = false
let silentQueued = false
let silentTimer = null
const scheduleSilentReload = () => {
  if (silentTimer) return
  silentTimer = setTimeout(() => {
    silentTimer = null
    loadData(true)
  }, 4000)
}
const loadData = async (silent = false) => {
  if (!classId.value || !lessonId.value) {
    data.value = {}
    return
  }
  if (dashboardLoading) {
    if (silent) silentQueued = true
    return
  }
  dashboardLoading = true
  if (!silent) loading.value = true
  try {
    const res = await getLessonActivityDashboard(lessonId.value, classId.value)
    data.value = res.data || {}
    await nextTick()
    renderChart()
    renderRadarCharts()
  } catch {
    if (!silent) ElMessage.error('加载学情数据失败')
    if (!silent) data.value = {}
  } finally {
    dashboardLoading = false
    loading.value = false
    if (silentQueued) {
      silentQueued = false
      scheduleSilentReload()
    }
  }
}

const connectSse = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  sseConnected.value = false
  if (!classId.value) return
  const token = localStorage.getItem('token')
  eventSource = new EventSource(`/api/dashboard/sse/${classId.value}?token=${token}`)
  eventSource.addEventListener('connected', () => { sseConnected.value = true })
  eventSource.addEventListener('dashboard', () => { scheduleSilentReload() })
  eventSource.onerror = () => { sseConnected.value = false }
}

const onGradeChange = async () => {
  await loadLessonsForGrade()
  syncQuery()
  await loadData()
}

const onLessonChange = async () => {
  syncQuery()
  await Promise.all([loadData(), loadParkApps()])
}

const onClassChange = async () => {
  syncQuery()
  connectSse()
  await loadClassLoginCode()
  await Promise.all([loadData(), loadParkApps()])
}

onMounted(async () => {
  const [gradeRes, clsRes] = await Promise.all([
    teacherGetTextbooks('MAIN'),
    getClasses()
  ])
  grades.value = gradeRes.data || []
  classes.value = clsRes.data || []
  classesReady.value = true

  const qGrade = route.query.gradeId ? Number(route.query.gradeId) : null
  const qLesson = route.query.lessonId ? Number(route.query.lessonId) : null
  const qClass = route.query.classId ? Number(route.query.classId) : null

  if (qGrade && grades.value.some((g) => g.id === qGrade)) {
    gradeId.value = qGrade
  } else if (grades.value.length) {
    gradeId.value = grades.value[0].id
  }

  await loadLessonsForGrade(qLesson)

  if (qClass && classes.value.some((c) => c.id === qClass)) {
    classId.value = qClass
  } else if (classes.value.length) {
    classId.value = classes.value[0].id
  } else {
    classId.value = null
  }

  syncQuery()
  connectSse()
  await loadClassLoginCode()
  await Promise.all([loadData(), loadParkApps()])
})

onUnmounted(() => {
  if (eventSource) eventSource.close()
  if (chart) chart.destroy()
  if (quizRadar) quizRadar.destroy()
  if (evalRadar) evalRadar.destroy()
})

watch(() => data.value.activities, () => nextTick(() => {
  renderChart()
  renderRadarCharts()
}))
watch(() => [quiz.value.dimensions, evaluation.value.dimensions], () => nextTick(renderRadarCharts), { deep: true })
</script>

<style scoped>
.cld-page {
  min-height: calc(100vh - 72px);
  margin: -16px -20px -24px;
  background: #eef0f6;
}
.class-code-banner {
  margin: 0;
  padding: 14px 20px;
  background: linear-gradient(90deg, #fef3c7 0%, #fff7ed 100%);
  border-bottom: 1px solid #fcd34d;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
}
.banner-text {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #92400e;
}
.banner-text strong {
  font-size: 15px;
  color: #78350f;
}
.banner-code-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}
.banner-code {
  font-family: ui-monospace, monospace;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: #b45309;
  min-width: 8.5em;
  text-align: center;
}
.banner-btn {
  border: 1px solid #f59e0b;
  background: #fff;
  color: #b45309;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.banner-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.banner-expires {
  width: 100%;
  margin: 0;
  font-size: 12px;
  color: #a16207;
}
.cld-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-row {
  display: flex;
  align-items: center;
  min-width: 0;
}
.header-row-filters {
  gap: 12px;
}
.header-row-actions {
  gap: 8px;
  flex-wrap: wrap;
  padding-left: 2px;
}
.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}
.title-icon { font-size: 20px; }
.live-tag { margin-left: 4px; }
.sse-tag { margin-left: 0; }
.header-pills {
  flex: 1;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}
.pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(4px);
  flex-shrink: 0;
}
.pill-label { opacity: 0.85; white-space: nowrap; }
.pill-value { font-weight: 600; white-space: nowrap; }
.pill-select { padding-right: 4px; }
.filter-select { width: 140px; }
.grade-select { width: 130px; }
.lesson-select { width: 200px; }
.class-select { width: 120px; }
.pill-code {
  padding-right: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.code-val {
  font-family: ui-monospace, monospace;
  font-size: 15px;
  letter-spacing: 0.12em;
}
.code-copy,
.code-refresh {
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}
.filter-select :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
}
.header-row-actions .action-btn,
.header-row-actions .icon-btn {
  flex-shrink: 0;
}
.action-btn {
  padding: 8px 14px;
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.action-btn.orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.action-btn.pink { background: linear-gradient(135deg, #ec4899, #f472b6); }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.action-btn.red { background: linear-gradient(135deg, #dc2626, #f87171); }
.action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.icon-btn {
  width: 36px; height: 36px;
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 8px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.cards-row {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.8fr;
  gap: 14px;
  padding: 16px 20px 0;
}
@media (max-width: 1200px) {
  .cards-row { grid-template-columns: 1fr; }
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}
.legend { display: flex; gap: 12px; font-size: 11px; color: #64748b; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.dot.green { background: #22c55e; }
.dot.red { background: #f87171; }
.dot.blue { background: #6366f1; }
.chart-wrap { height: 220px; position: relative; }
.radar-card h3 { margin-bottom: 6px; }
.radar-meta {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
}
.radar-wrap {
  height: 220px;
  position: relative;
}
.eval-charts-placeholder {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.placeholder-box {
  height: 72px;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #94a3b8;
}

.progress-section,
.matrix-section,
.park-section,
.analysis-section {
  margin: 16px 20px 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}
.analysis-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
@media (max-width: 1100px) {
  .analysis-section { grid-template-columns: 1fr; }
}
.analysis-col .section-head { margin-bottom: 8px; }
.analysis-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.analysis-tip {
  margin: 0 0 10px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.stat-pills,
.eval-dim-row,
.weak-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #475569;
}
.stat-pills span,
.eval-dim-row span {
  background: #f1f5f9;
  border-radius: 999px;
  padding: 4px 10px;
}
.weak-tag {
  background: #fff7ed;
  color: #c2410c;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 600;
}
.name-block { margin-top: 10px; }
.name-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
}
.name-label.warn { color: #b45309; }
.name-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.name-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #334155;
}
.name-chip small { color: #64748b; font-size: 11px; }
.name-chip.warn {
  background: #fff7ed;
  border-color: #fdba74;
  color: #9a3412;
}
.name-chip.warn small { color: #c2410c; }
.matrix-section { margin-bottom: 20px; }
.theme-count { color: #5b4fc7; font-weight: 700; font-size: 13px; }
.theme-list { display: grid; gap: 10px; }
.theme-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 12px;
  background: #f8fafc;
}
.theme-head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 4px; }
.theme-head span { color: #5b4fc7; font-weight: 700; }
.theme-card p { margin: 0; color: #334155; line-height: 1.6; }
.theme-missing { margin: 10px 0 0; color: #64748b; font-size: 13px; }
.vote-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0 0 14px;
  padding: 12px 14px;
  background: #faf9ff;
  border: 1px solid #e9e5ff;
  border-radius: 12px;
}
.vote-bar-row {
  display: grid;
  grid-template-columns: minmax(120px, 1.4fr) 2fr 64px;
  gap: 10px;
  align-items: center;
}
.vote-bar-label {
  font-size: 13px;
  color: #312e81;
  font-weight: 600;
  line-height: 1.4;
}
.vote-bar-track {
  height: 12px;
  border-radius: 999px;
  background: #ede9fe;
  overflow: hidden;
}
.vote-bar-track i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  border-radius: 999px;
}
.vote-bar-count {
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: #4338ca;
}
@media (max-width: 720px) {
  .vote-bar-row { grid-template-columns: 1fr; gap: 4px; }
  .vote-bar-count { text-align: left; }
}
.tier-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin: 0 0 12px;
  font-size: 13px;
  color: #475569;
}
.suggest-reason {
  margin-left: 6px;
  color: #64748b;
  font-size: 12px;
}
.park-actions { display: flex; gap: 8px; }
.park-sub { margin: 14px 0 8px; font-size: 14px; color: #334155; }
.park-lesson { margin-left: 6px; color: #5b4fc7; font-weight: 700; }
.park-tip {
  margin: -4px 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 14px;
}
.section-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}
.view-toggle {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
}
.view-toggle button {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}
.view-toggle button.active {
  background: #fff;
  color: #5b4fc7;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.activity-list { display: flex; flex-direction: column; gap: 10px; }
.activity-row {
  display: grid;
  grid-template-columns: 36px 1fr 180px 52px auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}
@media (max-width: 900px) {
  .activity-row { grid-template-columns: 1fr; }
}
.act-index {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: #ede9fe;
  color: #5b4fc7;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.act-title { font-weight: 600; font-size: 14px; color: #334155; display: flex; align-items: center; gap: 6px; }
.act-meta { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.act-tiers, .grid-tiers {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.5;
}
.act-rate { font-weight: 700; color: #5b4fc7; font-size: 14px; text-align: right; }
.act-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.act-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: #5b4fc7;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
.act-btn.outline { background: #fff; color: #5b4fc7; border: 1px solid #c4b5fd; }
.act-btn.sm { padding: 4px 8px; font-size: 11px; }
.act-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.grid-card {
  padding: 14px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}
.grid-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.grid-num { font-weight: 700; color: #5b4fc7; }
.grid-title { font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 8px; min-height: 36px; }
.grid-count { font-size: 12px; color: #64748b; margin-bottom: 6px; }
.grid-actions { margin-top: 8px; }

.matrix-wrap { overflow-x: auto; }
.matrix-legend { margin: 12px 0 0; font-size: 13px; }
.matrix-legend .done { color: #67c23a; font-weight: bold; margin-right: 20px; }
.matrix-legend .undone { color: #f56c6c; }
.done { color: #67c23a; font-weight: bold; }
.undone { color: #f56c6c; }
.is-fc { cursor: pointer; text-decoration: underline; }

.empty-page { padding: 48px 20px; }
.empty-hint { text-align: center; color: #94a3b8; font-size: 13px; padding: 12px; grid-column: 1 / -1; }
.fc-zoom-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #475569;
}
.fc-zoom-meta strong { color: #0f172a; }
</style>
