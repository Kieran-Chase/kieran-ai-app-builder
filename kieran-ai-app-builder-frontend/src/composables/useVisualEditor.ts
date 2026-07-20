import { onMounted, onUnmounted, ref, type Ref } from 'vue'
import {
  VISUAL_EDITOR_PROTOCOL,
  type VisualEditorElementInfo,
  type VisualEditorFrameMessage,
  type VisualEditorParentMessage,
} from '@/utils/visualEditorProtocol'
import { serializeElement } from '@/utils/elementSerializer'

const BIND_RETRY_DELAY = 100
const BIND_RETRY_LIMIT = 20

const createSessionId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

const resolveFrameContext = (iframeRef: Ref<HTMLIFrameElement | null>) => {
  const iframe = iframeRef.value
  if (!iframe) {
    return null
  }

  try {
    const doc = iframe.contentDocument
    const win = iframe.contentWindow
    if (!doc || !win) {
      return null
    }
    return { iframe, doc, win }
  } catch {
    return null
  }
}

const getElementFromTarget = (target: EventTarget | null) => {
  if (!target) {
    return null
  }

  const node = target as Node
  if (node.nodeType === Node.ELEMENT_NODE) {
    return node as Element
  }
  if (node.nodeType === Node.TEXT_NODE) {
    return node.parentElement
  }
  return null
}

const injectRuntimeStyle = (doc: Document) => {
  if (doc.getElementById(VISUAL_EDITOR_PROTOCOL.runtimeStyleId)) {
    return
  }

  const style = doc.createElement('style')
  style.id = VISUAL_EDITOR_PROTOCOL.runtimeStyleId
  style.textContent = `
    [${VISUAL_EDITOR_PROTOCOL.hoveredAttr}='true'] {
      outline: 2px dashed #1677ff !important;
      outline-offset: 2px !important;
      cursor: crosshair !important;
    }

    [${VISUAL_EDITOR_PROTOCOL.selectedAttr}='true'] {
      outline: 2px solid #0958d9 !important;
      outline-offset: 2px !important;
      box-shadow: 0 0 0 1px rgba(9, 88, 217, 0.45) !important;
      cursor: crosshair !important;
    }
  `
  doc.head.appendChild(style)
}

