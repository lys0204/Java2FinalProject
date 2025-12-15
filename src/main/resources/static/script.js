// Base URL for API
const API_BASE = '/api';

// Global Chart Instances
let trendsChartInstance = null;
let coocChartInstance = null;
let solvabilityChartInstance = null;

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
    // Default range: last 12 months
    const end = new Date().toISOString();
    const start = new Date(new Date().setFullYear(new Date().getFullYear() - 1)).toISOString();

    try {
        const response = await fetch(`${API_BASE}/trend?tagName=${tag}&start=${start}&end=${end}`);
        const data = await response.json(); // Map<String, Long>

        const labels = Object.keys(data).sort();
        const values = labels.map(k => data[k]);

        const ctx = document.getElementById('trendsChart').getContext('2d');
        if (trendsChartInstance) trendsChartInstance.destroy();

        trendsChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: `Activity for tag: ${tag}`,
                    data: values,
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1
                }]
            },
            options: { responsive: true, maintainAspectRatio: false }
        });
    } catch (e) {
        console.error("Failed to load trends", e);
    }
}

// --- Co-occurrence ---
async function loadCooccurrence() {
    const n = document.getElementById('coocN').value || 10;
    
    try {
        const response = await fetch(`${API_BASE}/topNpairs?topN=${n}`);
        const data = await response.json(); // List<Entry<String, Integer>>

        const labels = data.map(item => Object.keys(item)[0]); 
        const values = data.map(item => Object.values(item)[0]);

        const ctx = document.getElementById('cooccurrenceChart').getContext('2d');
        if (coocChartInstance) coocChartInstance.destroy();

        coocChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Co-occurrence Frequency',
                    data: values,
                    backgroundColor: 'rgba(244, 128, 36, 0.6)',
                    borderColor: 'rgba(244, 128, 36, 1)',
                    borderWidth: 1
                }]
            },
            options: { responsive: true, maintainAspectRatio: false }
        });
    } catch (e) {
        console.error("Failed to load co-occurrence", e);
    }
}

// --- Pitfalls (Word Cloud) ---
async function loadPitfalls() {
    try {
        const response = await fetch(`${API_BASE}/wordcloud`);
        const data = await response.json(); // Map<String, Long>

        // Transform to [[word, weight], ...]
        const list = Object.entries(data).map(([word, weight]) => [word, weight]);

        const canvas = document.getElementById('pitfallsCanvas');
        // Clear previous
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Check if WordCloud is loaded
        if (typeof WordCloud !== 'undefined') {
            WordCloud(canvas, {
                list: list,
                gridSize: 8,
                weightFactor: function (size) {
                    return Math.pow(size, 0.8) * 2; // Adjust scaling
                },
                fontFamily: 'Times, serif',
                color: 'random-dark',
                rotateRatio: 0.5,
                backgroundColor: '#f0f0f0'
            });
        } else {
            console.error("WordCloud library not loaded");
        }
    } catch (e) {
        console.error("Failed to load word cloud", e);
    }
}

// --- Solvability ---
async function loadSolvability() {
    try {
        const response = await fetch(`${API_BASE}/solvability`);
        const data = await response.json(); 
        // Data format: {"Trendiness": "12.5_10.0", ...}

        const categories = Object.keys(data);
        const solvableValues = [];
        const hardValues = [];

        categories.forEach(cat => {
            const parts = data[cat].split('_');
            solvableValues.push(parseFloat(parts[0]));
            hardValues.push(parseFloat(parts[1]));
        });

        const ctx = document.getElementById('solvabilityChart').getContext('2d');
        if (solvabilityChartInstance) solvabilityChartInstance.destroy();

        solvabilityChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: categories,
                datasets: [
                    {
                        label: 'Solvable Questions',
                        data: solvableValues,
                        backgroundColor: 'rgba(75, 192, 192, 0.6)'
                    },
                    {
                        label: 'Hard Questions',
                        data: hardValues,
                        backgroundColor: 'rgba(255, 99, 132, 0.6)'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true }
                }
            }
        });
    } catch (e) {
        console.error("Failed to load solvability", e);
    }
}

// Initial load
document.addEventListener('DOMContentLoaded', () => {
    // Do not auto-load trends to save API calls on startup, or keep it if desired
    // showTab('trends'); 
});
