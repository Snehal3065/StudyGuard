// StudyGuard Standalone Music Studio Controller
document.addEventListener('DOMContentLoaded', async () => {
  const engine = window.studyMusicEngine;
  let allTracks = [];
  let allPlaylists = [];
  let currentCategory = 'All';
  let activeTab = 'tracks';

  // Elements
  const tabTracks = document.getElementById('tabTracks');
  const tabPlaylists = document.getElementById('tabPlaylists');
  const tracksView = document.getElementById('tracksView');
  const playlistsView = document.getElementById('playlistsView');
  const categoryChips = document.querySelectorAll('.chip');
  const tracksCount = document.getElementById('tracksCount');
  const playlistsCount = document.getElementById('playlistsCount');

  // Player Elements
  const playerTrackTitle = document.getElementById('playerTrackTitle');
  const playerTrackCat = document.getElementById('playerTrackCat');
  const playerThumb = document.getElementById('playerThumb');
  const btnPlayPause = document.getElementById('btnPlayPause');
  const btnStop = document.getElementById('btnStop');
  const btnPrev = document.getElementById('btnPrev');
  const btnNext = document.getElementById('btnNext');
  const btnLoop = document.getElementById('btnLoop');
  const btnShuffle = document.getElementById('btnShuffle');
  const btnToggleCompact = document.getElementById('btnToggleCompact');
  const seekSlider = document.getElementById('seekSlider');
  const volumeSlider = document.getElementById('volumeSlider');
  const currentTimeText = document.getElementById('currentTimeText');
  const durationText = document.getElementById('durationText');

  // Modal & File Picker
  const filePicker = document.getElementById('filePicker');
  const btnAddMusic = document.getElementById('btnAddMusic');
  const btnCreatePlaylist = document.getElementById('btnCreatePlaylist');
  const modalCreatePlaylist = document.getElementById('modalCreatePlaylist');
  const inputPlaylistName = document.getElementById('inputPlaylistName');
  const inputPlaylistDesc = document.getElementById('inputPlaylistDesc');
  const btnCancelModal = document.getElementById('btnCancelModal');
  const btnSavePlaylist = document.getElementById('btnSavePlaylist');

  // 1. Initial Load
  await reloadData();

  // 2. Tab switching
  tabTracks.addEventListener('click', () => {
    activeTab = 'tracks';
    tabTracks.classList.add('active');
    tabPlaylists.classList.remove('active');
    tracksView.style.display = 'flex';
    playlistsView.style.display = 'none';
    document.getElementById('categoryChips').style.display = 'flex';
  });

  tabPlaylists.addEventListener('click', () => {
    activeTab = 'playlists';
    tabPlaylists.classList.add('active');
    tabTracks.classList.remove('active');
    tracksView.style.display = 'none';
    playlistsView.style.display = 'grid';
    document.getElementById('categoryChips').style.display = 'none';
  });

  // 3. Category Filter
  categoryChips.forEach(chip => {
    chip.addEventListener('click', () => {
      categoryChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentCategory = chip.dataset.cat;
      renderTracks();
    });
  });

  // 4. File picker
  btnAddMusic.addEventListener('click', () => filePicker.click());
  filePicker.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (file) {
      await engine.saveCustomTrack(file, file.name.replace(/\.[^/.]+$/, ""), "My Saved Tracks");
      await reloadData();
    }
  });

  // 5. Modal playlist creation
  btnCreatePlaylist.addEventListener('click', () => {
    inputPlaylistName.value = '';
    inputPlaylistDesc.value = '';
    modalCreatePlaylist.style.display = 'flex';
  });

  btnCancelModal.addEventListener('click', () => {
    modalCreatePlaylist.style.display = 'none';
  });

  btnSavePlaylist.addEventListener('click', async () => {
    const name = inputPlaylistName.value.trim();
    if (!name) return;
    const colors = ['#6366F1', '#10B981', '#06B6D4', '#F59E0B', '#EC4899', '#8B5CF6'];
    const color = colors[Math.floor(Math.random() * colors.length)];
    await engine.createPlaylist(name, inputPlaylistDesc.value, color);
    modalCreatePlaylist.style.display = 'none';
    await reloadData();
  });

  // 6. Player Control Handlers
  btnPlayPause.addEventListener('click', () => {
    if (!engine.currentTrack && allTracks.length > 0) {
      engine.playTrack(allTracks[0]);
    } else {
      engine.togglePlayPause();
    }
  });

  btnStop.addEventListener('click', () => engine.stop());

  btnLoop.addEventListener('click', () => {
    engine.toggleLoop();
    btnLoop.classList.toggle('active', engine.isLooping);
  });

  const labelCurrentMode = document.getElementById('labelCurrentMode');
  const labelPlaylistName = document.getElementById('labelPlaylistName');

  function updateModeLabel() {
    if (labelCurrentMode) {
      labelCurrentMode.textContent = engine.isShuffle ? 'Mode: 🔀 Shuffled' : 'Mode: ➡️ In Order';
    }
    if (labelPlaylistName) {
      labelPlaylistName.textContent = engine.currentPlaylist ? engine.currentPlaylist.name : 'Focus Presets';
    }
  }

  if (btnShuffle) {
    btnShuffle.addEventListener('click', () => {
      engine.toggleShuffle();
      btnShuffle.classList.toggle('active', engine.isShuffle);
      updateModeLabel();
    });
  }

  if (btnToggleCompact) {
    btnToggleCompact.addEventListener('click', () => {
      document.body.classList.toggle('compact-mode');
      const isCompact = document.body.classList.contains('compact-mode');
      btnToggleCompact.textContent = isCompact ? '⤢ Make Bigger' : '⤡ Make Smaller';
      try {
        localStorage.setItem('studyguard_compact_music', isCompact ? '1' : '0');
        if (window.resizeTo) {
          if (isCompact) {
            window.resizeTo(360, 270);
          } else {
            window.resizeTo(520, 720);
          }
        }
      } catch (_) {}
    });

    const savedCompact = localStorage.getItem('studyguard_compact_music');
    if (savedCompact === '0') {
      document.body.classList.remove('compact-mode');
      btnToggleCompact.textContent = '⤡ Make Smaller';
    } else {
      document.body.classList.add('compact-mode');
      btnToggleCompact.textContent = '⤢ Make Bigger';
    }
  }

  seekSlider.addEventListener('input', (e) => {
    engine.seekTo(parseFloat(e.target.value));
  });

  volumeSlider.addEventListener('input', (e) => {
    engine.setVolume(parseFloat(e.target.value));
  });

  btnNext.addEventListener('click', () => {
    if (allTracks.length === 0) return;
    const currentId = engine.currentTrack ? engine.currentTrack.id : null;
    const idx = allTracks.findIndex(t => t.id === currentId);
    const nextIdx = (idx + 1) % allTracks.length;
    engine.playTrack(allTracks[nextIdx]);
  });

  btnPrev.addEventListener('click', () => {
    if (allTracks.length === 0) return;
    const currentId = engine.currentTrack ? engine.currentTrack.id : null;
    const idx = allTracks.findIndex(t => t.id === currentId);
    const prevIdx = (idx - 1 + allTracks.length) % allTracks.length;
    engine.playTrack(allTracks[prevIdx]);
  });

  // 7. Engine State Observer
  engine.onStateChange((state) => {
    if (state.currentTrack) {
      playerTrackTitle.textContent = state.currentTrack.title;
      playerTrackCat.textContent = state.currentTrack.artistOrCategory;
      playerThumb.textContent = getTrackEmoji(state.currentTrack.artistOrCategory);
    }
    btnPlayPause.textContent = state.isPlaying ? '⏸️' : '▶️';
    currentTimeText.textContent = state.formattedCurrentTime;
    durationText.textContent = state.formattedDuration;
    seekSlider.max = state.duration || 3600;
    seekSlider.value = state.currentTime || 0;
    btnLoop.classList.toggle('active', state.isLooping);

    // Update track list playing state
    document.querySelectorAll('.track-card').forEach(card => {
      const isCardPlaying = state.isPlaying && state.currentTrack && card.dataset.id === state.currentTrack.id;
      card.classList.toggle('playing', isCardPlaying);
    });
  });

  function getTrackEmoji(cat) {
    if (cat.includes('Binaural')) return '🧘';
    if (cat.includes('Ambient')) return '🌧️';
    if (cat.includes('Lo-Fi')) return '☕';
    if (cat.includes('Classical')) return '🎹';
    return '🎧';
  }

  async function reloadData() {
    allTracks = await engine.getAllTracks();
    allPlaylists = await engine.getAllPlaylists();
    tracksCount.textContent = allTracks.length;
    playlistsCount.textContent = allPlaylists.length;
    renderTracks();
    renderPlaylists();
  }

  function renderTracks() {
    tracksView.innerHTML = '';
    const filtered = currentCategory === 'All' 
      ? allTracks 
      : allTracks.filter(t => t.artistOrCategory === currentCategory);

    if (filtered.length === 0) {
      tracksView.innerHTML = `
        <div style="text-align: center; padding: 40px; color: #64748B;">
          <div style="font-size: 36px; margin-bottom: 8px;">🎧</div>
          <div style="font-weight: 700; color: #94A3B8;">No tracks found in this category</div>
          <div style="font-size: 12px; margin-top: 4px;">Click "Add Music" to import your own study audio.</div>
        </div>
      `;
      return;
    }

    filtered.forEach(track => {
      const isPlaying = engine.isPlaying && engine.currentTrack && engine.currentTrack.id === track.id;
      const card = document.createElement('div');
      card.className = `track-card ${isPlaying ? 'playing' : ''}`;
      card.dataset.id = track.id;

      card.innerHTML = `
        <div class="track-meta">
          <div class="track-thumb">${getTrackEmoji(track.artistOrCategory)}</div>
          <div>
            <div class="track-title">${track.title}</div>
            <div class="track-sub">${track.artistOrCategory} • ${track.fileSizeFormatted}</div>
          </div>
        </div>
        <div class="track-actions">
          <button type="button" class="icon-btn btn-fav ${track.isFavorite ? 'fav' : ''}" title="Favorite">
            ${track.isFavorite ? '❤️' : '🤍'}
          </button>
          <button type="button" class="icon-btn btn-add-pl" title="Add to Playlist">
            ➕📑
          </button>
          ${!track.isPreset ? `
            <button type="button" class="icon-btn btn-del" title="Delete Track">🗑️</button>
          ` : ''}
          <button type="button" class="btn-primary" style="padding: 6px 12px; font-size: 11px;">
            ${isPlaying ? 'Playing 🎵' : 'Play ▶'}
          </button>
        </div>
      `;

      // Play track on card click (excluding action buttons)
      card.addEventListener('click', (e) => {
        if (e.target.closest('.icon-btn')) return;
        engine.playTrack(track);
      });

      // Favorite toggle
      const btnFav = card.querySelector('.btn-fav');
      btnFav.addEventListener('click', async (e) => {
        e.stopPropagation();
        await engine.toggleFavorite(track.id);
        await reloadData();
      });

      // Add to playlist button
      const btnAddPl = card.querySelector('.btn-add-pl');
      btnAddPl.addEventListener('click', (e) => {
        e.stopPropagation();
        showAddToPlaylistModal(track);
      });

      // Delete custom track
      const btnDel = card.querySelector('.btn-del');
      if (btnDel) {
        btnDel.addEventListener('click', async (e) => {
          e.stopPropagation();
          if (confirm(`Delete "${track.title}"?`)) {
            await engine.deleteTrack(track.id);
            await reloadData();
          }
        });
      }

      tracksView.appendChild(card);
    });
  }

  // Add to Playlist modal helper
  const modalAddToPlaylist = document.getElementById('modalAddToPlaylist');
  const addToPlaylistTrackTitle = document.getElementById('addToPlaylistTrackTitle');
  const addToPlaylistOptions = document.getElementById('addToPlaylistOptions');
  const btnCancelAddToPlaylist = document.getElementById('btnCancelAddToPlaylist');

  btnCancelAddToPlaylist?.addEventListener('click', () => {
    modalAddToPlaylist.style.display = 'none';
  });

  function showAddToPlaylistModal(track) {
    addToPlaylistTrackTitle.textContent = `🎵 ${track.title}`;
    addToPlaylistOptions.innerHTML = '';

    if (allPlaylists.length === 0) {
      addToPlaylistOptions.innerHTML = `
        <div style="color: #94A3B8; font-size: 12px; text-align: center; padding: 12px;">
          No playlists available. Please create a playlist first!
        </div>
      `;
    } else {
      allPlaylists.forEach(pl => {
        const item = document.createElement('div');
        item.style.cssText = `
          background: #0B0F19; border: 1px solid rgba(255,255,255,0.1); border-radius: 10px;
          padding: 10px 14px; display: flex; align-items: center; justify-content: space-between;
          cursor: pointer; transition: all 0.15s;
        `;
        const alreadyIn = pl.trackIds && pl.trackIds.includes(track.id);
        item.innerHTML = `
          <div>
            <div style="font-weight: 700; color: white; font-size: 13px;">${pl.name}</div>
            <div style="color: #64748B; font-size: 11px;">${pl.trackIds ? pl.trackIds.length : 0} songs • ${alreadyIn ? '✅ Already in playlist' : 'Click to add'}</div>
          </div>
          <button type="button" class="btn-primary" style="padding: 4px 10px; font-size: 11px; background: ${alreadyIn ? '#334155' : pl.colorHex};">
            ${alreadyIn ? 'Added ✓' : '+ Add'}
          </button>
        `;
        item.addEventListener('click', async () => {
          await engine.addTrackToPlaylist(pl.id, track.id);
          modalAddToPlaylist.style.display = 'none';
          await reloadData();
        });
        addToPlaylistOptions.appendChild(item);
      });
    }

    modalAddToPlaylist.style.display = 'flex';
  }

  function renderPlaylists() {
    playlistsView.innerHTML = '';
    allPlaylists.forEach(pl => {
      const card = document.createElement('div');
      card.className = 'playlist-card';
      card.style.borderLeft = `4px solid ${pl.colorHex}`;

      card.innerHTML = `
        <div style="flex: 1;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div class="playlist-badge" style="background: ${pl.colorHex}25; color: ${pl.colorHex};">
              📑
            </div>
            <button type="button" class="btn-del-pl" title="Delete Playlist" style="background: none; border: none; cursor: pointer; font-size: 13px; opacity: 0.6;">
              🗑️
            </button>
          </div>
          <div class="playlist-name">${pl.name}</div>
          <div class="playlist-desc">${pl.description || 'Focus study playlist'}</div>
        </div>
        <div class="playlist-footer">
          <span style="font-size: 11px; color: #94A3B8;">${pl.trackIds ? pl.trackIds.length : 0} tracks</span>
          <div style="display: flex; gap: 6px;">
            <button type="button" class="btn-primary btn-play-order" style="padding: 4px 8px; font-size: 10.5px; background: ${pl.colorHex};">
              ▶️ Order
            </button>
            <button type="button" class="btn-secondary btn-play-shuffle" style="padding: 4px 8px; font-size: 10.5px;">
              🔀 Shuffle
            </button>
          </div>
        </div>
      `;

      card.querySelector('.btn-play-order').addEventListener('click', (e) => {
        e.stopPropagation();
        engine.playPlaylist(pl, allTracks, false);
        updateModeLabel();
      });

      card.querySelector('.btn-play-shuffle').addEventListener('click', (e) => {
        e.stopPropagation();
        engine.playPlaylist(pl, allTracks, true);
        updateModeLabel();
      });

      card.querySelector('.btn-del-pl').addEventListener('click', async (e) => {
        e.stopPropagation();
        if (confirm(`Delete playlist "${pl.name}"?`)) {
          await engine.deletePlaylist(pl.id);
          await reloadData();
        }
      });

      playlistsView.appendChild(card);
    });
  }
});
