import { useState } from 'react'

type DraftTestCase = {
  title: string
  preconditions: string[]
  steps: string[]
  expectedResult: string
}

type GenerationResult = {
  requirementId: number
  status: 'DRAFT'
  testCases: DraftTestCase[]
  requirementIssues: string[]
}

type Props = {
  requirementId: number
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isStringArray(value: unknown): value is string[] {
  return (
    Array.isArray(value) &&
    value.every((item: unknown) => typeof item === 'string')
  )
}

function isDraftTestCase(value: unknown): value is DraftTestCase {
  return (
    isObject(value) &&
    typeof value.title === 'string' &&
    isStringArray(value.preconditions) &&
    isStringArray(value.steps) &&
    typeof value.expectedResult === 'string'
  )
}

function isGenerationResult(value: unknown): value is GenerationResult {
  return (
    isObject(value) &&
    typeof value.requirementId === 'number' &&
    value.status === 'DRAFT' &&
    Array.isArray(value.testCases) &&
    value.testCases.every(isDraftTestCase) &&
    isStringArray(value.requirementIssues) &&
    (value.testCases.length > 0 || value.requirementIssues.length > 0)
  )
}

function TestCaseGenerator({ requirementId }: Props) {
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<GenerationResult | null>(null)

  async function handleGenerate() {
    if (generating) return

    setGenerating(true)
    setError('')
    setResult(null)

    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 180000)

    try {
      const response = await fetch(
        `/api/requirements/${requirementId}/test-cases/generate`,
        {
          method: 'POST',
          signal: controller.signal,
        },
      )

      const data: unknown = await response.json().catch(() => null)

      if (!response.ok) {
        if (isObject(data) && typeof data.message === 'string') {
          setError(data.message)
        } else if (response.status === 504) {
          setError('Generation timed out. Please try again later.')
        } else if (response.status === 503) {
          setError('Test generation is temporarily unavailable.')
        } else if (response.status >= 500) {
          setError('Generation failed. Check the backend and try again.')
        } else {
          setError(`Could not generate test cases (HTTP ${response.status}).`)
        }
        return
      }

      if (
        !isGenerationResult(data) ||
        data.requirementId !== requirementId
      ) {
        setError('The backend returned an unexpected response.')
        return
      }

      setResult(data)
    } catch {
      if (controller.signal.aborted) {
        setError('Generation took too long. Please try again later.')
      } else {
        setError('Unable to reach the backend. Check that it is running.')
      }
    } finally {
      window.clearTimeout(timeout)
      setGenerating(false)
    }
  }

  return (
    <section className="test-generation">
      <h4>AI-generated test cases</h4>

      <p>
        Generated cases are drafts for human review. They are not saved
        and will be lost when you refresh.
      </p>

      <button
        type="button"
        onClick={handleGenerate}
        disabled={generating}
      >
        {generating ? 'Generating...' : 'Generate test cases'}
      </button>

      {generating && (
        <p role="status">
          Generating draft test cases. This may take a couple of minutes.
        </p>
      )}

      {error && <p className="generation-error" role="alert">{error}</p>}

      {result && (
        <div className="generation-results">
          <p role="status">
            {result.testCases.length} draft test cases generated.
          </p>

          {result.requirementIssues.length > 0 && (
            <div className="requirement-issues">
              <h5>Questions to review</h5>
              <p>
                These are suggested clarifications. Decide whether each
                one is relevant to your requirement.
              </p>

              <ul>
                {result.requirementIssues.map((issue, index) => (
                  <li key={index}>{issue}</li>
                ))}
              </ul>
            </div>
          )}

          {result.testCases.length === 0 && (
            <p>
              No reliable test cases were generated. Review the questions
              above before trying again.
            </p>
          )}

          {result.testCases.map((testCase, index) => (
            <article className="draft-test-case" key={index}>
              <h5>
                {index + 1}. {testCase.title}
              </h5>

              <h6>Preconditions</h6>
              {testCase.preconditions.length > 0 ? (
                <ul>
                  {testCase.preconditions.map((condition, conditionIndex) => (
                    <li key={conditionIndex}>{condition}</li>
                  ))}
                </ul>
              ) : (
                <p>None specified.</p>
              )}

              <h6>Steps</h6>
              <ol>
                {testCase.steps.map((step, stepIndex) => (
                  <li key={stepIndex}>{step}</li>
                ))}
              </ol>

              <h6>Expected result</h6>
              <p>{testCase.expectedResult}</p>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export default TestCaseGenerator
