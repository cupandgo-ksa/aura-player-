import React from 'react';
import { Music, Video, ListMusic, FolderOpen, Sliders } from 'lucide-react';
import { MainTab } from '../types/media';

interface GlassNavbarProps {
  currentTab: MainTab;
  onSelectTab: (tab: MainTab) => void;
  lang: 'bn' | 'en';
}

export const GlassNavbar: React.FC<GlassNavbarProps> = ({
  currentTab,
  onSelectTab,
  lang,
}) => {
  const tabs = [
    {
      id: 'music' as MainTab,
      labelBn: 'গান',
      labelEn: 'Music',
      icon: Music,
    },
    {
      id: 'video' as MainTab,
      labelBn: 'ভিডিও',
      labelEn: 'Video',
      icon: Video,
    },
    {
      id: 'playlists' as MainTab,
      labelBn: 'প্লেলিস্ট',
      labelEn: 'Playlists',
      icon: ListMusic,
    },
    {
      id: 'folders' as MainTab,
      labelBn: 'ফোল্ডার',
      labelEn: 'Folders',
      icon: FolderOpen,
    },
    {
      id: 'equalizer' as MainTab,
      labelBn: 'ইকুয়ালাইজার',
      labelEn: 'EQ',
      icon: Sliders,
    },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 px-2 pb-safe pt-1 glass-panel border-x-0 border-b-0 border-t border-white/10 backdrop-blur-2xl">
      <div className="max-w-lg mx-auto flex items-center justify-around py-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onSelectTab(tab.id)}
              className={`relative flex flex-col items-center justify-center py-1.5 px-2 rounded-2xl transition-all duration-200 active:scale-95 ${
                isActive
                  ? 'text-white'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-white/5'
              }`}
            >
              {isActive && (
                <div className="absolute inset-0 rounded-2xl bg-purple-500/20 border border-purple-400/30 shadow-[0_0_15px_rgba(168,85,247,0.35)] -z-10 animate-fade-in" />
              )}
              <Icon
                className={`w-5 h-5 transition-transform duration-200 ${
                  isActive ? 'scale-110 text-purple-300' : ''
                }`}
              />
              <span
                className={`text-[10px] font-medium mt-0.5 tracking-tight transition-colors ${
                  isActive ? 'text-purple-200 font-semibold' : ''
                }`}
              >
                {lang === 'bn' ? tab.labelBn : tab.labelEn}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
