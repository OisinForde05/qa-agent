import { useState } from 'react'
import type { FormEvent } from 'react'

type Requirement = {
  id: number
  title: string
  description: string
  acceptanceCriteria: string
}

function isErrorResponse(data: unknown): data is { message: string } {
  return (
    typeof data === 'object' &&
    data !== null &&
    'message' in data &&
    typeof data.message === 'string'
  )
}

function RequirementForm() {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [acceptanceCriteria, setAcceptanceCriteria] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [submitted, setSubmitted] = useState<Requirement | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (submitting) return

    setError('')
    setSubmitted(null)

    const requirement: Omit<Requirement, 'id'> = {
  title: title.trim(),
  description: description.trim(),
  acceptanceCriteria: acceptanceCriteria.trim(),
}

    if (
      !requirement.title ||
      !requirement.description ||
      !requirement.acceptanceCriteria
    ) {
      setError('Complete all fields. Spaces alone are not accepted.')
      return
    }

    setSubmitting(true)

    try {
      const response = await fetch('/api/requirements', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requirement),
      })

      if (!response.ok) {
        const details: unknown =
          await response.json().catch(() => null)

        if (response.status >= 500) {
          setError('Backend unavailable. Check the backend and try again.')
        } else if (isErrorResponse(details)) {
          setError(details.message)
        } else {
          setError(`The requirement could not be accepted (HTTP ${response.status}).`)
        }
        return
      }

      const accepted: Requirement = await response.json()
      setSubmitted(accepted)
    } catch {
      setError('The server could not process the requirement. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="requirement-panel">
      <h2>Submit a requirement</h2>
      <p>Enter a requirement to validate and save it.</p>

      <form onSubmit={handleSubmit}>
        <fieldset disabled={submitting}>
          <legend>Requirement details</legend>

          <label htmlFor="requirement-title">Title</label>
          <input
            id="requirement-title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            required
          />

          <label htmlFor="requirement-description">Description</label>
          <textarea
            id="requirement-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={4}
            required
          />

          <label htmlFor="requirement-criteria">Acceptance criteria</label>
          <textarea
            id="requirement-criteria"
            value={acceptanceCriteria}
            onChange={(event) => setAcceptanceCriteria(event.target.value)}
            rows={5}
            required
          />

          <button type="submit">
            {submitting ? 'Submitting...' : 'Submit requirement'}
          </button>
        </fieldset>
      </form>

      {error && <p role="alert">{error}</p>}

      {submitted && (
        <section className="requirement-preview">
          <h3>Saved requirement</h3>
          <p role="status">
          Requirement #{submitted.id} was saved successfully.
          </p>

          <h4>{submitted.title}</h4>
          <p>{submitted.description}</p>

          <h4>Acceptance criteria</h4>
          <p>{submitted.acceptanceCriteria}</p>
        </section>
      )}
    </section>
  )
}

export default RequirementForm