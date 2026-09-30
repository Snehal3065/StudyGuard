// StudyGuard YouTube Content Script
// Blocks Subscriptions, Shorts, and Algorithmic Recommendations
// Enforces Single-Video Study Marathon with 45m Study / 15m Break intervals

(function () {
  'use strict';

  const AVATAR_URL = chrome.runtime.getURL('avatar.png');
  const ICON_URL = chrome.runtime.getURL('icon128.png');

  let currentMarathon = null;
  let hudElement = null;
  let breakOverlayElement = null;
  let marathonInterval = null;
  let isShieldActive = true;

  // 1. INJECT DISTRACTION-BLOCKING CSS IMMEDIATELY (Zero flicker)
  const styleEl = document.createElement('style');
  styleEl.id = 'studyguard-style';
  styleEl.textContent = `
    /* HIDE SHORTS BUTTONS & SECTIONS */
    a#endpoint[title="Shorts"],
    ytd-guide-entry-renderer:has(a[title="Shorts"]),
    ytd-mini-guide-entry-renderer[aria-label="Shorts"],
    ytd-reel-shelf-renderer,
    ytd-rich-shelf-renderer[is-shorts],
    ytd-rich-section-renderer:has(ytd-reel-shelf-renderer),
    ytd-guide-entry-renderer:has(a[href^="/shorts"]) {
      display: none !important;
    }

    /* HIDE SUBSCRIPTIONS TAB & GUIDE SECTIONS */
    a#endpoint[href="/feed/subscriptions"],
    ytd-guide-entry-renderer:has(a[href="/feed/subscriptions"]),
    ytd-mini-guide-entry-renderer[aria-label="Subscriptions"],
    ytd-guide-section-renderer:has(#guide-section-title):has(a[href*="/@"]),
    ytd-guide-section-renderer:has(ytd-guide-entry-renderer a[href="/feed/subscriptions"]) {
      display: none !important;
    }

    /* HIDE 'YOU' / LIBRARY / EXPLORE SECTIONS IN SIDEBAR (KEEP ONLY HOME) */
    ytd-guide-entry-renderer:has(a[href="/feed/you"]),
    ytd-guide-entry-renderer:has(a[href="/feed/history"]),
    ytd-mini-guide-entry-renderer[aria-label="You"],
    ytd-mini-guide-entry-renderer[aria-label="Library"],
    #sections > ytd-guide-section-renderer:nth-child(n+3) {
      display: none !important;
    }

    /* HIDE HOME PAGE ALGORITHMIC RECOMMENDATIONS GRID */
    ytd-browse[page-subtype="home"] ytd-rich-grid-renderer #contents,
    ytd-browse[page-subtype="home"] #chips,
    ytd-browse[page-subtype="home"] ytd-feed-filter-chip-bar-renderer {
      display: none !important;
    }

    /* HIDE WATCH PAGE RECOMMENDATIONS & COMMENTS */
    #secondary,
    #related,
    ytd-watch-next-secondary-results-renderer,
    .html5-endscreen,
    .ytp-ce-element,
    .ytp-ce-video,
    .ytp-ce-playlist,
    #comments,
    ytd-comments {
      display: none !important;
    }

    /* EXPAND VIDEO PLAYER TO CLEAN CENTERED VIEW */
    ytd-watch-flexy:not([theater]):not([fullscreen]) #primary.ytd-watch-flexy {
      max-width: 100% !important;
      margin: 0 auto !important;
    }

    /* HOME FOCUS DASHBOARD STYLES */
    .studyguard-home-dashboard {
      max-width: 680px;
      margin: 60px auto 40px;
      background: linear-gradient(145deg, #0F172A 0%, #1E1B4B 100%);
      border: 1px solid rgba(99, 102, 241, 0.4);
      border-radius: 24px;
      padding: 36px 32px;
      text-align: center;
      color: #F8FAFC;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    .sg-home-avatar {
      width: 76px;
      height: 76px;
      border-radius: 50%;
      border: 3px solid #818CF8;
      box-shadow: 0 0 20px rgba(129, 140, 248, 0.6);
      margin-bottom: 16px;
      object-fit: cover;
    }
    .sg-home-title {
      font-size: 24px;
      font-weight: 800;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .sg-home-pill {
      display: inline-block;
      background: rgba(16, 185, 129, 0.2);
      color: #34D399;
      border: 1px solid rgba(52, 211, 153, 0.4);
      padding: 4px 14px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      margin-bottom: 18px;
    }
    .sg-home-desc {
      font-size: 14px;
      color: #94A3B8;
      line-height: 1.6;
      margin-bottom: 24px;
    }
    .sg-home-input-row {
      display: flex;
      gap: 10px;
      max-width: 520px;
      margin: 0 auto;
    }
    .sg-home-input {
      flex: 1;
      background: #0B0F19;
      border: 1px solid #334155;
      color: white;
      border-radius: 12px;
      padding: 12px 16px;
      font-size: 13px;
      outline: none;
    }
    .sg-home-input:focus {
      border-color: #818CF8;
    }
    .sg-home-btn {
      background: linear-gradient(135deg, #4F46E5, #7C3AED);
      color: white;
      border: none;
      border-radius: 12px;
      padding: 12px 20px;
      font-weight: 700;
      font-size: 13px;
      cursor: pointer;
      white-space: nowrap;
    }

    /* FLOATING STUDY MARATHON HUD */
    .studyguard-marathon-hud {
      position: fixed;
      top: 72px;
      right: 24px;
      z-index: 999999;
      background: rgba(15, 23, 42, 0.95);
      border: 1px solid rgba(99, 102, 241, 0.4);
      backdrop-filter: blur(12px);
      border-radius: 18px;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      transition: transform 0.2s ease, opacity 0.2s ease;
    }
    .sg-hud-avatar {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      border: 2px solid #818CF8;
      object-fit: cover;
    }
    .sg-hud-details {
      display: flex;
      flex-direction: column;
    }
    .sg-hud-status {
      font-size: 10px;
      font-weight: 800;
      color: #34D399;
      letter-spacing: 0.6px;
      text-transform: uppercase;
    }
    .sg-hud-timer {
      font-size: 24px;
      font-weight: 900;
      color: #FFFFFF;
      letter-spacing: 1px;
      font-variant-numeric: tabular-nums;
      line-height: 1.1;
    }
    .sg-hud-cycle {
      font-size: 11px;
      color: #94A3B8;
      font-weight: 600;
    }
    .sg-hud-controls {
      display: flex;
      gap: 6px;
      margin-left: 6px;
    }
    .sg-hud-btn {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: white;
      border-radius: 8px;
      padding: 6px 10px;
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
    }
    .sg-hud-btn:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    /* FULL-SCREEN 15-MIN BREAK OVERLAY */
    .studyguard-break-overlay {
      position: fixed;
      inset: 0;
      z-index: 10000000;
      background: rgba(11, 15, 25, 0.96);
      backdrop-filter: blur(20px);
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      padding: 24px;
    }
    .sg-break-card {
      background: linear-gradient(135deg, #1E1B4B 0%, #0F172A 100%);
      border: 2px solid rgba(245, 158, 11, 0.5);
      border-radius: 28px;
      padding: 44px 40px;
      max-width: 520px;
      width: 100%;
      text-align: center;
      color: #F8FAFC;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.8);
    }
    .sg-break-avatar {
      width: 88px;
      height: 88px;
      border-radius: 50%;
      border: 3px solid #F59E0B;
      box-shadow: 0 0 25px rgba(245, 158, 11, 0.5);
      margin: 0 auto 16px;
      object-fit: cover;
    }
    .sg-break-badge {
      display: inline-block;
      background: rgba(245, 158, 11, 0.2);
      color: #FBBF24;
      border: 1px solid rgba(245, 158, 11, 0.4);
      padding: 4px 16px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.5px;
      margin-bottom: 12px;
    }
    .sg-break-title {
      font-size: 28px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .sg-break-timer {
      font-size: 52px;
      font-weight: 900;
      color: #F59E0B;
      font-variant-numeric: tabular-nums;
      margin: 16px 0;
      letter-spacing: 2px;
    }
    .sg-break-tips {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 14px;
      padding: 14px 18px;
      font-size: 13px;
      color: #CBD5E1;
      line-height: 1.5;
      margin-bottom: 24px;
      text-align: left;
    }
    .sg-break-btn {
      background: linear-gradient(135deg, #10B981, #059669);
      color: white;
      border: none;
      padding: 12px 28px;
      border-radius: 12px;
      font-size: 14px;
      font-weight: 800;
      cursor: pointer;
      box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
    }

    /* FULL-SCREEN MARATHON LOCK RESTRICTION MODAL */
    .studyguard-locked-warning {
      position: fixed;
      inset: 0;
      z-index: 9999999;
      background: rgba(11, 15, 25, 0.98);
      backdrop-filter: blur(16px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      color: white;
      text-align: center;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
  `;
  document.documentElement.appendChild(styleEl);

  // 2. WEB AUDIO API CHIMES (Synthesizer, offline, zero-lag)
  function playSoundChime(type) {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();

      if (type === 'break') {
        // Melodic triple chime: C5 -> G5 -> C6 (Restful)
        const notes = [523.25, 783.99, 1046.50];
        notes.forEach((f, i) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(f, ctx.currentTime + i * 0.22);
          gain.gain.setValueAtTime(0.25, ctx.currentTime + i * 0.22);
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.22 + 0.6);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(ctx.currentTime + i * 0.22);
          osc.stop(ctx.currentTime + i * 0.22 + 0.6);
        });
      } else {
        // Upbeat chime: E5 -> A5 (Energizing resume)
        const notes = [659.25, 880.00];
        notes.forEach((f, i) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(f, ctx.currentTime + i * 0.18);
          gain.gain.setValueAtTime(0.28, ctx.currentTime + i * 0.18);
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.18 + 0.5);
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start(ctx.currentTime + i * 0.18);
          osc.stop(ctx.currentTime + i * 0.18 + 0.5);
        });
      }
    } catch (e) {
      console.warn('StudyGuard AudioContext warning:', e);
    }
  }

  // 3. EXTRACT YOUTUBE VIDEO ID
  function extractYouTubeId(url) {
    if (!url) return null;
    const trimmed = url.trim();
    if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) return trimmed;
    const match = trimmed.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|live\/))([a-zA-Z0-9_-]{11})/);
    return match ? match[1] : null;
  }

  function getCurrentVideoId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('v');
  }

  // 4. CLIENT NAVIGATION ENFORCEMENT
  function enforceNavigationRules() {
    if (!isShieldActive) return;

    const path = window.location.pathname;

    // Block Shorts URL navigation
    if (path.startsWith('/shorts/')) {
      window.location.replace('https://www.youtube.com/');
      return;
    }

    // Block Subscriptions, Explore, Trending URLs
    if (path.startsWith('/feed/subscriptions') ||
        path.startsWith('/feed/explore') ||
        path.startsWith('/feed/trending') ||
        path.startsWith('/feed/you') ||
        path.startsWith('/feed/history')) {
      window.location.replace('https://www.youtube.com/');
      return;
    }

    // MARATHON LOCK ENFORCEMENT
    if (currentMarathon && currentMarathon.active && currentMarathon.videoId) {
      const currentId = getCurrentVideoId();
      if (currentId !== currentMarathon.videoId) {
        // The user navigated away from the locked marathon lecture!
        showMarathonLockScreen(currentMarathon);
      } else {
        removeMarathonLockScreen();
      }
    } else {
      removeMarathonLockScreen();
    }
  }

  function showMarathonLockScreen(marathon) {
    let lockScreen = document.getElementById('studyguard-lock-screen');
    if (!lockScreen) {
      lockScreen = document.createElement('div');
      lockScreen.id = 'studyguard-lock-screen';
      lockScreen.className = 'studyguard-locked-warning';
      lockScreen.innerHTML = `
        <div style="max-width: 480px; background: #1E1B4B; padding: 36px; border-radius: 24px; border: 1px solid #818CF8; box-shadow: 0 20px 50px rgba(0,0,0,0.8);">
          <img src="${AVATAR_URL}" onerror="this.src='${ICON_URL}'" style="width: 80px; height: 80px; border-radius: 50%; border: 3px solid #818CF8; margin-bottom: 16px; object-fit: cover;">
          <h2 style="font-size: 22px; font-weight: 800; margin-bottom: 10px;">🎓 Study Marathon in Progress</h2>
          <p style="color: #CBD5E1; font-size: 14px; line-height: 1.5; margin-bottom: 24px;">
            You have committed to a locked study marathon. All other YouTube videos and feeds are locked until you complete your lecture!
          </p>
          <button id="sg-btn-return-lecture" style="background: linear-gradient(135deg, #10B981, #059669); color: white; border: none; padding: 12px 28px; border-radius: 12px; font-weight: 800; font-size: 14px; cursor: pointer;">
            Return to Lecture 📚
          </button>
        </div>
      `;
      document.body.appendChild(lockScreen);

      document.getElementById('sg-btn-return-lecture')?.addEventListener('click', () => {
        window.location.replace(`https://www.youtube.com/watch?v=${marathon.videoId}`);
      });
    }
  }

  function removeMarathonLockScreen() {
    const lockScreen = document.getElementById('studyguard-lock-screen');
    if (lockScreen) lockScreen.remove();
  }

  // 5. INJECT CLEAN HOME FOCUS DASHBOARD (Replacing recommendation grid)
  function renderHomeDashboard() {
    if (!isShieldActive) return;
    if (window.location.pathname !== '/' && window.location.pathname !== '') return;

    if (document.getElementById('studyguard-home-dash')) return;

    const browseContainer = document.querySelector('ytd-browse[page-subtype="home"]') || document.querySelector('ytd-browse');
    if (!browseContainer) return;

    const dash = document.createElement('div');
    dash.id = 'studyguard-home-dash';
    dash.className = 'studyguard-home-dashboard';
    dash.innerHTML = `
      <img src="${AVATAR_URL}" class="sg-home-avatar" onerror="this.src='${ICON_URL}'" alt="StudyGuard Avatar">
      <div class="sg-home-title">StudyGuard YouTube Focus Shield</div>
      <div class="sg-home-pill">⚡ RECOMMENDATIONS & SUBSCRIPTIONS LOCKED</div>
      <p class="sg-home-desc">
        Algorithmic clickbait and endless feeds are hidden to protect your focus.<br>
        Search directly for a study topic above, or paste a lecture link below to begin a <strong>45/15 Study Marathon</strong>.
      </p>
      <div class="sg-home-input-row">
        <input type="text" id="sg-home-yt-url" class="sg-home-input" placeholder="Paste YouTube lecture link here (e.g. 3-hour tutorial)...">
        <button id="sg-home-start-btn" class="sg-home-btn">🚀 Start Marathon</button>
      </div>
    `;

    browseContainer.prepend(dash);

    document.getElementById('sg-home-start-btn')?.addEventListener('click', () => {
      const input = document.getElementById('sg-home-yt-url');
      const videoId = extractYouTubeId(input?.value);
      if (!videoId) {
        alert('Please paste a valid YouTube video link!');
        return;
      }
      const marathonData = {
        active: true,
        videoId: videoId,
        videoUrl: `https://www.youtube.com/watch?v=${videoId}`,
        studyDurationMin: 45,
        breakDurationMin: 15,
        state: 'study',
        cycle: 1,
        targetTime: Date.now() + (45 * 60 * 1000),
        paused: false,
        pausedRemainingMs: 45 * 60 * 1000
      };
      chrome.storage.local.set({ studyMarathon: marathonData }, () => {
        window.location.href = marathonData.videoUrl;
      });
    });
  }

  // 6. IN-PAGE MARATHON FLOATING HUD & 15-MINUTE BREAK LOGIC
  function setupMarathonHud() {
    if (!currentMarathon || !currentMarathon.active) {
      if (hudElement) hudElement.remove();
      if (breakOverlayElement) breakOverlayElement.remove();
      if (marathonInterval) clearInterval(marathonInterval);
      return;
    }

    const currentId = getCurrentVideoId();
    if (currentId !== currentMarathon.videoId) {
      if (hudElement) hudElement.remove();
      return;
    }

    if (!hudElement || !document.getElementById('studyguard-marathon-hud')) {
      hudElement = document.createElement('div');
      hudElement.id = 'studyguard-marathon-hud';
      hudElement.className = 'studyguard-marathon-hud';
      hudElement.style.cursor = 'grab';
      hudElement.title = 'Click and drag to move this widget';

      // Restore saved position if available
      try {
        const savedPos = JSON.parse(localStorage.getItem('studyguard_hud_pos') || 'null');
        if (savedPos && savedPos.left && savedPos.top) {
          hudElement.style.right = 'auto';
          hudElement.style.left = savedPos.left;
          hudElement.style.top = savedPos.top;
        }
      } catch (_) {}

      // Draggable HUD by mouse
      let isDragging = false;
      let dragStartX = 0;
      let dragStartY = 0;
      let initialLeft = 0;
      let initialTop = 0;

      hudElement.addEventListener('mousedown', (e) => {
        if (e.target.closest('button') || e.target.closest('input')) return;
        isDragging = true;
        hudElement.style.cursor = 'grabbing';
        hudElement.style.userSelect = 'none';

        const rect = hudElement.getBoundingClientRect();
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        initialLeft = rect.left;
        initialTop = rect.top;

        hudElement.style.right = 'auto';
        hudElement.style.left = `${initialLeft}px`;
        hudElement.style.top = `${initialTop}px`;
        e.preventDefault();
      });

      document.addEventListener('mousemove', (e) => {
        if (!isDragging || !hudElement) return;
        const deltaX = e.clientX - dragStartX;
        const deltaY = e.clientY - dragStartY;

        let newLeft = initialLeft + deltaX;
        let newTop = initialTop + deltaY;

        newLeft = Math.max(10, Math.min(window.innerWidth - hudElement.offsetWidth - 10, newLeft));
        newTop = Math.max(10, Math.min(window.innerHeight - hudElement.offsetHeight - 10, newTop));

        hudElement.style.left = `${newLeft}px`;
        hudElement.style.top = `${newTop}px`;
      });

      document.addEventListener('mouseup', () => {
        if (isDragging && hudElement) {
          isDragging = false;
          hudElement.style.cursor = 'grab';
          hudElement.style.userSelect = '';
          try {
            localStorage.setItem('studyguard_hud_pos', JSON.stringify({
              left: hudElement.style.left,
              top: hudElement.style.top
            }));
          } catch (_) {}
        }
      });

      document.body.appendChild(hudElement);
    }

    updateMarathonHudDisplay();

    if (marathonInterval) clearInterval(marathonInterval);
    marathonInterval = setInterval(() => {
      chrome.storage.local.get(['studyMarathon'], (data) => {
        if (!data.studyMarathon || !data.studyMarathon.active) {
          setupMarathonHud();
          return;
        }
        currentMarathon = data.studyMarathon;
        tickMarathon();
      });
    }, 1000);
  }

  function formatTime(ms) {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  function tickMarathon() {
    if (!currentMarathon || !currentMarathon.active || currentMarathon.paused) {
      updateMarathonHudDisplay();
      return;
    }

    const remainingMs = currentMarathon.targetTime - Date.now();

    // VIDEO COMPLETION CHECK
    const video = document.querySelector('video');
    if (video && (video.ended || (video.duration > 30 && video.currentTime >= video.duration - 2))) {
      triggerMarathonCompleted();
      return;
    }

    if (remainingMs <= 0) {
      if (currentMarathon.state === 'study') {
        // STUDY PERIOD FINISHED -> START 15-MIN BREAK!
        playSoundChime('break');
        video?.pause();

        currentMarathon.state = 'break';
        currentMarathon.targetTime = Date.now() + (currentMarathon.breakDurationMin * 60 * 1000);
        chrome.storage.local.set({ studyMarathon: currentMarathon }, () => {
          showBreakOverlay();
        });
      } else {
        // BREAK PERIOD FINISHED -> START NEXT STUDY SPRINT!
        playSoundChime('resume');
        hideBreakOverlay();

        currentMarathon.state = 'study';
        currentMarathon.cycle = (currentMarathon.cycle || 1) + 1;
        currentMarathon.targetTime = Date.now() + (currentMarathon.studyDurationMin * 60 * 1000);
        chrome.storage.local.set({ studyMarathon: currentMarathon }, () => {
          video?.play();
        });
      }
    }

    if (currentMarathon.state === 'break') {
      // Force pause video during break
      if (video && !video.paused) {
        video.pause();
      }
      updateBreakOverlayTimer(remainingMs);
    }

    updateMarathonHudDisplay();
  }

  function updateMarathonHudDisplay() {
    if (!hudElement) return;

    let remainingMs = 0;
    if (currentMarathon.paused) {
      remainingMs = currentMarathon.pausedRemainingMs || 0;
    } else {
      remainingMs = Math.max(0, currentMarathon.targetTime - Date.now());
    }

    const timeStr = formatTime(remainingMs);
    const isStudy = currentMarathon.state === 'study';

    hudElement.innerHTML = `
      <img src="${AVATAR_URL}" class="sg-hud-avatar" onerror="this.src='${ICON_URL}'" alt="Avatar">
      <div class="sg-hud-details">
        <div class="sg-hud-status" style="color: ${isStudy ? '#34D399' : '#F59E0B'}">
          ${isStudy ? '📚 STUDY FOCUS' : '☕ REST BREAK'}
        </div>
        <div class="sg-hud-timer">${timeStr}</div>
        <div class="sg-hud-cycle">Sprint #${currentMarathon.cycle || 1} • Lecture Locked</div>
      </div>
      <div class="sg-hud-controls">
        <button id="sg-hud-btn-pause" class="sg-hud-btn">
          ${currentMarathon.paused ? '▶️' : '⏸️'}
        </button>
        <button id="sg-hud-btn-unlock" class="sg-hud-btn" style="color: #F87171;">
          ⏹️
        </button>
      </div>
    `;

    document.getElementById('sg-hud-btn-pause')?.addEventListener('click', togglePauseMarathon);
    document.getElementById('sg-hud-btn-unlock')?.addEventListener('click', unlockMarathonPrompt);
  }

  function togglePauseMarathon() {
    if (!currentMarathon) return;
    if (currentMarathon.paused) {
      currentMarathon.paused = false;
      currentMarathon.targetTime = Date.now() + (currentMarathon.pausedRemainingMs || 0);
    } else {
      currentMarathon.paused = true;
      currentMarathon.pausedRemainingMs = Math.max(0, currentMarathon.targetTime - Date.now());
    }
    chrome.storage.local.set({ studyMarathon: currentMarathon }, () => {
      updateMarathonHudDisplay();
    });
  }

  function unlockMarathonPrompt() {
    if (confirm('Unlock this Study Marathon? (You will be able to browse freely again)')) {
      chrome.storage.local.set({ studyMarathon: { active: false } }, () => {
        currentMarathon = null;
        setupMarathonHud();
      });
    }
  }

  // 7. BREAK OVERLAY (15-Minute Rest Screen)
  function showBreakOverlay() {
    if (breakOverlayElement) breakOverlayElement.remove();

    breakOverlayElement = document.createElement('div');
    breakOverlayElement.id = 'studyguard-break-screen';
    breakOverlayElement.className = 'studyguard-break-overlay';
    breakOverlayElement.innerHTML = `
      <div class="sg-break-card">
        <img src="${AVATAR_URL}" class="sg-break-avatar" onerror="this.src='${ICON_URL}'" alt="Avatar">
        <div class="sg-break-badge">☕ SCHEDULED STUDY BREAK</div>
        <h2 class="sg-break-title">Time to Recharge!</h2>
        <div class="sg-break-timer" id="sg-break-timer-display">15:00</div>
        <div class="sg-break-tips">
          💧 <strong>Hydrate & Stretch:</strong> Step away from the laptop, rest your eyes, drink a glass of water, and do a quick shoulder stretch. Video is paused.
        </div>
        <button id="sg-btn-end-break-early" class="sg-break-btn">
          ⚡ Ready to Study? Resume Sprint
        </button>
      </div>
    `;
    document.body.appendChild(breakOverlayElement);

    document.getElementById('sg-btn-end-break-early')?.addEventListener('click', () => {
      // User ended break early
      currentMarathon.state = 'study';
      currentMarathon.cycle = (currentMarathon.cycle || 1) + 1;
      currentMarathon.targetTime = Date.now() + (currentMarathon.studyDurationMin * 60 * 1000);
      chrome.storage.local.set({ studyMarathon: currentMarathon }, () => {
        hideBreakOverlay();
        document.querySelector('video')?.play();
      });
    });
  }

  function updateBreakOverlayTimer(remainingMs) {
    const el = document.getElementById('sg-break-timer-display');
    if (el) el.textContent = formatTime(remainingMs);
  }

  function hideBreakOverlay() {
    if (breakOverlayElement) {
      breakOverlayElement.remove();
      breakOverlayElement = null;
    }
  }

  function triggerMarathonCompleted() {
    playSoundChime('resume');
    currentMarathon.active = false;
    chrome.storage.local.set({ studyMarathon: { active: false } });

    alert('🎉 CONGRATULATIONS! You completed your Study Marathon lecture! Great work keeping high focus.');
    setupMarathonHud();
  }

  // 8. STORAGE CHANGE LISTENER
  chrome.storage.onChanged.addListener((changes) => {
    if (changes.shieldEnabled) {
      isShieldActive = changes.shieldEnabled.newValue !== false;
      styleEl.disabled = !isShieldActive;
    }
    if (changes.studyMarathon) {
      currentMarathon = changes.studyMarathon.newValue;
      setupMarathonHud();
      enforceNavigationRules();
    }
  });

  // 9. INITIAL LOAD
  chrome.storage.local.get(['shieldEnabled', 'studyMarathon'], (data) => {
    isShieldActive = data.shieldEnabled !== false;
    styleEl.disabled = !isShieldActive;
    currentMarathon = data.studyMarathon;

    enforceNavigationRules();
    renderHomeDashboard();
    setupMarathonHud();
  });

  // 10. RUN CHECKS ON DOM MUTATIONS (YouTube SPA navigation)
  const observer = new MutationObserver(() => {
    enforceNavigationRules();
    renderHomeDashboard();
  });

  observer.observe(document.body || document.documentElement, {
    childList: true,
    subtree: true
  });

  // Listen to popstate for URL history changes
  window.addEventListener('popstate', enforceNavigationRules);
  window.addEventListener('yt-navigate-finish', () => {
    enforceNavigationRules();
    renderHomeDashboard();
    setupMarathonHud();
  });
})();
