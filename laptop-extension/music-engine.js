// StudyGuard Offline Study Audio Engine & Music Manager
// Uses Web Audio API for 100% offline procedural study synthesis & IndexedDB for custom audio tracks

class StudyMusicEngine {
  constructor() {
    this.audioCtx = null;
    this.currentTrack = null;
    this.isPlaying = false;
    this.isLooping = true;
    this.volume = 0.8;
    this.currentTime = 0;
    this.duration = 3600; // 1 hour for infinite synth presets
    this.activeNodes = [];
    this.customAudioElem = null;
    this.progressInterval = null;
    this.onStateChangeCallbacks = [];
    this.masterGain = null;

    // Active playlist queue
    this.currentPlaylist = null;
    this.playlistIndex = 0;

    this.initStorage();
  }

  // Event listener
  onStateChange(cb) {
    this.onStateChangeCallbacks.push(cb);
  }

  notifyStateChange() {
    const state = this.getState();
    this.onStateChangeCallbacks.forEach(cb => {
      try { cb(state); } catch (e) { console.error(e); }
    });
  }

  getState() {
    return {
      currentTrack: this.currentTrack,
      isPlaying: this.isPlaying,
      isLooping: this.isLooping,
      volume: this.volume,
      currentTime: Math.floor(this.currentTime),
      duration: Math.floor(this.duration),
      formattedCurrentTime: this.formatTime(this.currentTime),
      formattedDuration: this.formatTime(this.duration)
    };
  }