const useVisualEditor = (iframeRef: Ref<HTMLIFrameElement | null>) => {
  const visualEditEnabled = ref(false)
  const selectedElement = ref<VisualEditorElementInfo | null>(null)

  let sessionId = ''
  let cleanupFrameRuntime: (() => void) | null = null
  let bindRetryTimer: number | null = null
  let bindRetryCount = 0

  const clearBindRetry = () => {
    if (bindRetryTimer !== null) {
      window.clearTimeout(bindRetryTimer)
      bindRetryTimer = null
    }
    bindRetryCount = 0
  }

  const postToFrame = (message: VisualEditorParentMessage) => {
    const frameContext = resolveFrameContext(iframeRef)
    const frameWindow = frameContext?.win
    if (!frameWindow) {
      return
    }

    try {
      frameWindow.postMessage(message, window.location.origin)
    } catch {
      // ignore postMessage failure when the iframe is not ready yet
    }
  }

  const clearSelection = (notifyFrame = true) => {
    selectedElement.value = null
    if (notifyFrame) {
      postToFrame({
        type: VISUAL_EDITOR_PROTOCOL.messageType.clear,
        sessionId,
      })
    }
  }

  const cleanupCurrentFrame = () => {
    if (cleanupFrameRuntime) {
      cleanupFrameRuntime()
      cleanupFrameRuntime = null
    }
  }

  const bindFrameRuntime = () => {
    const frameContext = resolveFrameContext(iframeRef)
    if (!frameContext || !visualEditEnabled.value) {
      return false
    }

    const { doc, win } = frameContext
    if (!doc.body || !doc.head) {
      return false
    }

    injectRuntimeStyle(doc)

    let hoveredElement: Element | null = null
    let selectedDomElement: Element | null = null

    const removeHover = () => {
      if (hoveredElement) {
        hoveredElement.removeAttribute(VISUAL_EDITOR_PROTOCOL.hoveredAttr)
        hoveredElement = null
      }
    }

    const removeSelection = () => {
      if (selectedDomElement) {
        selectedDomElement.removeAttribute(VISUAL_EDITOR_PROTOCOL.selectedAttr)
        selectedDomElement = null
      }
    }

    const setHover = (element: Element | null) => {
      if (!element || element === selectedDomElement) {
        removeHover()
        return
      }

      if (hoveredElement && hoveredElement !== element) {
        hoveredElement.removeAttribute(VISUAL_EDITOR_PROTOCOL.hoveredAttr)
      }

      hoveredElement = element
      hoveredElement.setAttribute(VISUAL_EDITOR_PROTOCOL.hoveredAttr, 'true')
    }

    const onMouseOver = (event: MouseEvent) => {
      if (!visualEditEnabled.value) {
        return
      }
      const element = getElementFromTarget(event.target)
      if (!element || element === doc.body || element === doc.documentElement) {
        removeHover()
        return
      }
      setHover(element)
    }

    const onMouseOut = (event: MouseEvent) => {
      if (!visualEditEnabled.value) {
        return
      }
      const relatedTarget = getElementFromTarget(event.relatedTarget)
      if (relatedTarget && hoveredElement && hoveredElement.contains(relatedTarget)) {
        return
      }
      if (hoveredElement && (!relatedTarget || hoveredElement !== relatedTarget)) {
        removeHover()
      }
    }

    const onClick = (event: MouseEvent) => {
      if (!visualEditEnabled.value) {
        return
      }
      const element = getElementFromTarget(event.target)
      if (!element || element === doc.body || element === doc.documentElement) {
        return
      }

      event.preventDefault()
      event.stopPropagation()

      removeHover()
      removeSelection()
      selectedDomElement = element
      selectedDomElement.setAttribute(VISUAL_EDITOR_PROTOCOL.selectedAttr, 'true')

      const info = serializeElement(element)
      selectedElement.value = info
      win.parent.postMessage(
        {
          type: VISUAL_EDITOR_PROTOCOL.messageType.select,
          sessionId,
          element: info,
        },
        window.location.origin,
      )
    }

    const onParentMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin || !event.data || typeof event.data !== 'object') {
        return
      }

      const data = event.data as VisualEditorParentMessage & { sessionId?: string }
      if (data.sessionId !== sessionId) {
        return
      }

      if (
        data.type === VISUAL_EDITOR_PROTOCOL.messageType.clear ||
        data.type === VISUAL_EDITOR_PROTOCOL.messageType.disable
      ) {
        removeHover()
        removeSelection()
      }
    }

    cleanupCurrentFrame()
    doc.addEventListener('mouseover', onMouseOver, true)
    doc.addEventListener('mouseout', onMouseOut, true)
    doc.addEventListener('click', onClick, true)
    win.addEventListener('message', onParentMessage)

    cleanupFrameRuntime = () => {
      doc.removeEventListener('mouseover', onMouseOver, true)
      doc.removeEventListener('mouseout', onMouseOut, true)
      doc.removeEventListener('click', onClick, true)
      win.removeEventListener('message', onParentMessage)
      removeHover()
      removeSelection()
    }

    return true
  }

  const scheduleBindFrameRuntime = () => {
    clearBindRetry()

    const tryBind = () => {
      if (!visualEditEnabled.value) {
        clearBindRetry()
        return
      }

      const bound = bindFrameRuntime()
      if (bound) {
        clearBindRetry()
        return
      }

      bindRetryCount += 1
      if (bindRetryCount >= BIND_RETRY_LIMIT) {
        clearBindRetry()
        return
      }

      bindRetryTimer = window.setTimeout(tryBind, BIND_RETRY_DELAY)
    }

    tryBind()
  }

  const enterEditMode = () => {
    sessionId = createSessionId()
    visualEditEnabled.value = true
    clearSelection(false)
    scheduleBindFrameRuntime()
  }

  const exitEditMode = () => {
    clearBindRetry()

    if (!visualEditEnabled.value) {
      cleanupCurrentFrame()
      clearSelection(false)
      sessionId = ''
      return
    }

    visualEditEnabled.value = false
    clearSelection()
    postToFrame({
      type: VISUAL_EDITOR_PROTOCOL.messageType.disable,
      sessionId,
    })
    cleanupCurrentFrame()
    sessionId = ''
  }

  const handlePreviewLoad = () => {
    cleanupCurrentFrame()
    clearSelection(false)
    if (!visualEditEnabled.value) {
      return
    }
    scheduleBindFrameRuntime()
  }

  const handleMessage = (event: MessageEvent) => {
    if (event.origin !== window.location.origin || !event.data || typeof event.data !== 'object') {
      return
    }

    const data = event.data as VisualEditorFrameMessage & { sessionId?: string }
    if (data.sessionId !== sessionId) {
      return
    }

    if (data.type === VISUAL_EDITOR_PROTOCOL.messageType.select) {
      selectedElement.value = data.element
    }
  }

  onMounted(() => {
    window.addEventListener('message', handleMessage)
  })

  onUnmounted(() => {
    window.removeEventListener('message', handleMessage)
    exitEditMode()
  })

  return {
    visualEditEnabled,
    selectedElement,
    enterEditMode,
    exitEditMode,
    clearSelection,
    handlePreviewLoad,
  }
}

export default useVisualEditor
