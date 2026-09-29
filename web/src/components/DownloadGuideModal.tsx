import React, { useState, useEffect } from 'react';
import {
  Download,
  Smartphone,
  Laptop,
  CheckCircle2,
  X,
  Share2,
  Copy,
  ExternalLink,
  Sparkles,
} from 'lucide-react';

interface DownloadGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
  lang: 'bn' | 'en';
}

export const DownloadGuideModal: React.FC<DownloadGuideModalProps> = ({
  isOpen,
  onClose,
  lang,
}) => {
  const [copied, setCopied] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);

  useEffect(() => {
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setDeferredPrompt(null);
      }
    } else {
      alert(
        lang === 'bn'
          ? 'আপনার ব্রাউজারের ৩-ডট (⋮) মেনু থেকে "Install app" অথবা "Add to Home screen" সিলেক্ট করুন।'
          : 'Please select "Install app" or "Add to Home screen" from your browser 3-dot menu.'
      );
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xl animate-fade-in">
      <div className="w-full max-w-md rounded-3xl glass-panel p-6 border border-white/20 shadow-2xl space-y-4 bg-slate-900/95 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-purple-600 to-cyan-500 p-[1.5px] shadow-lg shadow-purple-500/30">
              <div className="w-full h-full rounded-[14px] bg-[#0c101c] flex items-center justify-center">
                <Download className="w-5 h-5 text-purple-300" />
              </div>
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {lang === 'bn' ? 'অ্যাপ ডাউনলোড ও ইনস্টল' : 'Download & Install App'}
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'মোবাইলে সাধারণ অ্যাপ হিসেবে ব্যবহার করুন' : 'Use as a standalone mobile app'}
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

        {/* 1-Click Install Button if supported */}
        <button
          onClick={handleInstallClick}
          className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-xl shadow-purple-600/30 active:scale-95 transition"
        >
          <Download className="w-4 h-4" />
          <span>
            {lang === 'bn' ? 'সরাসরি ইনস্টল করুন (Install Now)' : 'Install as Standalone App'}
          </span>
        </button>

        {/* Mobile Instructions Card */}
        <div className="glass-card rounded-2xl p-4 border border-white/10 space-y-3">
          <div className="flex items-center gap-2 text-purple-300">
            <Smartphone className="w-4 h-4" />
            <h4 className="text-xs font-bold uppercase tracking-wider">
              {lang === 'bn' ? '১. ফোনে ইনস্টল করার নিয়ম (Android Mobile)' : '1. Install on Android Mobile'}
            </h4>
          </div>

          <ol className="text-xs text-slate-300 space-y-2 list-decimal list-inside leading-relaxed">
            <li>
              {lang === 'bn'
                ? 'ফোনের Chrome বা যেকোনো ব্রাউজারে এই অ্যাপের লিঙ্কটি খুলুন।'
                : 'Open the app URL in Chrome on your Android phone.'}
            </li>
            <li>
              {lang === 'bn'
                ? 'ব্রাউজারের উপরের ডান কোণায় ৩-ডট (⋮) মেনুতে চাপ দিন।'
                : 'Tap the 3-dot menu (⋮) at top-right of Chrome.'}
            </li>
            <li>
              {lang === 'bn' ? (
                <>
                  <strong className="text-white">"Install app"</strong> অথবা{' '}
                  <strong className="text-white">"Add to Home screen" (হোম স্ক্রিনে যোগ করুন)</strong> এ ক্লিক করুন।
                </>
              ) : (
                'Select "Install app" or "Add to Home screen".'
              )}
            </li>
            <li>
              {lang === 'bn'
                ? 'আপনার ফোনের হোমস্ক্রিনে অন্যান্য সাধারণ অ্যাপের মতো আইকন যুক্ত হবে এবং ইন্টারনেট ছাড়াই ফুলস্ক্রিনে চলবে!'
                : 'The app icon will appear on your phone home screen and work fully offline!'}
            </li>
          </ol>
        </div>

        {/* PC / Desktop Instructions Card */}
        <div className="glass-card rounded-2xl p-4 border border-white/10 space-y-2">
          <div className="flex items-center gap-2 text-cyan-300">
            <Laptop className="w-4 h-4" />
            <h4 className="text-xs font-bold uppercase tracking-wider">
              {lang === 'bn' ? '২. পিসি বা ল্যাপটপে ইনস্টল' : '2. Install on PC / Laptop'}
            </h4>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            {lang === 'bn'
              ? 'ব্রাউজারের ইউআরএল বারের ডানপাশে থাকা "Install" বাটনে চাপুন অথবা ব্রাউজার মেনু থেকে "Install A S Media Player" বেছে নিন।'
              : 'Click the Install icon in the browser address bar or select "Install A S Media Player" from browser menu.'}
          </p>
        </div>

        {/* Copy App Link */}
        <div className="flex items-center gap-2 pt-1">
          <button
            onClick={handleCopyLink}
            className="flex-1 py-2.5 px-3 rounded-xl glass-card hover:bg-white/10 border border-white/10 text-xs font-semibold text-slate-200 flex items-center justify-center gap-1.5 transition active:scale-95"
          >
            {copied ? (
              <>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span className="text-emerald-300">
                  {lang === 'bn' ? 'লিঙ্ক কপি হয়েছে!' : 'Link Copied!'}
                </span>
              </>
            ) : (
              <>
                <Copy className="w-4 h-4 text-purple-300" />
                <span>{lang === 'bn' ? 'অ্যাপের লিঙ্ক কপি করুন' : 'Copy App Link'}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
