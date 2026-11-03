## AI Test Case Generation

### 9. Google — Gemini API: Generating Content

- **Organisation:** Google.
- **URL:** https://ai.google.dev/api/generate-content
- **Relevant sections:** generateContent; request body; generation configuration; response candidates.
- **Used for:** Calling Gemini from `GeminiClient.java`, sending requirement details and system instructions, and reading generated content and finish reasons.
- **Accessed:** 3 November 2026.

### 10. Google — Gemini API: Structured Outputs

- **Organisation:** Google.
- **URL:** https://ai.google.dev/gemini-api/docs/structured-output
- **Relevant sections:** Structured output examples; JSON Schema support.
- **Used for:** Defining the JSON structure for generated test cases and requirement clarification questions. The backend also validates the returned data before accepting it.
- **Note:** A response matching the schema does not guarantee correct test cases or complete requirements coverage. Human review is still required.
- **Accessed:** 3 November 2026.

## Database Persistence

### 11. Spring Boot — Database Initialization

- **Organisation:** Spring.
- **URL:** https://docs.spring.io/spring-boot/how-to/data-initialization.html
- **Relevant sections:** Initialize a Database Using Hibernate; Execute Flyway Database Migrations on Startup.
- **Used for:** Managing database changes through versioned SQL files in `backend/src/main/resources/db/migration/`. Migrations create the requirement and generation tables and add the test-case version column. Hibernate uses `ddl-auto=validate` to check the entity mappings against the database.
- **Accessed:** 3 November 2026.

### 12. Spring Data JPA — Transactionality

- **Organisation:** Spring.
- **URL:** https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html
- **Relevant sections:** Service-level transaction boundaries; transactional query methods; read-only transactions.
- **Used for:** Applying `@Transactional` in `TestGenerationStore.java` and `TestCaseReviewService.java`. A generation and its related records are saved within one transaction. Saved results are converted into response objects within the transaction.
- **Accessed:** 3 November 2026.

## Test Case Editing and Review

### 13. Spring Framework — Spring MVC Validation

- **Organisation:** Spring.
- **URL:** https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
- **Relevant sections:** Validation of request bodies; MethodArgumentNotValidException; validation error handling.
- **Used for:** Validating `TestCaseUpdateRequest.java` and `TestCaseReviewRequest.java` through `@Valid` in `TestCaseReviewController.java`. Invalid requests receive HTTP 400 responses with validation messages.
- **Accessed:** 3 November 2026.

### 14. Jakarta Persistence — Version

- **Organisation:** Eclipse Foundation, Jakarta Persistence.
- **URL:** https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Version.html
- **Relevant sections:** Version fields; optimistic lock failures.
- **Used for:** Adding `@Version` to `GeneratedTestCase.java` to detect conflicting database updates. The application also compares the version submitted by the frontend with the stored version to reject edits based on outdated data.
- **Note:** Returning HTTP 409 and asking the user to refresh are QA-Agent implementation choices.
- **Accessed:** 3 November 2026.

### 15. React — Updating Arrays in State

- **Organisation:** React.
- **URL:** https://react.dev/learn/updating-arrays-in-state
- **Relevant sections:** Updating arrays without mutation; replacing items in an array; updating objects inside arrays.
- **Used for:** Updating edited test cases in `TestCaseGenerator.tsx` with `map()`, keeping the displayed generation and history consistent. Also supports adding, updating, and removing preconditions and steps in `TestCaseCard.tsx`.
- **Accessed:** 3 November 2026.

### 16. MDN — Using the Fetch API

- **Organisation:** MDN Web Docs.
- **URL:** https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
- **Relevant sections:** Making a request; setting the method and body; checking response status; reading JSON; cancelling a request.
- **Used for:** Sending frontend requests for requirements, generation, saved history, editing, and review. Supports checking `response.ok`, handling JSON responses, and using `AbortController` for request cancellation.
- **Note:** HTTP error responses do not automatically reject the fetch promise, so the application checks the response status explicitly.
- **Accessed:** 3 November 2026.

## Project-Specific Review Rules

The following are QA-Agent design decisions rather than requirements
prescribed by the documentation above:

- Newly generated test cases start with the status `DRAFT`.
- Users can approve or reject individual saved test cases.
- Saving an edit returns the case to `DRAFT` so the changed content
  requires another review.
- Generating again creates a separate generation and preserves
  earlier generations.
- Each generation records the model name and creation time.
- Requests based on an outdated test-case version are rejected
  instead of silently overwriting newer changes.