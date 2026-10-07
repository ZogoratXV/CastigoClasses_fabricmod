"""Original synthetic Orison cue, no sampled or extracted third-party audio.
Requires numpy and soundfile. Run from any directory; writes the bundled OGG.
"""
from pathlib import Path
import numpy as np
import soundfile as sf

rate = 44100
duration = 2.2
t = np.arange(int(rate * duration)) / rate
audio = np.zeros_like(t)
# A soft ascending major chord with delicate, slightly inharmonic bell partials.
for start, frequency, gain in [(0.03, 523.25, .26), (.15, 659.25, .20), (.29, 783.99, .17), (.45, 1046.50, .10)]:
    local = np.maximum(0, t - start)
    envelope = (1 - np.exp(-local * 65)) * np.exp(-local * 2.4) * (t >= start)
    voice = np.sin(2 * np.pi * frequency * local)
    voice += .18 * np.sin(2 * np.pi * frequency * 2.003 * local) * np.exp(-local * 4)
    voice += .06 * np.sin(2 * np.pi * frequency * 3.97 * local) * np.exp(-local * 6)
    audio += gain * envelope * voice
# Breathy upward sweep, filtered deterministic noise, then a short reverberant tail.
rng = np.random.default_rng(4026)
noise = np.convolve(rng.normal(size=len(t)), np.ones(23) / 23, mode='same')
audio += .11 * noise * np.sin(np.pi * np.minimum(t / .65, 1)) ** 2 * np.exp(-t * 2)
dry = audio.copy()
for delay, gain in [(.09, .17), (.17, .12), (.28, .08), (.41, .045)]:
    offset = int(delay * rate)
    audio[offset:] += dry[:-offset] * gain
audio *= np.minimum(1, np.maximum(0, (duration - t) / .25))
audio *= .72 / max(np.max(np.abs(audio)), 1e-8)
out = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/castigoclasses/sounds/skills/orison.ogg'
out.parent.mkdir(parents=True, exist_ok=True)
sf.write(out, audio, rate, format='OGG', subtype='VORBIS')
decoded, decoded_rate = sf.read(out)
assert decoded_rate == rate and decoded.ndim == 1 and np.isfinite(decoded).all()
assert np.max(np.abs(decoded)) < .95
print(f'{out.name}: {len(decoded)/rate:.2f}s mono, peak={np.max(np.abs(decoded)):.3f}, {out.stat().st_size} bytes')
