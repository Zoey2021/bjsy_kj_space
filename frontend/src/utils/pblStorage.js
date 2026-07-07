import { PBL_STORAGE_KEY, createEmptyPblProject } from '../constants/pblModule'

export function loadPblProject() {
  try {
    const raw = localStorage.getItem(PBL_STORAGE_KEY)
    if (!raw) return createEmptyPblProject()
    const parsed = JSON.parse(raw)
    return { ...createEmptyPblProject(), ...parsed }
  } catch {
    return createEmptyPblProject()
  }
}

export function savePblProject(project) {
  const payload = { ...project, updatedAt: new Date().toISOString() }
  localStorage.setItem(PBL_STORAGE_KEY, JSON.stringify(payload))
  return payload
}
