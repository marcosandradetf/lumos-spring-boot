import { create } from 'zustand';

interface AppState {
  isMenuOpen: boolean;
  showMobileDrawer: boolean;
  showNotificationDrawer: boolean;
  showAccountDrawer: boolean;
  isOnboarding: boolean;
  breadcrumb: string[];
  pageTitle: string;
  guideUrl?: string;

  toggleMenu: (open: boolean) => void;
  setShowMobileDrawer: (show: boolean) => void;
  setShowNotificationDrawer: (show: boolean) => void;
  toggleNotificationDrawer: () => void;
  setShowAccountDrawer: (show: boolean) => void;
  toggleAccountDrawer: () => void;
  setOnboarding: (val: boolean) => void;
  setPageContext: (breadcrumb: string[], title?: string) => void;
  setGuideUrl: (url: string | undefined) => void;

}

const _menuOpen = localStorage.getItem('menuOpen');

export const useAppStore = create<AppState>((set) => ({
  isMenuOpen: _menuOpen !== null ? JSON.parse(_menuOpen) : true,
  showMobileDrawer: false,
  showNotificationDrawer: false,
  showAccountDrawer: false,
  isOnboarding: localStorage.getItem('onboarding') === null,
  breadcrumb: [],
  pageTitle: '',
  guideUrl: undefined,

  toggleMenu: (open) => {
    localStorage.setItem('menuOpen', JSON.stringify(open));
    set({ isMenuOpen: open });
  },
  setShowMobileDrawer: (show) => set({ showMobileDrawer: show }),
  setShowNotificationDrawer: (show) => set({ showNotificationDrawer: show }),
  toggleNotificationDrawer: () => set((state) => ({ showNotificationDrawer: !state.showNotificationDrawer })),
  setShowAccountDrawer: (show) => set({ showAccountDrawer: show }),
  toggleAccountDrawer: () => set((state) => ({ showAccountDrawer: !state.showAccountDrawer })),
  setOnboarding: (val) => set({ isOnboarding: val }),
  setPageContext: (breadcrumb, title = '') =>
    set((state) => {
      const sameTitle = state.pageTitle === title;
      const sameBreadcrumb =
        state.breadcrumb.length === breadcrumb.length &&
        state.breadcrumb.every((item, index) => item === breadcrumb[index]);

      if (sameTitle && sameBreadcrumb) {
        return state;
      }

      return { breadcrumb, pageTitle: title };
    }),

  setGuideUrl: (url) => set({ guideUrl: url })

}));