  formatTime(seconds) {
    if (isNaN(seconds) || seconds < 0) return "00:00";
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  // --- IndexedDB for custom imported tracks & persistent playlists ---
  initStorage() {
    return new Promise((resolve) => {
      const req = indexedDB.open('StudyGuardMusicDB', 1);
      req.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains('tracks')) {
          db.createObjectStore('tracks', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('playlists')) {
          db.createObjectStore('playlists', { keyPath: 'id' });
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => resolve(null);
    });
  }

  async getDB() {
    return new Promise((resolve) => {
      const req = indexedDB.open('StudyGuardMusicDB', 1);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => resolve(null);
    });
  }

  // --- Default Built-in Focus Audio Presets ---
  getDefaultPresets() {
    return [
      {
        id: 'preset_alpha',
        title: 'Binaural Alpha Waves (432Hz)',
        artistOrCategory: 'Binaural Beats',
        uriOrPath: 'preset://alpha',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: true,
        dateAdded: Date.now()
      },
      {
        id: 'preset_rain',
        title: 'Deep Study Rain & Thunder',
        artistOrCategory: 'Ambient Sounds',
        uriOrPath: 'preset://rain',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: false,
        dateAdded: Date.now()
      },
      {
        id: 'preset_whitenoise',
        title: 'White Noise Flow State',
        artistOrCategory: 'Ambient Sounds',
        uriOrPath: 'preset://whitenoise',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: false,
        dateAdded: Date.now()
      },
      {
        id: 'preset_lofi',
        title: 'Lo-Fi Focus Chamber',
        artistOrCategory: 'Lo-Fi Study',
        uriOrPath: 'preset://lofi',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: true,
        dateAdded: Date.now()
      },
      {
        id: 'preset_nature',
        title: 'Ambient Forest & River Stream',
        artistOrCategory: 'Ambient Sounds',
        uriOrPath: 'preset://nature',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: false,
        dateAdded: Date.now()
      },
      {
        id: 'preset_piano',
        title: 'Classical Focus Flow',
        artistOrCategory: 'Classical Focus',
        uriOrPath: 'preset://piano',
        durationSec: 3600,
        fileSizeFormatted: 'Offline Synth',
        isPreset: true,
        isFavorite: false,
        dateAdded: Date.now()
      }
    ];
  }

  // Fetch all tracks (presets + user uploaded)
  async getAllTracks() {
    const defaultPresets = this.getDefaultPresets();
    const db = await this.getDB();
    if (!db) return defaultPresets;

    return new Promise((resolve) => {
      const tx = db.transaction('tracks', 'readonly');
      const store = tx.objectStore('tracks');
      const req = store.getAll();
      req.onsuccess = () => {
        const customTracks = req.result || [];
        // Merge preset favorites from chrome storage if saved
        chrome.storage.local.get(['presetFavorites'], (data) => {
          const favs = data.presetFavorites || {};
          defaultPresets.forEach(p => {
            if (favs[p.id] !== undefined) p.isFavorite = favs[p.id];
          });
          resolve([...defaultPresets, ...customTracks]);
        });
      };
      req.onerror = () => resolve(defaultPresets);
    });
  }

  // Save custom audio file imported by user
  async saveCustomTrack(file, customTitle, category) {
    const db = await this.getDB();
    if (!db) return null;

    const trackId = 'custom_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
    
    const track = {
      id: trackId,
      title: customTitle.trim() || file.name.replace(/\.[^/.]+$/, ""),
      artistOrCategory: category || 'My Saved Tracks',
      uriOrPath: 'blob://' + trackId,
      blob: file,
      fileSizeFormatted: `${sizeMb} MB`,
      durationSec: 180, // Updated on first load
      isPreset: false,
      isFavorite: false,
      dateAdded: Date.now()
    };

    return new Promise((resolve) => {
      const tx = db.transaction('tracks', 'readwrite');
      const store = tx.objectStore('tracks');
      store.put(track);
      tx.oncomplete = () => resolve(track);
      tx.onerror = () => resolve(null);
    });
  }

  async deleteTrack(trackId) {
    if (this.currentTrack && this.currentTrack.id === trackId) {
      this.stop();
    }
    const db = await this.getDB();
    if (!db) return;
    const tx = db.transaction('tracks', 'readwrite');
    tx.objectStore('tracks').delete(trackId);
  }

  async toggleFavorite(trackId) {
    const defaultPresets = this.getDefaultPresets();
    const isPreset = defaultPresets.some(p => p.id === trackId);
    if (isPreset) {
      chrome.storage.local.get(['presetFavorites'], (data) => {
        const favs = data.presetFavorites || {};
        favs[trackId] = !favs[trackId];
        chrome.storage.local.set({ presetFavorites: favs });
      });
      return;
    }

    const db = await this.getDB();
    if (!db) return;
    const tx = db.transaction('tracks', 'readwrite');
    const store = tx.objectStore('tracks');
    const req = store.get(trackId);
    req.onsuccess = () => {
      const track = req.result;
      if (track) {
        track.isFavorite = !track.isFavorite;
        store.put(track);
      }
    };
  }

  // --- Playlists Management ---
  async getAllPlaylists() {
    const db = await this.getDB();
    if (!db) return [];

    return new Promise((resolve) => {
      const tx = db.transaction('playlists', 'readonly');
      const req = tx.objectStore('playlists').getAll();
      req.onsuccess = () => {
        const list = req.result || [];
        if (list.length === 0) {
          // Provide default starter playlist
          const defaultPl = [
            {
              id: 'pl_deep_focus',
              name: 'Deep Focus Chamber',
              description: 'Calm ambient sounds and binaural alpha waves for 45m sprints',
              colorHex: '#6366F1',
              trackIds: ['preset_alpha', 'preset_rain', 'preset_lofi'],
              createdAt: Date.now()
            },
            {
              id: 'pl_lofi_relax',
              name: 'Lo-Fi Chill & Read',
              description: 'Gentle chords and white noise mask for reading textbooks',
              colorHex: '#10B981',
              trackIds: ['preset_lofi', 'preset_whitenoise'],
              createdAt: Date.now()
            }
          ];
          const writeTx = db.transaction('playlists', 'readwrite');
          const pStore = writeTx.objectStore('playlists');
          defaultPl.forEach(p => pStore.put(p));
          resolve(defaultPl);
        } else {
          resolve(list);
        }
      };
      req.onerror = () => resolve([]);
    });
  }

  async createPlaylist(name, description = '', colorHex = '#6366F1') {
    const db = await this.getDB();
    if (!db) return null;

    const id = 'pl_' + Date.now();
    const playlist = {
      id,
      name: name.trim() || 'Focus Playlist',
      description: description.trim(),
      colorHex: colorHex || '#6366F1',
      trackIds: [],
      createdAt: Date.now()
    };

    return new Promise((resolve) => {
      const tx = db.transaction('playlists', 'readwrite');
      tx.objectStore('playlists').put(playlist);
      tx.oncomplete = () => resolve(playlist);
      tx.onerror = () => resolve(null);
    });
  }

  async addTrackToPlaylist(playlistId, trackId) {
    const db = await this.getDB();
    if (!db) return;
    const tx = db.transaction('playlists', 'readwrite');
    const store = tx.objectStore('playlists');
    const req = store.get(playlistId);
    req.onsuccess = () => {
      const pl = req.result;
      if (pl) {
        if (!pl.trackIds.includes(trackId)) {
          pl.trackIds.push(trackId);
          store.put(pl);
        }
      }
    };
  }

  async removeTrackFromPlaylist(playlistId, trackId) {
    const db = await this.getDB();
    if (!db) return;
    const tx = db.transaction('playlists', 'readwrite');
    const store = tx.objectStore('playlists');
    const req = store.get(playlistId);
    req.onsuccess = () => {
      const pl = req.result;
      if (pl) {
        pl.trackIds = pl.trackIds.filter(id => id !== trackId);
        store.put(pl);
      }
    };
  }

  async deletePlaylist(playlistId) {
    const db = await this.getDB();
    if (!db) return;
    const tx = db.transaction('playlists', 'readwrite');
    tx.objectStore('playlists').delete(playlistId);
  }

  // --- Web Audio Playback & Synthesis ---
  ensureAudioContext() {
    if (!this.audioCtx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      this.audioCtx = new AudioCtx();
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    if (!this.masterGain) {
      this.masterGain = this.audioCtx.createGain();
      this.masterGain.gain.setValueAtTime(this.volume, this.audioCtx.currentTime);
      this.masterGain.connect(this.audioCtx.destination);
    }
  }

  setVolume(vol) {
    this.volume = Math.max(0, Math.min(1, vol));
    if (this.masterGain && this.audioCtx) {
      this.masterGain.gain.setValueAtTime(this.volume, this.audioCtx.currentTime);
    }
    if (this.customAudioElem) {
      this.customAudioElem.volume = this.volume;
    }
    this.notifyStateChange();
  }

  toggleLoop() {
    this.isLooping = !this.isLooping;
    if (this.customAudioElem) {
      this.customAudioElem.loop = this.isLooping;
    }
    this.notifyStateChange();
  }

  async playTrack(track) {
    this.stop();
    this.ensureAudioContext();
    this.currentTrack = track;
    this.isPlaying = true;
    this.currentTime = 0;

    if (track.isPreset) {
      this.duration = 3600; // 1 hour continuous synth
      this.startSynthesizer(track.uriOrPath);
    } else {
      await this.playCustomAudio(track);
    }

    this.startProgressTracker();
    this.notifyStateChange();
  }

  async playPlaylist(playlist, allTracks) {
    if (!playlist || !playlist.trackIds || playlist.trackIds.length === 0) return;
    this.currentPlaylist = playlist;
    this.playlistIndex = 0;
    const firstTrackId = playlist.trackIds[0];
    const track = allTracks.find(t => t.id === firstTrackId);
    if (track) {
      await this.playTrack(track);
    }
  }

  playNextInPlaylist(allTracks) {
    if (!this.currentPlaylist) return;
    this.playlistIndex++;
    if (this.playlistIndex >= this.currentPlaylist.trackIds.length) {
      if (this.isLooping) {
        this.playlistIndex = 0;
      } else {
        this.stop();
        return;
      }
    }
    const nextTrackId = this.currentPlaylist.trackIds[this.playlistIndex];
    const track = allTracks.find(t => t.id === nextTrackId);
    if (track) {
      this.playTrack(track);
    }
  }

  togglePlayPause() {
    if (!this.currentTrack) return;
    if (this.isPlaying) {
      this.pause();
    } else {
      this.resume();
    }
  }

  pause() {
    this.isPlaying = false;
    if (this.customAudioElem) {
      this.customAudioElem.pause();
    }
    if (this.audioCtx) {
      this.audioCtx.suspend();
    }
    if (this.progressInterval) clearInterval(this.progressInterval);
    this.notifyStateChange();
  }

  resume() {
    if (!this.currentTrack) return;
    this.ensureAudioContext();
    this.isPlaying = true;
    if (this.customAudioElem) {
      this.customAudioElem.play();
    }
    this.startProgressTracker();
    this.notifyStateChange();
  }

  stop() {
    this.isPlaying = false;
    this.currentTime = 0;
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
    // Clean up synth nodes
    this.activeNodes.forEach(node => {
      try {
        if (node.stop) node.stop();
        if (node.disconnect) node.disconnect();
      } catch (e) {}
    });
    this.activeNodes = [];

    // Clean up custom audio element
    if (this.customAudioElem) {
      try {
        this.customAudioElem.pause();
        this.customAudioElem.src = '';
      } catch (e) {}
      this.customAudioElem = null;
    }

    this.notifyStateChange();
  }

  seekTo(seconds) {
    this.currentTime = Math.max(0, Math.min(this.duration, seconds));
    if (this.customAudioElem) {
      this.customAudioElem.currentTime = this.currentTime;
    }
    this.notifyStateChange();
  }

  startProgressTracker() {
    if (this.progressInterval) clearInterval(this.progressInterval);
    this.progressInterval = setInterval(() => {
      if (!this.isPlaying) return;
      if (this.customAudioElem) {
        this.currentTime = this.customAudioElem.currentTime;
        this.duration = this.customAudioElem.duration || this.duration;
      } else {
        this.currentTime += 1;
        if (this.currentTime >= this.duration) {
          if (this.isLooping) {
            this.currentTime = 0;
          } else {
            this.stop();
          }
        }
      }
      this.notifyStateChange();
    }, 1000);
  }

  // --- Real Offline Synthesizers using Web Audio API ---
  startSynthesizer(presetUri) {
    const ctx = this.audioCtx;
    const dest = this.masterGain;

    if (presetUri.includes('alpha')) {
      // Binaural Alpha Waves: 432 Hz in Left ear, 442 Hz in Right ear -> 10Hz Alpha oscillation in brain
      const merger = ctx.createChannelMerger(2);

      // Left oscillator (432Hz)
      const oscL = ctx.createOscillator();
      oscL.type = 'sine';
      oscL.frequency.setValueAtTime(432, ctx.currentTime);
      const gainL = ctx.createGain();
      gainL.gain.setValueAtTime(0.3, ctx.currentTime);
      oscL.connect(gainL);
      gainL.connect(merger, 0, 0); // connect to left channel

      // Right oscillator (442Hz)
      const oscR = ctx.createOscillator();
      oscR.type = 'sine';
      oscR.frequency.setValueAtTime(442, ctx.currentTime);
      const gainR = ctx.createGain();
      gainR.gain.setValueAtTime(0.3, ctx.currentTime);
      oscR.connect(gainR);
      gainR.connect(merger, 0, 1); // connect to right channel

      // Ambient low sub-drone for grounding
      const subOsc = ctx.createOscillator();
      subOsc.type = 'triangle';
      subOsc.frequency.setValueAtTime(108, ctx.currentTime);
      const subGain = ctx.createGain();
      subGain.gain.setValueAtTime(0.1, ctx.currentTime);
      subOsc.connect(subGain);
      subGain.connect(dest);

      merger.connect(dest);
      oscL.start();
      oscR.start();
      subOsc.start();
      this.activeNodes.push(oscL, oscR, subOsc, gainL, gainR, subGain, merger);

    } else if (presetUri.includes('rain')) {
      // Deep Study Rain & Thunder: Continuous smooth pink noise + low rumble
      const bufferSize = ctx.sampleRate * 2;
      const noiseBuffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const output = noiseBuffer.getChannelData(0);
      let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        b0 = 0.99886 * b0 + white * 0.0555179;
        b1 = 0.99332 * b1 + white * 0.0750759;
        b2 = 0.96900 * b2 + white * 0.1538520;
        b3 = 0.86650 * b3 + white * 0.3104856;
        b4 = 0.55000 * b4 + white * 0.5329522;
        b5 = -0.7616 * b5 - white * 0.0168980;
        output[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.08;
        b6 = white * 0.115926;
      }

      const whiteNoiseNode = ctx.createBufferSource();
      whiteNoiseNode.buffer = noiseBuffer;
      whiteNoiseNode.loop = true;

      // Bandpass filter for soothing rain sound
      const filter = ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(800, ctx.currentTime);
      filter.Q.setValueAtTime(0.8, ctx.currentTime);

      const gain = ctx.createGain();
      gain.gain.setValueAtTime(0.4, ctx.currentTime);

      // Low rumble for rain/thunder depth
      const rumbleOsc = ctx.createOscillator();
      rumbleOsc.type = 'sine';
      rumbleOsc.frequency.setValueAtTime(55, ctx.currentTime);
      const rumbleGain = ctx.createGain();
      rumbleGain.gain.setValueAtTime(0.15, ctx.currentTime);
      rumbleOsc.connect(rumbleGain);
      rumbleGain.connect(dest);

      whiteNoiseNode.connect(filter);
      filter.connect(gain);
      gain.connect(dest);

      whiteNoiseNode.start();
      rumbleOsc.start();
      this.activeNodes.push(whiteNoiseNode, rumbleOsc, filter, gain, rumbleGain);

    } else if (presetUri.includes('whitenoise')) {
      // White & Brown Noise Flow State
      const bufferSize = ctx.sampleRate * 2;
      const noiseBuffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const output = noiseBuffer.getChannelData(0);
      let lastOut = 0.0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        output[i] = (lastOut + (0.02 * white)) / 1.02; // Brown noise algorithm
        lastOut = output[i];
        output[i] *= 3.5;
      }

      const noise = ctx.createBufferSource();
      noise.buffer = noiseBuffer;
      noise.loop = true;

      const lowpass = ctx.createBiquadFilter();
      lowpass.type = 'lowpass';
      lowpass.frequency.setValueAtTime(650, ctx.currentTime);

      const gain = ctx.createGain();
      gain.gain.setValueAtTime(0.35, ctx.currentTime);

      noise.connect(lowpass);
      lowpass.connect(gain);
      gain.connect(dest);

      noise.start();
      this.activeNodes.push(noise, lowpass, gain);

    } else if (presetUri.includes('lofi')) {
      // Lo-Fi Focus Chamber: Soft rhodes-like dreamy chords & subtle warmth
      const chords = [
        [261.63, 329.63, 392.00, 493.88], // Cmaj7
        [220.00, 261.63, 329.63, 392.00], // Am7
        [174.61, 220.00, 261.63, 329.63], // Fmaj7
        [196.00, 246.94, 293.66, 349.23]  // G7
      ];

      const lofiGain = ctx.createGain();
      lofiGain.gain.setValueAtTime(0.2, ctx.currentTime);
      lofiGain.connect(dest);

      // Low pass to give warm vintage lo-fi character
      const lofiFilter = ctx.createBiquadFilter();
      lofiFilter.type = 'lowpass';
      lofiFilter.frequency.setValueAtTime(900, ctx.currentTime);
      lofiFilter.connect(lofiGain);

      let chordIdx = 0;
      const playChord = () => {
        if (!this.isPlaying) return;
        const currentChord = chords[chordIdx % chords.length];
        chordIdx++;

        currentChord.forEach(freq => {
          const osc = ctx.createOscillator();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(freq, ctx.currentTime);

          const env = ctx.createGain();
          env.gain.setValueAtTime(0.001, ctx.currentTime);
          env.gain.exponentialRampToValueAtTime(0.08, ctx.currentTime + 0.3);
          env.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 3.8);

          osc.connect(env);
          env.connect(lofiFilter);
          osc.start(ctx.currentTime);
          osc.stop(ctx.currentTime + 4.0);
        });

        // Loop the chords every 4 seconds
        const timerId = setTimeout(playChord, 4000);
        this.activeNodes.push({ stop: () => clearTimeout(timerId) });
      };

      playChord();
      this.activeNodes.push(lofiGain, lofiFilter);

    } else if (presetUri.includes('nature')) {
      // Ambient Forest & River: Gentle filtered wind and bubbling stream
      const bufferSize = ctx.sampleRate * 2;
      const noiseBuffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const output = noiseBuffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        output[i] = (Math.random() * 2 - 1) * 0.1;
      }

      const streamSource = ctx.createBufferSource();
      streamSource.buffer = noiseBuffer;
      streamSource.loop = true;

      const streamFilter = ctx.createBiquadFilter();
      streamFilter.type = 'bandpass';
      streamFilter.frequency.setValueAtTime(1400, ctx.currentTime);
      streamFilter.Q.setValueAtTime(1.5, ctx.currentTime);

      const streamGain = ctx.createGain();
      streamGain.gain.setValueAtTime(0.25, ctx.currentTime);

      streamSource.connect(streamFilter);
      streamFilter.connect(streamGain);
      streamGain.connect(dest);

      streamSource.start();
      this.activeNodes.push(streamSource, streamFilter, streamGain);

    } else {
      // Classical Focus Flow: Soothing sine arpeggios
      const notes = [261.63, 329.63, 392.00, 523.25, 659.25, 523.25, 392.00, 329.63];
      let noteIdx = 0;

      const pianoFilter = ctx.createBiquadFilter();
      pianoFilter.type = 'lowpass';
      pianoFilter.frequency.setValueAtTime(1200, ctx.currentTime);
      pianoFilter.connect(dest);

      const playArp = () => {
        if (!this.isPlaying) return;
        const freq = notes[noteIdx % notes.length];
        noteIdx++;

        const osc = ctx.createOscillator();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, ctx.currentTime);

        const env = ctx.createGain();
        env.gain.setValueAtTime(0.001, ctx.currentTime);
        env.gain.exponentialRampToValueAtTime(0.12, ctx.currentTime + 0.05);
        env.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.9);

        osc.connect(env);
        env.connect(pianoFilter);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 1.0);

