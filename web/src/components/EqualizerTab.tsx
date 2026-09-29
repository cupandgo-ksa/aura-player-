import React, { useEffect, useRef, useState } from 'react';
import {
  Sliders,
  Sparkles,
  Zap,
  Activity,
  RotateCcw,
  Volume2,
  Headphones,
} from 'lucide-react';
import { EQ_FREQUENCIES, EQ_PRESETS, AudioEngine } from '../services/audioEngine';
import { EqualizerPreset } from '../types/media';

interface EqualizerTabProps {
  isPlaying: boolean;
  lang: 'bn' | 'en';
}

export const EqualizerTab: React.FC<EqualizerTabProps> = ({ isPlaying, lang }) => {
  const audioEngine = AudioEngine.getInstance();
  const [bandGains, setBandGains] = useState<number[]>([0, 0, 0, 0, 0]);
  const [selectedPresetId, setSelectedPresetId] = useState<string>('flat');
  const [bassBoost, setBassBoost] = useState<number>(30);
  const [trebleBoost, setTrebleBoost] = useState<number>(20);
  const [virtualizer, setVirtualizer] = useState<number>(25);
  const [superBoost, setSuperBoost] = useState<number>(Math.round(audioEngine.getSuperVolumeBoost() * 100));
  const [eightDEnabled, setEightDEnabled] = useState<boolean>(audioEngine.isEightD());

  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Sync with audio engine
  const handleBandChange = (index: number, val: number) => {
    const updated = [...bandGains];
    updated[index] = val;
    setBandGains(updated);
    setSelectedPresetId('custom');
    audioEngine.setEqualizerBandGain(index, val);
  };

  const handleSelectPreset = (preset: EqualizerPreset) => {
    setSelectedPresetId(preset.id);
    setBandGains([...preset.gains]);
    audioEngine.applyPreset(preset);
  };

  const handleBassBoostChange = (val: number) => {
    setBassBoost(val);
    audioEngine.setBassBoost(val);
  };

  const handleTrebleBoostChange = (val: number) => {
    setTrebleBoost(val);
    audioEngine.setTrebleBoost(val);
  };

  const handleSuperBoostChange = (val: number) => {
    setSuperBoost(val);
    audioEngine.setSuperVolumeBoost(val / 100);
  };

  const handleToggleEightD = () => {
    const next = !eightDEnabled;
    setEightDEnabled(next);
    audioEngine.setEightDMode(next);
  };

  const handleReset = () => {
    const flat = EQ_PRESETS.find((p) => p.id === 'flat')!;
    handleSelectPreset(flat);
    handleBassBoostChange(0);
    handleTrebleBoostChange(0);
    handleSuperBoostChange(100);
    setEightDEnabled(false);
    audioEngine.setEightDMode(false);
    setVirtualizer(0);
  };

  // Live Canvas Visualizer
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;
    const bufferLength = 64;
    const dataArray = new Uint8Array(bufferLength);

    const render = () => {
      animId = requestAnimationFrame(render);

      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);

      audioEngine.getVisualizerData(dataArray);

      // Draw glowing glass visualizer bars
      const barCount = 32;
      const barWidth = (width / barCount) * 0.7;
      const gap = (width / barCount) * 0.3;

      for (let i = 0; i < barCount; i++) {
        // Read real or synthesized frequency data
        let value = dataArray[i * 2] || 0;
        if (!isPlaying || value === 0) {
          // Subtle idle gentle wave when paused
          value = Math.sin(Date.now() * 0.003 + i * 0.3) * 12 + 15;
        }

        const barHeight = (value / 255) * (height * 0.85) + 4;
        const x = i * (barWidth + gap) + gap / 2;
        const y = height - barHeight;

        // Gradient for glass bar
        const grad = ctx.createLinearGradient(0, height, 0, y);
        grad.addColorStop(0, 'rgba(168, 85, 247, 0.4)');
        grad.addColorStop(0.7, 'rgba(192, 132, 252, 0.8)');
        grad.addColorStop(1, 'rgba(56, 189, 248, 0.95)');

        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.roundRect(x, y, barWidth, barHeight, [4, 4, 0, 0]);
        ctx.fill();

        // Top specular cap
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(x + barWidth / 2, y + 2, 1.5, 0, Math.PI * 2);
        ctx.fill();
      }
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, [isPlaying]);

  const freqLabels = ['60Hz', '230Hz', '910Hz', '3.6kHz', '14kHz'];

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
      {/* Visualizer Spectrum Panel */}
      <div className="glass-card rounded-3xl p-4 sm:p-5 border border-purple-500/20 bg-slate-900/60 shadow-xl space-y-3 relative overflow-hidden">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-purple-400 animate-pulse" />
            <h3 className="text-sm font-bold text-white">
              {lang === 'bn' ? 'অডিও স্পেকট্রাম ভিজ্যুয়ালাইজার' : 'Audio Spectrum Visualizer'}
            </h3>
          </div>
          <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-400/30">
            {isPlaying
              ? lang === 'bn'
                ? 'রিয়েলটাইম লাইভ'
                : 'LIVE ACTIVE'
              : lang === 'bn'
              ? 'স্ট্যান্ডবাই'
              : 'STANDBY'}
          </span>
        </div>

        {/* Canvas Visualizer */}
        <div className="w-full h-24 rounded-2xl bg-black/40 border border-white/10 p-2 flex items-center justify-center overflow-hidden">
          <canvas
            ref={canvasRef}
            width={500}
            height={90}
            className="w-full h-full object-contain"
          />
        </div>
      </div>

      {/* Preset Selector */}
      <div className="space-y-2">
        <div className="flex items-center justify-between px-1">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-amber-400" />
            <span>{lang === 'bn' ? 'সাউন্ড প্রিসেটস' : 'Sound Presets'}</span>
          </h4>

          <button
            onClick={handleReset}
            className="flex items-center gap-1 text-[11px] text-slate-400 hover:text-white glass-pill px-2.5 py-1 rounded-xl transition"
          >
            <RotateCcw className="w-3 h-3" />
            <span>{lang === 'bn' ? 'রিসেট' : 'Reset'}</span>
          </button>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {EQ_PRESETS.map((preset) => {
            const isSelected = selectedPresetId === preset.id;
            return (
              <button
                key={preset.id}
                onClick={() => handleSelectPreset(preset)}
                className={`py-2 px-3 rounded-xl text-xs font-medium transition-all duration-200 border text-center ${
                  isSelected
                    ? 'bg-purple-600/40 border-purple-400 text-white shadow-lg shadow-purple-600/30 font-bold scale-[1.02]'
                    : 'glass-card border-white/10 text-slate-300 hover:text-white hover:border-white/20'
                }`}
              >
                {lang === 'bn' ? preset.bnName : preset.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* 5-Band Equalizer Sliders */}
      <div className="glass-card rounded-3xl p-5 border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
            <Sliders className="w-3.5 h-3.5 text-purple-400" />
            <span>5-Band Professional EQ</span>
          </h4>
          <span className="text-[11px] font-mono text-purple-300">
            ±12 dB Precision
          </span>
        </div>

        {/* Vertical sliders grid */}
        <div className="grid grid-cols-5 gap-2 sm:gap-4 pt-2">
          {bandGains.map((gain, idx) => (
            <div
              key={idx}
              className="flex flex-col items-center justify-between space-y-3 bg-white/5 rounded-2xl p-2.5 border border-white/5"
            >
              {/* Gain dB value */}
              <span className="text-[11px] font-mono font-bold text-purple-300">
                {gain > 0 ? `+${gain}` : gain} dB
              </span>

              {/* Vertical slider wrapper */}
              <div className="h-36 flex items-center justify-center relative py-1">
                <input
                  type="range"
                  min="-12"
                  max="12"
                  step="1"
                  value={gain}
                  onChange={(e) => handleBandChange(idx, Number(e.target.value))}
                  aria-label={`Gain for ${freqLabels[idx]}`}
                  className="w-32 -rotate-90 origin-center accent-purple-500"
                />
              </div>

              {/* Frequency label */}
              <span className="text-[10px] font-mono font-medium text-slate-300 text-center">
                {freqLabels[idx]}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 200% Super Volume Booster & 8D Spatial Audio Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
        {/* 200% Super Volume Booster */}
        <div className="glass-card rounded-3xl p-4 sm:p-5 border border-amber-500/20 bg-gradient-to-br from-amber-950/20 via-slate-900/60 to-purple-950/20 space-y-3 shadow-xl">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-xl bg-amber-500/20 border border-amber-400/30 text-amber-300">
                <Zap className="w-4 h-4 animate-pulse" />
              </div>
              <div>
                <h4 className="text-xs sm:text-sm font-bold text-white flex items-center gap-1.5">
                  <span>{lang === 'bn' ? '২০০% সুপার ভলিউম বুস্টার' : '200% Super Volume Booster'}</span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/25 text-amber-300 font-mono font-bold">
                    PRO
                  </span>
                </h4>
                <p className="text-[11px] text-slate-400">
                  {lang === 'bn' ? 'কম সাউন্ডের ট্র্যাকে দ্বিগুণ লাউডনেস' : 'Drive gain up to 2.0x (+12dB)'}
                </p>
              </div>
            </div>
            <span
              className={`font-mono text-sm font-bold px-2.5 py-1 rounded-xl border ${
                superBoost > 150
                  ? 'bg-rose-500/20 text-rose-300 border-rose-500/40 animate-pulse'
                  : superBoost > 100
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                  : 'bg-white/10 text-slate-300 border-white/10'
              }`}
            >
              {superBoost}%
            </span>
          </div>

          <input
            type="range"
            min="100"
            max="200"
            step="5"
            value={superBoost}
            onChange={(e) => handleSuperBoostChange(Number(e.target.value))}
            aria-label="Super Volume Boost"
            className="w-full accent-amber-500 cursor-pointer"
          />

          {/* Preset Buttons: 100%, 125%, 150%, 175%, 200% */}
          <div className="grid grid-cols-5 gap-1 pt-1">
            {[100, 125, 150, 175, 200].map((presetVal) => {
              const isCurrent = superBoost === presetVal;
              return (
                <button
                  key={presetVal}
                  onClick={() => handleSuperBoostChange(presetVal)}
                  className={`py-1 rounded-xl text-[11px] font-mono font-semibold transition active:scale-95 ${
                    isCurrent
                      ? 'bg-amber-500 text-slate-950 font-bold shadow-md shadow-amber-500/30'
                      : 'bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5'
                  }`}
                >
                  {presetVal}%
                </button>
              );
            })}
          </div>

          {superBoost > 150 && (
            <p className="text-[10px] text-amber-400/90 font-medium flex items-center gap-1 bg-amber-500/10 p-2 rounded-xl border border-amber-500/20">
              ⚠️ {lang === 'bn' ? 'হেডফোনে উচ্চ ভলিউমে কান সুরক্ষার জন্য সতর্ক থাকুন।' : 'Hearing safety warning: High volume boost active.'}
            </p>
          )}
        </div>

        {/* 8D Spatial Audio 360° Orbit */}
        <div className="glass-card rounded-3xl p-4 sm:p-5 border border-cyan-500/20 bg-gradient-to-br from-cyan-950/20 via-slate-900/60 to-purple-950/20 space-y-3 shadow-xl flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div
                className={`p-2 rounded-xl border transition-all ${
                  eightDEnabled
                    ? 'bg-cyan-500/30 text-cyan-200 border-cyan-400/50 shadow-md shadow-cyan-500/30'
                    : 'bg-white/5 text-slate-400 border-white/10'
                }`}
              >
                <Headphones className={`w-4 h-4 ${eightDEnabled ? 'animate-bounce' : ''}`} />
              </div>
              <div>
                <h4 className="text-xs sm:text-sm font-bold text-white flex items-center gap-1.5">
                  <span>{lang === 'bn' ? '৮ডি অডিও ও ৩৬০° অরবিট' : '8D Spatial Audio & Orbit'}</span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-cyan-500/25 text-cyan-300 font-mono font-bold">
                    8D
                  </span>
                </h4>
                <p className="text-[11px] text-slate-400">
                  {lang === 'bn' ? 'হেডফোনে গান চারপাশ থেকে ৩৬০° ঘুরবে' : 'Stereo panning spatial rotation'}
                </p>
              </div>
            </div>

            <button
              onClick={handleToggleEightD}
              className={`w-12 h-6 rounded-full p-0.5 transition-colors duration-200 ${
                eightDEnabled ? 'bg-cyan-500' : 'bg-slate-700'
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform duration-200 ${
                  eightDEnabled ? 'translate-x-6' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* 8D Orbit Live Status Graphic */}
          <div className="p-3 rounded-2xl bg-black/40 border border-white/5 flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs">
              <span className="text-slate-400">{lang === 'bn' ? 'হেডফোন মোড:' : 'Headphone Mode:'}</span>
              <span
                className={`font-semibold ${
                  eightDEnabled ? 'text-cyan-300' : 'text-slate-500'
                }`}
              >
                {eightDEnabled
                  ? lang === 'bn' ? '৩৬০° ঘূর্ণন সক্রিয়' : '360° Orbiting Active'
                  : lang === 'bn' ? 'বন্ধ (সাধারণ স্টেরিও)' : 'Off (Standard Stereo)'}
              </span>
            </div>
            {eightDEnabled && (
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-400/30 animate-pulse">
                LIVE 8D
              </span>
            )}
          </div>

          <p className="text-[10px] text-slate-400">
            💡 {lang === 'bn' ? 'সেরা অভিজ্ঞতার জন্য হেডফোন বা ইয়ারফোন ব্যবহার করুন।' : 'Put on headphones for the true 360° spatial effect.'}
          </p>
        </div>
      </div>

      {/* Audio FX Dials: Bass Boost, Treble, 3D Surround */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {/* Bass Boost */}
        <div className="glass-card rounded-2xl p-4 border border-white/10 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-white flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-purple-400" />
              {lang === 'bn' ? 'ডিপ বাস বুস্ট' : 'Deep Bass Boost'}
            </span>
            <span className="font-mono text-purple-300 font-bold">{bassBoost}%</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={bassBoost}
            onChange={(e) => handleBassBoostChange(Number(e.target.value))}
            aria-label={lang === 'bn' ? 'ডিপ বাস বুস্ট' : 'Deep Bass Boost'}
            className="w-full accent-purple-500"
          />
          <p className="text-[10px] text-slate-400">
            {lang === 'bn' ? 'হেডফোন ও স্পিকারে ভারী সাব-বাস' : 'Enhanced low-frequency punch'}
          </p>
        </div>

        {/* Treble Boost */}
        <div className="glass-card rounded-2xl p-4 border border-white/10 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-white flex items-center gap-1.5">
              <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
              {lang === 'bn' ? 'ট্রেবল ও ভোকাল' : 'Treble Clarity'}
            </span>
            <span className="font-mono text-cyan-300 font-bold">{trebleBoost}%</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={trebleBoost}
            onChange={(e) => handleTrebleBoostChange(Number(e.target.value))}
            aria-label={lang === 'bn' ? 'ট্রেবল ও ভোকাল' : 'Treble Clarity'}
            className="w-full accent-cyan-400"
          />
          <p className="text-[10px] text-slate-400">
            {lang === 'bn' ? 'স্পষ্ট ভোকাল ও ঝকঝকে সাউন্ড' : 'Crisp high-frequency sparkle'}
          </p>
        </div>

        {/* 3D Surround */}
        <div className="glass-card rounded-2xl p-4 border border-white/10 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-white flex items-center gap-1.5">
              <Headphones className="w-3.5 h-3.5 text-pink-400" />
              {lang === 'bn' ? '৩ডি সারাউন্ড' : '3D Spatial FX'}
            </span>
            <span className="font-mono text-pink-300 font-bold">{virtualizer}%</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={virtualizer}
            onChange={(e) => setVirtualizer(Number(e.target.value))}
            aria-label={lang === 'bn' ? '৩ডি সারাউন্ড' : '3D Spatial FX'}
            className="w-full accent-pink-400"
          />
          <p className="text-[10px] text-slate-400">
            {lang === 'bn' ? 'সিনেমাটিক স্টুডিও অভিজ্ঞতা' : 'Immersive wide soundstage'}
          </p>
        </div>
      </div>
    </div>
  );
};
