import React from 'react';
import { Sparkles, Moon, Plus, Globe, ShieldCheck, Download, Wrench } from 'lucide-react';

interface GlassHeaderProps {
  lang: 'bn' | 'en';
  onToggleLang: () => void;
  onOpenSleepTimer: () => void;
  sleepTimerRemaining: number;
  onOpenImport: () => void;
  onOpenDownloadGuide: () => void;
  onOpenStudioTools: () => void;
}

export const GlassHeader: React.FC<GlassHeaderProps> = ({
  lang,
  onToggleLang,
  onOpenSleepTimer,
  sleepTimerRemaining,
  onOpenImport,
  onOpenDownloadGuide,
  onOpenStudioTools,
}) => {
  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <header className="sticky top-0 z-30 w-full px-4 py-3 glass-panel border-x-0 border-t-0 border-b border-white/10 backdrop-blur-2xl">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-3">
        {/* Brand identity */}
        <div className="flex items-center gap-2.5">
          <div className="relative w-10 h-10 rounded-2xl bg-gradient-to-tr from-purple-600 via-indigo-600 to-cyan-400 p-[1.5px] shadow-lg shadow-purple-500/25">
            <div className="w-full h-full rounded-[14px] bg-[#0c101c]/90 flex items-center justify-center backdrop-blur-md">
              <Sparkles className="w-5 h-5 text-purple-300 animate-pulse" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold tracking-tight bg-gradient-to-r from-white via-slate-100 to-purple-200 bg-clip-text text-transparent">
                Aura Glass
              </h1>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/15 border border-emerald-500/30 text-emerald-300">
                <ShieldCheck className="w-3 h-3" />
                {lang === 'bn' ? 'নো-অ্যাড • অফলাইন' : 'Ad-Free • Offline'}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium leading-none mt-0.5">
              {lang === 'bn' ? 'ব্যক্তিগত মিডিয়া প্লেয়ার' : 'Personal Soft Glass Player'}
            </p>
          </div>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-2">
          {/* Studio Tools Button */}
          <button
            onClick={onOpenStudioTools}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-amber-500/25 to-purple-600/25 hover:from-amber-500/35 hover:to-purple-600/35 border border-amber-400/40 text-amber-200 text-xs font-bold shadow-md transition active:scale-95"
            title={lang === 'bn' ? 'প্রিমিয়াম স্টুডিও টুলস (রিংটোন কাটার, কনভার্টার, কার মোড)' : 'Premium Studio Tools'}
          >
            <Wrench className="w-4 h-4 text-amber-300" />
            <span className="hidden sm:inline">
              {lang === 'bn' ? 'টুলস' : 'Tools'}
            </span>
            <span className="text-[9px] px-1 py-0.2 rounded bg-amber-500/30 text-amber-200 font-mono">
              7+
            </span>
          </button>

          {/* Download / Install App Button */}
          <button
            onClick={onOpenDownloadGuide}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-purple-600/30 to-cyan-500/30 hover:from-purple-600/50 hover:to-cyan-500/50 border border-cyan-400/40 text-cyan-200 text-xs font-bold shadow-md transition active:scale-95"
            title={lang === 'bn' ? 'অ্যাপ ডাউনলোড ও ইনস্টল করুন' : 'Download / Install App'}
          >
            <Download className="w-4 h-4 text-cyan-300" />
            <span className="hidden sm:inline">
              {lang === 'bn' ? 'ডাউনলোড' : 'Install'}
            </span>
          </button>

          {/* Add file / import button */}
          <button
            onClick={onOpenImport}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-500/20 hover:bg-purple-500/30 border border-purple-400/30 text-purple-200 text-xs font-semibold shadow-sm transition active:scale-95"
            title={lang === 'bn' ? 'মিডিয়া ফাইল যোগ করুন' : 'Import Local Files'}
          >
            <Plus className="w-4 h-4 text-purple-300" />
            <span className="hidden sm:inline">
              {lang === 'bn' ? 'ফাইল যোগ' : 'Import'}
            </span>
          </button>

          {/* Sleep timer button */}
          <button
            onClick={onOpenSleepTimer}
            className={`relative p-2 rounded-xl border transition active:scale-95 ${
              sleepTimerRemaining > 0
                ? 'bg-amber-500/20 border-amber-500/40 text-amber-300 shadow-amber-500/20 shadow-md'
                : 'bg-white/5 hover:bg-white/10 border-white/10 text-slate-300'
            }`}
            title={lang === 'bn' ? 'স্লিপ টাইমার' : 'Sleep Timer'}
          >
            <Moon className="w-4 h-4" />
            {sleepTimerRemaining > 0 && (
              <span className="absolute -bottom-1 -right-1 px-1 py-0.2 rounded-full text-[9px] font-bold bg-amber-500 text-black leading-tight">
                {formatTimer(sleepTimerRemaining)}
              </span>
            )}
          </button>

          {/* Language toggle */}
          <button
            onClick={onToggleLang}
            className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-slate-300 text-xs font-semibold transition active:scale-95"
            title="Language / ভাষা"
          >
            <Globe className="w-3.5 h-3.5 text-cyan-300" />
            <span>{lang === 'bn' ? 'বাংলা' : 'EN'}</span>
          </button>
        </div>
      </div>
    </header>
  );
};
