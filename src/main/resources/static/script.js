
const API_BASE = '/api';


let trendsChartInstance = null;
let coocChartInstance = null;
let solvabilityChartInstance = null;


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

async function loadTrends() {
    const tag = document.getElementById('trendTag').value;
    const startInput = document.getElementById('trendStart').value;
    const endInput = document.getElementById('trendEnd').value;

    // 如果输入为空，则使用默认值
    const start = startInput || '2008-01';
    const end = endInput || new Date().toISOString().slice(0, 7); // 当前 YYYY-MM

    try {
        // 使用正确的参数名：starttime 和 endtime
        const response = await fetch(`${API_BASE}/trend?tagName=${tag}&starttime=${start}&endtime=${end}`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json(); 
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


async function loadCooccurrence() {
    const n = document.getElementById('coocN').value || 10;
    
    try {
        const response = await fetch(`${API_BASE}/topNpairs?topNStr=${n}`);
        const data = await response.json(); 

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


async function loadPitfalls() {
    try {
        const response = await fetch(`${API_BASE}/wordcloud`);
        const data = await response.json(); 

        const list = Object.entries(data).map(([word, weight]) => [word, weight]);

        const canvas = document.getElementById('pitfallsCanvas');
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        if (typeof WordCloud !== 'undefined') {
            WordCloud(canvas, {
                list: list,
                gridSize: 8,
                weightFactor: function (size) {
                    return Math.pow(size, 0.8) * 2;
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

async function loadSolvability() {
    try {
        const response = await fetch(`${API_BASE}/solvability`);
        const data = await response.json(); 
        const categories = Object.keys(data);

        // 清理之前的图表实例
        if (Array.isArray(solvabilityChartInstance)) {
            solvabilityChartInstance.forEach(chart => chart.destroy());
        } else if (solvabilityChartInstance) {
            solvabilityChartInstance.destroy();
        }
        solvabilityChartInstance = [];

        const container = document.getElementById('solvabilityChartsContainer');
        container.innerHTML = ''; // 清除之前的图表

        categories.forEach(cat => {
            const parts = data[cat].split('_');
            const solvableVal = parseFloat(parts[0]);
            const hardVal = parseFloat(parts[1]);

            // 创建容器和画布
            const wrapper = document.createElement('div');
            wrapper.className = 'chart-wrapper';
            
            // 添加饼图标题
            const title = document.createElement('h3');
            title.innerText = cat;
            title.style.textAlign = 'center';
            wrapper.appendChild(title);

            const canvas = document.createElement('canvas');
            wrapper.appendChild(canvas);
            container.appendChild(wrapper);

            const ctx = canvas.getContext('2d');
            const newChart = new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: ['Solvable', 'Hard'],
                    datasets: [{
                        data: [solvableVal, hardVal],
                        backgroundColor: [
                            'rgba(75, 192, 192, 0.6)', // 绿色代表可解决
                            'rgba(255, 99, 132, 0.6)'  // 红色代表难以解决
                        ],
                        borderColor: [
                            'rgba(75, 192, 192, 1)',
                            'rgba(255, 99, 132, 1)'
                        ],
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const dataset = context.dataset;
                                    const total = context.chart._metasets[context.datasetIndex].total;
                                    
                                    const solvableVal = dataset.data[0];
                                    const hardVal = dataset.data[1];
                                    
                                    const solvablePct = ((solvableVal / total) * 100).toFixed(1) + '%';
                                    const hardPct = ((hardVal / total) * 100).toFixed(1) + '%';

                                    return [
                                        `Solvable: ${solvableVal} (${solvablePct})`,
                                        `Hard: ${hardVal} (${hardPct})`
                                    ];
                                }
                            }
                        }
                    }
                }
            });
            solvabilityChartInstance.push(newChart);
        });

    } catch (e) {
        console.error("Failed to load solvability", e);
    }
}

document.addEventListener('DOMContentLoaded', () => {

});
