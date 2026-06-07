import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAppStore } from '../../store/use-app-store';
import { useAuthStore } from '../../core/auth/useAuthStore';
import { useNotificationStore } from '../../core/notifications/useNotificationStore';
import { NAV_SECTIONS, type NavItem } from './nav-config';

interface SearchResult {
  label: string;
  customLabel?: string;
  searchTerms?: string[];
  path: string;
  icon: string;
  queryParams?: Record<string, string>;
}

function flattenNavItems(items: NavItem[], bag: SearchResult[]) {
  items.forEach((item) => {
    if (item.path) {
      bag.push({
        label: item.label,
        customLabel: item.customLabel,
        searchTerms: item.searchTerms,
        path: item.path,
        icon: `pi ${item.icon}`,
        queryParams: item.queryParams,
      });
    }
    if (item.children && item.children.length > 0) {
      flattenNavItems(item.children, bag);
    }
  });
}

function searchRoutes(term: string): SearchResult[] {
  if (!term || term.length < 2) {
    return [];
  }

  const all: SearchResult[] = [];
  NAV_SECTIONS.forEach((section) => flattenNavItems(section.children, all));

  const normalizedTerm = term.toLowerCase();
  return all
    .filter((result) =>
      result.label.toLowerCase().includes(normalizedTerm)
      || result.customLabel?.toLowerCase().includes(normalizedTerm)
      || result.searchTerms?.some((term) => term.includes(normalizedTerm))
      || result.path.toLowerCase().includes(normalizedTerm))
    .filter((value, index, self) =>
      index === self.findIndex((item) => item.path === value.path && item.label === value.label))
    .slice(0, 6);
}

