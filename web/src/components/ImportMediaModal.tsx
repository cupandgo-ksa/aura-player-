import React, { useRef, useState } from 'react';
import {
  X,
  Upload,
  Music,
  Video,
  FolderOpen,
  CheckCircle,
  AlertCircle,
  FileCheck,
} from 'lucide-react';
import { Track } from '../types/media';
import { createGlassAlbumArt } from '../services/mediaSynthesizer';

interface ImportMediaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportTracks: (newTracks: Track[]) => void;
  lang: 'bn' | 'en';
}

export const ImportMediaModal: React.FC<ImportMediaModalProps> = ({
  isOpen,
  onClose,
  onImportTracks,
  lang,
}) => {
  const audioInputRef = useRef<HTMLInputElement | null>(null);
  const videoInputRef = useRef<HTMLInputElement | null>(null);
  const folderInputRef = useRef<HTMLInputElement | null>(null);

  const [isProcessing, setIsProcessing] = useState(false);
  const [successCount, setSuccessCount] = useState<number | null>(null);

  if (!isOpen) return null;

  const handleFilesSelected = async (
    files: FileList | null,
    forcedType?: 'audio' | 'video'
  ) => {
    if (!files || files.length === 0) return;
    setIsProcessing(true);

    const imported: Track[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const isVideo =
        forcedType === 'video' ||
        file.type.startsWith('video') ||
        /\.(mp4|mkv|webm|mov|avi)$/i.test(file.name);

      const type = isVideo ? 'video' : 'audio';

      // Clean display title
      const cleanTitle = file.name.replace(/\.[^/.]+$/, '').replace(/_/g, ' ');
      const format = file.name.split('.').pop()?.toUpperCase() || (isVideo ? 'MP4' : 'MP3');

      // Relative path / folder
      const webkitPath = (file as any).webkitRelativePath;
      const folderName = webkitPath
        ? `Internal / ${webkitPath.split('/')[0]}`
        : isVideo
        ? 'Internal / Videos / Imported'
        : 'Internal / Music / Device Storage';

      const blobUrl = URL.createObjectURL(file);

      // Extract duration via temp element
      let duration = 180; // default estimate
      try {
        const mediaElem = document.createElement(isVideo ? 'video' : 'audio');
        mediaElem.preload = 'metadata';
        mediaElem.src = blobUrl;
        await new Promise((resolve) => {
          mediaElem.onloadedmetadata = () => {
            if (mediaElem.duration && !isNaN(mediaElem.duration)) {
              duration = Math.round(mediaElem.duration);
            }
            resolve(true);
          };
          mediaElem.onerror = () => resolve(false);
          setTimeout(resolve, 800); // safety timeout
        });
      } catch {
        // fallback to default duration
      }

      const colors = isVideo
        ? ['#0e7490', '#6366f1']
        : ['#7c3aed', '#ec4899'];

      const newTrack: Track = {
        id: `imported-${Date.now()}-${i}-${Math.random().toString(36).substr(2, 5)}`,
        title: cleanTitle,
        artist: 'Local Device Media',
        album: 'Device Storage',
        duration,
        url: blobUrl,
        coverUrl: createGlassAlbumArt(
          cleanTitle.slice(0, 12).toUpperCase(),
          colors[0],
          colors[1],
          isVideo ? 'video' : 'disc'
        ),
        type,
        size: file.size,
        format,
        addedAt: Date.now(),
        isFavorite: false,
        folder: folderName,
        genre: isVideo ? 'Local Video' : 'Device Audio',
        blob: file,
      };

      imported.push(newTrack);
    }

    if (imported.length > 0) {
      onImportTracks(imported);
      setSuccessCount(imported.length);
      setTimeout(() => {
        setIsProcessing(false);
        setSuccessCount(null);
        onClose();
      }, 1200);
    } else {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl animate-fade-in">
      <div className="w-full max-w-md rounded-3xl glass-panel p-6 border border-white/20 shadow-2xl space-y-4 bg-slate-900/90">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-cyan-500/20 border border-cyan-500/40 flex items-center justify-center text-cyan-300">
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {lang === 'bn' ? 'অফলাইন মিডিয়া যোগ করুন' : 'Import Offline Media'}
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'আপনার মেমোরি কার্ড বা ফোন থেকে ফাইল আনুন' : 'Select files or folders from storage'}
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

        {/* Hidden inputs */}
        <input
          ref={audioInputRef}
          type="file"
          accept="audio/*,.mp3,.wav,.ogg,.m4a,.flac,.aac"
          multiple
          className="hidden"
          onChange={(e) => handleFilesSelected(e.target.files, 'audio')}
        />
        <input
          ref={videoInputRef}
          type="file"
          accept="video/*,.mp4,.mkv,.webm,.mov"
          multiple
          className="hidden"
          onChange={(e) => handleFilesSelected(e.target.files, 'video')}
        />
        <input
          ref={folderInputRef}
          type="file"
          // @ts-ignore
          webkitdirectory=""
          directory=""
          multiple
          className="hidden"
          onChange={(e) => handleFilesSelected(e.target.files)}
        />

        {/* Feedback state */}
        {isProcessing && (
          <div className="glass-card rounded-2xl p-4 text-center space-y-2 border border-purple-400/40 bg-purple-950/20">
            <div className="w-8 h-8 rounded-full border-2 border-purple-400 border-t-transparent animate-spin mx-auto" />
            <p className="text-xs font-semibold text-purple-200">
              {lang === 'bn' ? 'ফাইল প্রসেস ও ক্যাশ করা হচ্ছে...' : 'Processing & caching offline files...'}
            </p>
          </div>
        )}

        {successCount !== null && (
          <div className="glass-card rounded-2xl p-4 text-center space-y-1 border border-emerald-400/40 bg-emerald-950/20">
            <CheckCircle className="w-6 h-6 text-emerald-400 mx-auto" />
            <p className="text-xs font-bold text-emerald-200">
              {successCount} {lang === 'bn' ? 'টি ফাইল সফলভাবে যোগ হয়েছে!' : 'Files imported successfully!'}
            </p>
          </div>
        )}

        {/* Selection buttons */}
        <div className="space-y-2.5">
          {/* Audio import */}
          <button
            onClick={() => audioInputRef.current?.click()}
            disabled={isProcessing}
            className="w-full p-3.5 rounded-2xl glass-card glass-card-hover border border-white/10 flex items-center gap-3 text-left transition"
          >
            <div className="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-300 flex items-center justify-center flex-shrink-0">
              <Music className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-semibold text-white">
                {lang === 'bn' ? 'অডিও বা গান যোগ করুন' : 'Import Audio Tracks'}
              </h4>
              <p className="text-xs text-slate-400 truncate">
                MP3, WAV, FLAC, M4A, AAC, OGG
              </p>
            </div>
          </button>

          {/* Video import */}
          <button
            onClick={() => videoInputRef.current?.click()}
            disabled={isProcessing}
            className="w-full p-3.5 rounded-2xl glass-card glass-card-hover border border-white/10 flex items-center gap-3 text-left transition"
          >
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-cyan-300 flex items-center justify-center flex-shrink-0">
              <Video className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-semibold text-white">
                {lang === 'bn' ? 'ভিডিও ফাইল যোগ করুন' : 'Import Video Files'}
              </h4>
              <p className="text-xs text-slate-400 truncate">
                MP4, MKV, WebM, 1080p, 4K
              </p>
            </div>
          </button>

          {/* Entire Folder import */}
          <button
            onClick={() => folderInputRef.current?.click()}
            disabled={isProcessing}
            className="w-full p-3.5 rounded-2xl glass-card glass-card-hover border border-white/10 flex items-center gap-3 text-left transition"
          >
            <div className="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-300 flex items-center justify-center flex-shrink-0">
              <FolderOpen className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-semibold text-white">
                {lang === 'bn' ? 'সম্পূর্ণ ফোল্ডার স্ক্যান করুন' : 'Scan Entire Folder'}
              </h4>
              <p className="text-xs text-slate-400 truncate">
                {lang === 'bn'
                  ? 'একসাথে সব গান ও ভিডিও লোড করুন'
                  : 'Batch import folder containing media'}
              </p>
            </div>
          </button>
        </div>

        <p className="text-[11px] text-slate-400 text-center leading-relaxed pt-1">
          {lang === 'bn'
            ? '🔒 সকল মিডিয়া সম্পূর্ণ আপনার ডিভাইসের অফলাইন স্টোরেজে থাকে। কোনো ডেটা সার্ভারে যায় না।'
            : '🔒 All media is kept 100% locally on your device storage. Zero external uploads.'}
        </p>
      </div>
    </div>
  );
};
