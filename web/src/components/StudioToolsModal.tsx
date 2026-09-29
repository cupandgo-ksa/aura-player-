import React from 'react';
import {
  Scissors,
  Film,
  Tag,
  Car,
  Zap,
  Headphones,
  Bookmark,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  X,
} from 'lucide-react';

interface StudioToolsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenAudioCutter: () => void;
  onOpenVideoExtractor: () => void;
  onOpenTagEditor: () => void;
  onOpenCarMode: () => void;
  onOpenEqualizer: () => void;
  onOpenBookmarks: () => void;
  lang: 'bn' | 'en';
}

export const StudioToolsModal: React.FC<StudioToolsModalProps> = ({
  isOpen,
  onClose,
  onOpenAudioCutter,
  onOpenVideoExtractor,
  onOpenTagEditor,
  onOpenCarMode,
  onOpenEqualizer,
  onOpenBookmarks,
  lang,
}) => {
  if (!isOpen) return null;

  const tools = [
    {
      id: 'cutter',
      icon: Scissors,
      titleBn: 'অডিও কাটার ও রিংটোন মেকার',
      titleEn: 'Audio Cutter & Ringtone Maker',
      descBn: 'যেকোনো গানের পছন্দের অংশ কেটে রিংটোন, অ্যালার্ম ও নোটিফিকেশন বানান',
      descEn: 'Trim audio waveforms and export ringtones, alarms, or notifications',
      color: 'from-purple-500/20 to-indigo-500/20 text-purple-300 border-purple-400/30',
      tag: 'NEW',
      onClick: () => {
        onClose();
        onOpenAudioCutter();
      },
    },
    {
      id: 'extractor',
      icon: Film,
      titleBn: 'ভিডিও থেকে অডিও কনভার্টার',
      titleEn: 'Video to MP3 Extractor',
      descBn: 'ভিডিও ফাইল থেকে সরাসরি হাই-কোয়ালিটি MP3 অডিও আলাদা করুন',
      descEn: 'Extract 320kbps MP3 audio soundtracks from your video files',
      color: 'from-cyan-500/20 to-blue-500/20 text-cyan-300 border-cyan-400/30',
      tag: 'PRO',
      onClick: () => {
        onClose();
        onOpenVideoExtractor();
      },
    },
    {
      id: 'tag',
      icon: Tag,
      titleBn: 'ট্যাগ ও কভার ফটো এডিটর',
      titleEn: 'ID3 Tag & Cover Art Editor',
      descBn: 'গানের ভুল নাম, শিল্পী বা অ্যালবামের নাম পরিবর্তন ও কভার ফটো সেট করুন',
      descEn: 'Edit song title, artist, album, genre, and custom album artwork',
      color: 'from-pink-500/20 to-rose-500/20 text-pink-300 border-pink-400/30',
      tag: 'STUDIO',
      onClick: () => {
        onClose();
        onOpenTagEditor();
      },
    },
    {
      id: 'car',
      icon: Car,
      titleBn: 'কার ড্রাইভিং মোড',
      titleEn: 'Car Driving Mode',
      descBn: 'গাড়ি চালানো বা হাঁটার সময় বড় বড় বাটন সহ নিরাপদ ডিসপ্লে',
      descEn: 'Distraction-free high-visibility interface with swipe gestures',
      color: 'from-amber-500/20 to-yellow-500/20 text-amber-300 border-amber-400/30',
      tag: 'OLED',
      onClick: () => {
        onClose();
        onOpenCarMode();
      },
    },
    {
      id: 'booster',
      icon: Zap,
      titleBn: '২০০% সুপার ভলিউম বুস্টার',
      titleEn: '200% Super Volume Booster',
      descBn: 'কম সাউন্ডের গান বা ভিডিওর জন্য দ্বিগুণ সাউন্ড বুস্ট সুবিধা',
      descEn: 'Drive audio gain up to 2.0x (+12dB) with speaker safe limiter',
      color: 'from-amber-500/20 to-red-500/20 text-amber-300 border-amber-400/30',
      tag: 'LOUD',
      onClick: () => {
        onClose();
        onOpenEqualizer();
      },
    },
    {
      id: 'eightd',
      icon: Headphones,
      titleBn: '৮ডি অডিও ও ৩৬০° সারাউন্ড',
      titleEn: '8D Spatial Audio & Orbit',
      descBn: 'হেডফোনে শুনলে গানটি চারপাশ থেকে ৩৬০ ডিগ্রিতে বৃত্তাকারে ঘুরবে',
      descEn: 'Immersive circular spatial rotation panning for headphones',
      color: 'from-cyan-500/20 to-teal-500/20 text-cyan-300 border-cyan-400/30',
      tag: '360°',
      onClick: () => {
        onClose();
        onOpenEqualizer();
      },
    },
    {
      id: 'bookmarks',
      icon: Bookmark,
      titleBn: 'স্মার্ট বুকমার্ক ও রিজিউম',
      titleEn: 'Smart Bookmarks & Resume',
      descBn: 'ওয়াজ, পডকাস্ট বা অডিওবুকের যেকোনো মুহূর্ত সেভ করে রাখুন',
      descEn: 'Save and jump to timestamps in long audiobooks or podcasts',
      color: 'from-emerald-500/20 to-teal-500/20 text-emerald-300 border-emerald-400/30',
      tag: 'SMART',
      onClick: () => {
        onClose();
        onOpenBookmarks();
      },
    },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-2xl animate-fade-in">
      <div className="relative w-full max-w-2xl glass-panel rounded-3xl p-5 sm:p-6 border border-white/20 bg-slate-900/90 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2.5 rounded-2xl bg-gradient-to-tr from-purple-600 to-cyan-500 text-white shadow-lg shadow-purple-500/30">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                <span>{lang === 'bn' ? 'প্রিমিয়াম স্টুডিও টুলস' : 'Premium Studio Tools'}</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 text-white font-mono font-bold">
                  ALL-IN-ONE
                </span>
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'আপনার প্রতিদিনের জন্য সেরা মিউজিক ও অডিও টুলবক্স' : 'Everyday essential audio & media utilities'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-2xl glass-card text-slate-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tools Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {tools.map((tool) => {
            const Icon = tool.icon;
            return (
              <button
                key={tool.id}
                onClick={tool.onClick}
                className="p-3.5 rounded-2xl glass-card border border-white/10 hover:border-purple-400/40 bg-gradient-to-br hover:scale-[1.01] transition text-left flex items-start gap-3 group active:scale-95 shadow-md"
              >
                <div
                  className={`p-2.5 rounded-2xl bg-gradient-to-br ${tool.color} border flex-shrink-0 mt-0.5 group-hover:scale-110 transition`}
                >
                  <Icon className="w-5 h-5" />
                </div>
                <div className="min-w-0 flex-1 space-y-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs sm:text-sm font-bold text-white group-hover:text-purple-200 transition">
                      {lang === 'bn' ? tool.titleBn : tool.titleEn}
                    </h4>
                    <span className="text-[9px] font-mono font-bold px-1.5 py-0.5 rounded bg-white/10 text-slate-300">
                      {tool.tag}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">
                    {lang === 'bn' ? tool.descBn : tool.descEn}
                  </p>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-white group-hover:translate-x-1 transition self-center flex-shrink-0" />
              </button>
            );
          })}
        </div>

        {/* Footer Note */}
        <div className="p-3 rounded-2xl bg-white/5 border border-white/10 text-center flex items-center justify-between text-xs text-slate-400">
          <span className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>{lang === 'bn' ? 'ফোনের মিডিয়া • ব্যক্তিগত' : 'Local media • Private'}</span>
          </span>
          <button
            onClick={onClose}
            className="px-3 py-1 rounded-xl glass-card text-white hover:bg-white/10 text-xs font-semibold"
          >
            {lang === 'bn' ? 'বন্ধ করুন' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