export function Header() {
  const navigate = useNavigate();
  const {
    isMenuOpen,
    setShowMobileDrawer,
    toggleNotificationDrawer,
    toggleAccountDrawer,
    breadcrumb,
    pageTitle,
  } = useAppStore();
  const { user } = useAuthStore();
  const {
    status: notificationStatus,
    count: notificationCount,
  } = useNotificationStore();

  const [isDark, setIsDark] = useState(false);
  const [isApple, setIsApple] = useState(false);
  const [isSpotlightOpen, setIsSpotlightOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(-1);

  useEffect(() => {
    setIsDark(document.documentElement.classList.contains('dark'));
    setIsApple(/iPhone|iPad|iPod|Macintosh/i.test(navigator.userAgent));
  }, []);

  const searchResults = useMemo(
    () => searchRoutes(searchQuery),
    [searchQuery],
  );

  useEffect(() => {
    setSelectedIndex(searchResults.length > 0 ? 0 : -1);
  }, [searchResults.length]);

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsSpotlightOpen(false);
        setSearchQuery('');
      }
    };

    const onShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setIsSpotlightOpen(true);
      }
    };

    const onNav = (event: KeyboardEvent) => {
      if (!isSpotlightOpen) {
        return;
      }
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setSelectedIndex((previous) => (previous + 1) % Math.max(searchResults.length, 1));
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        setSelectedIndex((previous) => (previous - 1 + Math.max(searchResults.length, 1)) % Math.max(searchResults.length, 1));
      } else if (event.key === 'Enter') {
        event.preventDefault();
        if (selectedIndex >= 0 && searchResults[selectedIndex]) {
          const result = searchResults[selectedIndex];
          const query = result.queryParams ? `?${new URLSearchParams(result.queryParams).toString()}` : '';
          navigate(result.path + query);
          setIsSpotlightOpen(false);
          setSearchQuery('');
        }
      }
    };

    window.addEventListener('keydown', onEsc);
    window.addEventListener('keydown', onShortcut);
    window.addEventListener('keydown', onNav);
    return () => {
      window.removeEventListener('keydown', onEsc);
      window.removeEventListener('keydown', onShortcut);
      window.removeEventListener('keydown', onNav);
    };
  }, [isSpotlightOpen, navigate, searchResults, selectedIndex]);

  const toggleDark = () => {
    const next = !isDark;
    document.documentElement.classList.toggle('dark', next);
    localStorage.setItem('theme', next ? 'dark' : 'light');
    setIsDark(next);
  };

  return (
    <>
      <header className="flex items-center gap-2 px-3 h-[52px] bg-white/80 dark:bg-zinc-900/80 backdrop-blur-md border-b border-slate-200 dark:border-zinc-800 sticky top-0 z-30 flex-shrink-0">
        <button
          onClick={() => setShowMobileDrawer(true)}
          className="xl:hidden flex items-center justify-center p-1 rounded-full bg-slate-100 dark:bg-zinc-800 border border-slate-200 dark:border-zinc-700 active:scale-90 transition-all cursor-pointer"
          aria-label="Abrir menu"
        >
          <span className="flex items-center justify-center w-7 h-7 rounded-full bg-white dark:bg-zinc-900 text-slate-600 dark:text-zinc-300 shadow-sm">
            <i className="pi pi-bars text-xs" />
          </span>
        </button>

        {!isMenuOpen && (
          <div
            className="hidden xl:flex items-center justify-center w-8 h-8 rounded-lg bg-gradient-to-br from-blue-200 to-indigo-200 cursor-pointer"
            onClick={() => navigate('/')}
          >
            <img src="/icon-192.png" alt="Lumos" width={22} height={22} className="opacity-90" />
          </div>
        )}

        <div className="hidden md:flex flex-col ml-1 leading-tight min-w-0">
          {breadcrumb.length > 0 && (
            <nav className="flex items-center gap-1 text-xs text-slate-500 dark:text-zinc-400">
              {breadcrumb.map((crumb, index) => (
                <span key={index} className="flex items-center gap-1">
                  {index > 0 && <i className="pi pi-chevron-right text-[9px] opacity-40" />}
                  <span className={index === breadcrumb.length - 1 ? 'text-slate-700 dark:text-zinc-200 font-medium' : ''}>
                    {crumb}
                  </span>
                </span>
              ))}
            </nav>
          )}
          {pageTitle && (
            <h3 className="text-[16px] font-bold tracking-tight text-slate-800 dark:text-zinc-100 truncate mt-0.5">
              {pageTitle}
            </h3>
          )}
        </div>

        <div className="flex-1" />

        <div className="relative flex items-center group cursor-pointer" onClick={() => setIsSpotlightOpen(true)}>
          <i className="pi pi-search absolute left-3 text-slate-400 text-sm group-hover:text-indigo-500 transition-colors" />
          <div className="md:hidden flex h-9 pl-9 rounded-full bg-slate-100 dark:bg-zinc-800/50 border border-transparent items-center text-slate-400 text-[13px] font-medium transition-all group-hover:bg-slate-200/50 dark:group-hover:bg-zinc-800" />
          <div className="hidden md:flex h-9 pl-9 pr-24 rounded-full bg-slate-100 dark:bg-zinc-800/50 border border-transparent items-center text-slate-400 text-[13px] font-medium transition-all group-hover:bg-slate-200/50 dark:group-hover:bg-zinc-800">
            Buscar no sistema...
          </div>
          <div className="absolute right-3 hidden md:flex items-center gap-1 pointer-events-none opacity-40">
              <span className="text-[10px] font-bold rounded px-1.5 py-0.5 bg-white dark:bg-zinc-800 border border-slate-200 dark:border-zinc-600 dark:text-white">
                {isApple ? '⌘' : 'CTRL'}
              </span>
              <span className="text-[10px] font-bold rounded px-1.5 py-0.5 bg-white dark:bg-zinc-800 border border-slate-200 dark:border-zinc-600 dark:text-white">
                K
              </span>
          </div>
        </div>


        <div className="flex items-center gap-2 ml-2">
          <button
            onClick={toggleDark}
            className="flex items-center justify-center w-9 h-9 rounded-xl bg-slate-100 dark:bg-zinc-800 text-slate-600 dark:text-zinc-300 hover:bg-slate-200 dark:hover:bg-zinc-700 transition-all"
            title={isDark ? 'Modo claro' : 'Modo escuro'}
          >
            <i className={`pi ${isDark ? 'pi-sun' : 'pi-moon'} text-base`} />
          </button>

          <button
            onClick={toggleNotificationDrawer}
            className="group relative flex items-center justify-center w-9 h-9 rounded-xl transition-all duration-300 active:scale-90 bg-slate-100 hover:bg-indigo-50 dark:bg-gray-800 dark:hover:bg-gray-700 border border-transparent "
          >
            <i className="pi pi-bell text-lg text-slate-600 dark:text-gray-300 group-hover:text-indigo-600 dark:group-hover:text-indigo-400" />
            {notificationStatus !== 'denied' ? (
              <>
                <span className={`absolute top-0 right-0 flex h-3 w-3 ${notificationCount > 0 ? '' : 'opacity-70'}`}>
                  <span className={`${notificationCount > 0 ? 'animate-ping' : ''} absolute inline-flex h-full w-full rounded-full opacity-75 bg-red-400`} />
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500" />
                  <span className="absolute -top-1.5 -right-1.5 flex items-center justify-center min-w-[20px] h-5 px-1 bg-red-600 text-white text-[10px] font-extrabold rounded-full border-2 border-white dark:border-gray-900 shadow-lg">
                  {notificationCount > 99 ? '99+' : notificationCount}
                </span>
                </span>
              </>
            ) : (
              <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center bg-amber-100 dark:bg-amber-900/30 rounded-full text-amber-500 shadow-sm border border-amber-200 dark:border-amber-800">
                <i className="pi pi-exclamation-triangle text-[10px]" />
              </span>
            )}
          </button>

          <button
            onClick={toggleAccountDrawer}
            className="flex items-center gap-3 px-2 py-1 rounded-xl hover:bg-slate-100 dark:hover:bg-zinc-800 transition-all"
          >
            <div className="relative flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-sm shadow-sm border-2 border-white dark:border-zinc-900">
                {user?.fullName?.charAt(0)?.toUpperCase() ?? 'U'}
              </div>
              <span className="absolute bottom-0 right-0 block h-2.5 w-2.5 rounded-full bg-green-500 ring-2 ring-white dark:ring-zinc-900" />
            </div>
            <div className="hidden md:flex flex-col items-start leading-tight">
              <span className="text-sm font-bold text-slate-800 dark:text-zinc-100 leading-none">{user?.username}</span>
              <span className="text-[11px] text-slate-500 dark:text-zinc-400 font-medium">Minha Conta</span>
            </div>
          </button>

          {/* <button
            onClick={() => void logout()}
            className="hidden md:flex items-center justify-center w-9 h-9 rounded-xl text-slate-500 dark:text-zinc-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-all"
            title="Sair"
          >
            <i className="pi pi-sign-out text-base" />
          </button> */}
        </div>
      </header>

      {isSpotlightOpen && (
        <div
          className="fixed inset-0 z-[100] flex items-start justify-center pt-[15vh] px-4 bg-slate-900/20 dark:bg-black/40 h-screen"
          onClick={() => {
            setIsSpotlightOpen(false);
            setSearchQuery('');
          }}
        >
          <div
            className="w-full max-w-2xl bg-white dark:bg-zinc-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-zinc-800 overflow-hidden"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-center p-4 border-b border-slate-100 dark:border-zinc-800">
              <i className="pi pi-search text-indigo-500 mr-3 text-lg" />
              <input
                autoFocus
                type="text"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="O que você está procurando?"
                className="w-full bg-transparent border-none outline-none text-lg text-slate-800 dark:text-zinc-100 placeholder:text-slate-400"
              />
              <button
                onClick={() => {
                  setIsSpotlightOpen(false);
                  setSearchQuery('');
                }}
                className="text-[10px] font-bold border border-slate-200 dark:border-zinc-800 dark:text-white rounded px-2 py-1 opacity-50 hover:opacity-100 transition-opacity"
              >
                ESC
              </button>
            </div>

            <div className="max-h-[60vh] overflow-y-auto p-2">
              {searchResults.length === 0 && searchQuery.length > 1 && (
                <div className="p-8 text-center">
                  <i className="pi pi-search text-3xl opacity-20 mb-2" />
                  <p className="text-slate-500">Nenhum resultado encontrado para "{searchQuery}"</p>
                </div>
              )}

              {searchResults.map((result, index) => {
                const isSelected = index === selectedIndex;
                return (
                  <button
                    key={`${result.path}-${result.label}`}
                    onClick={() => {
                      const query = result.queryParams ? `?${new URLSearchParams(result.queryParams).toString()}` : '';
                      navigate(result.path + query);
                      setIsSpotlightOpen(false);
                      setSearchQuery('');
                    }}
                    className={[
                      'group w-full flex items-center justify-between p-3 rounded-2xl transition-all text-left',
                      isSelected
                        ? 'bg-indigo-50 dark:bg-indigo-500/20'
                        : 'hover:bg-slate-50 dark:hover:bg-zinc-800/50',
                    ].join(' ')}
                  >
                    <div className="flex items-center gap-4 min-w-0">
                      <div
                        className={[
                          'w-10 h-10 flex items-center justify-center rounded-xl transition-colors',
                          isSelected ? 'bg-white dark:bg-zinc-700 shadow-sm' : 'bg-slate-100 dark:bg-zinc-800',
                        ].join(' ')}
                      >
                        <i className={`${result.icon} text-indigo-500 text-lg`} />
                      </div>
                      <div className="flex flex-col min-w-0">
                        <span className="text-sm font-semibold text-slate-700 dark:text-zinc-200 truncate">
                          {result.customLabel || result.label}
                        </span>
                        <span className="text-[11px] text-slate-400 font-medium tracking-tight truncate">
                          {result.path}
                        </span>
                      </div>
                    </div>
                    <i
                      className={[
                        'pi pi-chevron-right text-[10px] mr-2 transition-all duration-200',
                        isSelected ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0',
                      ].join(' ')}
                    />
                  </button>
                );
              })}
            </div>

            <div className="p-3 bg-slate-50 dark:bg-zinc-800/50 border-t border-slate-100 dark:border-zinc-800 flex gap-4 justify-center">
              <span className="text-[10px] text-slate-400">
                <b className="dark:text-zinc-300">↑↓</b> para navegar
              </span>
              <span className="text-[10px] text-slate-400">
                <b className="dark:text-zinc-300">↵</b> para selecionar
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
