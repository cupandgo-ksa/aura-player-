import { Track, EqualizerPreset } from '../types/media';
import './native';

export const EQ_FREQUENCIES = [60, 230, 910, 3600, 14000];

export const EQ_PRESETS: EqualizerPreset[] = [
  { id: 'flat', name: 'Normal / Flat', bnName: 'স্বাভাবিক (Normal)', gains: [0, 0, 0, 0, 0] },
  { id: 'bass', name: 'Bass Boost', bnName: 'স্ট্রং বাস (Bass Boost)', gains: [7, 5, 2, 0, -1] },
  { id: 'pop', name: 'Pop', bnName: 'পপ (Pop)', gains: [-1, 2, 5, 3, -1] },
  { id: 'rock', name: 'Rock', bnName: 'রক (Rock)', gains: [5, 3, -1, 3, 5] },
  { id: 'jazz', name: 'Jazz', bnName: 'জ্যাজ (Jazz)', gains: [4, 2, 1, 2, 4] },
  { id: 'vocal', name: 'Vocal / Acoustic', bnName: 'ভোকাল ও অ্যাকোস্টিক', gains: [-2, 1, 4, 4, 2] },
  { id: 'electronic', name: 'Electronic', bnName: 'ইলেকট্রনিক ও ডান্স', gains: [6, 4, 0, 3, 5] },
  { id: 'classical', name: 'Classical', bnName: 'ক্ল্যাসিকাল', gains: [4, 3, 2, 3, -2] },
];

export class AudioEngine {
  private static instance: AudioEngine | null = null;
  public audioCtx: AudioContext | null = null;
  public audioElement: HTMLAudioElement;
  public videoElement: HTMLVideoElement | null = null;
  private sourceNode: MediaElementAudioSourceNode | null = null;
  private filterNodes: BiquadFilterNode[] = [];
  private bassBoostNode: BiquadFilterNode | null = null;
  private trebleBoostNode: BiquadFilterNode | null = null;
  public analyserNode: AnalyserNode | null = null;
  private gainNode: GainNode | null = null;
  private isInitialized = false;
  private nativeTrack: Track | null = null;
  private pendingUri = '';
  private nativeTimer: number | null = null;

  private onTrackEndCallback: (() => void) | null = null;
  private onTimeUpdateCallback: ((time: number, duration: number) => void) | null = null;
  private onStateChangeCallback: ((isPlaying: boolean) => void) | null = null;

  private constructor() {
    this.audioElement = new Audio();
    this.audioElement.preload = 'auto';
    this.audioElement.crossOrigin = 'anonymous';

    this.audioElement.addEventListener('ended', () => {
      this.onTrackEndCallback?.();
    });

    this.audioElement.addEventListener('timeupdate', () => {
      this.onTimeUpdateCallback?.(this.audioElement.currentTime, this.audioElement.duration || 0);
      this.updateMediaSessionPosition();
    });

    this.audioElement.addEventListener('play', () => {
      this.onStateChangeCallback?.(true);
      this.updateMediaSessionState(true);
    });

    this.audioElement.addEventListener('pause', () => {
      this.onStateChangeCallback?.(false);
      this.updateMediaSessionState(false);
    });
  }

  public static getInstance(): AudioEngine {
    if (!AudioEngine.instance) {
      AudioEngine.instance = new AudioEngine();
    }
    return AudioEngine.instance;
  }

