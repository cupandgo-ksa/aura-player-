import React, { useState, useMemo } from 'react';
import {
  Video,
  Play,
  Headphones,
  Search,
  Plus,
  Clock,
  Sparkles,
  Layers,
  Heart,
  Folder,
  FolderOpen,
  ArrowLeft,
  ChevronRight,
  HardDrive,
  Film,
  MoreVertical,
  Check,
  FolderPlus,
} from 'lucide-react';
import { Track } from '../types/media';

interface VideoTabProps {
  tracks: Track[];
  onPlayVideo: (track: Track) => void;
  onPlayVideoAsAudio: (track: Track) => void;
  onToggleFavorite: (trackId: string) => void;
  onOpenImport: () => void;
  onUpdateTrackFolder?: (trackId: string, folderName: string) => void;
  lang: 'bn' | 'en';
}

export type VideoSearchScope = 'all' | 'title' | 'artist' | 'album';

export const VideoTab: React.FC<VideoTabProps> = ({
  tracks,
  onPlayVideo,
  onPlayVideoAsAudio,
  onToggleFavorite,
  onOpenImport,
  onUpdateTrackFolder,
  lang,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchScope, setSearchScope] = useState<VideoSearchScope>('all');
  const [viewMode, setViewMode] = useState<'folders' | 'all'>('folders');
  const [selectedFolder, setSelectedFolder] = useState<string | null>(null);
  const [activeMenuVideoId, setActiveMenuVideoId] = useState<string | null>(null);
  const [isCreatingFolder, setIsCreatingFolder] = useState<boolean>(false);
  const [newFolderName, setNewFolderName] = useState<string>('');

  const videoTracks = useMemo(() => {
    return tracks.filter((t) => t.type === 'video');
  }, [tracks]);

  // Group videos by folder with friendly defaults
  const folderMap = useMemo(() => {
    const map: { [folderName: string]: Track[] } = {};

    // Standard preset folders
    const standardFolders = [
      'Internal / Videos / Camera',
      'Internal / Videos / Downloads',
      'Internal / Videos / Visuals',
      'Internal / Videos / Nature',
      'Internal / Videos / Documentary',
    ];

    standardFolders.forEach((f) => {
      map[f] = [];
    });

    videoTracks.forEach((video) => {
      const folder = video.folder || 'Internal / Videos / Camera';
      if (!map[folder]) map[folder] = [];
      map[folder].push(video);
    });

    return map;
  }, [videoTracks]);

  const folderNames = Object.keys(folderMap).filter(
    (name) => folderMap[name].length > 0
  );

  const getCleanFolderName = (rawName: string) => {
    const parts = rawName.split('/');
    return parts[parts.length - 1].trim();
  };

  // Filtered videos based on search
  const filteredVideos = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();

    const pool = selectedFolder ? folderMap[selectedFolder] || [] : videoTracks;

    if (!q) return pool;

    return pool.filter((t) => {
      const matchTitle = t.title.toLowerCase().includes(q);
      const matchArtist = t.artist.toLowerCase().includes(q);
      const matchAlbum = Boolean(t.album && t.album.toLowerCase().includes(q));
      const matchGenre = Boolean(t.genre && t.genre.toLowerCase().includes(q));

      if (searchScope === 'title') return matchTitle;
      if (searchScope === 'artist') return matchArtist;
      if (searchScope === 'album') return matchAlbum;
      return matchTitle || matchArtist || matchAlbum || matchGenre;
    });
  }, [videoTracks, selectedFolder, folderMap, searchQuery, searchScope]);

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const formatSize = (bytes?: number) => {
    if (!bytes) return '4.2 MB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const getFolderTotalSize = (folderVideos: Track[]) => {
    const totalBytes = folderVideos.reduce(
      (acc, v) => acc + (v.size || 4200000),
      0
    );
    return (totalBytes / (1024 * 1024)).toFixed(1);
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4 animate-fade-in">
      {/* Top Search & Filter Bar */}
      <div className="glass-card rounded-3xl p-3 sm:p-4 border border-cyan-500/20 bg-slate-900/60 shadow-xl space-y-2.5 backdrop-blur-2xl">
        <div className="flex flex-col sm:flex-row gap-2.5">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cyan-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={
                searchScope === 'title'
                  ? lang === 'bn' ? 'ভিডিও নাম দিয়ে খুঁজুন...' : 'Search by video title...'
                  : searchScope === 'artist'
                  ? lang === 'bn' ? 'নির্মাতা/শিল্পীর নাম দিয়ে খুঁজুন...' : 'Search by artist or creator...'
                  : searchScope === 'album'
                  ? lang === 'bn' ? 'অ্যালবাম/সংকলন দিয়ে খুঁজুন...' : 'Search by collection/album...'
                  : lang === 'bn' ? 'ভিডিও নাম, শিল্পী বা অ্যালবাম খুঁজুন...' : 'Search title, artist, or album...'
              }
              className="w-full pl-10 pr-9 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-white placeholder-slate-400 text-sm focus:outline-none focus:border-cyan-400/50 transition backdrop-blur-xl"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-xs text-slate-400 hover:text-white bg-white/10 hover:bg-white/20 transition"
                title="Clear"
              >
                ✕
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* View Mode Toggle: Folders vs All Videos */}
            <div className="flex items-center bg-white/5 p-1 rounded-2xl border border-white/10">
              <button
                onClick={() => {
                  setViewMode('folders');
                  setSelectedFolder(null);
                }}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  viewMode === 'folders' && !selectedFolder
                    ? 'bg-cyan-500 text-slate-950 font-bold shadow-md'
                    : 'text-slate-300 hover:text-white'
                }`}
              >
                <Folder className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'ফোল্ডার ভিউ' : 'Folders'}</span>
              </button>
              <button
                onClick={() => {
                  setViewMode('all');
                  setSelectedFolder(null);
                }}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  viewMode === 'all'
                    ? 'bg-cyan-500 text-slate-950 font-bold shadow-md'
                    : 'text-slate-300 hover:text-white'
                }`}
              >
                <Film className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'সব ভিডিও' : 'All Videos'}</span>
              </button>
            </div>

            <button
              onClick={onOpenImport}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-cyan-600/30 hover:bg-cyan-600/40 border border-cyan-400/30 text-xs font-semibold text-cyan-200 transition active:scale-95 whitespace-nowrap justify-center"
            >
              <Plus className="w-4 h-4" />
              <span>{lang === 'bn' ? 'ভিডিও যোগ' : 'Import'}</span>
            </button>
          </div>
        </div>

        {/* Search Scope Filter Chips (All, Title, Artist, Album) */}
        <div className="flex items-center justify-between gap-2 pt-1 border-t border-white/5">
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
            <span className="text-[11px] text-slate-400 font-medium mr-1 hidden sm:inline">
              {lang === 'bn' ? 'ফিল্টার:' : 'Filter:'}
            </span>
            {(
              [
                { id: 'all', labelBn: 'সবগুলো', labelEn: 'All' },
                { id: 'title', labelBn: 'ভিডিও নাম', labelEn: 'Title' },
                { id: 'artist', labelBn: 'শিল্পী / চ্যানেল', labelEn: 'Artist' },
                { id: 'album', labelBn: 'অ্যালবাম', labelEn: 'Album' },
              ] as const
            ).map((scope) => {
              const isActive = searchScope === scope.id;
              return (
                <button
                  key={scope.id}
                  onClick={() => setSearchScope(scope.id)}
                  className={`px-2.5 py-1 rounded-xl text-xs font-medium transition active:scale-95 whitespace-nowrap ${
                    isActive
                      ? 'bg-cyan-600 text-white shadow-md border border-cyan-400/40'
                      : 'bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5'
                  }`}
                >
                  {lang === 'bn' ? scope.labelBn : scope.labelEn}
                </button>
              );
            })}
          </div>

          {searchQuery && (
            <div className="text-[11px] font-mono text-cyan-300 whitespace-nowrap">
              {filteredVideos.length} {lang === 'bn' ? 'টি ভিডিও' : 'found'}
            </div>
          )}
        </div>
      </div>

      {/* Breadcrumb Header when inside a specific folder */}
      {selectedFolder && (
        <div className="flex items-center justify-between p-3.5 rounded-3xl glass-card border border-cyan-500/30 bg-cyan-950/20 backdrop-blur-xl">
          <button
            onClick={() => setSelectedFolder(null)}
            className="flex items-center gap-2 text-xs font-bold text-cyan-300 hover:text-white glass-pill px-3 py-1.5 rounded-xl transition active:scale-95"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>{lang === 'bn' ? 'ফোল্ডার তালিকায় ফিরুন' : 'Back to Folders'}</span>
          </button>

          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-white truncate max-w-[200px] sm:max-w-none">
              📁 {getCleanFolderName(selectedFolder)}
            </span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-lg bg-cyan-500/20 text-cyan-300 border border-cyan-400/30">
              {filteredVideos.length} {lang === 'bn' ? 'ভিডিও' : 'videos'}
            </span>
          </div>

          {filteredVideos.length > 0 && (
            <button
              onClick={() => onPlayVideo(filteredVideos[0])}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition active:scale-95 shadow-md shadow-cyan-500/25"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>{lang === 'bn' ? 'সবগুলো চালান' : 'Play All'}</span>
            </button>
          )}
        </div>
      )}

      {/* MODE 1: FOLDER VIEW (Display list of folders with thumbnails) */}
      {viewMode === 'folders' && !selectedFolder ? (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-400 px-1">
            <span className="font-semibold uppercase tracking-wider flex items-center gap-1.5">
              <Folder className="w-4 h-4 text-cyan-400" />
              <span>{lang === 'bn' ? 'ডিভাইসের ভিডিও ফোল্ডারসমূহ' : 'Video Folders on Device'}</span>
              <span>({folderNames.length})</span>
            </span>
            <span className="text-[11px] text-cyan-300">
              {videoTracks.length} {lang === 'bn' ? 'টি মোট ভিডিও' : 'Total Videos'}
            </span>
          </div>

          {/* Folder Cards Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            {folderNames.map((folderPath) => {
              const videosInThisFolder = folderMap[folderPath] || [];
              const cleanName = getCleanFolderName(folderPath);
              const totalMb = getFolderTotalSize(videosInThisFolder);

              return (
                <div
                  key={folderPath}
                  onClick={() => setSelectedFolder(folderPath)}
                  className="glass-card glass-card-hover rounded-3xl p-4 border border-white/10 hover:border-cyan-400/40 cursor-pointer space-y-3 transition-all duration-300 group shadow-lg"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-2xl bg-cyan-500/20 border border-cyan-400/30 flex items-center justify-center text-cyan-300 group-hover:scale-105 transition">
                        <Folder className="w-6 h-6 fill-cyan-400/20" />
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-white group-hover:text-cyan-200 transition">
                          {cleanName}
                        </h4>
                        <p className="text-[11px] text-slate-400 font-mono">
                          {videosInThisFolder.length} {lang === 'bn' ? 'টি ভিডিও' : 'videos'} • {totalMb} MB
                        </p>
                      </div>
                    </div>

                    <div className="p-2 rounded-xl glass-card text-slate-400 group-hover:text-cyan-300 group-hover:translate-x-1 transition">
                      <ChevronRight className="w-4 h-4" />
                    </div>
                  </div>

                  {/* Horizontal Thumbnail Previews Strip */}
                  <div className="grid grid-cols-3 gap-2 pt-1">
                    {videosInThisFolder.slice(0, 3).map((v, idx) => (
                      <div
                        key={v.id + idx}
                        className="relative aspect-video rounded-xl overflow-hidden bg-slate-900 border border-white/10 group-hover:border-cyan-400/20 transition"
                      >
                        <img
                          src={v.coverUrl}
                          alt={v.title}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute bottom-1 right-1 px-1 py-0.2 rounded bg-black/70 text-[9px] font-mono text-slate-300">
                          {formatDuration(v.duration)}
                        </div>
                      </div>
                    ))}
                    {videosInThisFolder.length === 0 && (
                      <div className="col-span-3 py-3 text-center text-[11px] text-slate-500">
                        {lang === 'bn' ? 'ফোল্ডারটি খালি' : 'Empty folder'}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        /* MODE 2: VIDEO GRID (Inside a folder OR All Videos) */
        <div className="space-y-4">
          {/* Header Stats */}
          {!selectedFolder && (
            <div className="flex items-center justify-between text-xs text-slate-400 px-1">
              <span>
                {filteredVideos.length} {lang === 'bn' ? 'টি অফলাইন ভিডিও' : 'Offline Videos'}
              </span>
              <span className="flex items-center gap-1 text-[11px] text-cyan-300">
                <Sparkles className="w-3 h-3" />
                {lang === 'bn' ? 'এইচডি ও ৪কে সাপোর্ট' : 'HD & 4K Ready'}
              </span>
            </div>
          )}

          {/* Videos Grid */}
          {filteredVideos.length === 0 ? (
            <div className="glass-card rounded-3xl p-8 text-center space-y-3 border border-white/10">
              <Video className="w-12 h-12 mx-auto text-cyan-400/60" />
              <h3 className="text-base font-semibold text-white">
                {lang === 'bn' ? 'কোন ভিডিও পাওয়া যায়নি' : 'No videos found'}
              </h3>
              <p className="text-xs text-slate-400 max-w-sm mx-auto">
                {lang === 'bn'
                  ? 'আপনার মেমোরি কার্ড বা ফোন থেকে ভিডিও ইমপোর্ট করতে "ভিডিও যোগ করুন" বাটনে চাপুন।'
                  : 'Click "Import Video" to add offline MP4, MKV, or WebM clips from your device.'}
              </p>
              <button
                onClick={onOpenImport}
                className="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-cyan-600 text-white text-xs font-semibold shadow-lg shadow-cyan-600/30"
              >
                <Plus className="w-4 h-4" />
                {lang === 'bn' ? 'ডিভাইস থেকে ভিডিও আনুন' : 'Import Video'}
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {filteredVideos.map((video) => (
                <div
                  key={video.id}
                  className="glass-card glass-card-hover rounded-3xl overflow-hidden border border-white/10 hover:border-cyan-400/30 group flex flex-col justify-between transition-all duration-300 shadow-xl"
                >
                  {/* Thumbnail Container */}
                  <div
                    onClick={() => onPlayVideo(video)}
                    className="relative aspect-video w-full bg-slate-900 cursor-pointer overflow-hidden"
                  >
                    <img
                      src={video.coverUrl}
                      alt={video.title}
                      className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />

                    {/* Dark Vignette Overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

                    {/* Duration Badge */}
                    <div className="absolute bottom-2.5 right-2.5 px-2 py-0.5 rounded-lg bg-black/75 backdrop-blur-md text-[11px] font-mono text-white flex items-center gap-1 border border-white/10">
                      <Clock className="w-3 h-3 text-cyan-300" />
                      <span>{formatDuration(video.duration)}</span>
                    </div>

                    {/* Folder Badge */}
                    <div className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-lg bg-black/75 backdrop-blur-md text-[10px] text-cyan-300 flex items-center gap-1 border border-white/10">
                      <Folder className="w-3 h-3" />
                      <span>{getCleanFolderName(video.folder || 'Camera')}</span>
                    </div>

                    {/* Format Badge */}
                    <div className="absolute top-2.5 right-2.5 px-2 py-0.5 rounded-lg bg-cyan-500/25 border border-cyan-400/30 backdrop-blur-md text-[10px] font-mono text-cyan-200">
                      {video.format || 'MP4'}
                    </div>

                    {/* Center Play Button Overlay on hover */}
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-black/30 backdrop-blur-[1px]">
                      <div className="w-12 h-12 rounded-full bg-cyan-500 text-slate-950 flex items-center justify-center shadow-lg shadow-cyan-500/50 scale-90 group-hover:scale-100 transition-transform">
                        <Play className="w-6 h-6 fill-current ml-0.5" />
                      </div>
                    </div>
                  </div>

                  {/* Metadata and Quick Actions */}
                  <div className="p-3.5 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <h4
                          onClick={() => onPlayVideo(video)}
                          className="text-xs sm:text-sm font-bold text-white truncate hover:text-cyan-300 cursor-pointer transition"
                        >
                          {video.title}
                        </h4>
                        <p className="text-[11px] text-slate-400 truncate mt-0.5">
                          {video.artist} • {video.genre || 'Video'} • {formatSize(video.size)}
                        </p>
                      </div>

                      {/* Favorite Button */}
                      <button
                        onClick={() => onToggleFavorite(video.id)}
                        className={`p-2 rounded-xl transition active:scale-90 flex-shrink-0 ${
                          video.isFavorite
                            ? 'text-rose-500'
                            : 'text-slate-400 hover:text-rose-400'
                        }`}
                        title="Favorite"
                      >
                        <Heart
                          className={`w-4 h-4 ${
                            video.isFavorite ? 'fill-rose-500' : ''
                          }`}
                        />
                      </button>
                    </div>

                    {/* Actions: Play in Custom Window / Background Audio */}
                    <div className="flex items-center gap-2 pt-1 border-t border-white/5">
                      <button
                        onClick={() => onPlayVideo(video)}
                        className="flex-1 py-2 rounded-xl bg-cyan-600/25 hover:bg-cyan-600/35 border border-cyan-400/30 text-cyan-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-95"
                      >
                        <Play className="w-3.5 h-3.5 fill-current" />
                        <span>{lang === 'bn' ? 'ভিডিও দেখুন' : 'Watch Video'}</span>
                      </button>

                      <button
                        onClick={() => onPlayVideoAsAudio(video)}
                        className="flex items-center gap-1.5 px-3 py-2 rounded-xl glass-card hover:bg-white/10 text-slate-300 text-xs font-semibold transition active:scale-95"
                        title={
                          lang === 'bn'
                            ? 'স্ক্রিন অফ রেখে ব্যাকগ্রাউন্ডে অডিও শুনুন'
                            : 'Listen to audio in background'
                        }
                      >
                        <Headphones className="w-3.5 h-3.5 text-cyan-300" />
                        <span className="hidden xs:inline">
                          {lang === 'bn' ? 'অডিও' : 'Audio'}
                        </span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
