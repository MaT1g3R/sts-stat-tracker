let bossRelicsTable = null;

function calculateBossRelicsTableData(selectedActs) {
    const bossRelicsPickData = JSON.parse(document.getElementById("boss-relics-pick-data").innerHTML);
    const bossRelicsWinData = JSON.parse(document.getElementById("boss-relics-win-data").innerHTML);
    const bossRelicKeys = JSON.parse(document.getElementById("boss-relics-keys").innerHTML);

    const combinedData = {};

    if (selectedActs.length === 0) {
        return [];
    }

    bossRelicKeys.forEach((key) => {
        const relicData = {
            name: key, winRate: {yes: 0, no: 0}, pickRate: {yes: 0, no: 0}, seen: 0
        }

        selectedActs.forEach(act => {
            const winRate = bossRelicsWinData[act][key];
            if (winRate) {
                relicData.winRate.yes += winRate.yes;
                relicData.winRate.no += winRate.no;
            }

            if (act === 0) {
                if (winRate) {
                    relicData.seen += winRate.yes + winRate.no;
                }
                return;
            }

            const pickRate = bossRelicsPickData[act][key];
            if (pickRate) {
                relicData.pickRate.yes += pickRate.yes;
                relicData.pickRate.no += pickRate.no;
                relicData.seen += pickRate.yes + pickRate.no;
            }
        })
        combinedData[key] = relicData;
    })

    function rate2Percent(r) {
        const total = r.yes + r.no;
        if (total === 0) {
            return 0;
        }
        return 100 * r.yes / total;
    }

    return Object.values(combinedData).map(row => [
        row.name,
        rate2Percent(row.winRate),
        `(${row.winRate.yes}/${row.winRate.yes + row.winRate.no})`,
        rate2Percent(row.pickRate),
        `(${row.pickRate.yes}/${row.pickRate.yes + row.pickRate.no})`,
        row.seen
    ]);
}

function getBossRelicsTableSelectedActs() {
    const ids = ["boss-relics-act-1", "boss-relics-act-2", "boss-relics-swap"];
    const idToAct = {
        "boss-relics-act-1": 1, "boss-relics-act-2": 2, "boss-relics-swap": 0
    }
    const selected = [];
    for (let id of ids) {
        if (document.getElementById(id).checked) {
            selected.push(idToAct[id]);
        }
    }
    return selected;
}

function filterBossRelicsTableByActs() {
    if (!bossRelicsTable) return;

    const selectedActs = getBossRelicsTableSelectedActs();
    const newData = calculateBossRelicsTableData(selectedActs);

    bossRelicsTable.clear();
    bossRelicsTable.rows.add(newData);
    bossRelicsTable.draw();
}

document.filterBossRelicsTableByActs = filterBossRelicsTableByActs;

function initBossRelicsTable() {
    const selectedActs = getBossRelicsTableSelectedActs();
    const initialData = calculateBossRelicsTableData(selectedActs);

    if (bossRelicsTable != null) {
        bossRelicsTable.destroy();
    }

    bossRelicsTable = $('#boss-relics-table').DataTable({
        "responsive": true,
        layout: {
            top1: {
                searchBuilder: {
                    columns: [0, 1, 3, 5], conditions: defaultDataTableSearchConditions
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
    if (!document.getElementById('boss-relics-table')) {
        if (bossRelicsTable) {
            bossRelicsTable.destroy();
        }
        return
    }
    if (e.detail.target.id === "player-stats-content") {
        initBossRelicsTable();
    }
})
