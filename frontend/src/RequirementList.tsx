import { useEffect, useState } from 'react'
import TestCaseGenerator from './TestCaseGenerator'

type Requirement = {
  id: number
  title: string
  description: string
  acceptanceCriteria: string
}

type RequirementListProps = {
  refreshKey: number
}

function isRequirement(data: unknown): data is Requirement {
  return (
    typeof data === 'object' &&
    data !== null &&
    'id' in data &&
    typeof data.id === 'number' &&
    'title' in data &&
    typeof data.title === 'string' &&
    'description' in data &&
    typeof data.description === 'string' &&
    'acceptanceCriteria' in data &&
    typeof data.acceptanceCriteria === 'string'
  )
}

function RequirementList({ refreshKey }: RequirementListProps) {
  const [requirements, setRequirements] = useState<Requirement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadRequirements() {
      setLoading(true)
      setError('')

      try {
        const response = await fetch('/api/requirements', {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error('Requirements request failed')
        }

        const data: unknown = await response.json()

        if (!Array.isArray(data) || !data.every(isRequirement)) {
          throw new Error('Unexpected requirements response')
        }

        if (!controller.signal.aborted) {
          setRequirements(data)
        }
      } catch {
        if (!controller.signal.aborted) {
          setError(
            'Unable to load saved requirements. Check the backend and refresh the page.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    loadRequirements()

    return () => controller.abort()
  }, [refreshKey])

  return (
    <section
      className="requirements-list"
      aria-labelledby="requirements-heading"
      aria-busy={loading}
    >
      <h2 id="requirements-heading">Saved requirements</h2>

      {loading && <p role="status">Loading requirements...</p>}

      {error && <p role="alert">{error}</p>}

      {!loading && !error && requirements.length === 0 && (
        <p>No requirements have been saved yet.</p>
      )}

      {!loading && !error && requirements.map((requirement) => (
        <article className="requirement-card" key={requirement.id}>
          <details className="requirement-details">
            <summary className="requirement-summary">
              <h3>{requirement.title}</h3>
              <span className="requirement-id">
                Requirement #{requirement.id}
              </span>
            </summary>

            <div className="requirement-details-content">
              <h4>Description</h4>
              <p className="requirement-text">
                {requirement.description}
              </p>

              <h4>Acceptance criteria</h4>
              <p className="requirement-text">
                {requirement.acceptanceCriteria}
              </p>

              <TestCaseGenerator
                key={requirement.id}
                requirementId={requirement.id}
              />
            </div>
          </details>
        </article>
      ))}
    </section>
  )
}

export default RequirementList
