import { Track } from '../types/media';

// Helper to generate a PCM WAV blob directly in browser memory (100% offline, zero network)
export function createSynthesizedAudioBlob(
  type: 'lofi' | 'synthwave' | 'ambient' | 'piano' | 'flute',
  durationSeconds = 12
): Blob {
  const sampleRate = 22050;
  const numChannels = 2;
  const totalSamples = sampleRate * durationSeconds;
  const buffer = new ArrayBuffer(44 + totalSamples * numChannels * 2);
  const view = new DataView(buffer);

  // WAV header
  function writeString(offset: number, string: string) {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i));
    }
  }

  writeString(0, 'RIFF');
  view.setUint32(4, 36 + totalSamples * numChannels * 2, true);
  writeString(8, 'WAVE');
  writeString(12, 'fmt ');
  view.setUint32(16, 16, true); // Subchunk1Size (16 for PCM)
  view.setUint16(20, 1, true); // AudioFormat (1 = PCM)
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * numChannels * 2, true); // ByteRate
  view.setUint16(32, numChannels * 2, true); // BlockAlign
  view.setUint16(34, 16, true); // BitsPerSample
  writeString(36, 'data');
  view.setUint32(40, totalSamples * numChannels * 2, true);

  // Synthesize musical waveforms based on type
  const chords = {
    lofi: [
      [261.63, 329.63, 392.0, 493.88], // Cmaj7
      [220.0, 261.63, 329.63, 392.0],  // Am7
      [174.61, 220.0, 261.63, 329.63], // Fmaj7
      [196.0, 246.94, 293.66, 349.23], // G7
    ],
    synthwave: [
      [130.81, 196.0, 246.94, 293.66], // C minor synth
      [116.54, 174.61, 220.0, 261.63], // Bb synth
      [103.83, 155.56, 196.0, 233.08], // Ab synth
      [116.54, 174.61, 220.0, 261.63], // Bb synth
    ],
    ambient: [
      [174.61, 220.0, 261.63, 329.63],
      [196.0, 246.94, 293.66, 349.23],
      [146.83, 174.61, 220.0, 261.63],
    ],
    piano: [
      [261.63, 329.63, 392.0, 523.25],
      [196.0, 246.94, 293.66, 392.0],
      [220.0, 261.63, 329.63, 440.0],
      [174.61, 220.0, 261.63, 349.23],
    ],
    flute: [
      [293.66, 369.99, 440.0, 587.33], // D E G A B
      [329.63, 440.0, 493.88, 659.25],
      [261.63, 329.63, 392.0, 523.25],
    ],
  }[type];

  let offset = 44;
  const chordDuration = 3.5; // seconds per chord

  for (let i = 0; i < totalSamples; i++) {
    const t = i / sampleRate;
    const chordIndex = Math.floor(t / chordDuration) % chords.length;
    const currentChord = chords[chordIndex];
    const chordTime = t % chordDuration;
    const envelope = Math.sin(Math.min(Math.PI, (chordTime / chordDuration) * Math.PI));

    let sampleL = 0;
    let sampleR = 0;

    // Bass note
    const rootFreq = currentChord[0] / 2;
    const bass = Math.sin(2 * Math.PI * rootFreq * t) * 0.35;

    // Harmonic chord notes
    currentChord.forEach((freq, idx) => {
      const pan = (idx - 1.5) * 0.3; // Stereo separation
      let wave = 0;
      if (type === 'synthwave') {
        // Sawtooth approximation for synthwave
        wave = (Math.sin(2 * Math.PI * freq * t) +
                0.5 * Math.sin(4 * Math.PI * freq * t) +
                0.25 * Math.sin(6 * Math.PI * freq * t)) * 0.15;
      } else if (type === 'flute') {
        // Soft airy sine with vibrato
        const vibrato = 1 + 0.015 * Math.sin(2 * Math.PI * 5.5 * t);
        wave = Math.sin(2 * Math.PI * freq * vibrato * t) * 0.18;
      } else {
        // Soft warm sine with subtle overtone
        wave = (Math.sin(2 * Math.PI * freq * t) + 0.2 * Math.sin(4 * Math.PI * freq * t)) * 0.18;
      }
      sampleL += wave * (1 - pan) * envelope;
      sampleR += wave * (1 + pan) * envelope;
    });

    // Subtle rhythmic pulse or vinyl crackle
    const beat = (t % 0.5 < 0.04) ? Math.sin(2 * Math.PI * 80 * (t % 0.5)) * 0.2 : 0;
    const crackle = (Math.random() - 0.5) * 0.008;

    sampleL += bass * 0.8 + beat + crackle;
    sampleR += bass * 0.8 + beat + crackle;

    // Master volume clamp
    const masterVolume = 0.65;
    const intL = Math.max(-1, Math.min(1, sampleL * masterVolume));
    const intR = Math.max(-1, Math.min(1, sampleR * masterVolume));

    view.setInt16(offset, intL < 0 ? intL * 0x8000 : intL * 0x7fff, true);
    view.setInt16(offset + 2, intR < 0 ? intR * 0x8000 : intR * 0x7fff, true);
    offset += 4;
  }

  return new Blob([buffer], { type: 'audio/wav' });
}

