import React, { useEffect, useState, useCallback, useRef } from 'react';
import { Track, Playlist, MainTab, RepeatMode } from './types/media';
import { AudioEngine } from './services/audioEngine';
import { phoneMedia } from './services/native';
import {
  getAllTracks,
  saveTrack,
  saveMultipleTracks,
  getAllPlaylists,
  savePlaylist,
  deletePlaylist as dbDeletePlaylist,
  getSetting,
  saveSetting,
} from './services/db';
import { getInitialTracks, getInitialPlaylists } from './data/defaultTracks';
import { getInitialVideos } from './data/defaultVideos';

import { GlassBackground } from './components/GlassBackground';
import { GlassHeader } from './components/GlassHeader';
import { GlassNavbar } from './components/GlassNavbar';
import { MiniPlayerBar } from './components/MiniPlayerBar';
import { AudioTab } from './components/AudioTab';
import { VideoTab } from './components/VideoTab';
import { PlaylistsTab } from './components/PlaylistsTab';
import { FoldersTab } from './components/FoldersTab';
import { EqualizerTab } from './components/EqualizerTab';
import { NowPlayingModal } from './components/NowPlayingModal';
import { VideoPlayerModal } from './components/VideoPlayerModal';
import { SleepTimerModal } from './components/SleepTimerModal';
import { CreatePlaylistModal } from './components/CreatePlaylistModal';
import { ImportMediaModal } from './components/ImportMediaModal';
import { DownloadGuideModal } from './components/DownloadGuideModal';
import { StudioToolsModal } from './components/StudioToolsModal';
import { AudioCutterModal } from './components/AudioCutterModal';
import { VideoToAudioModal } from './components/VideoToAudioModal';
import { TagEditorModal } from './components/TagEditorModal';
import { CarModeModal } from './components/CarModeModal';
import { BookmarksModal } from './components/BookmarksModal';

