// Persistent file-backed Demo Mode sample store
// Samples are saved to a local JSON file so they survive server restarts
const path = require('path');
const fs = require('fs');

const DATA_FILE = path.join(__dirname, '../data/demo_samples.json');

// Ensure data directory exists
const dataDir = path.join(__dirname, '../data');
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

// Load existing samples from disk on startup
function loadSamples() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const raw = fs.readFileSync(DATA_FILE, 'utf-8');
      const parsed = JSON.parse(raw);
      // Convert createdAt strings back to Date objects
      return parsed.map(s => ({
        ...s,
        createdAt: new Date(s.createdAt)
      }));
    }
  } catch (err) {
    console.warn('[Demo Data] Could not load demo_samples.json, starting fresh:', err.message);
  }
  return [];
}

// Save current samples array to disk
function saveSamples() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(MOCK_SAMPLES, null, 2), 'utf-8');
  } catch (err) {
    console.warn('[Demo Data] Could not persist demo_samples.json:', err.message);
  }
}

// Initialize the in-memory array from disk
const MOCK_SAMPLES = loadSamples();

// Helper that adds a sample AND persists to disk
function addMockSample(sample) {
  MOCK_SAMPLES.unshift(sample);
  saveSamples();
}

// Helper that removes a sample AND persists to disk
function removeMockSample(id) {
  const index = MOCK_SAMPLES.findIndex(s => s._id === id);
  if (index !== -1) {
    MOCK_SAMPLES.splice(index, 1);
    saveSamples();
    return true;
  }
  return false;
}

// Helper that updates a sample AND persists to disk
function updateMockSample(id, updates) {
  const index = MOCK_SAMPLES.findIndex(s => s._id === id);
  if (index !== -1) {
    MOCK_SAMPLES[index] = { ...MOCK_SAMPLES[index], ...updates };
    saveSamples();
    return MOCK_SAMPLES[index];
  }
  return null;
}

module.exports = {
  MOCK_SAMPLES,
  addMockSample,
  removeMockSample,
  updateMockSample
};
