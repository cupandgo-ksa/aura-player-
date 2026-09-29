import React, { useEffect, useRef, useState } from 'react';
import { Music2, Plus, MoreHorizontal, Moon, Globe } from 'lucide-react';

interface GlassHeaderProps {
  lang: 'bn' | 'en';
  onToggleLang: () => void;
  onOpenSleepTimer: () => void;
  sleepTimerRemaining: number;
  onOpenImport: () => void;
}

export const GlassHeader: React.FC<GlassHeaderProps> = ({
  lang, onToggleLang, onOpenSleepTimer, sleepTimerRemaining, onOpenImport,
}) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!menuOpen) return;
    const outside = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false);
    };
    const back = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenuOpen(false); };
    document.addEventListener('pointerdown', outside);
    window.addEventListener('keydown', back);
    return () => {
      document.removeEventListener('pointerdown', outside);
      window.removeEventListener('keydown', back);
    };
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-[100] px-5 py-3 bg-[#0a0d16]/80 border-b border-white/10 backdrop-blur-2xl">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-2">
        <div className="flex items-center gap-3 min-w-0">
          <span className="w-9 h-9 shrink-0 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center"><Music2 className="w-5 h-5" /></span>
          <h1 className="text-lg font-semibold tracking-tight text-white truncate">A S media-player</h1>
        </div>
        <div className="flex gap-2 shrink-0">
          <button onClick={onOpenImport} aria-label={lang === 'bn' ? 'গান যোগ করুন' : 'Add audio'} className="w-9 h-9 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center"><Plus className="w-5 h-5" /></button>
          <div ref={menuRef} data-header-menu={menuOpen ? "open" : undefined} className="relative">
            <button onClick={() => setMenuOpen((open) => !open)} aria-label={lang === 'bn' ? 'আরো অপশন' : 'More options'} className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center"><MoreHorizontal className="w-5 h-5" /></button>
            {menuOpen && <div className="absolute right-0 top-11 z-[110] w-44 rounded-2xl p-1.5 bg-[#252b3c]/95 border border-white/20 shadow-2xl backdrop-blur-xl text-sm">
              <button onClick={() => { onOpenSleepTimer(); setMenuOpen(false); }} className="flex gap-3 w-full rounded-xl px-3 py-2.5 hover:bg-white/10"><Moon className="w-4 h-4" />{lang === 'bn' ? 'স্লিপ টাইমার' : 'Sleep timer'}{sleepTimerRemaining > 0 ? ' •' : ''}</button>
              <button onClick={() => { onToggleLang(); setMenuOpen(false); }} className="flex gap-3 w-full rounded-xl px-3 py-2.5 hover:bg-white/10"><Globe className="w-4 h-4" />{lang === 'bn' ? 'English' : 'বাংলা'}</button>
            </div>}
          </div>
        </div>
      </div>
    </header>
  );
};
