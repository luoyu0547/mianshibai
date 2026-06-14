# Resume Editor Polish Design

## Scope

Improve the resume editing and live preview experience for the current Vue 3 frontend. The work covers layout overlap, avatar upload, select-style fields, lightweight description formatting, contact icons, skill tag sizing, and Chinese date picker locale.

## Goals

- Prevent the live preview and AI panel from overlapping in the resume editor.
- Replace avatar URL-only input with a frontend upload button that immediately previews the selected image.
- Make common identity fields easier to fill with editable dropdowns.
- Add lightweight formatting shortcuts for project descriptions without changing the stored data model.
- Improve contact information readability with small inline SVG icons in resume templates.
- Make skill tags more compact and visually balanced.
- Localize Element Plus date picker month names to Chinese.

## Non-Goals

- No backend upload API is added.
- No rich text editor dependency is introduced.
- No resume section schema migration is introduced.
- No unrelated resume template redesign is included.

## Approach

Use the smallest frontend-only changes that preserve existing data shapes. Avatar upload reads image files with `FileReader` and stores the resulting data URL in `formData.avatar`. Project descriptions remain plain strings; toolbar actions insert Markdown-style markers into the textarea.

## Component Changes

### `ResumeEditPage.vue`

- Adjust the right-side panel layout so preview and AI card split available height without covering each other.
- Keep scrolling local to the preview body and AI tab content.
- Preserve existing desktop split layout and current mobile stacking behavior.

### `BasicInfoEditor.vue`

- Replace the avatar URL form item with upload/change/remove buttons.
- Validate selected avatar files on the frontend by MIME type and size.
- Use editable `el-select` for `currentStatus` and `city` so users can choose presets or type custom values.
- Keep `formData.avatar`, `formData.currentStatus`, and `formData.city` as string fields.

### `ProjectEditor.vue`

- Add a small toolbar above `项目描述` with buttons for bold, unordered list, and ordered list.
- Insert text at the current cursor position when possible.
- Keep description content as a plain string.

### `SkillsEditor.vue`

- Reduce tag and input dimensions.
- Improve spacing and vertical alignment inside skill category cards.
- Keep existing add/remove behavior.

### Resume Templates

- Add small inline SVG icons before contact fields in `MinimalTech.vue`, `ModernTwoCol.vue`, and `ClassicFormal.vue`.
- Use current template typography and color systems.
- Do not add external icon assets.

### `main.ts`

- Configure Element Plus with `zh-cn` locale so month picker labels display in Chinese.

## Error Handling

- Invalid avatar file types show an Element Plus warning.
- Oversized avatar images show an Element Plus warning.
- Formatting toolbar actions gracefully append text if cursor selection is unavailable.

## Testing

- Run frontend type checking or build after changes.
- Manually verify the resume editor page for layout, upload preview, dropdown selection, toolbar insertion, template contact icons, skill tag sizing, and Chinese month picker labels.
