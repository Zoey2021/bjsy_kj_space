/** 与后端 TextbookPdfHelper、migration_textbook_pdf.sql 文件名一致 */
const DEFAULT_PDF_BY_NAME = {
  '三年级上册': '/textbooks/grade3-up.pdf',
  '三年级下册': '/textbooks/grade3-down.pdf',
  '四年级上册': '/textbooks/grade4-up.pdf',
  '四年级下册': '/textbooks/grade4-down.pdf',
  '五年级上册': '/textbooks/grade5-up.pdf',
  '五年级下册': '/textbooks/grade5-down.pdf',
  '六年级上册': '/textbooks/grade6-up.pdf',
  '六年级下册': '/textbooks/grade6-down.pdf'
}

export function resolveTextbookPdfUrl(grade) {
  if (!grade) return ''
  if (grade.pdfUrl) return grade.pdfUrl
  const name = (grade.name || '').trim()
  return DEFAULT_PDF_BY_NAME[name] || ''
}
