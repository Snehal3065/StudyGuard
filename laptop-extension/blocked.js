const quotes = [
  '"Concentrate all your thoughts upon the work in hand. The sun\'s rays do not burn until brought to a focus." — Alexander Graham Bell',
  '"It is during our darkest moments that we must focus to see the light." — Aristotle',
  '"Discipline is choosing between what you want now and what you want most." — Abraham Lincoln',
  '"Small daily improvements over time lead to stunning results." — Robin Sharma',
  '"You don\'t have to be great to start, but you have to start to be great." — Zig Ziglar'
];

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  const site = params.get('site');
  const reason = params.get('reason');

  const targetBadge = document.getElementById('targetBadge');
  const subMessage = document.getElementById('subMessage');
  const quoteText = document.getElementById('quoteText');
  const btnBack = document.getElementById('btnBack');

  if (reason === 'shorts') {
    targetBadge.textContent = '🛑 YouTube Shorts Blocked';
    subMessage.textContent = 'Short-form algorithmic doomscrolling is disabled! Educational YouTube video lectures and tutorials are permitted.';
  } else if (reason === 'subscriptions') {
    targetBadge.textContent = '🛑 Subscriptions Feed Locked';
    subMessage.textContent = 'Subscription updates and random uploads are locked to prevent rabbit holes. Only your specific lecture or intentional search is permitted.';
  } else if (reason === 'explore' || reason === 'trending') {
    targetBadge.textContent = '🛑 Trending & Explore Blocked';
    subMessage.textContent = 'Algorithmic entertainment feeds are restricted during your study session.';
  } else if (site) {
    targetBadge.textContent = `🛑 ${site} Restricted`;
    subMessage.textContent = `${site} has been locked by StudyGuard to keep you in the flow state.`;
  }

  // Random quote
  const randomQuote = quotes[Math.floor(Math.random() * quotes.length)];
  quoteText.textContent = randomQuote;

  btnBack.addEventListener('click', () => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      window.location.href = 'https://www.youtube.com/';
    }
  });
});
