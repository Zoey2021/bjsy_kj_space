/**
 * 学习空间 · 飞象探究单提交桥接
 * 在飞象页面 submitQuiz 完成后，向父页面（学生端 Vue）发送成绩数据
 */
(function () {
  var MSG = 'LEARN_SPACE_SUBMIT'

  function postToParent(payload) {
    if (window.parent && window.parent !== window) {
      window.parent.postMessage({ type: MSG, payload: payload }, '*')
    }
  }

  function hookInner() {
    var iframe = document.querySelector('.content-iframe')
    if (!iframe) return
    try {
      var win = iframe.contentWindow
      if (!win || win.__learnSpaceHooked) return
      if (typeof win.submitQuiz !== 'function') return

      win.__learnSpaceHooked = true
      var orig = win.submitQuiz
      win.submitQuiz = function () {
        orig.apply(this, arguments)
        var state = win.state || {}
        var quizScore = state.quizScore || 0
        var totalQ = 10
        var correctCount = Math.round(quizScore / 10)
        postToParent({
          source: 'feixiang',
          userAnswers: state.userAnswers || {},
          quizScore: quizScore,
          quizPassed: !!state.quizPassed,
          correctCount: correctCount,
          totalQuestions: totalQ,
          correctRate: totalQ > 0 ? correctCount / totalQ : 0,
          userScore: quizScore,
          totalScore: 100
        })
      }
    } catch (e) {
      console.warn('[learn-space] hook submitQuiz failed', e)
    }
  }

  function scheduleHook() {
    hookInner()
    setTimeout(hookInner, 800)
    setTimeout(hookInner, 2000)
    setTimeout(hookInner, 4000)
  }

  var iframe = document.querySelector('.content-iframe')
  if (iframe) {
    iframe.addEventListener('load', scheduleHook)
    if (document.readyState === 'complete') scheduleHook()
    else window.addEventListener('load', scheduleHook)
  }
})()
