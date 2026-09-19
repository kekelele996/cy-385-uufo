import { request } from './client';

export interface Baby {
  id: number;
  name: string;
  birthday: string;
}

export interface Reaction {
  id: number;
  mealId: number;
  reacted: boolean;
  symptoms: string | null;
  reactedAt: string;
  revoked: boolean;
}

export interface Meal {
  id: number;
  babyId: number;
  mealType: string;
  mealTime: string;
  ingredients: string[];
  reaction: Reaction | null;
}

export interface IngredientStatus {
  id: number;
  ingredientName: string;
  status: 'SAFE' | 'EXCLUDED';
  reason: string;
  firstConfirmedAt: string;
  updatedAt: string;
}

export interface ReactionResult {
  duplicated: boolean;
  reaction: Reaction;
  statuses: IngredientStatus[];
}

export interface Recipe {
  id: number;
  name: string;
  ingredients: string;
  steps: string;
  nutrition: string;
  allergens: string | null;
}

export const listBabies = () => request<Baby[]>('/babies');

export const createBaby = (data: { name: string; birthday: string }) =>
  request<Baby>('/babies', { method: 'POST', body: JSON.stringify(data) });

export const listMeals = (babyId: number) =>
  request<Meal[]>(`/feeding/meals?babyId=${babyId}`);

export const createMeal = (data: {
  babyId: number;
  mealType: string;
  mealTime: string;
  ingredientsText: string;
}) => request<Meal>('/feeding/meals', { method: 'POST', body: JSON.stringify(data) });

export const registerReaction = (
  mealId: number,
  data: { reacted: boolean; symptoms?: string; reactedAt?: string },
) =>
  request<ReactionResult>(`/reactions/meal/${mealId}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const revokeReaction = (reactionId: number, reason: string) =>
  request<{ revoked: boolean; message: string }>(`/reactions/${reactionId}/revoke`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });

export const listIngredientStatus = (babyId: number) =>
  request<IngredientStatus[]>(`/ingredients/status?babyId=${babyId}`);

export const recommendRecipes = (monthAge: number, babyId: number) =>
  request<Recipe[]>(`/foods/recommend?monthAge=${monthAge}&babyId=${babyId}`);