export const App: React.FC = () => {
  const audioEngine = AudioEngine.getInstance();

  // Core State
  const [tracks, setTracks] = useState<Track[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [currentTab, setCurrentTab] = useState<MainTab>('music');
  const [lang, setLang] = useState<'bn' | 'en'>('bn');

  // Player State
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [volume, setVolume] = useState<number>(0.85);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [playbackRate, setPlaybackRate] = useState<number>(1.0);
  const [isShuffle, setIsShuffle] = useState<boolean>(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('all');
  const [queue, setQueue] = useState<Track[]>([]);

  // Modals
  const [isNowPlayingOpen, setIsNowPlayingOpen] = useState(false);
  const [isVideoPlayerOpen, setIsVideoPlayerOpen] = useState(false);
  const [activeVideoTrack, setActiveVideoTrack] = useState<Track | null>(null);
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false);
  const [isCreatePlaylistOpen, setIsCreatePlaylistOpen] = useState(false);
  const [isImportMediaOpen, setIsImportMediaOpen] = useState(false);
  const [isDownloadGuideOpen, setIsDownloadGuideOpen] = useState(false);

  // Premium Studio Tools Modals
  const [isStudioToolsOpen, setIsStudioToolsOpen] = useState(false);
  const [isAudioCutterOpen, setIsAudioCutterOpen] = useState(false);
  const [activeCutterTrack, setActiveCutterTrack] = useState<Track | null>(null);
  const [isVideoExtractorOpen, setIsVideoExtractorOpen] = useState(false);
  const [isTagEditorOpen, setIsTagEditorOpen] = useState(false);
  const [activeTagTrack, setActiveTagTrack] = useState<Track | null>(null);
  const [isCarModeOpen, setIsCarModeOpen] = useState(false);
  const [isBookmarksOpen, setIsBookmarksOpen] = useState(false);

  // Sleep Timer
  const [sleepTimerRemaining, setSleepTimerRemaining] = useState<number>(0);
  const sleepTimerRef = useRef<any>(null);

  // Refs for current values inside callbacks
  const currentTrackRef = useRef(currentTrack);
  currentTrackRef.current = currentTrack;
  const queueRef = useRef(queue);
  queueRef.current = queue;
  const repeatModeRef = useRef(repeatMode);
  repeatModeRef.current = repeatMode;
  const isShuffleRef = useRef(isShuffle);
  isShuffleRef.current = isShuffle;

  // Initialize data on mount
  useEffect(() => {
    async function initData() {
      // 1. Load language preference
      const savedLang = await getSetting<'bn' | 'en'>('lang', 'bn');
      setLang(savedLang);

      // 2. Load stored tracks or seed initial tracks
      const storedTracks = await getAllTracks();
      if (window.AuraNative) {
        const found = phoneMedia().map((item) => ({ ...item,
          isFavorite: storedTracks.find((old) => old.id === item.id)?.isFavorite || false }));
        setTracks(found);
        setQueue(found.filter((t) => t.type === 'audio'));
        const first = found.find((t) => t.type === 'audio');
        if (first) { setCurrentTrack(first); audioEngine.setTrack(first, false); }
      } else if (storedTracks.length === 0) {
        const initialAudio = getInitialTracks();
        const initialVideo = getInitialVideos();
        const combined = [...initialAudio, ...initialVideo];
        await saveMultipleTracks(combined);
        setTracks(combined);
        setQueue(combined.filter((t) => t.type === 'audio'));
        if (combined.length > 0) {
          setCurrentTrack(combined[0]);
          audioEngine.setTrack(combined[0], false);
        }
      } else {
        setTracks(storedTracks);
        const audioList = storedTracks.filter((t) => t.type === 'audio');
        setQueue(audioList.length > 0 ? audioList : storedTracks);
        if (storedTracks.length > 0) {
          setCurrentTrack(storedTracks[0]);
          audioEngine.setTrack(storedTracks[0], false);
        }
      }

      // 3. Load playlists or seed initial playlists
      const storedPlaylists = await getAllPlaylists();
      if (storedPlaylists.length === 0) {
        const initPlaylists = window.AuraNative
          ? [{ id: 'playlist-favorites', name: 'প্রিয় গান (Favorites)', trackIds: [], createdAt: Date.now(), isSystem: true }]
          : getInitialPlaylists();
        for (const pl of initPlaylists) {
          await savePlaylist(pl);
        }
        setPlaylists(initPlaylists);
      } else {
        setPlaylists(storedPlaylists);
      }
    }

    initData();
  }, []);

  useEffect(() => {
    if (!window.AuraNative) return;
    const refresh = () => {
      const found = phoneMedia();
      setTracks((previous) => {
        const updated = found.map((item) => ({ ...item,
          isFavorite: previous.find((old) => old.id === item.id)?.isFavorite || false }));
        setQueue(updated.filter((t) => t.type === 'audio'));
        return updated;
      });
    };
    window.addEventListener('auraLibraryChanged', refresh);
    return () => window.removeEventListener('auraLibraryChanged', refresh);
  }, []);

  useEffect(() => {
    if (!window.AuraNative) return;
    const changed = (event: Event) => {
      const track = tracks.find((item) => item.url === (event as CustomEvent<string>).detail);
      if (track) { setCurrentTrack(track); audioEngine.setTrack(track, false); }
    };
    window.addEventListener('auraNativeTrack', changed);
    return () => window.removeEventListener('auraNativeTrack', changed);
  }, [tracks]);

  // Set up Audio Engine callbacks & MediaSession
  useEffect(() => {
    audioEngine.setCallbacks(
      () => {
        // Track ended handler
        handleTrackEnded();
      },
      (time, dur) => {
        setCurrentTime(time);
        setDuration(dur);
      },
      (playing) => {
        setIsPlaying(playing);
      }
    );

    audioEngine.setupMediaSessionHandlers({
      onPlay: () => handleTogglePlay(),
      onPause: () => handleTogglePlay(),
      onPrevious: () => handlePreviousTrack(),
      onNext: () => handleNextTrack(),
      onSeek: (seconds) => audioEngine.seek(seconds),
    });
  }, []);

  // Sleep Timer Interval countdown
  useEffect(() => {
    if (sleepTimerRemaining > 0) {
      sleepTimerRef.current = setInterval(() => {
        setSleepTimerRemaining((prev) => {
          if (prev <= 1) {
            audioEngine.pause();
            setIsPlaying(false);
            if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
    }

    return () => {
      if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
    };
  }, [sleepTimerRemaining]);

  // Audio Playback Actions
  const handlePlayTrack = useCallback((track: Track) => {
    setCurrentTrack(track);
    audioEngine.setTrack(track, true);
    setIsPlaying(true);
  }, []);

  const handleTogglePlay = useCallback(() => {
    if (!currentTrack) {
      if (queue.length > 0) {
        handlePlayTrack(queue[0]);
      }
      return;
    }
    audioEngine.togglePlay();
  }, [currentTrack, queue, handlePlayTrack]);

  const handleNextTrack = useCallback(() => {
    const cur = currentTrackRef.current;
    const q = queueRef.current;
    if (q.length === 0) return;

    if (isShuffleRef.current) {
      const randomIndex = Math.floor(Math.random() * q.length);
      handlePlayTrack(q[randomIndex]);
      return;
    }

    const currentIndex = q.findIndex((t) => t.id === cur?.id);
    const nextIndex = (currentIndex + 1) % q.length;
    handlePlayTrack(q[nextIndex]);
  }, [handlePlayTrack]);

  const handlePreviousTrack = useCallback(() => {
    const cur = currentTrackRef.current;
    const q = queueRef.current;
    if (q.length === 0) return;

    // If more than 3 seconds in, restart track
    if (currentTime > 3) {
      audioEngine.seek(0);
      return;
    }

    const currentIndex = q.findIndex((t) => t.id === cur?.id);
    const prevIndex = (currentIndex - 1 + q.length) % q.length;
    handlePlayTrack(q[prevIndex]);
  }, [currentTime, handlePlayTrack]);

  const handleTrackEnded = useCallback(() => {
    const mode = repeatModeRef.current;
    if (mode === 'one') {
      audioEngine.seek(0);
      audioEngine.play();
    } else if (mode === 'all') {
      handleNextTrack();
    } else {
      // Off
      const cur = currentTrackRef.current;
      const q = queueRef.current;
      const currentIndex = q.findIndex((t) => t.id === cur?.id);
      if (currentIndex < q.length - 1) {
        handleNextTrack();
      } else {
        setIsPlaying(false);
      }
    }
  }, [handleNextTrack]);

  const handleSeek = (seconds: number) => {
    audioEngine.seek(seconds);
    setCurrentTime(seconds);
  };

  const handleSetVolume = (vol: number) => {
    setVolume(vol);
    setIsMuted(vol === 0);
    audioEngine.setVolume(vol);
  };

  const handleToggleMute = () => {
    if (isMuted) {
      setIsMuted(false);
      audioEngine.setVolume(volume || 0.8);
    } else {
      setIsMuted(true);
      audioEngine.setVolume(0);
    }
  };

  const handleSetPlaybackRate = (rate: number) => {
    setPlaybackRate(rate);
    audioEngine.setPlaybackRate(rate);
  };

  const handleToggleShuffle = () => {
    setIsShuffle((prev) => !prev);
  };

  const handleCycleRepeatMode = () => {
    const modes: RepeatMode[] = ['all', 'one', 'off'];
    const next = modes[(modes.indexOf(repeatMode) + 1) % modes.length];
    setRepeatMode(next);
  };

  // Favorite toggle
  const handleToggleFavorite = async (trackId: string) => {
    const updated = tracks.map((t) => {
      if (t.id === trackId) {
        const nextFav = !t.isFavorite;
        const updatedTrack = { ...t, isFavorite: nextFav };
        saveTrack(updatedTrack);
        return updatedTrack;
      }
      return t;
    });
    setTracks(updated);

    if (currentTrack && currentTrack.id === trackId) {
      setCurrentTrack((prev) => prev ? { ...prev, isFavorite: !prev.isFavorite } : null);
    }
  };

  // Video Actions
  const handlePlayVideo = (track: Track) => {
    // Pause audio player first
    audioEngine.pause();
    setIsPlaying(false);
    if (window.AuraNative) { window.AuraNative.openVideo(track.url); return; }
    setActiveVideoTrack(track);
    setIsVideoPlayerOpen(true);
  };

  const handlePlayVideoAsAudio = (track: Track) => {
    // Play video track in background audio player
    handlePlayTrack(track);
  };

  // Playlist Actions
  const handlePlayPlaylist = (playlist: Playlist) => {
    const plTracks =
      playlist.id === 'playlist-favorites'
        ? tracks.filter((t) => t.isFavorite)
        : playlist.trackIds
            .map((id) => tracks.find((t) => t.id === id))
            .filter(Boolean) as Track[];

    if (plTracks.length > 0) {
      setQueue(plTracks);
      handlePlayTrack(plTracks[0]);
    }
  };

  const handleCreatePlaylist = async (name: string, description: string) => {
    const newPlaylist: Playlist = {
      id: `playlist-${Date.now()}`,
      name,
      description,
      trackIds: [],
      createdAt: Date.now(),
      isSystem: false,
    };
    await savePlaylist(newPlaylist);
    setPlaylists((prev) => [...prev, newPlaylist]);
  };

  const handleDeletePlaylist = async (playlistId: string) => {
    await dbDeletePlaylist(playlistId);
    setPlaylists((prev) => prev.filter((p) => p.id !== playlistId));
  };

  const handleAddTrackToPlaylist = async (playlistId: string, trackId: string) => {
    const updated = playlists.map((p) => {
      if (p.id === playlistId && !p.trackIds.includes(trackId)) {
        const updatedPl = { ...p, trackIds: [...p.trackIds, trackId] };
        savePlaylist(updatedPl);
        return updatedPl;
      }
      return p;
    });
    setPlaylists(updated);
  };

  const handleRemoveTrackFromPlaylist = async (playlistId: string, trackId: string) => {
    const updated = playlists.map((p) => {
      if (p.id === playlistId) {
        const updatedPl = {
          ...p,
          trackIds: p.trackIds.filter((id) => id !== trackId),
        };
        savePlaylist(updatedPl);
        return updatedPl;
      }
      return p;
    });
    setPlaylists(updated);
  };

  const handleAddToQueue = (track: Track) => {
    setQueue((prev) => [...prev, track]);
  };

  // Import local media
  const handleImportTracks = async (newTracks: Track[]) => {
    await saveMultipleTracks(newTracks);
    setTracks((prev) => [...prev, ...newTracks]);
    const newAudio = newTracks.filter((t) => t.type === 'audio');
    if (newAudio.length > 0) {
      setQueue((prev) => [...prev, ...newAudio]);
    }
  };

  const openImport = () => {
    if (window.AuraNative) window.AuraNative.pickMedia();
    else setIsImportMediaOpen(true);
  };

  // Toggle Language
  const handleToggleLang = () => {
    const nextLang = lang === 'bn' ? 'en' : 'bn';
    setLang(nextLang);
    saveSetting('lang', nextLang);
  };

  // Studio Tools Handlers
  const handleSaveTrimmedTrack = async (newTrack: Track) => {
    await saveTrack(newTrack);
    setTracks((prev) => [newTrack, ...prev]);
    setQueue((prev) => [newTrack, ...prev]);
  };

  const handleAddExtractedTrack = async (newTrack: Track) => {
    await saveTrack(newTrack);
    setTracks((prev) => [newTrack, ...prev]);
    setQueue((prev) => [newTrack, ...prev]);
    handlePlayTrack(newTrack);
    setCurrentTab('music');
  };

  const handleUpdateTrackTags = async (updatedTrack: Track) => {
    await saveTrack(updatedTrack);
    setTracks((prev) => prev.map((t) => (t.id === updatedTrack.id ? updatedTrack : t)));
    setQueue((prev) => prev.map((t) => (t.id === updatedTrack.id ? updatedTrack : t)));
    if (currentTrack?.id === updatedTrack.id) {
      setCurrentTrack(updatedTrack);
    }
  };

  return (
    <div className="relative min-h-screen text-slate-100 flex flex-col justify-between selection:bg-purple-500/30 overflow-x-hidden">
      {/* Background Soft Glass Glow and Aurora Orbs */}
      <GlassBackground />

      {/* Top Glass Header */}
      <GlassHeader
        lang={lang}
        onToggleLang={handleToggleLang}
        onOpenSleepTimer={() => setIsSleepTimerOpen(true)}
        sleepTimerRemaining={sleepTimerRemaining}
        onOpenImport={openImport}
        onOpenStudioTools={() => setIsStudioToolsOpen(true)}
      />

      {/* Main Tab Content based on cleanly separated selection */}
      <main className="flex-1 w-full relative z-10">
        {currentTab === 'music' && (
          <AudioTab
            tracks={tracks}
            currentTrack={currentTrack}
            isPlaying={isPlaying}
            onPlayTrack={handlePlayTrack}
            onTogglePlay={handleTogglePlay}
            onToggleFavorite={handleToggleFavorite}
            onAddToQueue={handleAddToQueue}
            onOpenImport={openImport}
            playlists={playlists}
            onAddTrackToPlaylist={handleAddTrackToPlaylist}
            onOpenCutter={(t) => {
              setActiveCutterTrack(t);
              setIsAudioCutterOpen(true);
            }}
            onOpenTagEditor={(t) => {
              setActiveTagTrack(t);
              setIsTagEditorOpen(true);
            }}
            onOpenBookmarks={() => {
              setIsBookmarksOpen(true);
            }}
            lang={lang}
          />
        )}

        {currentTab === 'video' && (
          <VideoTab
            tracks={tracks}
            onPlayVideo={handlePlayVideo}
            onPlayVideoAsAudio={handlePlayVideoAsAudio}
            onToggleFavorite={handleToggleFavorite}
            onOpenImport={openImport}
            lang={lang}
          />
        )}

        {currentTab === 'playlists' && (
          <PlaylistsTab
            playlists={playlists}
            tracks={tracks}
            onPlayPlaylist={handlePlayPlaylist}
            onPlayTrack={handlePlayTrack}
            onCreatePlaylistOpen={() => setIsCreatePlaylistOpen(true)}
            onDeletePlaylist={handleDeletePlaylist}
            onRemoveTrackFromPlaylist={handleRemoveTrackFromPlaylist}
            onToggleFavorite={handleToggleFavorite}
            lang={lang}
          />
        )}

        {currentTab === 'folders' && (
          <FoldersTab
            tracks={tracks}
            onPlayTrack={handlePlayTrack}
            onOpenImport={openImport}
            lang={lang}
          />
        )}

        {currentTab === 'equalizer' && (
          <EqualizerTab isPlaying={isPlaying} lang={lang} />
        )}
      </main>

      {/* Floating Mini Player Bar above Navigation */}
      <MiniPlayerBar
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        currentTime={currentTime}
        duration={duration}
        onTogglePlay={handleTogglePlay}
        onNextTrack={handleNextTrack}
        onToggleFavorite={handleToggleFavorite}
        onOpenNowPlaying={() => setIsNowPlayingOpen(true)}
      />

      {/* Bottom Separated Glass Navbar */}
      <GlassNavbar
        currentTab={currentTab}
        onSelectTab={(tab) => setCurrentTab(tab)}
        lang={lang}
      />

      {/* Full Screen Soft Glass Now Playing Modal */}
      <NowPlayingModal
        isOpen={isNowPlayingOpen}
        onClose={() => setIsNowPlayingOpen(false)}
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        currentTime={currentTime}
        duration={duration}
        volume={volume}
        isMuted={isMuted}
        playbackRate={playbackRate}
        isShuffle={isShuffle}
        repeatMode={repeatMode}
        queue={queue}
        onTogglePlay={handleTogglePlay}
        onSeek={handleSeek}
        onPreviousTrack={handlePreviousTrack}
        onNextTrack={handleNextTrack}
        onToggleFavorite={handleToggleFavorite}
        onSetVolume={handleSetVolume}
        onToggleMute={handleToggleMute}
        onSetPlaybackRate={handleSetPlaybackRate}
        onToggleShuffle={handleToggleShuffle}
        onCycleRepeatMode={handleCycleRepeatMode}
        onOpenEqualizer={() => {
          setIsNowPlayingOpen(false);
          setCurrentTab('equalizer');
        }}
        onOpenSleepTimer={() => setIsSleepTimerOpen(true)}
        lang={lang}
      />

      {/* Fullscreen Video Player Modal */}
      <VideoPlayerModal
        isOpen={isVideoPlayerOpen}
        onClose={() => setIsVideoPlayerOpen(false)}
        videoTrack={activeVideoTrack}
        onPlayVideoAsAudio={handlePlayVideoAsAudio}
        lang={lang}
      />

      {/* Sleep Timer Modal */}
      <SleepTimerModal
        isOpen={isSleepTimerOpen}
        onClose={() => setIsSleepTimerOpen(false)}
        remainingSeconds={sleepTimerRemaining}
        onSetTimer={(mins) => setSleepTimerRemaining(mins * 60)}
        onCancelTimer={() => setSleepTimerRemaining(0)}
        lang={lang}
      />

      {/* Create Playlist Modal */}
      <CreatePlaylistModal
        isOpen={isCreatePlaylistOpen}
        onClose={() => setIsCreatePlaylistOpen(false)}
        onCreatePlaylist={handleCreatePlaylist}
        lang={lang}
      />

      {/* Import Local Media Modal */}
      <ImportMediaModal
        isOpen={isImportMediaOpen}
        onClose={() => setIsImportMediaOpen(false)}
        onImportTracks={handleImportTracks}
        lang={lang}
      />

      {/* Download & Install Guide Modal */}
      <DownloadGuideModal
        isOpen={isDownloadGuideOpen}
        onClose={() => setIsDownloadGuideOpen(false)}
        lang={lang}
      />

      {/* Premium Studio Tools Hub Modal */}
      <StudioToolsModal
        isOpen={isStudioToolsOpen}
        onClose={() => setIsStudioToolsOpen(false)}
        onOpenAudioCutter={() => {
          setActiveCutterTrack(currentTrack || tracks[0]);
          setIsAudioCutterOpen(true);
        }}
        onOpenVideoExtractor={() => setIsVideoExtractorOpen(true)}
        onOpenTagEditor={() => {
          setActiveTagTrack(currentTrack || tracks[0]);
          setIsTagEditorOpen(true);
        }}
        onOpenCarMode={() => setIsCarModeOpen(true)}
        onOpenEqualizer={() => setCurrentTab('equalizer')}
        onOpenBookmarks={() => setIsBookmarksOpen(true)}
        lang={lang}
      />

      {/* Audio Cutter & Ringtone Maker Modal */}
      <AudioCutterModal
        isOpen={isAudioCutterOpen}
        onClose={() => setIsAudioCutterOpen(false)}
        tracks={tracks.filter((t) => t.type === 'audio')}
        initialTrack={activeCutterTrack || currentTrack}
        onSaveTrimmedTrack={handleSaveTrimmedTrack}
        lang={lang}
      />

      {/* Video to MP3 Extractor Modal */}
      <VideoToAudioModal
        isOpen={isVideoExtractorOpen}
        onClose={() => setIsVideoExtractorOpen(false)}
        videoTracks={tracks.filter((t) => t.type === 'video')}
        onAddExtractedTrack={handleAddExtractedTrack}
        lang={lang}
      />

      {/* ID3 Tag & Cover Art Editor Modal */}
      <TagEditorModal
        isOpen={isTagEditorOpen}
        onClose={() => setIsTagEditorOpen(false)}
        track={activeTagTrack || currentTrack}
        onSaveTrack={handleUpdateTrackTags}
        lang={lang}
      />

      {/* Car Driving Mode Modal */}
      <CarModeModal
        isOpen={isCarModeOpen}
        onClose={() => setIsCarModeOpen(false)}
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        onTogglePlay={handleTogglePlay}
        onPreviousTrack={handlePreviousTrack}
        onNextTrack={handleNextTrack}
        onToggleFavorite={handleToggleFavorite}
        volume={volume}
        onSetVolume={handleSetVolume}
        isShuffle={isShuffle}
        onToggleShuffle={handleToggleShuffle}
        lang={lang}
      />

      {/* Smart Bookmarks & Audiobook Resume Modal */}
      <BookmarksModal
        isOpen={isBookmarksOpen}
        onClose={() => setIsBookmarksOpen(false)}
        currentTrack={currentTrack}
        currentTime={currentTime}
        onSeek={handleSeek}
        lang={lang}
      />
    </div>
  );
};
