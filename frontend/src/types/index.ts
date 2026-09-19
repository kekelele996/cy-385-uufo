// 后端统一返回结构（错误时）
export interface ApiErrorBody {
  success: false;
  code: string;
  message: string;
  status?: number;
}

export interface FeedingIngredient {
  name: string;
  status: 'UNVERIFIED' | 'SAFE' | 'EXCLUDED';
  statusLabel: string;
  reason: string;
}

export interface Reaction {
  id: number;
  feedingId: number;
  babyId: number;
  reactionType: 'POSITIVE' | 'NEGATIVE';
  reactionTypeLabel: string;
  observedAt: string;
  symptoms?: string | null;
  note?: string | null;
  revoked: boolean;
  revokedReason?: string | null;
  createdAt: string;
  revokedAt?: string | null;
  ingredients: string[];
}

export interface Feeding {
  id: number;
  babyId: number;
  mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
  mealTypeLabel: string;
  eatenAt: string;
  note?: string | null;
  ingredients: FeedingIngredient[];
  reaction: Reaction | null;
}

export interface IngredientStatus {
  id: number;
  babyId: number;
  ingredientName: string;
  status: 'UNVERIFIED' | 'SAFE' | 'EXCLUDED';
  statusLabel: string;
  reason: string;
}

export interface FoodRecipe {
  id: number;
  monthAgeMin: number;
  monthAgeMax: number;
  name: string;
  ingredients?: string;
  steps?: string;
  nutrition?: string;
  allergens?: string;
}

export interface BlockedRecipe {
  id: number;
  name: string;
  hitIngredients: string[];
}

export interface RecipeRecommend {
  recipes: FoodRecipe[];
  excludedIngredients: string[];
  blockedRecipes: BlockedRecipe[];
}

export interface Baby {
  id: number;
  name: string;
  birthday: string;
  bloodType?: string | null;
  initialHeight?: number | null;
  initialWeight?: number | null;
}
