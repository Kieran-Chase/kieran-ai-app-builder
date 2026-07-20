import type { VisualEditorElementInfo } from '@/utils/visualEditorProtocol'

const TEXT_MAX_LENGTH = 80
const SELECTOR_MAX_DEPTH = 5

const normalizeText = (text?: string | null) => {
  const normalized = text?.replace(/\s+/g, ' ').trim() ?? ''
  if (normalized.length <= TEXT_MAX_LENGTH) {
    return normalized
  }
  return `${normalized.slice(0, TEXT_MAX_LENGTH)}...`
}

const escapeCssIdentifier = (value: string) => {
  if (typeof CSS !== 'undefined' && CSS.escape) {
    return CSS.escape(value)
  }
  return value.replace(/([ #.;?%&,.+*~':"!^$[\]()=>|/@])/g, '\\$1')
}

const getNthOfType = (element: Element) => {
  let index = 1
  let sibling = element.previousElementSibling
  while (sibling) {
    if (sibling.tagName === element.tagName) {
      index += 1
    }
    sibling = sibling.previousElementSibling
  }
  return index
}

export const getElementSelector = (element: Element) => {
  if (element.id) {
    return `#${escapeCssIdentifier(element.id)}`
  }

  const parts: string[] = []
  let current: Element | null = element

  while (current && current.nodeType === Node.ELEMENT_NODE && parts.length < SELECTOR_MAX_DEPTH) {
    const tagName = current.tagName.toLowerCase()
    const classList = Array.from(current.classList)
      .filter((className) => !className.startsWith('kieran-visual-editor-'))
      .slice(0, 2)
      .map((className) => `.${escapeCssIdentifier(className)}`)
      .join('')
    const nthOfType = getNthOfType(current)
    const part = `${tagName}${classList}:nth-of-type(${nthOfType})`
    parts.unshift(part)
    current = current.parentElement
  }

  return parts.join(' > ')
}

const getElementAttributes = (element: Element) => {
  const attributes: Record<string, string> = {}
  Array.from(element.attributes).forEach((attr) => {
    const name = attr.name
    if (
      name === 'id' ||
      name === 'class' ||
      name === 'role' ||
      name.startsWith('aria-') ||
      name.startsWith('data-')
    ) {
      attributes[name] = normalizeText(attr.value)
    }
  })
  return attributes
}

export const serializeElement = (element: Element): VisualEditorElementInfo => {
  const rect = element.getBoundingClientRect()
  const className = element.getAttribute('class') || undefined
  const text = normalizeText(element.textContent)

  return {
    tagName: element.tagName.toLowerCase(),
    selector: getElementSelector(element),
    id: element.id || undefined,
    className: className ? normalizeText(className) : undefined,
    text: text || undefined,
    attributes: getElementAttributes(element),
    rect: {
      x: Math.round(rect.x),
      y: Math.round(rect.y),
      width: Math.round(rect.width),
      height: Math.round(rect.height),
    },
  }
}

export const formatElementInfoForPrompt = (element: VisualEditorElementInfo) => {
  const lines = [
    '',
    '当前选中元素信息：',
    `- tag: ${element.tagName}`,
    `- selector: ${element.selector}`,
  ]

  if (element.text) {
    lines.push(`- text: ${element.text}`)
  }
  if (Object.keys(element.attributes).length > 0) {
    lines.push(`- attributes: ${JSON.stringify(element.attributes)}`)
  }
  lines.push(`- rect: ${JSON.stringify(element.rect)}`)

  return lines.join('\n')
}

export const formatElementInfoForDisplay = (element: VisualEditorElementInfo) => {
  const text = element.text ? `，文本：${element.text}` : ''
  return `${element.tagName}（${element.selector}）${text}`
}
