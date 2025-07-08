let neowTable = null;

function initNeowTable() {
    if (neowTable != null) {
        neowTable.destroy();
    }
    neowTable = $('#neow-table').DataTable({
        responsive: true,
        layout: {
            top1: {
                searchBuilder: {
                    columns: [0, 1, 2, 4, 6],
                    conditions: defaultDataTableSearchConditions
                },
            }
        },
        order: [
            [2, 'desc']
        ],
        columns: [
            {type: 'string'},
            {type: 'string'},
            {
                type: 'num',
                render: DataTable.render.number(null, null, 2, null, null)
            },
            {type: 'string', orderable: false},
            {
                type: 'num',
                render: DataTable.render.number(null, null, 2, null, null)
            },
            {type: 'string', orderable: false},
            {type: 'num'}
        ]
    });
}

document.body.addEventListener("htmx:afterSettle", (e) => {
    if (!document.getElementById('neow-table')) {
        if (neowTable != null) {
            neowTable.destroy();
        }
        return
    }
    if (e.detail.target.id === "player-stats-content") {
        initNeowTable();
    }
})
