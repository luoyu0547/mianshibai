# Resume AI Chat Tools Design

## Goal

The resume editor AI chat should be able to propose concrete resume edits, while never applying them without user approval. Chat responses should stream token by token, and any proposed edit should be shown as a compare-and-confirm workflow before it updates the editor form.

## Current Context

- Backend already exposes `POST /api/resume/{resumeId}/chat` as `text/event-stream` using `SseEmitter` and `ResumeAiService.chatStream`.
- Backend uses Spring AI `ChatClient` with DeepSeek (`spring-ai.version=1.1.2`). Spring AI supports tool calling through `ChatClient.tools(...)`, `@Tool`, and `FunctionToolCallback`.
- Frontend `AiChatPanel.vue` already reads `ReadableStream`, but it expects JSON payloads while the backend sends plain SSE data, so streaming can appear non-streaming or inconsistent.
- Frontend already has section application paths in `ResumeEditPage.vue`: `handleOptimizeApplied`, `handleWholeOptimizeApplied`, and `handleExtracted` update local editor state before existing save/autosave persists it.
- Existing `handleExtracted` directly applies extracted data without a compare-confirm step and is not backed by a reliable framework tool call.

## Architecture

Use Spring AI tool calling for edit proposals. Do not invent a custom tool-call syntax in model text.

The backend registers a resume edit proposal tool on the chat request:

```java
chatClient.prompt()
        .system(systemPrompt)
        .user(message)
        .tools(new ResumePatchTools(proposalCollector))
        .stream()
        .content();
```

The tool is a safe proposal tool, not an executor:

```java
public class ResumePatchTools {

    @Tool(description = "Propose a resume section edit for user review. This tool must not save or apply changes.")
    public String proposeResumePatch(ResumePatchRequest request) {
        proposals.add(toProposal(request));
        return "A resume edit proposal has been prepared and is waiting for user confirmation.";
    }
}
```

The model can call `proposeResumePatch`, Spring AI handles the tool-call loop, and the application collects resulting proposals. The tool never writes database rows and never mutates the resume.

## Tool Contract

Initial scope supports one operation:

- `proposeResumePatch`
- `operation`: `replace_section`
- `sectionType`: `basic | education | work | project | skills | summary`
- `sectionData`: the full replacement payload for the target section
- `reason`: a short explanation shown in the confirmation UI

This matches the current editor model, where modules are edited and saved as whole section payloads. It avoids introducing fragile JSON Patch merging rules.

## Backend Flow

1. Authenticate the user and verify resume ownership as today.
2. Load current resume sections and include them in the system prompt.
3. Register `ResumePatchTools` for the current request.
4. Stream normal assistant text through SSE as it arrives.
5. Collect any `ResumePatchProposal` emitted by the tool.
6. Send a structured SSE event for each proposal, for example `event: resume_patch_proposal` with JSON data.
7. Save the assistant chat message. The saved visible content should not include internal tool metadata.

If the model does not call the tool, the chat remains a normal streamed answer.

## Frontend Flow

1. `AiChatPanel.vue` parses standard SSE frames, including named events.
2. `message` events append assistant text immediately for streaming display.
3. `resume_patch_proposal` events attach proposals to the current assistant message.
4. The assistant bubble shows a compact action strip: section name, reason, `查看对比`, `忽略`.
5. Clicking `查看对比` emits the proposal to `ResumeEditPage.vue`.
6. `ResumeEditPage.vue` opens a compare dialog showing current local editor state versus proposed `sectionData`.
7. Clicking `应用` updates the local form state only. Existing autosave/save logic persists it.
8. Clicking `忽略` marks the proposal ignored and performs no mutation.

## Confirmation UI

The confirmation UI should reuse the existing compare visual language from `AiOptimizeDialog.vue` where practical:

- left side: current section content from local editor state
- right side: proposed section content from the tool call
- changed rows highlighted
- footer actions: `取消` and `应用到简历`

This avoids a new visual pattern and keeps the interaction consistent with existing AI optimization.

## Validation And Safety

Backend validation:

- Reject unknown `sectionType` values.
- Reject unsupported operations.
- Normalize object/array shapes to match section type.
- Do not call `ResumeService.updateSection` from the tool.

Frontend validation:

- Ignore unknown tool event payloads.
- Only allow `replace_section`.
- Only update the section selected by `sectionType`.
- Never apply a proposal until the user clicks `应用到简历`.

## Streaming Fix

The frontend should parse SSE frames according to the actual backend format instead of assuming every `data:` line contains JSON with `content`.

Expected behavior:

- Plain `data:` from the default message event is appended directly.
- Named event `resume_patch_proposal` is parsed as JSON and not rendered as assistant text.
- `[DONE]` is optional and should not be required for UI completion.

The backend should send explicit named events for non-text payloads so the frontend does not need to infer action data from assistant text.

## Error Handling

- If tool execution fails, continue streaming a normal answer when possible and log the tool error.
- If proposal parsing fails on the frontend, show the assistant text and skip actions.
- If applying a proposal throws, leave editor state unchanged and show an error toast.
- If the user navigates away before applying, no pending proposal is persisted.

## Testing

Backend tests:

- Tool request validation accepts valid section replacements and rejects invalid section types or operations.
- `chatStream` can stream text without proposals.
- `chatStream` can emit proposal events without mutating resume sections.

Frontend tests:

- SSE parser appends plain text chunks incrementally.
- SSE parser handles named `resume_patch_proposal` events.
- Proposal action opens compare dialog.
- Applying a proposal updates only the target section local state.
- Ignoring a proposal performs no mutation.

## Non-Goals

- No direct database writes from AI tools.
- No multi-step autonomous edits across multiple sections in the first iteration.
- No generic JSON Patch engine in the first iteration.
- No custom hidden tool-call markup in assistant text.