  public initAudioContext(): void {
    if (this.isInitialized) return;

    try {
      const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.audioCtx = new AudioContextClass();

      this.sourceNode = this.audioCtx.createMediaElementSource(this.audioElement);

      // Bass boost shelf
      this.bassBoostNode = this.audioCtx.createBiquadFilter();
      this.bassBoostNode.type = 'lowshelf';
      this.bassBoostNode.frequency.value = 100;
      this.bassBoostNode.gain.value = 0;

      // 5-band EQ
      this.filterNodes = EQ_FREQUENCIES.map((freq, index) => {
        const filter = this.audioCtx!.createBiquadFilter();
        if (index === 0) {
          filter.type = 'lowshelf';
        } else if (index === EQ_FREQUENCIES.length - 1) {
          filter.type = 'highshelf';
        } else {
          filter.type = 'peaking';
          filter.Q.value = 1.4;
        }
        filter.frequency.value = freq;
        filter.gain.value = 0;
        return filter;
      });

      // Treble boost shelf
      this.trebleBoostNode = this.audioCtx.createBiquadFilter();
      this.trebleBoostNode.type = 'highshelf';
      this.trebleBoostNode.frequency.value = 8000;
      this.trebleBoostNode.gain.value = 0;

      // Analyser node for soft glass audio visualizer
      this.analyserNode = this.audioCtx.createAnalyser();
      this.analyserNode.fftSize = 128;
      this.analyserNode.smoothingTimeConstant = 0.8;

      // Master gain
      this.gainNode = this.audioCtx.createGain();

      // Connect pipeline:
      // Source -> BassBoost -> EQ Bands (0..4) -> TrebleBoost -> Analyser -> MasterGain -> Destination
      let lastNode: AudioNode = this.sourceNode;
      lastNode.connect(this.bassBoostNode);
      lastNode = this.bassBoostNode;

      for (const filter of this.filterNodes) {
        lastNode.connect(filter);
        lastNode = filter;
      }

      lastNode.connect(this.trebleBoostNode);
      lastNode = this.trebleBoostNode;

      lastNode.connect(this.analyserNode);
      this.analyserNode.connect(this.gainNode);
      this.gainNode.connect(this.audioCtx.destination);

      this.isInitialized = true;
    } catch (e) {
      console.warn('AudioContext initialization deferred or restricted by browser:', e);
    }
  }

