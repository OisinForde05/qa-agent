import { useEffect, useState } from 'react'
import TestCaseCard from './TestCaseCard'
import { isSavedTestCase } from './testCaseTypes'
import type { SavedTestCase } from './testCaseTypes'

type GenerationResult = {
  id: number
  requirementId: number
  createdAt: string
  modelName: string
  testCases: SavedTestCase[]
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

function isGenerationResult(value: unknown): value is GenerationResult {
  return (
    isObject(value) &&
    typeof value.id === 'number' &&
    typeof value.requirementId === 'number' &&
    typeof value.createdAt === 'string' &&
    typeof value.modelName === 'string' &&
    Array.isArray(value.testCases) &&
    value.testCases.every(isSavedTestCase) &&
    isStringArray(value.requirementIssues) &&
    (value.testCases.length > 0 || value.requirementIssues.length > 0)
  )
}

function TestCaseGenerator({ requirementId }: Props) {
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<GenerationResult | null>(null)
  const [history, setHistory] = useState<GenerationResult[]>([])
  const [loadingHistory, setLoadingHistory] = useState(true)

  useEffect(() => {
    const controller = new AbortController()

    async function loadHistory() {
      setLoadingHistory(true)
      setError('')
      setResult(null)
      setHistory([])

      try {
        const response = await fetch(
          `/api/requirements/${requirementId}/test-generations`,
          { signal: controller.signal },
        )

        const data: unknown = await response.json().catch(() => null)

        if (!response.ok) {
          const message =
            isObject(data) && typeof data.message === 'string'
              ? data.message
              : 'Could not load saved generations. Refresh to try again.'

          throw new Error(message)
        }

        if (!Array.isArray(data) || !data.every(isGenerationResult)) {
          throw new Error('The backend returned unexpected generation history.')
        }

        const generations: GenerationResult[] = data

        if (generations.some((item) => item.requirementId !== requirementId)) {
          throw new Error('The generation history belongs to another requirement.')
        }

        if (!controller.signal.aborted) {
          setHistory(generations)
          setResult(generations[0] ?? null)
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setError(
            error instanceof Error
              ? error.message
              : 'Could not load saved generations.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoadingHistory(false)
        }
      }
    }

    void loadHistory()

    return () => controller.abort()
  }, [requirementId])

  function handleCaseSaved(updatedCase: SavedTestCase) {
    function updateGeneration(generation: GenerationResult): GenerationResult {
      return {
        ...generation,
        testCases: generation.testCases.map((testCase) =>
          testCase.id === updatedCase.id ? updatedCase : testCase,
        ),
      }
    }

    setHistory((previous) => previous.map(updateGeneration))
    setResult((previous) => (previous === null ? null : updateGeneration(previous)))
  }

  async function handleGenerate() {
    if (generating || loadingHistory) return

    setGenerating(true)
    setError('')

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

      if (!isGenerationResult(data) || data.requirementId !== requirementId) {
        setError('The backend returned an unexpected response.')
        return
      }

      setHistory((previous) => [data, ...previous])
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
        New test cases are saved as drafts. Review and edit each case,
        then approve or reject it. Generating again keeps earlier generations.
      </p>

      <button
        type="button"
        onClick={handleGenerate}
        disabled={generating || loadingHistory}
      >
        {generating ? 'Generating...' : 'Generate test cases'}
      </button>

      {loadingHistory && <p role="status">Loading saved generations...</p>}

      {history.length > 0 && (
        <div className="generation-history">
          <label htmlFor={`generation-history-${requirementId}`}>
            Saved generation
          </label>

          <select
            id={`generation-history-${requirementId}`}
            value={result?.id ?? ''}
            disabled={generating || loadingHistory}
            onChange={(event) => {
              const selectedId = Number(event.target.value)
              const selected = history.find((item) => item.id === selectedId)
              setResult(selected ?? null)
            }}
          >
            {history.map((generation) => (
              <option key={generation.id} value={generation.id}>
                #{generation.id} — {new Date(generation.createdAt).toLocaleString()}
              </option>
            ))}
          </select>
        </div>
      )}

      {generating && (
        <p role="status">
          Generating draft test cases. This may take a couple of minutes.
        </p>
      )}

      {error && <p className="generation-error" role="alert">{error}</p>}

      {result && (
        <div className="generation-results">
          <p role="status">
            Showing {result.testCases.length} saved test cases from generation #{result.id}.
          </p>

          <p>
            Generated using {result.modelName} on{' '}
            {new Date(result.createdAt).toLocaleString()}.
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

          {result.testCases.map((testCase) => (
            <TestCaseCard
              key={testCase.id}
              testCase={testCase}
              onSaved={handleCaseSaved}
            />
          ))}
        </div>
      )}
    </section>
  )
}

export default TestCaseGenerator
