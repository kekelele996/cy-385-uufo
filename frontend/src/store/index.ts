import { reactive, readonly } from 'vue';
import type { Baby } from '../types';
import { api } from '../api';

interface AppState {
  babies: Baby[];
  currentBabyId: number | null;
  loaded: boolean;
}

const state = reactive<AppState>({
  babies: [],
  currentBabyId: null,
  loaded: false,
});

const STORAGE_KEY = 'babytracker.currentBabyId';

function restoreCurrent() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw) {
    const id = Number(raw);
    if (state.babies.some((b) => b.id === id)) {
      state.currentBabyId = id;
      return;
    }
  }
  state.currentBabyId = state.babies[0]?.id ?? null;
}

export const store = {
  state: readonly(state),

  async loadBabies(force = false) {
    if (state.loaded && !force) return;
    state.babies = await api.listBabies();
    state.loaded = true;
    restoreCurrent();
  },

  async addBaby(baby: Baby) {
    state.babies = [...state.babies, baby];
    if (state.currentBabyId == null) {
      this.setCurrentBaby(baby.id);
    }
  },

  setCurrentBaby(id: number) {
    state.currentBabyId = id;
    localStorage.setItem(STORAGE_KEY, String(id));
  },

  currentBaby(): Baby | undefined {
    return state.babies.find((b) => b.id === state.currentBabyId);
  },
};

export function monthAgeOf(birthday: string, at: Date = new Date()): number {
  const birth = new Date(birthday);
  let months = (at.getFullYear() - birth.getFullYear()) * 12 + (at.getMonth() - birth.getMonth());
  if (at.getDate() < birth.getDate()) months -= 1;
  return Math.max(0, months);
}
