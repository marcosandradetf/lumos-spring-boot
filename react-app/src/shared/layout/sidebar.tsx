import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAppStore } from '../../store/use-app-store';
import { useAuthStore } from '../../core/auth/useAuthStore';
import { NAV_SECTIONS } from './nav-config';
import type { NavItem, NavSection as NavSectionType } from './nav-config';

interface SidebarProps {
  mobile?: boolean;
  desktopWidth?: number;
}

export function Sidebar({ mobile = false, desktopWidth = 240 }: SidebarProps) {
  const { isMenuOpen, toggleMenu, setShowMobileDrawer, isOnboarding } = useAppStore();
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  const isSupport = user?.support === true;
  const open = mobile ? true : isMenuOpen;

  useEffect(() => {
    const saved: Record<string, boolean> = {};
    NAV_SECTIONS.forEach(section => {
      if (section.storageKey) {
        const v = localStorage.getItem(section.storageKey);
        saved[section.id] = v !== null ? JSON.parse(v) : (section.defaultExpanded ?? false);
      } else {
        saved[section.id] = section.defaultExpanded ?? false;
      }
    });
    setExpanded(saved);
  }, []);

  const toggleSection = (id: string, storageKey?: string) => {
    setExpanded(prev => {
      const next = !prev[id];
      if (storageKey) localStorage.setItem(storageKey, JSON.stringify(next));
      return { ...prev, [id]: next };
    });
  };

  const handleNavigate = (path: string, queryParams?: Record<string, string>) => {
    const search = queryParams ? '?' + new URLSearchParams(queryParams).toString() : '';
    navigate(path + search);
    if (mobile) setShowMobileDrawer(false);
  };

  const isActive = (path?: string) => path ? location.pathname === path : false;

  const containerClass = mobile
    ? 'flex flex-col h-screen w-64 bg-white dark:bg-zinc-900 border-r border-slate-200 dark:border-zinc-800'
    : 'hidden xl:flex flex-col h-screen bg-white dark:bg-zinc-900 border-r border-slate-200 dark:border-zinc-800 transition-[width] duration-200';

  const desktopStyle = !mobile
    ? { width: isMenuOpen ? desktopWidth : 56 }
    : undefined;

  return (
    <div className={'relative z-20 ' + containerClass} style={desktopStyle}>
      {/* Brand */}
      <div className={`flex items-center ${open ? 'justify-between px-3' : 'justify-center px-2'} py-2 border-b border-slate-200 dark:border-zinc-800 min-h-[52px] flex-shrink-0`}>
        {open && (
          <div
            className="flex items-center gap-2.5 cursor-pointer group select-none"
            onClick={() => { navigate('/'); if (mobile) setShowMobileDrawer(false); }}
          >
            <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-gradient-to-br from-blue-200 to-indigo-200 shadow-sm group-hover:shadow-md transition-all duration-200 flex-shrink-0">
              <img src="/icon-192.png" alt="Lumos" width={20} height={20} className="opacity-90" />
            </div>
            <span className="flex items-center tracking-tight min-w-0">
              <span className="font-semibold text-[15px] text-gray-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">Lumos</span>
              <span className="ml-1 text-[15px] font-medium text-gray-500 dark:text-zinc-400">IP</span>
              <span className="text-[10px] ml-0.5 text-gray-400 dark:text-zinc-500 align-top">™</span>
            </span>
          </div>
        )}

        {mobile && (
          <button
            onClick={() => setShowMobileDrawer(false)}
            className="flex items-center justify-center w-7 h-7 rounded-full bg-white text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:bg-zinc-700 dark:hover:text-white transition-colors border border-gray-200 dark:border-zinc-700 flex-shrink-0"
          >
            <i className="pi pi-times text-xs" />
          </button>
        )}

        {!mobile && (
          <button
            onClick={() => toggleMenu(!isMenuOpen)}
            className="flex items-center justify-center w-7 h-7 rounded-md bg-white text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:bg-zinc-700 dark:hover:text-white transition-colors border border-gray-200 dark:border-zinc-700 flex-shrink-0"
            title={isMenuOpen ? 'Recolher menu' : 'Expandir menu'}
          >
            <i className={`pi ${isMenuOpen ? 'pi-chevron-left' : 'pi-chevron-right'} text-xs`} />
          </button>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-2 px-2 space-y-0.5 mac-scroll">
        {NAV_SECTIONS.map(section => {
          if (section.requiresSupport && !isSupport) return null;
          if (section.requiresOnboarding && !isOnboarding) return null;

          return (
            <SidebarSection
              key={section.id}
              section={section}
              isExpanded={expanded[section.id] ?? section.defaultExpanded ?? false}
              onToggle={() => toggleSection(section.id, section.storageKey)}
              open={open}
              isActive={isActive}
              onNavigate={handleNavigate}
            />
          );
        })}
      </nav>
    </div>
  );
}

interface SidebarSectionProps {
  section: NavSectionType;
  isExpanded: boolean;
  onToggle: () => void;
  open: boolean;
  isActive: (path?: string) => boolean;
  onNavigate: (path: string, queryParams?: Record<string, string>) => void;
}

function SidebarSection({ section, isExpanded, onToggle, open, isActive, onNavigate }: SidebarSectionProps) {
  return (
    <div>
      <button
        onClick={onToggle}
        className={`w-full flex items-center ${open ? 'gap-2.5 px-2.5' : 'justify-center px-0'} py-2 rounded-lg text-gray-600 dark:text-zinc-400 hover:bg-slate-100 dark:hover:bg-zinc-800 hover:text-gray-900 dark:hover:text-zinc-100 transition-colors`}
        title={!open ? section.label : undefined}
      >
        <i className={`pi ${section.icon} text-base flex-shrink-0`} />
        {open && (
          <>
            <span className="flex-1 text-left text-[13px] font-medium truncate">{section.label}</span>
            <i className={`pi ${isExpanded ? 'pi-chevron-down' : 'pi-chevron-right'} text-[10px] opacity-40`} />
          </>
        )}
      </button>

      {open && isExpanded && (
        <div className="ml-3 pl-3 border-l border-slate-200 dark:border-zinc-700 mt-0.5 space-y-0.5">
          {section.children.map(item => (
            <NavItemRow
              key={item.id}
              item={item}
              isActive={isActive}
              onNavigate={onNavigate}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface NavItemRowProps {
  item: NavItem;
  isActive: (path?: string) => boolean;
  onNavigate: (path: string, queryParams?: Record<string, string>) => void;
}

function NavItemRow({ item, isActive, onNavigate }: NavItemRowProps) {
  const [childExpanded, setChildExpanded] = useState(false);
  const active = isActive(item.path);

  if (item.children && item.children.length > 0) {
    return (
      <div>
        <button
          onClick={() => setChildExpanded(prev => !prev)}
          disabled={item.disabled}
          className={`w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-[13px] transition-colors text-gray-600 dark:text-zinc-400 ${item.disabled ? 'opacity-40 cursor-not-allowed' : 'hover:bg-slate-100 dark:hover:bg-zinc-800 hover:text-gray-900 dark:hover:text-zinc-100 cursor-pointer'}`}
        >
          <i className={`pi ${item.icon} text-sm ${item.iconColor ?? ''} flex-shrink-0`} />
          <span className="flex-1 text-left truncate">{item.label}</span>
          <i className={`pi ${childExpanded ? 'pi-chevron-down' : 'pi-chevron-right'} text-[10px] opacity-40`} />
        </button>
        {childExpanded && (
          <div className="ml-3 pl-3 border-l border-slate-200 dark:border-zinc-700 mt-0.5 space-y-0.5">
            {item.children.map(child => (
              <NavItemRow key={child.id} item={child} isActive={isActive} onNavigate={onNavigate} />
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <button
      onClick={() => item.path && onNavigate(item.path, item.queryParams)}
      disabled={item.disabled || !item.path}
      className={[
        'w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-[13px] transition-colors text-left',
        active
          ? 'bg-indigo-50 dark:bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 font-medium'
          : item.disabled
            ? 'opacity-40 cursor-not-allowed text-gray-600 dark:text-zinc-400'
            : 'text-gray-600 dark:text-zinc-400 hover:bg-slate-100 dark:hover:bg-zinc-800 hover:text-gray-900 dark:hover:text-zinc-100 cursor-pointer',
      ].join(' ')}
    >
      <i className={`pi ${item.icon} text-sm ${active ? '' : (item.iconColor ?? '')} flex-shrink-0`} />
      <span className="flex-1 truncate">{item.label}</span>
      {item.badge && (
        <span className="px-1.5 py-0.5 text-[10px] font-bold bg-amber-400 text-white rounded-full leading-none">{item.badge}</span>
      )}
    </button>
  );
}