// Generate animated SVG / Canvas poster art
export function createGlassAlbumArt(title: string, color1: string, color2: string, iconType: string): string {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500" width="100%" height="100%">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${color1}"/>
          <stop offset="100%" stop-color="${color2}"/>
        </linearGradient>
        <radialGradient id="glow" cx="50%" cy="30%" r="60%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.35"/>
          <stop offset="100%" stop-color="#000000" stop-opacity="0.6"/>
        </radialGradient>
        <filter id="blur" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="30"/>
        </filter>
      </defs>
      <rect width="500" height="500" fill="url(#bg)"/>
      <circle cx="250" cy="180" r="160" fill="${color1}" filter="url(#blur)" opacity="0.6"/>
      <circle cx="340" cy="320" r="140" fill="${color2}" filter="url(#blur)" opacity="0.5"/>
      <rect x="25" y="25" width="450" height="450" rx="36" fill="rgba(255, 255, 255, 0.08)" stroke="rgba(255, 255, 255, 0.22)" stroke-width="2"/>
      
      <!-- Center vinyl groove design -->
      <circle cx="250" cy="230" r="120" fill="none" stroke="rgba(255, 255, 255, 0.15)" stroke-width="2"/>
      <circle cx="250" cy="230" r="90" fill="none" stroke="rgba(255, 255, 255, 0.18)" stroke-width="1.5"/>
      <circle cx="250" cy="230" r="60" fill="rgba(255, 255, 255, 0.12)" stroke="rgba(255, 255, 255, 0.3)" stroke-width="2"/>
      <circle cx="250" cy="230" r="18" fill="${color2}" stroke="#fff" stroke-width="3"/>
      
      <!-- Soft glass specular slash -->
      <path d="M 50,50 L 450,450" stroke="rgba(255,255,255,0.06)" stroke-width="80" stroke-linecap="round"/>
      
      <text x="250" y="400" font-family="Plus Jakarta Sans, sans-serif" font-weight="700" font-size="24" fill="#ffffff" text-anchor="middle" letter-spacing="1">
        ${title}
      </text>
      <text x="250" y="430" font-family="Plus Jakarta Sans, sans-serif" font-weight="500" font-size="14" fill="rgba(255,255,255,0.7)" text-anchor="middle" letter-spacing="2">
        AURA SOFT GLASS
      </text>
    </svg>
  `;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

// Generate animated offline video Blob using Canvas and audio
export async function createOfflineSampleVideo(
  title: string,
  themeColor: string,
  duration = 15
): Promise<Blob> {
  const canvas = document.createElement('canvas');
  canvas.width = 640;
  canvas.height = 360;
  const ctx = canvas.getContext('2d')!;

  const stream = canvas.captureStream(30);

  // Audio track inside video
  const audioBlob = createSynthesizedAudioBlob('synthwave', duration);
  const audioUrl = URL.createObjectURL(audioBlob);
  const audio = new Audio(audioUrl);

  const audioCtx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
  const source = audioCtx.createMediaElementSource(audio);
  const dest = audioCtx.createMediaStreamDestination();
  source.connect(dest);
  source.connect(audioCtx.destination);

  dest.stream.getAudioTracks().forEach((track) => stream.addTrack(track));

  let mimeType = 'video/webm;codecs=vp9,opus';
  if (!MediaRecorder.isTypeSupported(mimeType)) {
    mimeType = 'video/webm';
  }

  const recorder = new MediaRecorder(stream, { mimeType });
  const chunks: Blob[] = [];

  recorder.ondataavailable = (e) => {
    if (e.data.size > 0) chunks.push(e.data);
  };

  return new Promise((resolve) => {
    recorder.onstop = () => {
      audio.pause();
      URL.revokeObjectURL(audioUrl);
      resolve(new Blob(chunks, { type: mimeType }));
    };

    recorder.start();
    audio.play();

    let frame = 0;
    const totalFrames = duration * 30;

    const interval = setInterval(() => {
      frame++;
      const t = frame / 30;

      // Draw beautiful glass visualizer backdrop
      const grad = ctx.createLinearGradient(0, 0, 640, 360);
      grad.addColorStop(0, '#090d16');
      grad.addColorStop(0.5, themeColor);
      grad.addColorStop(1, '#05070a');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 640, 360);

      // Glass orbs
      ctx.save();
      ctx.beginPath();
      ctx.arc(320 + Math.sin(t * 1.5) * 100, 180 + Math.cos(t * 1.2) * 50, 90, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255, 255, 255, 0.08)';
      ctx.fill();
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.2)';
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.restore();

      // Sound spectrum simulation
      ctx.fillStyle = 'rgba(168, 85, 247, 0.7)';
      for (let b = 0; b < 24; b++) {
        const h = Math.abs(Math.sin(t * 4 + b * 0.4)) * 90 + 15;
        const x = 120 + b * 17;
        const y = 280 - h;
        ctx.fillRect(x, y, 12, h);
      }

      // Title & Glass card
      ctx.fillStyle = 'rgba(0, 0, 0, 0.4)';
      ctx.roundRect(140, 40, 360, 70, 16);
      ctx.fill();
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
      ctx.stroke();

      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 22px Plus Jakarta Sans, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(title, 320, 75);

      ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
      ctx.font = '13px Plus Jakarta Sans, sans-serif';
      ctx.fillText('A S Media Player', 320, 96);

      if (frame >= totalFrames) {
        clearInterval(interval);
        recorder.stop();
      }
    }, 1000 / 30);
  });
}
