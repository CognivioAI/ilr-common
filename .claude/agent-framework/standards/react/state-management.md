# State Management Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how state is managed in React applications.

---

## 1. State Categories

| Category | Tool | Example |
|----------|------|---------|
| Server state | React Query | API data, cached responses |
| Client/UI state | Zustand | Sidebar open, theme, filters |
| Local component state | `useState` | Form inputs, toggles |
| URL state | React Router | Page, tab, search params |
| Form state | React Hook Form | Validation, submission |

---

## 2. Server State — React Query

**All API data is managed by React Query. Never store API data in local state.**

```tsx
// Custom hook wrapping React Query
export function useOrders(filters: OrderFilters) {
  return useQuery({
    queryKey: ['orders', filters],
    queryFn: () => ordersApi.getAll(filters),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Mutation with cache invalidation
export function useCreateOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ordersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
  });
}
```

---

## 3. Client State — Zustand

**Use Zustand for global UI state that doesn't come from the server.**

```tsx
interface AppStore {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
}

export const useAppStore = create<AppStore>((set) => ({
  sidebarOpen: true,
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  theme: 'light',
  setTheme: (theme) => set({ theme }),
}));
```

---

## 4. Local State — useState

For state that belongs to one component and doesn't need sharing:

```tsx
function SearchInput({ onSearch }: SearchInputProps) {
  const [query, setQuery] = useState('');

  const handleSubmit = () => onSearch(query);

  return <TextField value={query} onChange={(e) => setQuery(e.target.value)} />;
}
```

---

## 5. URL State — React Router

Persist filter/pagination state in the URL for shareability:

```tsx
function OrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get('page') ?? '0');
  const status = searchParams.get('status') ?? 'ALL';

  const { data } = useOrders({ page, status });

  const handlePageChange = (newPage: number) => {
    setSearchParams({ page: String(newPage), status });
  };
}
```

---

## 6. Decision Matrix

```
Is it from the server?
  → YES → React Query

Does it need to persist in URL?
  → YES → React Router (useSearchParams)

Is it shared across components?
  → YES → Zustand

Is it form-related?
  → YES → React Hook Form

Is it local to one component?
  → YES → useState
```

---

## 7. Anti-Patterns

- ❌ Storing API data in `useState` (use React Query)
- ❌ Prop drilling 3+ levels (use Zustand or Context)
- ❌ Global state for local concerns
- ❌ `useEffect` to sync state (derive it instead)
- ❌ Redux for new projects (Zustand is simpler)
- ❌ Multiple sources of truth for the same data

---

## 8. Checklist

- [ ] React Query for all server state
- [ ] Zustand for shared UI state
- [ ] useState for local component state
- [ ] URL params for filters/pagination
- [ ] No prop drilling beyond 2 levels
- [ ] No duplicate state (single source of truth)
- [ ] Cache invalidation on mutations
