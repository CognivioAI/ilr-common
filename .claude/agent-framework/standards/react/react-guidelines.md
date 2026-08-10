# React Guidelines

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how all React frontends are built.

---

## 1. Principles

- TypeScript everywhere — no `any`
- Functional components only
- Composition over inheritance
- Separation of UI from logic
- Accessibility first
- Mobile-first responsive design

---

## 2. Project Structure

```
src/
├── components/       # Shared, reusable UI components
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── index.ts
│   └── ...
├── pages/            # Route-level page components
├── features/         # Feature modules (self-contained)
│   ├── orders/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api/
│   │   ├── types.ts
│   │   └── index.ts
│   └── ...
├── hooks/            # Shared custom hooks
├── services/         # API client layer
├── types/            # Shared type definitions
├── utils/            # Pure utility functions
└── contexts/         # React Context providers
```

---

## 3. Technology Stack

| Concern | Technology |
|---------|-----------|
| Language | TypeScript (strict mode) |
| Framework | React 18+ |
| Routing | React Router |
| Server state | React Query (TanStack Query) |
| Client state | Zustand |
| UI library | Material UI |
| Styling | Material UI + Tailwind (utility) |
| Testing | Jest + React Testing Library |
| E2E | Playwright |

---

## 4. Component Rules

- One component per file
- Functional components only — never class components
- Props defined as interface above component
- Destructure props in function signature
- Components under 100 lines (extract if larger)

```tsx
interface UserCardProps {
  user: User;
  onSelect?: (userId: string) => void;
}

export function UserCard({ user, onSelect }: UserCardProps) {
  return (
    <article className="user-card" onClick={() => onSelect?.(user.id)}>
      <h3>{user.name}</h3>
      <p>{user.email}</p>
    </article>
  );
}
```

---

## 5. API Integration

```
React Component
  ↓ (uses hook)
Custom Hook (useOrders)
  ↓ (uses React Query)
API Client (ordersApi.ts)
  ↓ (HTTP calls)
Spring Boot Backend
```

**Never call APIs directly from components.**

```tsx
// ✅ API client layer
export const ordersApi = {
  getAll: (params: OrderParams) =>
    httpClient.get<Page<Order>>('/api/v1/orders', { params }),
  getById: (id: string) =>
    httpClient.get<Order>(`/api/v1/orders/${id}`),
  create: (input: CreateOrderInput) =>
    httpClient.post<Order>('/api/v1/orders', input),
};

// ✅ Custom hook
export function useOrders(params: OrderParams) {
  return useQuery({
    queryKey: ['orders', params],
    queryFn: () => ordersApi.getAll(params),
  });
}
```

---

## 6. Avoid

| ❌ Never | ✅ Instead |
|----------|-----------|
| Class components | Functional components |
| `any` type | Proper types or `unknown` |
| Business logic in JSX | Extract to hooks/utils |
| Direct API calls in components | API client + React Query |
| Prop drilling (3+ levels) | Context or Zustand |
| Inline styles (non-dynamic) | Material UI / Tailwind |

---

## 7. Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Components | PascalCase | `OrderList.tsx` |
| Hooks | camelCase, `use` prefix | `useOrders.ts` |
| Utilities | camelCase | `formatDate.ts` |
| Types | PascalCase | `OrderStatus` |
| Constants | UPPER_SNAKE_CASE | `API_BASE_URL` |
| Files (components) | PascalCase | `UserCard.tsx` |
| Files (utils) | camelCase | `formatCurrency.ts` |

---

## 8. Checklist

- [ ] TypeScript strict mode enabled
- [ ] No `any` types
- [ ] Functional components only
- [ ] API calls through client + hooks (not in components)
- [ ] React Query for server state
- [ ] Material UI for design system
- [ ] Responsive (mobile-first)
- [ ] Accessible (semantic HTML + ARIA)
- [ ] Tests with React Testing Library

---

## 9. AI Agent Prompt

```
Create reusable React components.
Use TypeScript strict mode — no any.
Separate UI from API calls.
Use React Query for server state.
Use Material UI for components.
Create responsive pages (mobile-first).
Follow the component structure: pages → features → components.
Write tests with React Testing Library.
```
