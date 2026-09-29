import React from 'react';
import { Moon, X, Clock, Check } from 'lucide-react';

interface SleepTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
  remainingSeconds: number;
  onSetTimer: (minutes: number) => void;
  onCancelTimer: () => void;
  lang: 'bn' | 'en';
}

export const SleepTimerModal: React.FC<SleepTimerModalProps> = ({
  isOpen,
  onClose,
  remainingSeconds,
  onSetTimer,
  onCancelTimer,
  lang,
}) => {
  if (!isOpen) return null;

  const timerOptions = [
    { minutes: 15, label: lang === 'bn' ? '১৫ মিনিট' : '15 Minutes' },
    { minutes: 30, label: lang === 'bn' ? '৩০ মিনিট' : '30 Minutes' },
    { minutes: 45, label: lang === 'bn' ? '৪৫ মিনিট' : '45 Minutes' },
    { minutes: 60, label: lang === 'bn' ? '১ ঘণ্টা' : '1 Hour' },
    { minutes: 90, label: lang === 'bn' ? '১.৫ ঘণ্টা' : '90 Minutes' },
  ];

  const formatRemaining = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs < 10 ? '0' : ''}${secs}s`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl animate-fade-in">
      <div className="w-full max-w-sm rounded-3xl glass-panel p-6 border border-white/20 shadow-2xl space-y-4 bg-slate-900/90">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-300">
              <Moon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {lang === 'bn' ? 'স্লিপ টাইমার' : 'Sleep Timer'}
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'সময় শেষে গান আপনাআপনি বন্ধ হবে' : 'Automatically stops music'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-xl glass-card text-slate-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Current status if active */}
        {remainingSeconds > 0 && (
          <div className="glass-card rounded-2xl p-3 border border-amber-500/30 bg-amber-950/20 flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs text-amber-300">
              <Clock className="w-4 h-4 animate-spin-slow" />
              <span>
                {lang === 'bn' ? 'বাকি আছে:' : 'Remaining:'} <strong>{formatRemaining(remainingSeconds)}</strong>
              </span>
            </div>
            <button
              onClick={onCancelTimer}
              className="text-xs text-red-400 hover:text-red-300 font-semibold"
            >
              {lang === 'bn' ? 'বন্ধ করুন' : 'Turn Off'}
            </button>
          </div>
        )}

        {/* Option list */}
        <div className="space-y-2">
          {timerOptions.map((opt) => (
            <button
              key={opt.minutes}
              onClick={() => {
                onSetTimer(opt.minutes);
                onClose();
              }}
              className="w-full py-2.5 px-4 rounded-xl glass-card hover:bg-white/10 border border-white/10 text-left text-xs font-semibold text-slate-200 flex items-center justify-between transition"
            >
              <span>{opt.label}</span>
              <Clock className="w-3.5 h-3.5 text-purple-400" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
