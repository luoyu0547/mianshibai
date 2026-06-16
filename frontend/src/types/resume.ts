// src/types/resume.ts

export interface BasicSectionData {
  name: string
  email: string
  phone: string
  targetPosition: string
  city: string
  github: string
  blog: string
  avatar?: string
  currentStatus?: string
  expectedLocation?: string
  expectedSalary?: string
  wechat?: string
  website?: string
}

export interface EducationSectionData {
  school: string
  major: string
  degree: string
  startDate: string
  endDate: string
  gpa: string
  activities: string
  highlights: string[]
}

export interface WorkSectionData {
  company: string
  position: string
  startDate: string
  endDate: string
  description: string
  highlights: string[]
}

export interface ProjectSectionData {
  name: string
  role: string
  techStack: string[]
  startDate: string
  endDate: string
  description: string
  highlights: string[]
}

export interface SkillCategory {
  name: string
  items: string[]
}

export interface SkillsSectionData {
  categories: SkillCategory[]
}

export interface SummarySectionData {
  content: string
}

export type SectionType = 'basic' | 'education' | 'work' | 'project' | 'skills' | 'summary'

export interface ResumeStyleSettings {
  fontSize?: number
  lineHeight?: number
  accentColor?: string
  spacing?: 'compact' | 'normal' | 'relaxed'
}

export interface SectionVO {
  id: number
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sortOrder: number
  aiGenerated: number
  createTime: string
  updateTime: string
}

export interface ResumeVO {
  id: number
  title: string
  templateType: string
  status: string
  source: string
  version: number
  styleSettings?: ResumeStyleSettings
  createTime: string
  updateTime: string
}

export interface ResumeDetailVO {
  id: number
  title: string
  templateType: string
  status: string
  source: string
  version: number
  styleSettings?: ResumeStyleSettings
  createTime: string
  updateTime: string
  sections: SectionVO[]
}

export interface ResumeCreateRequest {
  title: string
  templateType?: string
}

export interface ResumeUpdateRequest {
  title?: string
  templateType?: string
  status?: string
  styleSettings?: ResumeStyleSettings
}

export interface SectionCreateRequest {
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sortOrder?: number
}

export interface SectionUpdateRequest {
  sectionData?: Record<string, unknown>
  sortOrder?: number
}

export interface SectionSortItem {
  sectionId: number
  sortOrder: number
}

export interface SectionSortRequest {
  orders: SectionSortItem[]
}

export interface AiGenerateRequest {
  targetPosition: string
  techDirection?: string
  workYears?: number
}

export interface AiOptimizeRequest {
  sectionId: number
  sectionType: SectionType
  sectionData: Record<string, unknown> | Record<string, unknown>[]
}

export interface AiScoreDimensions {
  completeness: number
  completenessComment: string
  professionalism: number
  professionalismComment: string
  matching: number
  matchingComment: string
}

export interface AiScoreVO {
  score: number
  dimensions: AiScoreDimensions
  suggestions: string[]
}

export interface VersionVO {
  id: number
  version: number
  changeSummary: string
  createTime: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  relatedSectionType?: string
}

export interface ChatMessageVO {
  id: number
  role: string
  content: string
  relatedSectionType?: string
  createTime: string
}

export interface ResumeImportRequest {
  fileName?: string
  fileType?: string
  rawText: string
}

export interface ResumeImportPreviewVO {
  title: string
  templateType: string
  sections: SectionVO[]
  warnings: string[]
}

export interface ResumeWholeOptimizeRequest {
  resumeId: number
  jobId?: number
  targetPosition?: string
  optimizeGoal?: string
}

export interface ResumeWholeOptimizeVO {
  beforeScore: number
  estimatedAfterScore: number
  globalSuggestions: string[]
  optimizedSections: SectionVO[]
}

export interface ResumePatchProposal {
  sectionType: SectionType
  operation: 'replace_section'
  reason?: string
  sectionData: Record<string, unknown>
}
