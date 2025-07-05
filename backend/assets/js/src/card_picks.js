let cardPicksTable = null;
let cardPicksTableOpts = {
    "responsive": true,
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
};

function calculateCardPicksTableData(selectedActs) {
    const cardPicksRawData = JSON.parse(document.getElementById("card-picks-raw-data").innerHTML);
    const combinedData = {};

    selectedActs.forEach(act => {
        const actData = cardPicksRawData[act];
        if (!actData) return;

        Object.entries(actData).forEach(([name, data]) => {
            if (combinedData[name]) {
                combinedData[name].timesPicked += data.yes;
                combinedData[name].timesSkipped += data.no;
                combinedData[name].timesOffered += data.yes + data.no;
            } else {
                combinedData[name] = {
                    name: name,
                    timesPicked: data.yes,
                    timesSkipped: data.no,
                    timesOffered: data.yes + data.no
                };
            }
        });
    });

    return Object.values(combinedData).map(row => {
        row.pickRate = row.timesOffered > 0 ? (100 * row.timesPicked / row.timesOffered) : 0;
        res = [
            row.name,
            row.pickRate,
            row.timesPicked,
            row.timesSkipped,
            row.timesOffered
        ];
        return res;
    });
}

function getCardPicksTableSelectedActs() {
    const selected = [];
    for (let i = 1; i <= 4; i++) {
        if (document.getElementById(`act-${i}`).checked) {
            selected.push(i);
        }
    }
    return selected;
}

function filterCardPicksTableByActs() {
    if (!cardPicksTable) return;

    const selectedActs = getCardPicksTableSelectedActs();
    const newData = calculateCardPicksTableData(selectedActs);

    cardPicksTable.clear();
    cardPicksTable.rows.add(newData);
    cardPicksTable.draw();
}

function initCardPicksTable() {
    const selectedActs = getCardPicksTableSelectedActs();
    const initialData = calculateCardPicksTableData(selectedActs);

    if (cardPicksTable != null) {
        cardPicksTable.destroy();
    }

    cardPicksTable = $('#card-picks-table').DataTable({
        ...cardPicksTableOpts,
        data: initialData
    });
}

document.body.addEventListener("htmx:afterSettle", (e) => {
    if (!document.getElementById('card-picks-table')) {
        if (cardPicksTable) {
            cardPicksTable.destroy();
        }
        return
    }
    if (e.detail.target.id === "player-stats-content") {
        initCardPicksTable();
    }
})
