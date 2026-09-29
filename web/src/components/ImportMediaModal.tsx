import React, { useEffect, useRef, useState } from 'react';
import { X, Music, FolderOpen } from 'lucide-react';
import { Track } from '../types/media';
import { createGlassAlbumArt } from '../services/mediaSynthesizer';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onImportTracks: (tracks: Track[]) => void;
  lang: 'bn' | 'en';
}
export const ImportMediaModal: React.FC<Props> = ({ isOpen, onClose, onImportTracks, lang }) => {
  const audioInput = useRef<HTMLInputElement>(null);
  const folderInput = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!isOpen) return;
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose(); };
    window.addEventListener('keydown', close);
    return () => window.removeEventListener('keydown', close);
  }, [isOpen, onClose]);
  if (!isOpen) return null;
  const importFiles = (files: FileList | null) => {
    if (!files || busy) return;
    setBusy(true);
    const selected = Array.from(files).filter(file =>
      file.type.startsWith('audio/') || /\.(mp3|wav|ogg|m4a|flac|aac|opus)$/i.test(file.name));
    const tracks: Track[] = selected.map((file, index) => {
      const title = file.name.replace(/\.[^.]+$/, '').replace(/_/g, ' ');
      const relative = (file as File & { webkitRelativePath?: string }).webkitRelativePath;
      return {
        id: `imported-${Date.now()}-${index}-${Math.random().toString(36).slice(2)}`,
        title, artist: 'Local media', album: '', duration: 0,
        url: URL.createObjectURL(file),
        coverUrl: createGlassAlbumArt(title.slice(0, 12), '#7c3aed', '#6bb9cf', 'disc'),
        type: 'audio', size: file.size, format: file.name.split('.').pop()?.toUpperCase() || 'AUDIO',
        addedAt: Date.now(), isFavorite: false,
        folder: relative ? relative.split('/').slice(0, -1).join('/') : 'Added files',
        blob: file,
      };
    });
    if (tracks.length) onImportTracks(tracks);
    setBusy(false);
    onClose();
  };
  return <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-xl" onPointerDown={e => { if (e.target === e.currentTarget) onClose(); }}>
    <div className="w-full max-w-sm rounded-3xl border border-white/20 bg-[#202637]/95 p-5 shadow-2xl">
      <div className="flex items-center justify-between mb-5"><h3 className="text-lg font-semibold text-white">{lang === 'bn' ? 'গান যোগ করুন' : 'Add audio'}</h3><button aria-label="Close" onClick={onClose} className="p-2 rounded-xl bg-white/10 text-white"><X size={18}/></button></div>
      <input ref={audioInput} type="file" accept="audio/*,.mp3,.wav,.ogg,.m4a,.flac,.aac,.opus" multiple className="hidden" onChange={e => importFiles(e.target.files)}/>
      <input ref={folderInput} type="file" accept="audio/*,.mp3,.wav,.ogg,.m4a,.flac,.aac,.opus" {...({ webkitdirectory: '' } as any)} multiple className="hidden" onChange={e => importFiles(e.target.files)}/>
      <button disabled={busy} onClick={() => audioInput.current?.click()} className="w-full flex items-center gap-3 rounded-2xl border border-white/15 bg-white/10 p-4 text-left text-white mb-3"><Music size={22}/><span>{lang === 'bn' ? 'অডিও ফাইল বাছুন' : 'Choose audio files'}</span></button>
      <button disabled={busy} onClick={() => folderInput.current?.click()} className="w-full flex items-center gap-3 rounded-2xl border border-white/15 bg-white/10 p-4 text-left text-white"><FolderOpen size={22}/><span>{lang === 'bn' ? 'গানের ফোল্ডার বাছুন' : 'Choose music folder'}</span></button>
    </div>
  </div>;
};
