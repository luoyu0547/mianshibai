// src/types/job.ts

export interface CompanyCertificationVO {
  id: number
  certificationType: string
  status: string
  evidenceSource: string
  evidenceUrl: string
  evidenceText: string
  confidenceScore: number
}

export interface CompanyVO {
  id: number
  name: string
  normalizedName: string
  website: string
  industry: string
  city: string
  scale: string
  description: string
  mainBusiness: string
  techDirection: string
  isSpecializedNew: boolean
  isLittleGiant: boolean
  certificationConfidence: string
  certifications: CompanyCertificationVO[]
}

export interface JobAnalysisVO {
  id: number
  requirementSummary: string
  coreSkills: string
  hiddenRequirements: string
  interviewFocus: string
  resumeSuggestions: string
  riskPoints: string
}

export interface JobMatchVO {
  id: number
  matchScore: number
  growthScore: number
  techGrowthScore: number
  salaryCityScore: number
  experienceFitScore: number
  totalScore: number
  recommendation: string
  reason: string
  gaps: string
}

export interface JobVO {
  id: number
  company: CompanyVO | null
  companyName: string
  title: string
  sourcePlatform: string
  sourceUrl: string
  city: string
  salaryRange: string
  experienceRequirement: string
  educationRequirement: string
  jobDescription: string
  jobRequirement: string
  techStack: string
  status: string
  analysis: JobAnalysisVO | null
  matchResult: JobMatchVO | null
  favorited: boolean
}

export interface JobImportRequest {
  url: string
  importType: 'job' | 'company_website' | 'company_career_page'
}

export interface JobImportResultVO {
  resultType: string
  jobId: number | null
  companyId: number | null
  job: JobVO | null
  company: CompanyVO | null
}

export interface JobMatchRequest {
  resumeId: number
}
