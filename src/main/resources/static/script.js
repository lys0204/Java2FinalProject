// Base URL for API
const API_BASE = '/api';

// Global Chart Instances
let trendsChartInstance = null;
let coocChartInstance = null;

// --- Data Collection ---
async function triggerCollection() {
    const btn = document.getElementById('collectBtn');
    const status = document.getElementById('collectionStatus');
    
    btn.disabled = true;
    status.innerText = "Collecting data... (Check backend logs)";
    
    try {
        const response = await fetch(`${API_BASE}/collect`);
        const text = await response.text();
        status.innerText = "Started: " + text;
    } catch (error) {
        status.innerText = "Error: " + error.message;
    } finally {
        setTimeout(() => { btn.disabled = false; }, 5000);
    }
}

// --- Tab Navigation ---
function showTab(tabId) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    
    document.getElementById(tabId).classList.add('active');
    const buttons = document.querySelectorAll('.tab-btn');
    if(tabId === 'trends') buttons[0].classList.add('active');
    if(tabId === 'cooccurrence') buttons[1].classList.add('active');
    if(tabId === 'pitfalls') buttons[2].classList.add('active');
    if(tabId === 'solvability') buttons[3].classList.add('active');

    if(tabId === 'trends') loadTrends();
    if(tabId === 'cooccurrence') loadCooccurrence();
    if(tabId === 'pitfalls') loadPitfalls();
    if(tabId === 'solvability') loadSolvability();
}

// --- Topic Trends ---
async function loadTrends() {
    const tag = document.getElementById('trendTag').value;
    
    console.log(`Fetching trends for ${tag}...`);
    
    const mockLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];
    const mockData = [120, 190, 300, 500, 200, 300];

    const ctx = document.getElementById('trendsChart').getContext('2d');
    
    if (trendsChartInstance) trendsChartInstance.destroy();

    trendsChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: mockLabels,
            datasets: [{
                label: `Activity for tag: ${tag}`,
                data: mockData,
                borderColor: 'rgb(75, 192, 192)',
                tension: 0.1
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

// --- Co-occurrence ---
async function loadCooccurrence() {
    const n = document.getElementById('coocN').value;
    
    const mockLabels = ['java+spring', 'java+swing', 'java+multithreading', 'java+collections'];
    const mockData = [500, 300, 200, 150];

    const ctx = document.getElementById('cooccurrenceChart').getContext('2d');
    
    if (coocChartInstance) coocChartInstance.destroy();

    coocChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: mockLabels,
            datasets: [{
                label: 'Co-occurrence Frequency',
                data: mockData,
                backgroundColor: 'rgba(244, 128, 36, 0.6)',
                borderColor: 'rgba(244, 128, 36, 1)',
                borderWidth: 1
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

// --- Pitfalls ---
async function loadPitfalls() {
    const list = document.getElementById('pitfallsList');
    list.innerHTML = '<p>Loading analysis...</p>';

    const mockPitfalls = [
        "NullPointerException in concurrent maps: 150 occurrences",
        "Deadlock in synchronized blocks: 89 occurrences",
        "Race condition in shared variables: 76 occurrences",
        "Thread starvation: 45 occurrences"
    ];

    let html = '<ul>';
    mockPitfalls.forEach(item => {
        html += `<li class="data-item">${item}</li>`;
    });
    html += '</ul>';
    
    list.innerHTML = html;
}

// --- Solvability ---
async function loadSolvability() {
    console.log("Loading solvability analysis...");
}

// Initial load
document.addEventListener('DOMContentLoaded', () => {
    showTab('trends');
});
