# Accessibility Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Ensure all React UIs meet WCAG 2.1 AA compliance.

---

## 1. Principles

- Accessibility is a requirement, not a feature
- Design for keyboard, screen readers, and assistive tech
- Semantic HTML first, ARIA second
- Test with real assistive technologies

---

## 2. Semantic HTML

Use the correct HTML element for its purpose:

| ❌ Don't | ✅ Do |
|----------|------|
| `<div onClick>` | `<button>` |
| `<div class="nav">` | `<nav>` |
| `<span class="heading">` | `<h2>` |
| `<div class="list">` | `<ul>` / `<ol>` |
| `<div class="main">` | `<main>` |

---

## 3. ARIA Labels

Use when semantic HTML isn't sufficient:

```tsx
// Icon-only button needs a label
<IconButton aria-label="Close dialog">
  <CloseIcon />
</IconButton>

// Custom component needs a role
<div role="tabpanel" aria-labelledby="tab-1">
  {content}
</div>

// Dynamic content needs live region
<div aria-live="polite" aria-atomic="true">
  {statusMessage}
</div>
```

---

## 4. Keyboard Navigation

- All interactive elements must be keyboard-accessible
- Tab order must be logical (follow visual order)
- Custom components must handle Enter and Space
- Focus management for modals and dynamic content

```tsx
// Focus trap in modal
function Modal({ isOpen, onClose, children }: ModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) modalRef.current?.focus();
  }, [isOpen]);

  return (
    <div ref={modalRef} role="dialog" aria-modal="true" tabIndex={-1}>
      {children}
    </div>
  );
}
```

---

## 5. Forms

```tsx
// ✅ Every input has a label
<label htmlFor="email">Email address</label>
<input id="email" type="email" aria-required="true" aria-invalid={!!error} />

// ✅ Error messages linked to inputs
{error && <span id="email-error" role="alert">{error}</span>}
<input aria-describedby="email-error" />
```

---

## 6. Color and Contrast

- Minimum contrast ratio: 4.5:1 (normal text), 3:1 (large text)
- Never use color alone to convey information
- Provide text labels alongside color indicators

```tsx
// ❌ Color only
<span className="status-red" />

// ✅ Color + text
<span className="status-red" aria-label="Error">Error</span>
```

---

## 7. Images and Media

```tsx
// Informative image
<img src={chart} alt="Monthly revenue showing 20% growth" />

// Decorative image
<img src={decoration} alt="" role="presentation" />

// Video
<video controls>
  <track kind="captions" src="captions.vtt" />
</video>
```

---

## 8. Testing

| Tool | Purpose |
|------|---------|
| axe-core (jest-axe) | Automated accessibility testing |
| eslint-plugin-jsx-a11y | Lint rules for common issues |
| Screen readers (NVDA/VoiceOver) | Manual verification |
| Keyboard-only testing | Tab through entire flows |

```tsx
import { axe } from 'jest-axe';

test('OrderForm has no accessibility violations', async () => {
  const { container } = render(<OrderForm />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

---

## 9. Anti-Patterns

- ❌ `<div>` with onClick instead of `<button>`
- ❌ Missing alt text on informative images
- ❌ Color as the only indicator
- ❌ `tabIndex > 0` (breaks natural tab order)
- ❌ Auto-playing media without controls
- ❌ Missing focus indicators
- ❌ Unlabeled form inputs

---

## 10. Checklist

- [ ] Semantic HTML elements used
- [ ] All interactive elements keyboard-accessible
- [ ] ARIA labels on icon buttons and custom widgets
- [ ] Form inputs have labels
- [ ] Error messages linked with `aria-describedby`
- [ ] Color contrast meets WCAG AA (4.5:1)
- [ ] Focus management for modals/dialogs
- [ ] axe-core tests in test suite
- [ ] Tested with screen reader
