export type TestCaseStatus = 'DRAFT' | 'APPROVED' | 'REJECTED'

export type SavedTestCase = {
  id: number
  version: number
  title: string
  preconditions: string[]
  steps: string[]
  expectedResult: string
  status: TestCaseStatus
}

function isStringArray(value: unknown): value is string[] {
  return (
    Array.isArray(value) &&
    value.every((item: unknown) => typeof item === 'string')
  )
}

export function isSavedTestCase(value: unknown): value is SavedTestCase {
  if (typeof value !== 'object' || value === null) return false

  const data = value as Record<string, unknown>

  return (
    typeof data.id === 'number' &&
    typeof data.version === 'number' &&
    Number.isInteger(data.version) &&
    data.version >= 0 &&
    typeof data.title === 'string' &&
    isStringArray(data.preconditions) &&
    isStringArray(data.steps) &&
    typeof data.expectedResult === 'string' &&
    (
      data.status === 'DRAFT' ||
      data.status === 'APPROVED' ||
      data.status === 'REJECTED'
    )
  )
}
