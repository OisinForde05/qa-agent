import { useRef, useState } from 'react'

type RequirementDeleteButtonProps = {
  requirementId: number
  requirementTitle: string
  onDeleted: (requirementId: number) => void
}

function isErrorResponse(data: unknown): data is { message: string } {
  return (
    typeof data === 'object' &&
    data !== null &&
    'message' in data &&
    typeof data.message === 'string'
  )
}

function RequirementDeleteButton({
  requirementId,
  requirementTitle,
  onDeleted,
}: RequirementDeleteButtonProps) {
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')
  const requestInProgress = useRef(false)

  async function handleDelete() {
    if (requestInProgress.current) return

    const confirmed = window.confirm(
      `Delete "${requirementTitle}" and all its saved generations and test cases? This cannot be undone.`,
    )

    if (!confirmed) return

    requestInProgress.current = true
    setDeleting(true)
    setError('')

    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => controller.abort(), 30_000)

    try {
      const response = await fetch(`/api/requirements/${requirementId}`, {
        method: 'DELETE',
        signal: controller.signal,
      })

      if (!response.ok) {
        const details: unknown = await response.json().catch(() => null)

        if (response.status === 404) {
          throw new Error(
            'This requirement no longer exists. Refresh the page to update the list.',
          )
        }

        throw new Error(
          isErrorResponse(details)
            ? details.message
            : `The requirement could not be deleted (HTTP ${response.status}).`,
        )
      }

      if (response.status !== 204) {
        throw new Error(
          'An unexpected response was received. Refresh the page to check whether the requirement was deleted.',
        )
      }

      onDeleted(requirementId)
    } catch (caught) {
      if (controller.signal.aborted) {
        setError(
          'The deletion request timed out. Refresh the page to check whether the requirement was deleted.',
        )
      } else if (caught instanceof TypeError) {
        setError(
          'Unable to confirm deletion. Check the backend connection and refresh the page.',
        )
      } else {
        setError(
          caught instanceof Error
            ? caught.message
            : 'The requirement could not be deleted.',
        )
      }
    } finally {
      window.clearTimeout(timeoutId)
      requestInProgress.current = false
      setDeleting(false)
    }
  }

  return (
    <>
      <button
        type="button"
        className="requirement-delete-button"
        onClick={handleDelete}
        disabled={deleting}
        aria-label={
          deleting
            ? `Deleting requirement: ${requirementTitle}`
            : `Delete requirement: ${requirementTitle}`
        }
        title={deleting ? 'Deleting requirement...' : 'Delete requirement'}
      >
        <span aria-hidden="true">{deleting ? '…' : '×'}</span>
      </button>

      {deleting && (
        <p className="requirement-delete-progress" role="status">
          Deleting requirement...
        </p>
      )}

      {error && (
        <p className="requirement-delete-error" role="alert">
          {error}
        </p>
      )}
    </>
  )
}

export default RequirementDeleteButton