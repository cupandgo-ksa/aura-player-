import React, { useEffect, useRef, useState } from 'react';
import { AudioEngine } from '../services/audioEngine';
import {
  BarChart3,
  Activity,
  Sparkles,
  Disc,
  Settings2,
  Sliders,
  Check,
  Zap,
  Palette,
  Eye,
  X,
} from 'lucide-react';

interface NowPlayingVisualizerProps {
  isPlaying: boolean;
  coverUrl: string;
  title: string;
  lang: 'bn' | 'en';
}

export type VisualizerStyle = 'bars' | 'wave' | 'particle' | 'radial';

export type ColorTheme = 'cyber' | 'emerald' | 'sunset' | 'cosmic';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  baseSize: number;
  alpha: number;
  freqIndex: number;
  color: string;
}

export const NowPlayingVisualizer: React.FC<NowPlayingVisualizerProps> = ({
  isPlaying,
  coverUrl,
  title,
  lang,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Settings State
  const [style, setStyle] = useState<VisualizerStyle>('bars');
  const [colorTheme, setColorTheme] = useState<ColorTheme>('cyber');
  const [sensitivity, setSensitivity] = useState<number>(1.2);
  const [showFloorReflection, setShowFloorReflection] = useState<boolean>(true);
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);

  // Audio Readouts
  const [bassLevel, setBassLevel] = useState<number>(0);
  const [energyLevel, setEnergyLevel] = useState<number>(0);

  // Refs for animation loop
  const styleRef = useRef<VisualizerStyle>(style);
  styleRef.current = style;

  const colorThemeRef = useRef<ColorTheme>(colorTheme);
  colorThemeRef.current = colorTheme;

  const sensitivityRef = useRef<number>(sensitivity);
  sensitivityRef.current = sensitivity;

  const showFloorReflectionRef = useRef<boolean>(showFloorReflection);
  showFloorReflectionRef.current = showFloorReflection;

  // Particle pool
  const particlesRef = useRef<Particle[]>([]);

  // Initialize particle pool once
  useEffect(() => {
    const pArray: Particle[] = [];
    const colors = ['#c084fc', '#38bdf8', '#818cf8', '#f472b6', '#34d399', '#ffffff'];
    for (let i = 0; i < 75; i++) {
      pArray.push({
        x: Math.random() * 440,
        y: Math.random() * 220,
        vx: (Math.random() - 0.5) * 1.2,
        vy: (Math.random() - 0.5) * 1.2,
        size: Math.random() * 3 + 2,
        baseSize: Math.random() * 3 + 2,
        alpha: Math.random() * 0.7 + 0.3,
        freqIndex: Math.floor(Math.random() * 40),
        color: colors[Math.floor(Math.random() * colors.length)],
      });
    }
    particlesRef.current = pArray;
  }, []);

  // Theme Color Configurations
  const getThemePalette = (theme: ColorTheme) => {
    switch (theme) {
      case 'emerald':
        return {
          primary: '#10b981',
          secondary: '#06b6d4',
          accent: '#34d399',
          glow: 'rgba(16, 185, 129, 0.4)',
          gradient: ['rgba(6, 182, 212, 0.4)', 'rgba(16, 185, 129, 0.8)', 'rgba(52, 211, 153, 0.95)', '#ffffff'],
        };
      case 'sunset':
        return {
          primary: '#f43f5e',
          secondary: '#fb923c',
          accent: '#e11d48',
          glow: 'rgba(244, 63, 94, 0.4)',
          gradient: ['rgba(251, 146, 60, 0.4)', 'rgba(244, 63, 94, 0.8)', 'rgba(225, 29, 72, 0.95)', '#ffffff'],
        };
      case 'cosmic':
        return {
          primary: '#3b82f6',
          secondary: '#6366f1',
          accent: '#60a5fa',
          glow: 'rgba(59, 130, 246, 0.4)',
          gradient: ['rgba(99, 102, 241, 0.4)', 'rgba(59, 130, 246, 0.8)', 'rgba(96, 165, 250, 0.95)', '#ffffff'],
        };
      case 'cyber':
      default:
        return {
          primary: '#a855f7',
          secondary: '#06b6d4',
          accent: '#c084fc',
          glow: 'rgba(168, 85, 247, 0.4)',
          gradient: ['rgba(147, 51, 234, 0.35)', 'rgba(168, 85, 247, 0.75)', 'rgba(56, 189, 248, 0.9)', '#ffffff'],
        };
    }
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const audioEngine = AudioEngine.getInstance();
    const bufferLength = 64;
    const dataArray = new Uint8Array(bufferLength);

    let animId: number;
    let rotationAngle = 0;
    const smoothedValues = new Float32Array(bufferLength);

    const render = () => {
      animId = requestAnimationFrame(render);

      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);

      // Web Audio API frequency analysis
      audioEngine.getVisualizerData(dataArray);

      // Energy & Bass calculations
      let bassSum = 0;
      let totalSum = 0;
      for (let i = 0; i < 8; i++) {
        bassSum += dataArray[i];
      }
      for (let i = 0; i < bufferLength; i++) {
        totalSum += dataArray[i];
      }

      const currentBass = Math.min(100, Math.round((bassSum / (8 * 255)) * 100 * sensitivityRef.current));
      const currentEnergy = Math.min(100, Math.round((totalSum / (bufferLength * 255)) * 100 * sensitivityRef.current));
      setBassLevel(currentBass);
      setEnergyLevel(currentEnergy);

      // Smooth values
      const currentStyle = styleRef.current;
      const currentTheme = getThemePalette(colorThemeRef.current);
      const sens = sensitivityRef.current;

      for (let i = 0; i < bufferLength; i++) {
        let raw = dataArray[i];
        if (!isPlaying || totalSum === 0) {
          raw = Math.sin(Date.now() * 0.0025 + i * 0.25) * 14 + 16;
        }
        smoothedValues[i] = smoothedValues[i] * 0.65 + raw * sens * 0.35;
      }

      // ==========================================
      // ALGORITHM 1: GLASS SPECTRUM BARS
      // ==========================================
      if (currentStyle === 'bars') {
        const barCount = 36;
        const totalBarWidth = width / barCount;
        const barWidth = totalBarWidth * 0.68;
        const gap = totalBarWidth * 0.32;
        const baseline = showFloorReflectionRef.current ? height * 0.72 : height * 0.9;

        // Reflection floor line
        if (showFloorReflectionRef.current) {
          const floorGrad = ctx.createLinearGradient(0, baseline, width, baseline);
          floorGrad.addColorStop(0, 'rgba(255, 255, 255, 0)');
          floorGrad.addColorStop(0.5, currentTheme.glow);
          floorGrad.addColorStop(1, 'rgba(255, 255, 255, 0)');
          ctx.strokeStyle = floorGrad;
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.moveTo(0, baseline);
          ctx.lineTo(width, baseline);
          ctx.stroke();
        }

        for (let i = 0; i < barCount; i++) {
          const dataIdx = Math.floor((i / barCount) * (bufferLength * 0.75));
          const val = Math.min(255, smoothedValues[dataIdx] || 0);
          const maxBarHeight = baseline * 0.88;
          const barHeight = Math.max(4, (val / 255) * maxBarHeight);

          const x = i * (barWidth + gap) + gap / 2;
          const y = baseline - barHeight;

          // Upper Glass Bar Gradient
          const barGrad = ctx.createLinearGradient(0, baseline, 0, y);
          barGrad.addColorStop(0, currentTheme.gradient[0]);
          barGrad.addColorStop(0.5, currentTheme.gradient[1]);
          barGrad.addColorStop(0.85, currentTheme.gradient[2]);
          barGrad.addColorStop(1, currentTheme.gradient[3]);

          ctx.fillStyle = barGrad;
          ctx.beginPath();
          ctx.roundRect(x, y, barWidth, barHeight, [4, 4, 0, 0]);
          ctx.fill();

          // Specular white glowing cap
          ctx.fillStyle = '#ffffff';
          ctx.beginPath();
          ctx.arc(x + barWidth / 2, y + 2, barWidth * 0.3, 0, Math.PI * 2);
          ctx.fill();

          // Reflection below baseline
          if (showFloorReflectionRef.current) {
            const reflectionHeight = barHeight * 0.35;
            const refGrad = ctx.createLinearGradient(0, baseline, 0, baseline + reflectionHeight);
            refGrad.addColorStop(0, currentTheme.glow);
            refGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

            ctx.fillStyle = refGrad;
            ctx.beginPath();
            ctx.roundRect(x, baseline + 1, barWidth, reflectionHeight, [0, 0, 3, 3]);
            ctx.fill();
          }
        }
      }

      // ==========================================
      // ALGORITHM 2: HARMONIC AURORA FLUID WAVE
      // ==========================================
      else if (currentStyle === 'wave') {
        const points = 48;
        const step = width / (points - 1);
        const centerY = height * 0.52;

        ctx.beginPath();
        ctx.moveTo(0, centerY);

        for (let i = 0; i < points; i++) {
          const dataIdx = Math.floor((i / points) * (bufferLength * 0.7));
          const val = smoothedValues[dataIdx] || 0;
          const amplitude = (val / 255) * (height * 0.38);
          const wave = Math.sin(i * 0.3 + Date.now() * 0.003) * 8;
          const x = i * step;
          const y = centerY - amplitude + wave;

          if (i === 0) {
            ctx.moveTo(x, y);
          } else {
            const prevX = (i - 1) * step;
            const prevDataIdx = Math.floor(((i - 1) / points) * (bufferLength * 0.7));
            const prevVal = smoothedValues[prevDataIdx] || 0;
            const prevAmp = (prevVal / 255) * (height * 0.38);
            const prevWave = Math.sin((i - 1) * 0.3 + Date.now() * 0.003) * 8;
            const prevY = centerY - prevAmp + prevWave;

            const cx = (prevX + x) / 2;
            const cy = (prevY + y) / 2;
            ctx.quadraticCurveTo(prevX, prevY, cx, cy);
          }
        }

        ctx.lineTo(width, height);
        ctx.lineTo(0, height);
        ctx.closePath();

        const fillGrad = ctx.createLinearGradient(0, centerY - height * 0.3, 0, height);
        fillGrad.addColorStop(0, currentTheme.gradient[2]);
        fillGrad.addColorStop(0.4, currentTheme.gradient[1]);
        fillGrad.addColorStop(1, 'rgba(15, 23, 42, 0)');
        ctx.fillStyle = fillGrad;
        ctx.fill();

        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2.5;
        ctx.stroke();
      }

      // ==========================================
      // ALGORITHM 3: AUDIO-REACTIVE PARTICLE FIELD
      // ==========================================
      else if (currentStyle === 'particle') {
        const particles = particlesRef.current;
        const cx = width / 2;
        const cy = height / 2;
        const bassBoost = currentBass > 60 ? (currentBass / 60) * 1.8 : 1.0;

        // Draw connecting spiderweb glass lines between nearby energetic particles
        ctx.lineWidth = 0.8;
        for (let i = 0; i < particles.length; i++) {
          for (let j = i + 1; j < particles.length; j++) {
            const dx = particles[i].x - particles[j].x;
            const dy = particles[i].y - particles[j].y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 48) {
              const alpha = (1 - dist / 48) * 0.35 * (currentEnergy / 100);
              ctx.strokeStyle = `rgba(255, 255, 255, ${alpha})`;
              ctx.beginPath();
              ctx.moveTo(particles[i].x, particles[i].y);
              ctx.lineTo(particles[j].x, particles[j].y);
              ctx.stroke();
            }
          }
        }

        // Update and draw each particle
        for (let i = 0; i < particles.length; i++) {
          const p = particles[i];
          const freqVal = smoothedValues[p.freqIndex] || 0;
          const freqRatio = freqVal / 255;

          // React to audio frequency
          const dynamicSize = p.baseSize * (1 + freqRatio * 2.2 * bassBoost);
          const speedMultiplier = 1 + freqRatio * 2.5;

          p.x += p.vx * speedMultiplier;
          p.y += p.vy * speedMultiplier;

          // Wrap edges
          if (p.x < 0) p.x = width;
          if (p.x > width) p.x = 0;
          if (p.y < 0) p.y = height;
          if (p.y > height) p.y = 0;

          // If intense bass drop, push outward from center
          if (currentBass > 70) {
            const angleFromCenter = Math.atan2(p.y - cy, p.x - cx);
            p.x += Math.cos(angleFromCenter) * 1.5;
            p.y += Math.sin(angleFromCenter) * 1.5;
          }

          // Outer Glow
          ctx.beginPath();
          ctx.arc(p.x, p.y, dynamicSize * 1.8, 0, Math.PI * 2);
          ctx.fillStyle = currentTheme.glow;
          ctx.fill();

          // Particle Core
          ctx.beginPath();
          ctx.arc(p.x, p.y, dynamicSize, 0, Math.PI * 2);
          ctx.fillStyle = p.color;
          ctx.fill();

          // White specular highlight
          ctx.beginPath();
          ctx.arc(p.x - dynamicSize * 0.3, p.y - dynamicSize * 0.3, dynamicSize * 0.3, 0, Math.PI * 2);
          ctx.fillStyle = '#ffffff';
          ctx.fill();
        }
      }

      // ==========================================
      // ALGORITHM 4: RADIAL VINYL ORBIT
      // ==========================================
      else if (currentStyle === 'radial') {
        const cx = width / 2;
        const cy = height / 2;
        const innerRadius = Math.min(width, height) * 0.22;
        const barCount = 42;
        rotationAngle += isPlaying ? 0.008 : 0.002;

        // Core Circle
        const coreGrad = ctx.createRadialGradient(cx, cy, 5, cx, cy, innerRadius);
        coreGrad.addColorStop(0, currentTheme.gradient[1]);
        coreGrad.addColorStop(0.8, currentTheme.gradient[0]);
        coreGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
        ctx.fillStyle = coreGrad;
        ctx.beginPath();
        ctx.arc(cx, cy, innerRadius, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        for (let i = 0; i < barCount; i++) {
          const angle = (i / barCount) * Math.PI * 2 + rotationAngle;
          const dataIdx = Math.floor((i / barCount) * (bufferLength * 0.6));
          const val = smoothedValues[dataIdx] || 0;
          const spikeLength = Math.max(6, (val / 255) * (innerRadius * 0.9));

          const x1 = cx + Math.cos(angle) * (innerRadius + 4);
          const y1 = cy + Math.sin(angle) * (innerRadius + 4);
          const x2 = cx + Math.cos(angle) * (innerRadius + 4 + spikeLength);
          const y2 = cy + Math.sin(angle) * (innerRadius + 4 + spikeLength);

          const grad = ctx.createLinearGradient(x1, y1, x2, y2);
          grad.addColorStop(0, currentTheme.primary);
          grad.addColorStop(1, currentTheme.secondary);

          ctx.strokeStyle = grad;
          ctx.lineWidth = 3;
          ctx.lineCap = 'round';
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        }
      }
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, [isPlaying]);

  const styleOptions: { id: VisualizerStyle; labelBn: string; labelEn: string; icon: React.FC<{ className?: string }> }[] = [
    { id: 'bars', labelBn: 'বার্স', labelEn: 'Bars', icon: BarChart3 },
    { id: 'wave', labelBn: 'ওয়েভ', labelEn: 'Wave', icon: Activity },
    { id: 'particle', labelBn: 'পার্টিকেল', labelEn: 'Particle', icon: Sparkles },
    { id: 'radial', labelBn: 'রেডিয়াল', labelEn: 'Radial', icon: Disc },
  ];

  const colorThemes: { id: ColorTheme; nameBn: string; nameEn: string; color: string }[] = [
    { id: 'cyber', nameBn: 'সাইবার নিয়ন', nameEn: 'Cyber Neon', color: '#a855f7' },
    { id: 'emerald', nameBn: 'এমারেল্ড গ্লাস', nameEn: 'Emerald Glass', color: '#10b981' },
    { id: 'sunset', nameBn: 'সানসেট রোজি', nameEn: 'Sunset Glow', color: '#f43f5e' },
    { id: 'cosmic', nameBn: 'কসমিক ব্লু', nameEn: 'Cosmic Blue', color: '#3b82f6' },
  ];

  return (
    <div className="w-full flex flex-col items-center justify-between space-y-3 p-3.5 glass-card rounded-3xl border border-purple-500/20 bg-slate-950/60 shadow-2xl relative overflow-hidden backdrop-blur-2xl">
      {/* Dynamic Ambient Background Glow */}
      <div
        className="absolute inset-0 pointer-events-none transition-opacity duration-300 rounded-3xl blur-2xl -z-10"
        style={{
          background: `radial-gradient(circle at 50% 50%, rgba(168, 85, 247, ${
            0.15 + (bassLevel / 100) * 0.35
          }), rgba(56, 189, 248, 0.05) 70%, transparent 100%)`,
        }}
      />

      {/* Top Header: Title, Style Quick-Pills & Settings Gear */}
      <div className="w-full flex items-center justify-between gap-2 px-1">
        <div className="flex items-center gap-2">
          <Activity className="w-4 h-4 text-purple-400 animate-pulse" />
          <span className="text-xs font-bold text-white tracking-wide">
            {lang === 'bn' ? 'ভিজ্যুয়ালাইজার' : 'Visualizer'}
          </span>
        </div>

        {/* Quick Style Switcher Pills (bars, wave, particle, radial) */}
        <div className="flex items-center gap-1 bg-white/5 p-1 rounded-2xl border border-white/10 overflow-x-auto no-scrollbar">
          {styleOptions.map((opt) => {
            const Icon = opt.icon;
            const isSelected = style === opt.id;
            return (
              <button
                key={opt.id}
                onClick={() => setStyle(opt.id)}
                className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-semibold whitespace-nowrap transition active:scale-95 ${
                  isSelected
                    ? 'bg-purple-600 text-white shadow-md border border-purple-400/40'
                    : 'text-slate-400 hover:text-white'
                }`}
                title={lang === 'bn' ? opt.labelBn : opt.labelEn}
              >
                <Icon className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">
                  {lang === 'bn' ? opt.labelBn : opt.labelEn}
                </span>
              </button>
            );
          })}

          {/* Visualizer Settings Modal Toggle */}
          <button
            onClick={() => setIsSettingsOpen(!isSettingsOpen)}
            className={`p-1.5 rounded-xl transition border active:scale-95 ${
              isSettingsOpen
                ? 'bg-cyan-500/30 text-cyan-200 border-cyan-400/50 shadow-md'
                : 'text-slate-400 hover:text-white border-transparent'
            }`}
            title={lang === 'bn' ? 'ভিজ্যুয়ালাইজার সেটিংস' : 'Visualizer Settings'}
          >
            <Settings2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Main Canvas Viewport */}
      <div className="w-full h-52 sm:h-56 rounded-2xl bg-black/55 border border-white/10 p-2 flex items-center justify-center relative overflow-hidden shadow-inner">
        <canvas
          ref={canvasRef}
          width={440}
          height={220}
          className="w-full h-full object-contain"
        />

        {/* Center Floating Glass Track Badge in Radial Mode */}
        {style === 'radial' && (
          <div className="absolute w-20 h-20 rounded-full overflow-hidden border-2 border-white/40 shadow-2xl pointer-events-none">
            <img
              src={coverUrl}
              alt={title}
              className={`w-full h-full object-cover ${isPlaying ? 'animate-spin-slow' : ''}`}
            />
          </div>
        )}

        {/* In-Canvas Active Style Badge */}
        <div className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-md bg-black/60 backdrop-blur-md border border-white/15 text-[10px] font-mono font-bold text-purple-300 uppercase">
          {style} MODE
        </div>
      </div>

      {/* Live Energy & Bass Meter Bar */}
      <div className="w-full grid grid-cols-2 gap-2 text-xs font-mono">
        <div className="flex items-center justify-between px-3 py-1.5 rounded-xl glass-card border border-white/5">
          <span className="text-[11px] text-slate-400">
            {lang === 'bn' ? 'সাব-বাস' : 'Sub Bass'}:
          </span>
          <div className="flex items-center gap-1.5">
            <div className="w-16 h-1.5 rounded-full bg-white/10 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full transition-all duration-75"
                style={{ width: `${bassLevel}%` }}
              />
            </div>
            <span className="text-purple-300 font-bold text-[10px] w-6 text-right">
              {bassLevel}%
            </span>
          </div>
        </div>

        <div className="flex items-center justify-between px-3 py-1.5 rounded-xl glass-card border border-white/5">
          <span className="text-[11px] text-slate-400">
            {lang === 'bn' ? 'শক্তি' : 'Energy'}:
          </span>
          <div className="flex items-center gap-1.5">
            <div className="w-16 h-1.5 rounded-full bg-white/10 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-cyan-500 to-blue-500 rounded-full transition-all duration-75"
                style={{ width: `${energyLevel}%` }}
              />
            </div>
            <span className="text-cyan-300 font-bold text-[10px] w-6 text-right">
              {energyLevel}%
            </span>
          </div>
        </div>
      </div>

      {/* ==================================================== */}
      {/* VISUALIZER SETTINGS PANEL / DRAWER */}
      {/* ==================================================== */}
      {isSettingsOpen && (
        <div className="w-full rounded-2xl glass-panel p-4 border border-white/15 bg-slate-900/90 space-y-3.5 animate-fade-in">
          <div className="flex items-center justify-between border-b border-white/10 pb-2">
            <div className="flex items-center gap-2">
              <Sliders className="w-4 h-4 text-cyan-300" />
              <h4 className="text-xs font-bold text-white uppercase tracking-wider">
                {lang === 'bn' ? 'ভিজ্যুয়ালাইজার সেটিংস' : 'Visualizer Settings'}
              </h4>
            </div>
            <button
              onClick={() => setIsSettingsOpen(false)}
              className="p-1 rounded-lg glass-card text-slate-400 hover:text-white"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 1. Visualization Style Rendering Algorithm */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-semibold text-slate-300 block">
              {lang === 'bn' ? 'ভিজ্যুয়ালাইজেশন স্টাইল (Algorithm)' : 'Visualization Style (Algorithm)'}
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {styleOptions.map((opt) => {
                const Icon = opt.icon;
                const isSelected = style === opt.id;
                return (
                  <button
                    key={opt.id}
                    onClick={() => setStyle(opt.id)}
                    className={`p-2 rounded-xl flex items-center gap-2 text-xs font-semibold border transition active:scale-95 ${
                      isSelected
                        ? 'bg-purple-600/40 border-purple-400 text-white shadow-md'
                        : 'glass-card border-white/10 text-slate-400 hover:text-white'
                    }`}
                  >
                    <Icon className="w-3.5 h-3.5 text-purple-300 flex-shrink-0" />
                    <span className="truncate">{lang === 'bn' ? opt.labelBn : opt.labelEn}</span>
                    {isSelected && <Check className="w-3 h-3 text-emerald-400 ml-auto" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* 2. Color Theme */}
          <div className="space-y-1.5">
            <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-300">
              <Palette className="w-3.5 h-3.5 text-purple-400" />
              <span>{lang === 'bn' ? 'কালার থিম' : 'Color Palette'}</span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {colorThemes.map((th) => {
                const isSelected = colorTheme === th.id;
                return (
                  <button
                    key={th.id}
                    onClick={() => setColorTheme(th.id)}
                    className={`p-2 rounded-xl flex items-center gap-2 text-xs font-semibold border transition active:scale-95 ${
                      isSelected
                        ? 'bg-white/15 border-white/40 text-white shadow-sm'
                        : 'glass-card border-white/10 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span
                      className="w-3 h-3 rounded-full flex-shrink-0"
                      style={{ backgroundColor: th.color }}
                    />
                    <span className="truncate">{lang === 'bn' ? th.nameBn : th.nameEn}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 3. Sensitivity / Reactivity Slider */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-[11px] font-semibold text-slate-300">
              <div className="flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5 text-amber-400" />
                <span>{lang === 'bn' ? 'সংবেদনশীলতা (Reactivity)' : 'Audio Sensitivity'}</span>
              </div>
              <span className="font-mono text-cyan-300 font-bold">{sensitivity.toFixed(1)}x</span>
            </div>
            <input
              type="range"
              min="0.5"
              max="2.5"
              step="0.1"
              value={sensitivity}
              onChange={(e) => setSensitivity(parseFloat(e.target.value))}
              className="w-full accent-purple-500 cursor-pointer h-1.5 bg-white/10 rounded-lg appearance-none"
            />
          </div>

          {/* 4. Floor Reflection Toggle (for Bars) */}
          {style === 'bars' && (
            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] font-semibold text-slate-300">
                {lang === 'bn' ? 'মিরর গ্লাস ফ্লোর রিফ্লেকশন' : 'Glass Floor Mirror Reflection'}
              </span>
              <button
                onClick={() => setShowFloorReflection(!showFloorReflection)}
                className={`w-10 h-5 rounded-full p-0.5 transition-colors duration-200 ${
                  showFloorReflection ? 'bg-purple-600' : 'bg-slate-700'
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform duration-200 ${
                    showFloorReflection ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
