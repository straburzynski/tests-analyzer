const ZOOM_MULTIPLIER = 1.5;
const MAX_ZOOM = 100;
const MIN_ZOOM = 0.1;
const DEFAULT_ZOOM = 1.0;
const TOOLTIP_OFFSET_X = 12;
const TOOLTIP_OFFSET_Y = -28;
const TEXT_PADDING_X = 3;
const BAR_VERTICAL_PADDING = 2;
const LABEL_PADDING_X = 4;
const SIDEBAR_COLLAPSED_MARGIN = 10;
const LABEL_CLIP_INSET = 8;
const SEARCHABLE_COLUMNS_COUNT = 3;
const TIME_COLUMN_INDEX = 3;
const BOTTOM_PADDING = 20;

function switchTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tabName}`).classList.add('active');
    document.querySelector(`.tab-btn[onclick*="${tabName}"]`).classList.add('active');
    if (tabName === 'chart') recalcChart();
}

function applyFilters() {
    const search = document.getElementById('searchInput').value.toLowerCase();
    const showSuccess = document.getElementById('cbSuccess').checked;
    const showFailed = document.getElementById('cbFailed').checked;
    const showSkipped = document.getElementById('cbSkipped').checked;
    const rows = document.querySelectorAll('#resultsTable tbody tr');

    rows.forEach(row => {
        const status = row.getAttribute('data-status');
        const statusVisible = (status === 'SUCCESS' && showSuccess) ||
                              (status === 'FAILED' && showFailed) ||
                              (status === 'SKIPPED' && showSkipped);
        let textMatch = true;
        if (search) {
            const cells = row.querySelectorAll('td');
            let text = '';
            for (let i = 0; i < SEARCHABLE_COLUMNS_COUNT; i++) {
                text += cells[i].textContent.toLowerCase() + ' ';
            }
            textMatch = text.includes(search);
        }
        row.classList.toggle('hidden', !(statusVisible && textMatch));
    });
}

const sortDir = {};

function sortTable(col, forceDir = null) {
    const table = document.getElementById('resultsTable');
    const tbody = table.tBodies[0];
    const rows = Array.from(tbody.rows);
    const dir = forceDir || (sortDir[col] === 'asc' ? 'desc' : 'asc');
    sortDir[col] = dir;

    rows.sort((a, b) => {
        let aVal = a.cells[col].textContent.trim();
        let bVal = b.cells[col].textContent.trim();
        if (col === TIME_COLUMN_INDEX) {
            aVal = parseInt(aVal) || 0;
            bVal = parseInt(bVal) || 0;
        }
        const cmp = (typeof aVal === 'number') ? aVal - bVal : aVal.localeCompare(bVal);
        return dir === 'asc' ? cmp : -cmp;
    });

    rows.forEach(row => tbody.appendChild(row));
}

// Sort by time column descending on initial load
sortTable(TIME_COLUMN_INDEX, 'desc');

// Tooltip for chart bars and labels
(() => {
    const tip = document.getElementById('chartTooltip');
    document.querySelectorAll('.chart-bar, .chart-label').forEach(el => {
        el.addEventListener('mouseenter', () => {
            tip.textContent = el.getAttribute('data-tooltip');
            tip.style.display = 'block';
        });
        el.addEventListener('mousemove', (e) => {
            tip.style.left = `${e.clientX + TOOLTIP_OFFSET_X}px`;
            tip.style.top = `${e.clientY + TOOLTIP_OFFSET_Y}px`;
        });
        el.addEventListener('mouseleave', () => {
            tip.style.display = 'none';
        });
    });
})();

// Theme toggle (light/dark)
function toggleTheme() {
    const html = document.documentElement;
    if (html.getAttribute('data-theme') === 'light') {
        html.removeAttribute('data-theme');
        localStorage.setItem('theme', 'dark');
    } else {
        html.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
    }
}

// Restore previously saved theme preference
(() => {
    const saved = localStorage.getItem('theme');
    if (saved === 'light') document.documentElement.setAttribute('data-theme', 'light');
})();

// Zoom and sidebar state
let sidebarVisible = true;
let chartZoomFactor = DEFAULT_ZOOM;

function chartZoomIn() {
    chartZoomFactor = Math.min(chartZoomFactor * ZOOM_MULTIPLIER, MAX_ZOOM);
    recalcChart();
}

function chartZoomOut() {
    chartZoomFactor = Math.max(chartZoomFactor / ZOOM_MULTIPLIER, MIN_ZOOM);
    recalcChart();
}

function chartZoomReset() {
    chartZoomFactor = DEFAULT_ZOOM;
    recalcChart();
}

function toggleChartSidebar() {
    sidebarVisible = !sidebarVisible;
    recalcChart();
}

/**
 * Recalculates the chart layout based on current zoom level and sidebar visibility.
 * Repositions all SVG elements (axis, ticks, bars, labels) to reflect the new dimensions.
 */
function recalcChart() {
    const svg = document.getElementById('ganttSvg');
    const container = svg.parentElement;
    const containerWidth = container.clientWidth;
    const labelWidth = parseInt(svg.getAttribute('data-label-width'));
    const maxEnd = parseFloat(svg.getAttribute('data-max-end'));
    const margin = sidebarVisible ? labelWidth : SIDEBAR_COLLAPSED_MARGIN;

    // Available chart area in pixels, scaled by zoom factor
    const chartAreaPx = (containerWidth - margin) * chartZoomFactor;
    const totalWidth = margin + chartAreaPx;
    // Pixels per millisecond
    const scale = maxEnd > 0 ? chartAreaPx / maxEnd : 1;

    const viewBox = svg.getAttribute('viewBox').split(' ');
    const svgHeight = parseFloat(viewBox[3]);

    // Set SVG dimensions explicitly (no viewBox scaling)
    svg.setAttribute('width', totalWidth);
    svg.setAttribute('height', svgHeight);
    svg.setAttribute('viewBox', `0 0 ${totalWidth} ${svgHeight}`);
    svg.setAttribute('data-base-width', totalWidth);

    // Toggle sidebar/chart labels visibility
    svg.querySelectorAll('.gantt-group-tests foreignObject').forEach(fo => {
        fo.style.display = sidebarVisible ? '' : 'none';
    });
    svg.querySelectorAll('.group-label-sidebar').forEach(fo => {
        fo.style.display = sidebarVisible ? '' : 'none';
    });
    svg.querySelectorAll('.group-label-chart').forEach(fo => {
        fo.setAttribute('x', margin + LABEL_PADDING_X);
        fo.style.display = sidebarVisible ? 'none' : '';
    });

    // Update group header/line widths to span full chart
    svg.querySelectorAll('.group-header-bg').forEach(rect => {
        rect.setAttribute('width', totalWidth);
    });
    svg.querySelectorAll('.group-header-line').forEach(line => {
        line.setAttribute('x2', totalWidth);
    });

    // Recalculate axis position
    svg.querySelectorAll('.chart-axis').forEach(el => {
        el.setAttribute('x1', margin);
        el.setAttribute('x2', totalWidth);
    });

    // Recalculate tick positions from their time value (data-tick-ms)
    svg.querySelectorAll('.chart-tick').forEach(el => {
        const ms = parseFloat(el.getAttribute('data-tick-ms'));
        const x = margin + ms * scale;
        el.setAttribute('x1', x);
        el.setAttribute('x2', x);
    });
    svg.querySelectorAll('.chart-tick-text').forEach(el => {
        const ms = parseFloat(el.getAttribute('data-tick-ms'));
        const x = margin + ms * scale + 2;
        el.setAttribute('x', x);
    });

    // Recalculate bar positions from their start time and duration
    svg.querySelectorAll('.chart-bar').forEach(el => {
        const startMs = parseFloat(el.getAttribute('data-start-ms'));
        const durMs = parseFloat(el.getAttribute('data-duration-ms'));
        const x = margin + startMs * scale;
        const width = Math.max(durMs * scale, 1);
        el.setAttribute('x', x);
        el.setAttribute('width', width);
    });

    // Recalculate bar text label positions
    svg.querySelectorAll('.bar-text-outside, .bar-text-inside').forEach(el => {
        const startMs = parseFloat(el.getAttribute('data-start-ms'));
        const x = margin + startMs * scale + TEXT_PADDING_X;
        el.setAttribute('x', x);
    });

    // Recalculate clip path rectangles for text clipping inside bars
    svg.querySelectorAll('clipPath rect').forEach(el => {
        const startMs = parseFloat(el.getAttribute('data-start-ms'));
        const durMs = parseFloat(el.getAttribute('data-duration-ms'));
        const x = margin + startMs * scale;
        const width = Math.max(durMs * scale, 1);
        el.setAttribute('x', x);
        el.setAttribute('width', width);
    });

    // Update label clip path width based on sidebar state
    const labelClipRect = svg.querySelector('#labelClip rect');
    if (labelClipRect) {
        labelClipRect.setAttribute('width', sidebarVisible ? (labelWidth - LABEL_CLIP_INSET) : 0);
    }
}

// Initialize chart layout on page load and window resize
window.addEventListener('DOMContentLoaded', () => recalcChart());
window.addEventListener('resize', () => recalcChart());

// Collapsible gantt chart groups — handles click-to-collapse and repositions elements
(() => {
    const svg = document.getElementById('ganttSvg');
    const headerHeight = parseInt(svg.getAttribute('data-header-height'));
    const rowHeight = parseInt(svg.getAttribute('data-row-height'));
    const rowGap = parseInt(svg.getAttribute('data-row-gap'));
    const topMargin = parseInt(svg.getAttribute('data-top-margin'));

    document.querySelectorAll('.gantt-group-header').forEach(header => {
        header.addEventListener('click', () => {
            const groupId = header.getAttribute('data-group');
            const tests = document.querySelector(`.gantt-group-tests[data-group="${groupId}"]`);
            const arrow = header.querySelector('.collapse-arrow');
            const isCollapsed = tests.style.display === 'none';
            tests.style.display = isCollapsed ? '' : 'none';
            if (arrow) arrow.classList.toggle('collapsed', !isCollapsed);
            recalcGanttPositions(headerHeight, rowHeight, rowGap, topMargin);
        });
    });

    /**
     * Recalculates vertical positions of all gantt chart elements after collapse/expand.
     * @param {number} groupHeaderHeight - Height of each class group header row
     * @param {number} testRowHeight - Height of each individual test bar row
     * @param {number} gapBetweenRows - Vertical gap between rows
     * @param {number} chartTopMargin - Top margin before first group starts
     */
    function recalcGanttPositions(groupHeaderHeight, testRowHeight, gapBetweenRows, chartTopMargin) {
        const svg = document.querySelector('.chart-container svg');
        let currentY = chartTopMargin;
        const groups = svg.querySelectorAll('.gantt-group-header, .gantt-group-tests');

        groups.forEach(g => {
            if (g.classList.contains('gantt-group-header')) {
                const rect = g.querySelector('rect');
                const line = g.querySelector('line');
                const foreignObjects = g.querySelectorAll('foreignObject');
                if (rect) rect.setAttribute('y', currentY);
                if (line) {
                    line.setAttribute('y1', currentY + groupHeaderHeight);
                    line.setAttribute('y2', currentY + groupHeaderHeight);
                }
                foreignObjects.forEach(fo => fo.setAttribute('y', currentY));
                currentY += groupHeaderHeight + gapBetweenRows;
            } else {
                if (g.style.display === 'none') return;
                const items = g.children;
                let i = 0;
                while (i < items.length) {
                    const el = items[i];
                    const tag = el.tagName.toLowerCase();
                    // Vertical center of the row for text baseline alignment
                    const textCenterY = currentY + testRowHeight / 2 + 5;

                    if (tag === 'foreignobject') {
                        el.setAttribute('y', currentY);
                        i++;
                    } else if (tag === 'rect' && el.classList.contains('chart-bar')) {
                        el.setAttribute('y', currentY + BAR_VERTICAL_PADDING);
                        i++;
                    } else if (tag === 'text') {
                        el.setAttribute('y', textCenterY);
                        i++;
                    } else if (tag === 'clippath') {
                        const clipRect = el.querySelector('rect');
                        if (clipRect) clipRect.setAttribute('y', currentY + BAR_VERTICAL_PADDING);
                        i++;
                        // The text element following a clipPath shares the same row
                        if (i < items.length && items[i].tagName.toLowerCase() === 'text') {
                            items[i].setAttribute('y', textCenterY);
                            i++;
                        }
                        currentY += testRowHeight + gapBetweenRows;
                    } else {
                        i++;
                    }
                }
            }
        });

        svg.setAttribute('viewBox', `0 0 ${svg.getAttribute('data-base-width')} ${currentY + BOTTOM_PADDING}`);
        svg.setAttribute('data-base-height', currentY + BOTTOM_PADDING);
        recalcChart();
    }
})();
