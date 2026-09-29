import React, { useState, useMemo } from 'react';
import {
  Folder,
  FolderOpen,
  HardDrive,
  Music,
  Video,
  FileAudio,
  Play,
  ArrowLeft,
  Upload,
  Sparkles,
  ChevronRight,
  Database,
} from 'lucide-react';
import { Track } from '../types/media';

interface FoldersTabProps {
  tracks: Track[];
  onPlayTrack: (track: Track) => void;
  onOpenImport: () => void;
  lang: 'bn' | 'en';
}

export const FoldersTab: React.FC<FoldersTabProps> = ({
  tracks,
  onPlayTrack,
  onOpenImport,
  lang,
}) => {
  const [selectedFolder, setSelectedFolder] = useState<string | null>(null);

  // Group tracks by their assigned folder
  const folderMap = useMemo(() => {
    const map: { [folderName: string]: Track[] } = {};

    // Standard preset device folders
    const defaultFolders = [
      'Internal / Music / Chill',
      'Internal / Music / Electronic',
      'Internal / Music / Folk',
      'Internal / Music / Classical',
      'Internal / Music / Ambient',
      'Internal / Videos / Visuals',
      'Internal / Videos / Nature',
    ];

    defaultFolders.forEach((f) => {
      map[f] = [];
    });

    tracks.forEach((track) => {
      const folder = track.folder || 'Internal / Music / Imported';
      if (!map[folder]) map[folder] = [];
      map[folder].push(track);
    });

    return map;
  }, [tracks]);

  const folderNames = Object.keys(folderMap).filter(
    (name) => folderMap[name].length > 0
  );

  const totalOfflineSize = useMemo(() => {
    const totalBytes = tracks.reduce((acc, t) => acc + (t.size || 3500000), 0);
    return (totalBytes / (1024 * 1024)).toFixed(1);
  }, [tracks]);

  // If a folder is opened
  if (selectedFolder) {
    const tracksInFolder = folderMap[selectedFolder] || [];
    return (
      <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
        <button
          onClick={() => setSelectedFolder(null)}
          className="flex items-center gap-1.5 text-xs text-slate-300 hover:text-white glass-pill px-3 py-1.5 rounded-xl transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>{lang === 'bn' ? 'ফোল্ডার তালিকায় ফিরুন' : 'Back to Folders'}</span>
        </button>

        <div className="glass-card rounded-2xl p-4 flex items-center gap-3 border border-white/10">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-300">
            <FolderOpen className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">{selectedFolder}</h3>
            <p className="text-xs text-slate-400">
              {tracksInFolder.length} {lang === 'bn' ? 'টি ফাইল' : 'files found'}
            </p>
          </div>
        </div>

        <div className="space-y-2">
          {tracksInFolder.map((track) => (
            <div
              key={track.id}
              onClick={() => onPlayTrack(track)}
              className="glass-card glass-card-hover rounded-2xl p-2.5 flex items-center gap-3 cursor-pointer border border-white/10"
            >
              <div className="w-10 h-10 rounded-xl overflow-hidden bg-slate-800 flex-shrink-0">
                <img
                  src={track.coverUrl}
                  alt={track.title}
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="flex-1 min-w-0">
                <h4 className="text-sm font-semibold text-white truncate">
                  {track.title}
                </h4>
                <p className="text-xs text-slate-400 truncate">
                  {track.artist} • {track.format || 'AUDIO'}
                </p>
              </div>
              <div className="p-2 text-purple-300">
                <Play className="w-4 h-4 fill-purple-300" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
      {/* Storage Overview Card */}
      <div className="glass-card rounded-3xl p-5 border border-white/10 space-y-3 bg-gradient-to-br from-slate-900/60 to-purple-950/20">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-purple-500/20 border border-purple-400/30 flex items-center justify-center text-purple-300">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">
                {lang === 'bn' ? 'ডিভাইস ইন্টারনাল স্টোরেজ' : 'Device Storage'}
              </h3>
              <p className="text-xs text-slate-400 font-mono">
                {tracks.length} {lang === 'bn' ? 'টি অফলাইন মিডিয়া' : 'offline files'} ({totalOfflineSize} MB)
              </p>
            </div>
          </div>

          <button
            onClick={onOpenImport}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/30 hover:bg-purple-600/40 border border-purple-400/30 text-xs font-semibold text-purple-200 transition active:scale-95"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>{lang === 'bn' ? 'ফাইল বা ফোল্ডার স্ক্যান' : 'Scan Files'}</span>
          </button>
        </div>

        {/* Storage progress bar */}
        <div className="space-y-1">
          <div className="w-full h-2 rounded-full bg-white/10 overflow-hidden flex">
            <div className="h-full bg-purple-500" style={{ width: '45%' }} />
            <div className="h-full bg-cyan-400" style={{ width: '30%' }} />
          </div>
          <div className="flex items-center justify-between text-[11px] text-slate-400">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-purple-500" />
              {lang === 'bn' ? 'গান (অডিও)' : 'Audio Music'}
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-cyan-400" />
              {lang === 'bn' ? 'ভিডিও ফাইল' : 'Video Clips'}
            </span>
            <span>100% {lang === 'bn' ? 'অফলাইন' : 'Offline'}</span>
          </div>
        </div>
      </div>

      {/* Folders List */}
      <div className="space-y-2">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 px-1">
          {lang === 'bn' ? 'মিউজিক ফোল্ডারসমূহ' : 'Storage Directories'}
        </h4>

        {folderNames.map((folder) => {
          const count = folderMap[folder].length;
          const isVideoFolder = folder.toLowerCase().includes('video');

          return (
            <div
              key={folder}
              onClick={() => setSelectedFolder(folder)}
              className="glass-card glass-card-hover rounded-2xl p-3.5 flex items-center justify-between cursor-pointer border border-white/10 group"
            >
              <div className="flex items-center gap-3 min-w-0">
                <div
                  className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 ${
                    isVideoFolder
                      ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-400/30'
                      : 'bg-amber-500/20 text-amber-300 border border-amber-400/30'
                  }`}
                >
                  <Folder className="w-5 h-5 fill-current opacity-80" />
                </div>
                <div className="min-w-0">
                  <h4 className="text-sm font-semibold text-white truncate group-hover:text-purple-300 transition-colors">
                    {folder}
                  </h4>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {count} {lang === 'bn' ? 'টি মিডিয়া ফাইল' : 'media files'}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-shrink-0 text-slate-400 group-hover:text-white">
                <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition" />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
