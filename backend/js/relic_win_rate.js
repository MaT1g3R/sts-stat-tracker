let relicWinRateTable = null;

function getRelicWinRateTableData(isPurchasedMode) {
    const allRelicsData = JSON.parse(document.getElementById("relic-all-rows-data").innerHTML);
    const purchasedRelicsData = JSON.parse(document.getElementById("relic-purchased-rows-data").innerHTML);

    const dataSource = isPurchasedMode ? purchasedRelicsData : allRelicsData;

    return dataSource.map(row => [
        row.relic,
        row.win_rate,
        row.win_loss_str,
        row.times_seen
    ]);
}

function toggleRelicWinRateDisplay() {
    if (!relicWinRateTable) return;

    const isPurchasedMode = document.getElementById("relic-win-rate-toggle").checked;
    const newData = getRelicWinRateTableData(isPurchasedMode);

    relicWinRateTable.clear();
    relicWinRateTable.rows.add(newData);
    relicWinRateTable.draw();
}

document.toggleRelicWinRateDisplay = toggleRelicWinRateDisplay;

function initRelicWinRateTable() {
    const isPurchasedMode = document.getElementById("relic-win-rate-toggle").checked;
    const initialData = getRelicWinRateTableData(isPurchasedMode);

    if (relicWinRateTable != null) {
        relicWinRateTable.destroy();
    }

    relicWinRateTable = $('#relic-win-rate-table').DataTable({
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
    if (!document.getElementById('relic-win-rate-table')) {
        if (relicWinRateTable) {
            relicWinRateTable.destroy();
        }
        return
    }
    if (e.detail.target.id === "player-stats-content") {
        initRelicWinRateTable();
    }
})
