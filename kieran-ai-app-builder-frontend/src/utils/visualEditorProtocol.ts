export const VISUAL_EDITOR_PROTOCOL = {
  runtimeScriptId: 'kieran-visual-editor-runtime',
  runtimeStyleId: 'kieran-visual-editor-style',
  hoveredAttr: 'data-kieran-visual-editor-hovered',
  selectedAttr: 'data-kieran-visual-editor-selected',
  messageType: {
    select: 'KIERAN_VISUAL_EDITOR_SELECT',
    clear: 'KIERAN_VISUAL_EDITOR_CLEAR',
    disable: 'KIERAN_VISUAL_EDITOR_DISABLE',
  } as const,
} as const

export interface VisualEditorRect {
  x: number
  y: number
  width: number
  height: number
}

export interface VisualEditorElementInfo {
  tagName: string
  selector: string
  id?: string
  className?: string
  text?: string
  attributes: Record<string, string>
  rect: VisualEditorRect
}

export interface VisualEditorSelectMessage {
  type: typeof VISUAL_EDITOR_PROTOCOL.messageType.select
  sessionId: string
  element: VisualEditorElementInfo
}

export interface VisualEditorClearMessage {
  type: typeof VISUAL_EDITOR_PROTOCOL.messageType.clear
  sessionId: string
}

export interface VisualEditorDisableMessage {
  type: typeof VISUAL_EDITOR_PROTOCOL.messageType.disable
  sessionId: string
}

export type VisualEditorParentMessage = VisualEditorClearMessage | VisualEditorDisableMessage
export type VisualEditorFrameMessage = VisualEditorSelectMessage
