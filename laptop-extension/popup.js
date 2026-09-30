// StudyGuard Chrome Extension - Popup Controller with Music & Cross-Device Sync
document.addEventListener('DOMContentLoaded', async () => {
  const engine = window.studyMusicEngine;

  // --- TOP NAVIGATION ---
  const tabNavShields = document.getElementById('tabNavShields');
  const tabNavMusic = document.getElementById('tabNavMusic');
  const tabNavSync = document.getElementById('tabNavSync');
  const viewShieldsTab = document.getElementById('viewShieldsTab');
  const viewMusicTab = document.getElementById('viewMusicTab');
  const viewSyncTab = document.getElementById('viewSyncTab');

  const btnPopOutMusic = document.getElementById('btnPopOutMusic');
  const btnTriggerPopOut = document.getElementById('btnTriggerPopOut');

  function openMusicStudioPopup() {
    chrome.windows.create({
      url: chrome.runtime.getURL('music.html'),
      type: 'popup',
      width: 360,
      height: 270,
      focused: true
    });
  }
  if (btnPopOutMusic) btnPopOutMusic.addEventListener('click', openMusicStudioPopup);
  if (btnTriggerPopOut) btnTriggerPopOut.addEventListener('click', openMusicStudioPopup);

  tabNavShields.addEventListener('click', () => switchNavTab('shields'));
  tabNavMusic.addEventListener('click', () => switchNavTab('music'));
  tabNavSync.addEventListener('click', () => switchNavTab('sync'));

  function switchNavTab(tab) {
    tabNavShields.classList.toggle('active', tab === 'shields');
    tabNavMusic.classList.toggle('active', tab === 'music');
    tabNavSync.classList.toggle('active', tab === 'sync');

    viewShieldsTab.style.display = tab === 'shields' ? 'block' : 'none';
    viewMusicTab.style.display = tab === 'music' ? 'block' : 'none';
    viewSyncTab.style.display = tab === 'sync' ? 'block' : 'none';

    if (tab === 'music') reloadMusicData();
    if (tab === 'sync') reloadSyncData();
  }

  // --- SECTION 1: SHIELDS & MARATHON ---
  const shieldToggle = document.getElementById('shieldToggle');
  const statusBadge = document.getElementById('statusBadge');
  const ytVideoUrl = document.getElementById('ytVideoUrl');
  const btnStartMarathon = document.getElementById('btnStartMarathon');
  const presetButtons = document.querySelectorAll('.preset-btn');

  const marathonCard = document.getElementById('marathonCard');
  const marathonSetupView = document.getElementById('marathonSetupView');
  const marathonActiveView = document.getElementById('marathonActiveView');

  const hudIntervalType = document.getElementById('hudIntervalType');
  const hudTimerDigits = document.getElementById('hudTimerDigits');
  const hudCycleText = document.getElementById('hudCycleText');
  const btnOpenVideo = document.getElementById('btnOpenVideo');
  const btnPauseResume = document.getElementById('btnPauseResume');
  const btnStopMarathon = document.getElementById('btnStopMarathon');

  let selectedStudyMin = 45;
  let selectedBreakMin = 15;
  let timerInterval = null;

  // Load saved shield and marathon state
  chrome.storage.local.get(['shieldEnabled', 'studyMarathon'], (data) => {
    const isShieldOn = data.shieldEnabled !== false;
    shieldToggle.checked = isShieldOn;
    updateStatusBadge(isShieldOn);

    if (data.studyMarathon && data.studyMarathon.active) {
      renderActiveMarathon(data.studyMarathon);
    } else {
      renderInactiveMarathon();
    }
  });

  shieldToggle.addEventListener('change', () => {
    const isEnabled = shieldToggle.checked;
    chrome.storage.local.set({ shieldEnabled: isEnabled });
    updateStatusBadge(isEnabled);

    chrome.declarativeNetRequest.updateEnabledRulesets({
      [isEnabled ? 'enableRulesetIds' : 'disableRulesetIds']: ['ruleset_distractions']
    });
  });

  function updateStatusBadge(isEnabled) {
    if (isEnabled) {
      statusBadge.textContent = '⚡ SHIELD ACTIVE';
      statusBadge.style.color = '#34D399';
      statusBadge.style.background = 'rgba(16, 185, 129, 0.2)';
    } else {
      statusBadge.textContent = '⏸️ GUARD PAUSED';
      statusBadge.style.color = '#F87171';
      statusBadge.style.background = 'rgba(239, 68, 68, 0.2)';
    }
  }

  presetButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      presetButtons.forEach(b => b.classList.remove('selected'));
      btn.classList.add('selected');
      selectedStudyMin = parseInt(btn.dataset.study, 10) || 45;
      selectedBreakMin = parseInt(btn.dataset.break, 10) || 15;
    });
  });

  function extractYouTubeId(url) {
    if (!url) return null;
    const trimmed = url.trim();
    if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) return trimmed;
    const match = trimmed.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|live\/))([a-zA-Z0-9_-]{11})/);
    return match ? match[1] : null;
  }

  btnStartMarathon.addEventListener('click', () => {
    const rawUrl = ytVideoUrl.value.trim();
    if (!rawUrl) {
      alert('Please enter a YouTube video URL to start your study marathon!');
      ytVideoUrl.focus();
      return;
    }

    const videoId = extractYouTubeId(rawUrl);
    if (!videoId) {
      alert('Invalid YouTube URL! Please paste a valid YouTube video link (e.g. https://www.youtube.com/watch?v=...)');
      return;
    }

    const targetTime = Date.now() + (selectedStudyMin * 60 * 1000);
    const marathonData = {
      active: true,
      videoId: videoId,
      videoUrl: `https://www.youtube.com/watch?v=${videoId}`,
      studyDurationMin: selectedStudyMin,
      breakDurationMin: selectedBreakMin,
      state: 'study',
      cycle: 1,
      targetTime: targetTime,
      paused: false,
      pausedRemainingMs: selectedStudyMin * 60 * 1000
    };

    chrome.storage.local.set({ studyMarathon: marathonData }, () => {
      renderActiveMarathon(marathonData);

      // Open tab
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const currentTab = tabs[0];
        if (currentTab && currentTab.url && currentTab.url.includes('youtube.com')) {
          chrome.tabs.update(currentTab.id, { url: marathonData.videoUrl });
        } else {
          chrome.tabs.create({ url: marathonData.videoUrl });
        }
      });
    });
  });

  function renderActiveMarathon(marathon) {
    marathonSetupView.style.display = 'none';
    marathonActiveView.style.display = 'block';
    marathonCard.classList.add('active-state');

    updateHudDisplay(marathon);

    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(() => {
      chrome.storage.local.get(['studyMarathon', 'laptopFocusMinutes', 'laptopSessionsCount'], (data) => {
        if (!data.studyMarathon || !data.studyMarathon.active) {
          renderInactiveMarathon();
          return;
        }

        const m = data.studyMarathon;
        const now = Date.now();
        if (!m.paused && now >= m.targetTime) {
          // Completed study cycle or break
          if (m.state === 'study') {
            // Credit study time to laptop stats
            const curMins = data.laptopFocusMinutes || 45;
            const curSessions = data.laptopSessionsCount || 2;
            const newMins = curMins + m.studyDurationMin;
            chrome.storage.local.set({
              laptopFocusMinutes: newMins,
              laptopSessionsCount: curSessions + 1,
              lastSyncTimestamp: Date.now()
            });

            // Transition to break
            m.state = 'break';
            m.targetTime = now + (m.breakDurationMin * 60 * 1000);
            chrome.storage.local.set({ studyMarathon: m });
          } else {
            // Break finished -> next cycle
            m.state = 'study';
            m.cycle = (m.cycle || 1) + 1;
            m.targetTime = now + (m.studyDurationMin * 60 * 1000);
            chrome.storage.local.set({ studyMarathon: m });
          }
        }

        updateHudDisplay(m);
      });
    }, 1000);
  }

  function renderInactiveMarathon() {
    if (timerInterval) clearInterval(timerInterval);
    marathonSetupView.style.display = 'block';
    marathonActiveView.style.display = 'none';
    marathonCard.classList.remove('active-state');
  }

  function formatTime(ms) {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  function updateHudDisplay(marathon) {
    let remainingMs = 0;
    if (marathon.paused) {
      remainingMs = marathon.pausedRemainingMs || 0;
      btnPauseResume.innerHTML = '<span>▶️</span> Resume';
      btnPauseResume.className = 'btn-hud btn-resume';
    } else {
      remainingMs = Math.max(0, marathon.targetTime - Date.now());
      btnPauseResume.innerHTML = '<span>⏸️</span> Pause';
      btnPauseResume.className = 'btn-hud btn-resume';
    }

    hudTimerDigits.textContent = formatTime(remainingMs);

    if (marathon.state === 'study') {
      hudIntervalType.textContent = '📚 STUDY SPRINT';
      hudIntervalType.style.color = '#34D399';
      hudCycleText.textContent = `Cycle #${marathon.cycle || 1} • Lecture Locked`;
    } else {
      hudIntervalType.textContent = '☕ REST BREAK';
      hudIntervalType.style.color = '#F59E0B';
      hudCycleText.textContent = `Break for Cycle #${marathon.cycle || 1}`;
    }
  }

  btnPauseResume.addEventListener('click', () => {
    chrome.storage.local.get(['studyMarathon'], (data) => {
      const marathon = data.studyMarathon;
      if (!marathon || !marathon.active) return;

      if (marathon.paused) {
        marathon.paused = false;
        marathon.targetTime = Date.now() + marathon.pausedRemainingMs;
      } else {
        marathon.paused = true;
        marathon.pausedRemainingMs = Math.max(0, marathon.targetTime - Date.now());
      }

      chrome.storage.local.set({ studyMarathon: marathon }, () => {
        updateHudDisplay(marathon);
      });
    });
  });

  btnOpenVideo.addEventListener('click', () => {
    chrome.storage.local.get(['studyMarathon'], (data) => {
      const marathon = data.studyMarathon;
      if (marathon && marathon.videoUrl) {
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          const currentTab = tabs[0];
          if (currentTab && currentTab.url && currentTab.url.includes(marathon.videoId)) {
            // Already there
          } else {
            chrome.tabs.create({ url: marathon.videoUrl });
          }
        });
      }
    });
  });

  btnStopMarathon.addEventListener('click', () => {
    if (confirm('Are you sure you want to unlock and stop the Study Marathon?')) {
      chrome.storage.local.set({ studyMarathon: { active: false } }, () => {
        renderInactiveMarathon();
      });
    }
  });


  // --- SECTION 2: STUDY MUSIC TAB ---
  let allTracks = [];
  let allPlaylists = [];
  let currentCategory = 'All';
  let activeMusicSubtab = 'tracks';

  const subtabTracks = document.getElementById('subtabTracks');
  const subtabPlaylists = document.getElementById('subtabPlaylists');
  const popupCategoryChips = document.querySelectorAll('.music-chip');
  const popupTracksList = document.getElementById('popupTracksList');
  const popupPlaylistsList = document.getElementById('popupPlaylistsList');
  const popupTracksCount = document.getElementById('popupTracksCount');
  const popupPlaylistsCount = document.getElementById('popupPlaylistsCount');

  const popupFilePicker = document.getElementById('popupFilePicker');
  const btnPopupAddMusic = document.getElementById('btnPopupAddMusic');
  const btnPopupNewPlaylist = document.getElementById('btnPopupNewPlaylist');
  const popupVolumeSlider = document.getElementById('popupVolumeSlider');
  const btnPopupLoop = document.getElementById('btnPopupLoop');

  // Mini-player elements
  const miniPlayerBar = document.getElementById('miniPlayerBar');
  const miniTrackEmoji = document.getElementById('miniTrackEmoji');
  const miniTrackTitle = document.getElementById('miniTrackTitle');
  const miniTrackTime = document.getElementById('miniTrackTime');
  const btnMiniPlayPause = document.getElementById('btnMiniPlayPause');
  const btnMiniStop = document.getElementById('btnMiniStop');

  subtabTracks.addEventListener('click', () => {
    activeMusicSubtab = 'tracks';
    subtabTracks.classList.add('active');
    subtabPlaylists.classList.remove('active');
    popupTracksList.style.display = 'flex';
    popupPlaylistsList.style.display = 'none';
    document.getElementById('popupCategoryChips').style.display = 'flex';
  });

  subtabPlaylists.addEventListener('click', () => {
    activeMusicSubtab = 'playlists';
    subtabPlaylists.classList.add('active');
    subtabTracks.classList.remove('active');
    popupTracksList.style.display = 'none';
    popupPlaylistsList.style.display = 'flex';
    document.getElementById('popupCategoryChips').style.display = 'none';
  });

  popupCategoryChips.forEach(chip => {
    chip.addEventListener('click', () => {
      popupCategoryChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentCategory = chip.dataset.cat;
      renderPopupTracks();
    });
  });

  btnPopupAddMusic.addEventListener('click', () => popupFilePicker.click());
  popupFilePicker.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (file) {
      await engine.saveCustomTrack(file, file.name.replace(/\.[^/.]+$/, ""), "My Saved Tracks");
      await reloadMusicData();
    }
  });

  btnPopupNewPlaylist.addEventListener('click', async () => {
    const name = prompt("Enter Playlist Name (e.g. Focus Flow):");
    if (name && name.trim()) {
      const colors = ['#6366F1', '#10B981', '#06B6D4', '#F59E0B', '#EC4899', '#8B5CF6'];
      const color = colors[Math.floor(Math.random() * colors.length)];
      await engine.createPlaylist(name, "Study Focus Playlist", color);
      await reloadMusicData();
    }
  });

  popupVolumeSlider.addEventListener('input', (e) => {
    engine.setVolume(parseFloat(e.target.value));
  });

  const btnPopupShuffle = document.getElementById('btnPopupShuffle');
  if (btnPopupShuffle) {
    btnPopupShuffle.addEventListener('click', () => {
      engine.toggleShuffle();
      btnPopupShuffle.textContent = engine.isShuffle ? '🔀 Shuffle: ON' : '🔀 Shuffle: OFF';
      btnPopupShuffle.style.color = engine.isShuffle ? '#818CF8' : '#94A3B8';
      btnPopupShuffle.style.background = engine.isShuffle ? 'rgba(99,102,241,0.2)' : 'rgba(255,255,255,0.06)';
    });
  }

  btnPopupLoop.addEventListener('click', () => {
    engine.toggleLoop();
    btnPopupLoop.textContent = engine.isLooping ? '🔁 Loop: ON' : '➡️ Loop: OFF';
    btnPopupLoop.style.color = engine.isLooping ? '#818CF8' : '#94A3B8';
  });

  btnMiniPlayPause.addEventListener('click', () => engine.togglePlayPause());
  btnMiniStop.addEventListener('click', () => engine.stop());

  engine.onStateChange((state) => {
    if (state.currentTrack) {
      miniTrackTitle.textContent = state.currentTrack.title;
      miniTrackTime.textContent = `${state.formattedCurrentTime} / ${state.formattedDuration}`;
      miniTrackEmoji.textContent = getTrackEmoji(state.currentTrack.artistOrCategory);
    }
    btnMiniPlayPause.textContent = state.isPlaying ? '⏸️' : '▶️';

    // Highlight playing track row
    document.querySelectorAll('.track-row').forEach(row => {
      const isThis = state.isPlaying && state.currentTrack && row.dataset.id === state.currentTrack.id;
      row.classList.toggle('playing', isThis);
    });
  });

  function getTrackEmoji(cat) {
    if (cat.includes('Binaural')) return '🧘';
    if (cat.includes('Ambient')) return '🌧️';
    if (cat.includes('Lo-Fi')) return '☕';
    if (cat.includes('Classical')) return '🎹';
    return '🎧';
  }

  async function reloadMusicData() {
    allTracks = await engine.getAllTracks();
    allPlaylists = await engine.getAllPlaylists();
    popupTracksCount.textContent = allTracks.length;
    popupPlaylistsCount.textContent = allPlaylists.length;
    renderPopupTracks();
    renderPopupPlaylists();
  }

  function renderPopupTracks() {
    popupTracksList.innerHTML = '';
    const filtered = currentCategory === 'All' 
      ? allTracks 
      : allTracks.filter(t => t.artistOrCategory === currentCategory);

    filtered.forEach(track => {
      const isPlaying = engine.isPlaying && engine.currentTrack && engine.currentTrack.id === track.id;
      const row = document.createElement('div');
      row.className = `track-row ${isPlaying ? 'playing' : ''}`;
      row.dataset.id = track.id;

      row.innerHTML = `
        <div class="track-row-left">
          <div class="track-row-thumb">${getTrackEmoji(track.artistOrCategory)}</div>
          <div style="min-width: 0;">
            <div class="track-row-title">${track.title}</div>
            <div class="track-row-cat">${track.artistOrCategory}</div>
          </div>
        </div>
        <div class="track-row-actions">
          <button type="button" class="btn-fav-small ${track.isFavorite ? 'fav' : ''}" title="Favorite">
            ${track.isFavorite ? '❤️' : '🤍'}
          </button>
          <button type="button" class="btn-fav-small btn-add-pl" title="Add to Playlist" style="font-size: 11px;">
            ➕📑
          </button>
          <button type="button" class="btn-action" style="padding: 4px 8px; font-size: 10px; width: auto;">
            ${isPlaying ? 'Playing' : 'Play'}
          </button>
        </div>
      `;

      row.addEventListener('click', (e) => {
        if (e.target.closest('.btn-fav-small')) return;
        engine.playTrack(track);
      });

      const btnFav = row.querySelector('.btn-fav-small');
      btnFav.addEventListener('click', async (e) => {
        e.stopPropagation();
        await engine.toggleFavorite(track.id);
        await reloadMusicData();
      });

      const btnAddPl = row.querySelector('.btn-add-pl');
      if (btnAddPl) {
        btnAddPl.addEventListener('click', async (e) => {
          e.stopPropagation();
          if (allPlaylists.length === 0) {
            alert("No playlists created yet. Create a playlist first using 'New Playlist'!");
            return;
          }
          const options = allPlaylists.map((p, idx) => `${idx + 1}. ${p.name}`).join('\n');
          const choice = prompt(`Add "${track.title}" to playlist:\n\n${options}\n\nEnter number (1-${allPlaylists.length}):`);
          if (!choice) return;
          const num = parseInt(choice.trim(), 10);
          if (num >= 1 && num <= allPlaylists.length) {
            const targetPl = allPlaylists[num - 1];
            await engine.addTrackToPlaylist(targetPl.id, track.id);
            alert(`Added "${track.title}" to playlist "${targetPl.name}"!`);
            await reloadMusicData();
          }
        });
      }

      popupTracksList.appendChild(row);
    });
  }

  function renderPopupPlaylists() {
    popupPlaylistsList.innerHTML = '';
    allPlaylists.forEach(pl => {
      const row = document.createElement('div');
      row.className = 'playlist-row';
      row.style.borderLeft = `3px solid ${pl.colorHex}`;

      row.innerHTML = `
        <div style="flex: 1; min-width: 0;">
          <div style="font-size: 12px; font-weight: 800; color: white;">${pl.name}</div>
          <div style="font-size: 10px; color: #94A3B8;">${pl.trackIds ? pl.trackIds.length : 0} study tracks</div>
        </div>
        <div style="display: flex; gap: 4px; align-items: center;">
          <button type="button" class="btn-action btn-play-order" title="Play In Order (Sequential)" style="padding: 4px 7px; font-size: 9.5px; width: auto; background: ${pl.colorHex}; border-radius: 6px;">
            ▶️ Order
          </button>
          <button type="button" class="btn-action btn-play-shuffle" title="Play Shuffled (Randomized)" style="padding: 4px 7px; font-size: 9.5px; width: auto; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.15); border-radius: 6px;">
            🔀 Shuffle
          </button>
          <button type="button" class="btn-del-pl btn-fav-small" title="Delete Playlist" style="font-size: 11px; opacity: 0.7; padding: 2px;">
            🗑️
          </button>
        </div>
      `;

      const btnOrder = row.querySelector('.btn-play-order');
      btnOrder.addEventListener('click', (e) => {
        e.stopPropagation();
        engine.playPlaylist(pl, allTracks, false);
      });

      const btnShuffle = row.querySelector('.btn-play-shuffle');
      btnShuffle.addEventListener('click', (e) => {
        e.stopPropagation();
        engine.playPlaylist(pl, allTracks, true);
      });

      const btnDel = row.querySelector('.btn-del-pl');
      btnDel.addEventListener('click', async (e) => {
        e.stopPropagation();
        if (confirm(`Delete playlist "${pl.name}"?`)) {
          await engine.deletePlaylist(pl.id);
          await reloadMusicData();
        }
      });

      popupPlaylistsList.appendChild(row);
    });
  }

  // Initial preload of music tracks
  reloadMusicData();


  // --- SECTION 3: CROSS-DEVICE SYNC HUB ---
  const syncPhoneMins = document.getElementById('syncPhoneMins');
  const syncLaptopMins = document.getElementById('syncLaptopMins');
  const syncCombinedMins = document.getElementById('syncCombinedMins');
  const syncLastSyncText = document.getElementById('syncLastSyncText');
  const inputSyncPairingCode = document.getElementById('inputSyncPairingCode');
  const btnPairDevice = document.getElementById('btnPairDevice');
  const btnCopyLaptopJson = document.getElementById('btnCopyLaptopJson');
  const btnImportPhoneJson = document.getElementById('btnImportPhoneJson');
  const addTimeBtns = document.querySelectorAll('.btn-add-time');

  function reloadSyncData() {
    chrome.storage.local.get([
      'phoneFocusMinutes',
      'laptopFocusMinutes',
      'laptopSessionsCount',
      'laptopDistractionsBlocked',
      'syncPairingCode',
      'lastSyncTimestamp'
    ], (data) => {
      const phoneM = data.phoneFocusMinutes !== undefined ? data.phoneFocusMinutes : 75;
      const laptopM = data.laptopFocusMinutes !== undefined ? data.laptopFocusMinutes : 45;
      const combined = phoneM + laptopM;

      syncPhoneMins.textContent = `${phoneM}m`;
      syncLaptopMins.textContent = `${laptopM}m`;
      syncCombinedMins.textContent = combined >= 60 ? `${(combined / 60).toFixed(1)}h` : `${combined}m`;

      if (data.syncPairingCode) {
        inputSyncPairingCode.value = data.syncPairingCode;
      }

      const lastTs = data.lastSyncTimestamp;
      if (lastTs) {
        const diffSec = Math.floor((Date.now() - lastTs) / 1000);
        if (diffSec < 60) syncLastSyncText.textContent = "Last Synced: Just now";
        else syncLastSyncText.textContent = `Last Synced: ${Math.floor(diffSec / 60)}m ago`;
      }
    });
  }

  // Quick Add Laptop Study Time buttons (+15m, +30m, etc.)
  addTimeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const added = parseInt(btn.dataset.mins, 10) || 15;
      chrome.storage.local.get(['laptopFocusMinutes', 'laptopSessionsCount'], (data) => {
        const curMins = data.laptopFocusMinutes || 45;
        const curSessions = data.laptopSessionsCount || 2;
        chrome.storage.local.set({
          laptopFocusMinutes: curMins + added,
          laptopSessionsCount: curSessions + 1,
          lastSyncTimestamp: Date.now()
        }, () => {
          reloadSyncData();
          alert(`Added +${added}m study time to your laptop stats! 🚀`);
        });
      });
    });
  });

  // Pair code
  btnPairDevice.addEventListener('click', () => {
    const code = inputSyncPairingCode.value.trim().toUpperCase();
    if (!code) {
      alert("Please enter the sync code shown in your Android app's Sync Hub!");
      return;
    }
    chrome.storage.local.set({
      syncPairingCode: code,
      lastSyncTimestamp: Date.now()
    }, () => {
      reloadSyncData();
      alert(`Paired successfully with StudyGuard (${code})! 🔗`);
    });
  });

  // Copy laptop stats JSON to paste into Android app's Sync screen
  btnCopyLaptopJson.addEventListener('click', () => {
    chrome.storage.local.get([
      'laptopFocusMinutes',
      'laptopSessionsCount',
      'laptopDistractionsBlocked',
      'syncPairingCode'
    ], (data) => {
      const payload = {
        version: 2,
        syncCode: data.syncPairingCode || 'SG-CHROME',
        timestamp: Date.now(),
        deviceName: "Google Chrome (Laptop)",
        laptopFocusMinutes: data.laptopFocusMinutes || 45,
        laptopSessionsCount: data.laptopSessionsCount || 2,
        laptopDistractionsBlocked: data.laptopDistractionsBlocked || 8,
        totalFocusMinutes: data.laptopFocusMinutes || 45
      };
      navigator.clipboard.writeText(JSON.stringify(payload, null, 2)).then(() => {
        alert("Copied Laptop Study Stats JSON! 📋\nNow in the Android app, go to Sync Hub -> 'Import Sync' and paste it!");
      });
    });
  });

  // Paste phone JSON from Android app
  btnImportPhoneJson.addEventListener('click', () => {
    const raw = prompt("Paste the Sync JSON exported from your StudyGuard phone app:");
    if (!raw) return;
    try {
      const obj = JSON.parse(raw);
      const phoneMins = obj.phoneFocusMinutes !== undefined ? obj.phoneFocusMinutes : (obj.totalFocusMinutes || 0);
      const phoneDistr = obj.phoneDistractionsBlocked || obj.distractionsBlocked || 0;
      const code = obj.syncCode || '';

      chrome.storage.local.set({
        phoneFocusMinutes: phoneMins,
        phoneDistractions: phoneDistr,
        syncPairingCode: code || undefined,
        lastSyncTimestamp: Date.now()
      }, () => {
        reloadSyncData();
        alert(`Successfully imported Phone Stats! 📱\nPhone Study Time: ${phoneMins}m\nCombined Total updated!`);
      });
    } catch (e) {
      alert("Invalid JSON format. Please copy the export data directly from the phone app.");
    }
  });

  reloadSyncData();
});
