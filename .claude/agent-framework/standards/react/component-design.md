# Component Design Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how React components are designed, composed, and organized.

---

## 1. Component Categories

| Category | Purpose | Example |
|----------|---------|---------|
| Page | Route-level container, data fetching | `OrdersPage` |
| Feature | Business feature module | `OrderList`, `OrderForm` |
| UI | Reusable, generic presentation | `Button`, `Modal`, `Card` |
| Layout | Page structure | `Sidebar`, `PageHeader` |

---

## 2. Component Anatomy

```tsx
// 1. Imports
import { useState, useCallback } from 'react';
import { Button } from '@mui/material';
import { useOrders } from '../hooks/useOrders';
import type { Order } from '../types';

// 2. Props interface
interface OrderListProps {
  customerId: string;
  onOrderSelect: (order: Order) => void;
}

// 3. Component
export function OrderList({ customerId, onOrderSelect }: OrderListProps) {
  // Hooks first
  const { data: orders, isLoading, error } = useOrders(customerId);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Event handlers
  const handleSelect = useCallback((order: Order) => {
    setSelectedId(order.id);
    onOrderSelect(order);
  }, [onOrderSelect]);

  // Early returns for loading/error states
  if (isLoading) return <OrderListSkeleton />;
  if (error) return <ErrorMessage error={error} />;
  if (!orders?.length) return <EmptyState message="No orders found" />;

  // Render
  return (
    <ul role="list" aria-label="Orders">
      {orders.map(order => (
        <OrderListItem
          key={order.id}
          order={order}
          isSelected={order.id === selectedId}
          onSelect={handleSelect}
        />
      ))}
    </ul>
  );
}
```

---

## 3. Composition Patterns

### Compound Components

```tsx
<DataTable>
  <DataTable.Header>
    <DataTable.Column sortable>Name</DataTable.Column>
    <DataTable.Column>Status</DataTable.Column>
  </DataTable.Header>
  <DataTable.Body>
    {items.map(item => (
      <DataTable.Row key={item.id}>
        <DataTable.Cell>{item.name}</DataTable.Cell>
        <DataTable.Cell>{item.status}</DataTable.Cell>
      </DataTable.Row>
    ))}
  </DataTable.Body>
</DataTable>
```

### Render Props (when composition isn't enough)

```tsx
<Autocomplete
  options={users}
  renderOption={(user) => <UserChip user={user} />}
/>
```

---

## 4. State Handling

### Loading States

Always handle: loading, error, empty, and success.

```tsx
if (isLoading) return <Skeleton />;
if (error) return <ErrorBanner error={error} onRetry={refetch} />;
if (!data?.length) return <EmptyState />;
return <DataView data={data} />;
```

### Form State

```tsx
import { useForm, Controller } from 'react-hook-form';

function OrderForm({ onSubmit }: OrderFormProps) {
  const { control, handleSubmit, formState: { errors } } = useForm<CreateOrderInput>();

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <Controller
        name="customerName"
        control={control}
        rules={{ required: 'Name is required' }}
        render={({ field }) => (
          <TextField {...field} error={!!errors.customerName} helperText={errors.customerName?.message} />
        )}
      />
    </form>
  );
}
```

---

## 5. Performance Patterns

```tsx
// Memoize expensive renders
const MemoizedChart = React.memo(Chart);

// Memoize callbacks passed to children
const handleClick = useCallback(() => {
  onSelect(item.id);
}, [item.id, onSelect]);

// Lazy load heavy components
const ReportDashboard = React.lazy(() => import('./ReportDashboard'));
```

---

## 6. Anti-Patterns

- ❌ Components over 100 lines (split them)
- ❌ Business logic in JSX (extract to hooks)
- ❌ Deeply nested ternaries in render
- ❌ Props drilling more than 2 levels
- ❌ `useEffect` for derived state (use `useMemo`)
- ❌ Index as key for dynamic lists
- ❌ Mutating props or state directly

---

## 7. Checklist

- [ ] Component has single responsibility
- [ ] Props interface defined and typed
- [ ] All states handled (loading, error, empty, success)
- [ ] No business logic in render
- [ ] Under 100 lines
- [ ] Accessible (semantic HTML + ARIA)
- [ ] Memoized where performance-critical
- [ ] Tested with React Testing Library
