# React Guidelines

## Purpose

Define React/TypeScript frontend development standards — component architecture, hooks, state management, testing, accessibility, and performance.

---

## Tech Stack

```yaml
stack:
  framework: React 18+
  language: TypeScript (strict mode)
  build: Vite
  styling: Tailwind CSS
  server_state: TanStack Query (React Query)
  client_state: Zustand (if needed)
  forms: React Hook Form + Zod
  routing: React Router v6
  testing: Vitest + React Testing Library + Playwright
  api_client: Generated from OpenAPI (orval/openapi-typescript)
```

---

## Project Structure (Feature-Based)

```
src/
├── app/                          # App shell, providers, routing
│   ├── App.tsx
│   ├── routes.tsx
│   └── providers.tsx
│
├── features/                     # Feature modules (primary organization)
│   ├── documents/
│   │   ├── components/
│   │   │   ├── DocumentUpload.tsx
│   │   │   ├── DocumentList.tsx
│   │   │   └── DocumentCard.tsx
│   │   ├── hooks/
│   │   │   ├── useDocuments.ts
│   │   │   └── useUploadDocument.ts
│   │   ├── api/
│   │   │   └── documentApi.ts
│   │   ├── types/
│   │   │   └── document.types.ts
│   │   └── index.ts              # Public exports
│   │
│   └── auth/
│       ├── components/
│       ├── hooks/
│       └── api/
│
├── shared/                       # Shared across features
│   ├── components/               # Reusable UI components
│   │   ├── Button.tsx
│   │   ├── Modal.tsx
│   │   ├── DataTable.tsx
│   │   └── ErrorBoundary.tsx
│   ├── hooks/                    # Shared hooks
│   │   ├── useAuth.ts
│   │   └── usePagination.ts
│   ├── utils/                    # Pure utility functions
│   │   ├── format.ts
│   │   └── validation.ts
│   └── types/                    # Shared type definitions
│       └── api.types.ts
│
├── config/                       # Environment config
│   └── env.ts
│
└── test/                         # Test utilities
    ├── setup.ts
    ├── renderWithProviders.tsx
    └── mocks/
        └── handlers.ts           # MSW handlers
```

---

## Component Standards

### Functional Components (Always)

```tsx
// ✅ Functional component with TypeScript props
interface DocumentCardProps {
  document: Document;
  onDelete?: (id: string) => void;
}

export function DocumentCard({ document, onDelete }: DocumentCardProps) {
  return (
    <article className="rounded-lg border p-4" aria-label={`Document: ${document.filename}`}>
      <h3 className="text-lg font-medium">{document.filename}</h3>
      <p className="text-sm text-gray-600">{formatFileSize(document.size)}</p>
      <StatusBadge status={document.status} />
      {onDelete && (
        <button
          onClick={() => onDelete(document.id)}
          aria-label={`Delete ${document.filename}`}
          className="text-red-600 hover:text-red-800"
        >
          Delete
        </button>
      )}
    </article>
  );
}
```

### Rules

```yaml
component_rules:
  structure:
    - One component per file
    - Component name matches filename (DocumentCard → DocumentCard.tsx)
    - Export named (not default) — enables refactoring tools
    - Props interface defined above component
  
  composition:
    - Prefer composition over prop drilling
    - Extract hooks for reusable logic
    - Keep components < 150 lines (extract sub-components)
    - Single responsibility: render OR logic, not both
  
  forbidden:
    - Class components (always functional)
    - Default exports (use named exports)
    - Inline styles (use Tailwind classes)
    - Any type (always explicit TypeScript types)
    - Direct DOM manipulation (use refs if necessary)
```

---

## State Management

### State Types

| State Type | Tool | Example |
|-----------|------|---------|
| Server state (API data) | TanStack Query | Documents list, user profile |
| UI state (local) | useState | Modal open/closed, form input |
| Form state | React Hook Form | Complex forms with validation |
| Global client state | Zustand | Theme, sidebar collapsed |
| URL state | React Router | Filters, pagination, tabs |

### TanStack Query (Server State)

```tsx
// hooks/useDocuments.ts
export function useDocuments(status?: DocumentStatus) {
  return useQuery({
    queryKey: ['documents', { status }],
    queryFn: () => documentApi.list({ status }),
    staleTime: 30_000,  // 30s before refetch
  });
}

export function useUploadDocument() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: documentApi.upload,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });
}
```

### Rules

```yaml
state_rules:
  - Server data: ALWAYS TanStack Query (never manual useEffect + setState)
  - Never store server data in client state (Zustand/Redux)
  - URL is state: filters, pagination, sort → URL params
  - Lift state only as high as needed (not everything at root)
  - Derive state: compute from existing state, don't store computed values
```

---

## Accessibility (WCAG 2.1 AA)

```yaml
accessibility:
  mandatory:
    - All interactive elements keyboard-accessible
    - All images have alt text (or aria-hidden if decorative)
    - Form inputs have associated labels
    - Colour contrast ratio ≥ 4.5:1 (text), 3:1 (large text)
    - Focus visible on all interactive elements
    - Error messages associated with inputs (aria-describedby)
    - Page structure uses semantic HTML (header, main, nav, article)
    - Dynamic content announced to screen readers (aria-live)
  
  testing:
    - axe-core in CI (vitest-axe)
    - Manual screen reader testing (quarterly)
    - Keyboard-only navigation testing
```

---

## Testing (Frontend)

### Unit/Component Tests (Vitest + RTL)

```tsx
// DocumentCard.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DocumentCard } from './DocumentCard';
import { aDocument } from '@/test/fixtures';

describe('DocumentCard', () => {
  it('should display document filename and size', () => {
    render(<DocumentCard document={aDocument()} />);
    
    expect(screen.getByText('report.pdf')).toBeInTheDocument();
    expect(screen.getByText('1 KB')).toBeInTheDocument();
  });

  it('should call onDelete when delete button clicked', async () => {
    const onDelete = vi.fn();
    render(<DocumentCard document={aDocument()} onDelete={onDelete} />);
    
    await userEvent.click(screen.getByRole('button', { name: /delete/i }));
    
    expect(onDelete).toHaveBeenCalledWith(aDocument().id);
  });

  it('should not render delete button when onDelete not provided', () => {
    render(<DocumentCard document={aDocument()} />);
    
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });
});
```

### Testing Rules

```yaml
frontend_testing:
  approach: "Test behavior, not implementation"
  
  do:
    - Query by role, label, text (what users see)
    - Test user interactions (click, type, submit)
    - Test rendered output
    - Test loading and error states
    - Test accessibility (role, aria attributes)
  
  do_not:
    - Test internal state directly
    - Test implementation details (hook internals)
    - Snapshot test everything (only for stable UI)
    - Mock excessively (prefer MSW for API mocking)
```

---

## Performance

```yaml
performance:
  bundle:
    - Code splitting per route (React.lazy)
    - Tree shaking (named exports, no side effects)
    - Dynamic imports for heavy libraries
    - Target: initial bundle < 200KB (gzipped)
  
  rendering:
    - Memoize expensive computations (useMemo)
    - Memoize callbacks passed to children (useCallback)
    - Virtualize long lists (tanstack-virtual)
    - Avoid unnecessary re-renders (React DevTools Profiler)
  
  data:
    - Paginate API calls (never load all records)
    - Prefetch likely-needed data (TanStack Query prefetchQuery)
    - Optimistic updates for instant UI feedback
    - Stale-while-revalidate pattern (built into TanStack Query)
```
