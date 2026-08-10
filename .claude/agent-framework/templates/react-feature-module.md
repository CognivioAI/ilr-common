# React Feature Module Template

## Usage

Use this template when creating a new feature module in a React app.

## Structure

```
src/features/<feature-name>/
├── components/
│   ├── FeatureList.tsx
│   ├── FeatureDetail.tsx
│   └── FeatureForm.tsx
├── hooks/
│   ├── useFeatureList.ts
│   └── useFeatureDetail.ts
├── api/
│   └── featureApi.ts
├── types.ts
└── index.ts
```

## Key Files

### types.ts

```tsx
export interface Feature {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeatureInput {
  name: string;
}
```

### api/featureApi.ts

```tsx
import { apiClient } from '@/api/client';
import { Feature, CreateFeatureInput } from '../types';

export const featureApi = {
  getAll: () => apiClient.get<Feature[]>('/api/v1/features'),
  getById: (id: string) => apiClient.get<Feature>(`/api/v1/features/${id}`),
  create: (input: CreateFeatureInput) => apiClient.post<Feature>('/api/v1/features', input),
};
```