  public resumeContext(): void {
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  public setTrack(track: Track, autoPlay = true): void {
    if (window.AuraNative) {
      this.nativeTrack = track;
      if (autoPlay) {
        this.pendingUri = track.url;
        window.AuraNative.play(track.url, track.title, track.artist);
      }
      return;
    }
    this.initAudioContext();
    this.resumeContext();

    this.audioElement.src = track.url;
    this.audioElement.load();

    if (autoPlay) {
      this.audioElement.play().catch((err) => {
        console.warn('AutoPlay prevented, waiting for user gesture:', err);
      });
    }

    this.setupMediaSession(track);
  }

  public play(): Promise<void> {
    if (window.AuraNative) {
      if (this.nativeTrack) {
        let current = '';
        try { current = JSON.parse(window.AuraNative.state()).uri; } catch { /* empty */ }
        if (current === this.nativeTrack.url) window.AuraNative.resume();
        else { this.pendingUri = this.nativeTrack.url; window.AuraNative.play(this.nativeTrack.url, this.nativeTrack.title, this.nativeTrack.artist); }
      }
      return Promise.resolve();
    }
    this.initAudioContext();
    this.resumeContext();
    return this.audioElement.play();
  }

  public pause(): void {
    if (window.AuraNative) { window.AuraNative.pause(); return; }
    this.audioElement.pause();
  }

  public togglePlay(): void {
    if (window.AuraNative) {
      let state: any = {};
      try { state = JSON.parse(window.AuraNative.state()); } catch { /* empty */ }
      if (state.playing) this.pause(); else void this.play();
      return;
    }
    if (this.audioElement.paused) {
      this.play();
    } else {
      this.pause();
    }
  }

  public seek(seconds: number): void {
    if (window.AuraNative) { window.AuraNative.seek(Math.round(seconds * 1000)); return; }
    if (Number.isFinite(seconds)) {
      this.audioElement.currentTime = Math.max(0, Math.min(seconds, this.audioElement.duration || 0));
    }
  }

  public setVolume(volume: number): void {
    this.audioElement.volume = Math.max(0, Math.min(1, volume));
  }

  public setPlaybackRate(rate: number): void {
    if (window.AuraNative) { window.AuraNative.speed(rate); return; }
    this.audioElement.playbackRate = rate;
  }

  public setEqualizerBandGain(index: number, gainDb: number): void {
    if (this.filterNodes[index]) {
      this.filterNodes[index].gain.value = Math.max(-12, Math.min(12, gainDb));
    }
  }

  public applyPreset(preset: EqualizerPreset): void {
    preset.gains.forEach((gain, idx) => {
      this.setEqualizerBandGain(idx, gain);
    });
  }

  public setBassBoost(level: number): void {
    // level: 0 to 100
    if (this.bassBoostNode) {
      this.bassBoostNode.gain.value = (level / 100) * 12; // up to +12dB
    }
  }

  public setTrebleBoost(level: number): void {
    if (this.trebleBoostNode) {
      this.trebleBoostNode.gain.value = (level / 100) * 10;
    }
  }

  public getVisualizerData(dataArray: Uint8Array): void {
    if (this.analyserNode) {
      this.analyserNode.getByteFrequencyData(dataArray as any);
    } else {
      dataArray.fill(0);
    }
  }

  public setCallbacks(
    onTrackEnd: () => void,
    onTimeUpdate: (time: number, duration: number) => void,
    onStateChange: (isPlaying: boolean) => void
  ): void {
    this.onTrackEndCallback = onTrackEnd;
    this.onTimeUpdateCallback = onTimeUpdate;
    this.onStateChangeCallback = onStateChange;
    if (window.AuraNative && this.nativeTimer === null) {
      this.nativeTimer = window.setInterval(() => {
        try {
          const state = JSON.parse(window.AuraNative!.state());
          if (this.pendingUri && state.uri === this.pendingUri) this.pendingUri = '';
          if (!this.pendingUri && state.uri && this.nativeTrack?.url !== state.uri) {
            window.dispatchEvent(new CustomEvent('auraNativeTrack', { detail: state.uri }));
          }
          this.onTimeUpdateCallback?.((state.position || 0) / 1000, (state.duration || 0) / 1000);
          this.onStateChangeCallback?.(!!state.playing);
        } catch { /* service may be starting */ }
      }, 450);
    }
  }

  public setupMediaSessionHandlers(handlers: {
    onPlay: () => void;
    onPause: () => void;
    onPrevious: () => void;
    onNext: () => void;
    onSeek: (seconds: number) => void;
  }): void {
    if (window.AuraNative) return;
    if ('mediaSession' in navigator) {
      navigator.mediaSession.setActionHandler('play', () => handlers.onPlay());
      navigator.mediaSession.setActionHandler('pause', () => handlers.onPause());
      navigator.mediaSession.setActionHandler('previoustrack', () => handlers.onPrevious());
      navigator.mediaSession.setActionHandler('nexttrack', () => handlers.onNext());
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (details.seekTime !== undefined) {
          handlers.onSeek(details.seekTime);
        }
      });
      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        const offset = details.seekOffset || 10;
        this.seek(this.audioElement.currentTime - offset);
      });
      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        const offset = details.seekOffset || 10;
        this.seek(this.audioElement.currentTime + offset);
      });
    }
  }

  private setupMediaSession(track: Track): void {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title,
        artist: track.artist || 'Aura Player',
        album: track.album || 'Aura Glass Collection',
        artwork: [
          { src: track.coverUrl, sizes: '96x96', type: 'image/jpeg' },
          { src: track.coverUrl, sizes: '256x256', type: 'image/jpeg' },
          { src: track.coverUrl, sizes: '512x512', type: 'image/jpeg' },
        ],
      });
    }
  }

  private updateMediaSessionState(isPlaying: boolean): void {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
    }
  }

  private updateMediaSessionPosition(): void {
    if ('mediaSession' in navigator && 'setPositionState' in navigator.mediaSession) {
      if (this.audioElement.duration && !isNaN(this.audioElement.duration)) {
        try {
          navigator.mediaSession.setPositionState({
            duration: this.audioElement.duration,
            playbackRate: this.audioElement.playbackRate,
            position: Math.min(this.audioElement.currentTime, this.audioElement.duration),
          });
        } catch {
          // ignore transient edge cases
        }
      }
    }
  }
}
