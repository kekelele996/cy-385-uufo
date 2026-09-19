import { request } from './http';
import type {
  Baby,
  Feeding,
  IngredientStatus,
  Reaction,
  RecipeRecommend,
} from '../types';

export interface FeedingCreatePayload {
  babyId: number;
  mealType: string;
  eatenAt: string;
  ingredients: string[];
  note?: string;
}

export interface ReactionCreatePayload {
  babyId: number;
  reactionType: 'POSITIVE' | 'NEGATIVE';
  observedAt?: string;
  symptoms?: string;
  note?: string;
}

export const api = {
  listBabies: () => request<Baby[]>('/babies'),
  createBaby: (payload: Pick<Baby, 'name' | 'birthday' | 'bloodType'>) =>
    request<Baby>('/babies', { method: 'POST', body: JSON.stringify(payload) }),

  listFeedings: (babyId: number, limit = 50) =>
    request<Feeding[]>(`/feedings?babyId=${babyId}&limit=${limit}`),
  getFeeding: (id: number, babyId?: number) =>
    request<Feeding>(`/feedings/${id}${babyId ? `?babyId=${babyId}` : ''}`),
  createFeeding: (payload: FeedingCreatePayload) =>
    request<Feeding>('/feedings', { method: 'POST', body: JSON.stringify(payload) }),

  registerReaction: (feedingId: number, payload: ReactionCreatePayload) =>
    request<Reaction>(`/feedings/${feedingId}/reactions`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  revokeReaction: (feedingId: number, reactionId: number, reason: string) =>
    request<Reaction>(`/feedings/${feedingId}/reactions/${reactionId}/revoke`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    }),

  listReactions: (babyId: number, includeRevoked = false) =>
    request<Reaction[]>(`/reactions?babyId=${babyId}&includeRevoked=${includeRevoked}`),

  listIngredientStatus: (babyId: number) =>
    request<IngredientStatus[]>(`/ingredients/status?babyId=${babyId}`),

  recommendSafe: (babyId: number, monthAge: number) =>
    request<RecipeRecommend>(`/foods/recommend-safe?babyId=${babyId}&monthAge=${monthAge}`),
};
