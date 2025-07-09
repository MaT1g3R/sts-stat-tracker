let eventWinRateTable = null;

function calculateEventWinRateTableData(selectedActs) {
    const winRatePerAct = JSON.parse(document.getElementById("event-win-rate-data").innerHTML);

    if (selectedActs.length === 0) {
        return [];
    }

    // Combine data from selected acts
    const combinedData = {};

    selectedActs.forEach(act => {
        const actData = winRatePerAct[act];
        if (!actData) return;

        // Process each event in this act
        Object.entries(actData).forEach(([eventName, rate]) => {
            if (!combinedData[eventName]) {
                combinedData[eventName] = {
                    event: eventName,
                    wins: 0,
                    total: 0,
                    timesSeen: 0
                };
            }

            // Add the wins and total from this act
            combinedData[eventName].wins += rate.yes;
            combinedData[eventName].total += (rate.yes + rate.no);
            combinedData[eventName].timesSeen += (rate.yes + rate.no);
        });
    });

    // Calculate combined win rates
    Object.values(combinedData).forEach(row => {
        if (row.total > 0) {
            row.winRate = (row.wins / row.total) * 100;
            row.winLossStr = `(${row.wins}/${row.total})`;
        } else {
            row.winRate = 0;
            row.winLossStr = "(0/0)";
        }
    });

    // Convert to table data format
    return Object.values(combinedData).map(row => [
        row.event,
        row.winRate,
        row.winLossStr,
        row.timesSeen
    ]);
}

function getEventWinRateTableSelectedActs() {
    const ids = ["event-win-rate-act-1", "event-win-rate-act-2", "event-win-rate-act-3"];
    const idToAct = {
        "event-win-rate-act-1": 1,
        "event-win-rate-act-2": 2,
        "event-win-rate-act-3": 3
    };
    const selected = [];
    for (let id of ids) {
        if (document.getElementById(id).checked) {
            selected.push(idToAct[id]);
        }
    }
    return selected;
}

function filterEventWinRateTableByActs() {
    if (!eventWinRateTable) return;

    const selectedActs = getEventWinRateTableSelectedActs();
    const newData = calculateEventWinRateTableData(selectedActs);

    eventWinRateTable.clear();
    eventWinRateTable.rows.add(newData);
    eventWinRateTable.draw();
}

document.filterEventWinRateTableByActs = filterEventWinRateTableByActs;

function initEventWinRateTable() {
    const selectedActs = getEventWinRateTableSelectedActs();
    const initialData = calculateEventWinRateTableData(selectedActs);

    if (eventWinRateTable != null) {
        eventWinRateTable.destroy();
    }

    eventWinRateTable = $('#event-win-rate-table').DataTable({
        "responsive": true,
        layout: {
            top1: {
                searchBuilder: {
                    columns: [0, 1, 3], conditions: defaultDataTableSearchConditions
                }
            }
        },
        order: [[1, 'desc']],
        columns: [
            {type: 'string'},
            {
                type: 'num', render: DataTable.render.number(null, null, 2, null, null)
            },
            {type: 'string', orderable: false},
            {type: 'num'}
        ],
        data: initialData
    });
}

document.body.addEventListener("htmx:afterSettle", (e) => {
    if (!document.getElementById('event-win-rate-table')) {
        if (eventWinRateTable) {
            eventWinRateTable.destroy();
        }
        return;
    }
    if (e.detail.target.id === "player-stats-content") {
        initEventWinRateTable();
    }
});
