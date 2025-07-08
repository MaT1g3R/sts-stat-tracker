let cardWinRateTable = null;

function initCardWinRateTable() {
    if (cardWinRateTable != null) {
        cardWinRateTable.destroy();
    }
    cardWinRateTable = $('#card-win-rate-table').DataTable({
        responsive: true,
        layout: {
            top1: {
                searchBuilder: {
                    columns: [0, 1, 4],
                    conditions: defaultDataTableSearchConditions
                }
            }
        },
        order: [
            [1, 'desc']
        ],
        columns: [
            {type: 'string'},
            {
                type: 'num',
                render: DataTable.render.number(null, null, 2, null, null)
            },
            {type: 'num'},
            {type: 'num'},
            {type: 'num'}
        ],
    });
}

document.body.addEventListener("htmx:afterSettle", (e) => {
    if (!document.getElementById('card-win-rate-table')) {
        if (cardWinRateTable != null) {
            cardWinRateTable.destroy();
        }
        return
    }
    if (e.detail.target.id === "player-stats-content") {
        initCardWinRateTable();
    }
})