        const timerId = setTimeout(playArp, 600);
        this.activeNodes.push({ stop: () => clearTimeout(timerId) });
      };

      playArp();
      this.activeNodes.push(pianoFilter);
    }
  }

  // --- HTML5 Audio for Custom User Audio Files ---
  async playCustomAudio(track) {
    let blob = track.blob;
    if (!blob) {
      const db = await this.getDB();
      if (db) {
        blob = await new Promise(resolve => {
          const tx = db.transaction('tracks', 'readonly');
          const req = tx.objectStore('tracks').get(track.id);
          req.onsuccess = () => resolve(req.result ? req.result.blob : null);
          req.onerror = () => resolve(null);
        });
      }
    }

    if (!blob) {
      console.warn("Audio file data missing");
      return;
    }

    const audioUrl = URL.createObjectURL(blob);
    this.customAudioElem = new Audio(audioUrl);
    this.customAudioElem.volume = this.volume;
    this.customAudioElem.loop = this.isLooping;

    this.customAudioElem.onloadedmetadata = () => {
      this.duration = this.customAudioElem.duration || 180;
      this.notifyStateChange();
    };

    this.customAudioElem.onended = () => {
      if (!this.isLooping) {
        if (this.currentPlaylist) {
          // Play next in playlist
          this.getAllTracks().then(tracks => this.playNextInPlaylist(tracks));
        } else {
          this.stop();
        }
      }
    };

    try {
      await this.customAudioElem.play();
    } catch (e) {
      console.error("Audio playback error:", e);
    }
  }
}

// Global singleton instance for extension
window.studyMusicEngine = new StudyMusicEngine();
