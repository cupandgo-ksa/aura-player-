import React, { useMemo, useState } from 'react';
import { Folder, FolderOpen, ArrowLeft, Play, Plus } from 'lucide-react';
import { Track } from '../types/media';

interface FoldersTabProps {
  tracks: Track[];
  onPlayTrack: (track: Track) => void;
  onOpenImport: () => void;
  lang: 'bn' | 'en';
}

export const FoldersTab: React.FC<FoldersTabProps> = ({ tracks, onPlayTrack, onOpenImport, lang }) => {
  const [selectedFolder, setSelectedFolder] = useState<string | null>(null);
  const folders = useMemo(() => {
    const grouped: Record<string, Track[]> = {};
    tracks.filter((track) => track.type === 'audio').forEach((track) => {
      const folder = track.folder || (lang === 'bn' ? 'ফোনের গান' : 'Phone audio');
      (grouped[folder] ??= []).push(track);
    });
    return grouped;
  }, [tracks, lang]);
  const names = Object.keys(folders).sort((a, b) => a.localeCompare(b));

  return (
    <div className="pb-36 pt-4 px-4 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        {selectedFolder ? (
          <button onClick={() => setSelectedFolder(null)} className="flex items-center gap-2 text-sm text-slate-200">
            <ArrowLeft className="w-4 h-4" /> {lang === 'bn' ? 'ফোল্ডারে ফিরুন' : 'All folders'}
          </button>
        ) : <h2 className="text-lg font-semibold text-white">{lang === 'bn' ? 'গানের ফোল্ডার' : 'Music folders'}</h2>}
        <button onClick={onOpenImport} className="p-2 rounded-xl glass-card" aria-label={lang === 'bn' ? 'গান যোগ করুন' : 'Add audio'}><Plus className="w-5 h-5" /></button>
      </div>
      {selectedFolder ? (
        <div className="space-y-2">
          <div className="glass-card rounded-2xl p-4 flex items-center gap-3">
            <FolderOpen className="w-6 h-6 text-purple-300" />
            <div className="min-w-0"><h3 className="truncate font-semibold">{selectedFolder}</h3><p className="text-xs text-slate-400">{folders[selectedFolder]?.length || 0} {lang === 'bn' ? 'টি গান' : 'songs'}</p></div>
          </div>
          {(folders[selectedFolder] || []).map((track) => (
            <button key={track.id} onClick={() => onPlayTrack(track)} className="w-full glass-card rounded-2xl p-3 flex items-center gap-3 text-left">
              <img src={track.coverUrl} alt="" className="w-12 h-12 rounded-xl object-cover" />
              <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{track.title}</span><span className="block truncate text-xs text-slate-400">{track.artist}</span></span>
              <Play className="w-4 h-4 text-purple-300" />
            </button>
          ))}
        </div>
      ) : names.length ? (
        <div className="space-y-2">{names.map((name) => (
          <button key={name} onClick={() => setSelectedFolder(name)} className="w-full glass-card rounded-2xl p-4 flex items-center gap-3 text-left">
            <Folder className="w-6 h-6 text-amber-300 shrink-0" />
            <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{name}</span><span className="text-xs text-slate-400">{folders[name].length} {lang === 'bn' ? 'টি গান' : 'songs'}</span></span>
          </button>
        ))}</div>
      ) : (
        <div className="glass-card rounded-2xl p-8 text-center text-sm text-slate-400">
          {lang === 'bn' ? 'এখনো কোনো অডিও ফোল্ডার নেই।' : 'No audio folders yet.'}
        </div>
      )}
    </div>
  );
};
